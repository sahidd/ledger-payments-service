package com.sahidanwar.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahidanwar.ledger.api.dto.TransferRequest;
import com.sahidanwar.ledger.application.HttpResult;
import com.sahidanwar.ledger.application.TransferService;
import com.sahidanwar.ledger.infrastructure.gateway.SimulatedPaymentGateway;
import com.sahidanwar.ledger.infrastructure.persistence.OutboxRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class LedgerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1"));

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TransferService transferService;
    @Autowired SimulatedPaymentGateway gateway;
    @Autowired OutboxRepository outboxRepository;

    @BeforeEach
    void resetGateway() {
        gateway.resetAttempts();
    }

    @Test
    void postsABalancedTransferOnceAndPublishesBothEvents() throws Exception {
        UUID from = openAccount("Asha", "500.00");
        UUID to = openAccount("Ben", "20.00");

        JsonNode created = transfer(from, to, "25.50", "INR", "rent", "rent-1");
        UUID transferId = UUID.fromString(created.get("id").asText());

        assertThat(created.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(new BigDecimal(created.get("amount").asText())).isEqualByComparingTo("25.50");
        assertThat(created.get("riskFlags").toString()).contains("NEW_BENEFICIARY");
        assertThat(balance(from)).isEqualByComparingTo("474.50");
        assertThat(balance(to)).isEqualByComparingTo("45.50");

        JsonNode replay = transfer(from, to, "25.50", "INR", "rent", "rent-1");
        assertThat(replay.get("id").asText()).isEqualTo(transferId.toString());
        assertThat(balance(from)).isEqualByComparingTo("474.50");

        MvcResult conflict = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "rent-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(from, to, "10.00", "INR", "other")))
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(conflict.getResponse().getContentAsString()).contains("idempotency-conflict");

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var events = outboxRepository.findByAggregateId(transferId);
            assertThat(events).extracting(event -> event.getEventType())
                    .containsExactlyInAnyOrder("PaymentCreated", "PaymentCompleted");
            assertThat(events).allMatch(event -> event.getPublishedAt() != null);
        });
        assertThat(readEvents(transferId)).contains("PaymentCreated", "PaymentCompleted");
        assertThat(reconcile().get("balanced").asBoolean()).isTrue();
    }

    @Test
    void rejectsInsufficientFundsWithoutPostingAJournal() throws Exception {
        UUID from = openAccount("Low", "5.00");
        UUID to = openAccount("High", "0.00");

        MvcResult result = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "too-much")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(from, to, "5.01", "INR", "nope")))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("application/problem+json");
        assertThat(result.getResponse().getContentAsString()).contains("insufficient-funds");
        assertThat(balance(from)).isEqualByComparingTo("5.00");

        JsonNode replay = transferRaw(from, to, "5.01", "INR", "nope", "too-much", 422);
        assertThat(replay.get("type").asText()).contains("insufficient-funds");
        assertThat(balance(from)).isEqualByComparingTo("5.00");
    }

    @Test
    void recordsAGatewayDeclineAndATransientOutage() throws Exception {
        UUID from = openAccount("Payer", "200.00");
        UUID declinedTo = openAccount("Declined", "0.00");
        UUID downTo = openAccount("Down", "0.00");

        JsonNode declined = transfer(from, declinedTo, "15.00", "INR", "FORCE_DECLINE invoice", "decline-1");
        assertThat(declined.get("status").asText()).isEqualTo("FAILED");
        assertThat(declined.get("failureReason").asText()).contains("declined");
        assertThat(balance(from)).isEqualByComparingTo("200.00");

        int before = gateway.attempts();
        JsonNode unavailable = transfer(from, downTo, "15.00", "INR", "FORCE_TRANSIENT invoice", "down-1");
        assertThat(unavailable.get("status").asText()).isEqualTo("FAILED");
        assertThat(unavailable.get("failureReason").asText()).contains("unavailable");
        assertThat(gateway.attempts() - before).isEqualTo(3);
        assertThat(balance(from)).isEqualByComparingTo("200.00");

        UUID declinedId = UUID.fromString(declined.get("id").asText());
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(readEvents(declinedId)).contains("PaymentFailed"));
    }

    @Test
    void flagsUnusualAmountAndExplainsItWithoutAnApiKey() throws Exception {
        UUID from = openAccount("Whale", "5000.00");
        UUID to = openAccount("Shop", "0.00");

        JsonNode created = transfer(from, to, "1000.00", "INR", "equipment", "big-1");
        assertThat(created.get("riskFlags").toString()).contains("UNUSUAL_AMOUNT").contains("NEW_BENEFICIARY");

        MvcResult explanation = mockMvc.perform(get("/api/transactions/" + created.get("id").asText() + "/risk-explanation"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(explanation.getResponse().getContentAsByteArray());
        assertThat(body.get("source").asText()).isEqualTo("fallback");
        assertThat(body.get("explanation").asText()).contains("unusually large");
    }

    @Test
    void flagsVelocityOnTheThirdTransfer() throws Exception {
        UUID from = openAccount("Fast", "100.00");
        UUID to = openAccount("Steady", "0.00");

        transfer(from, to, "1.00", "INR", "one", "vel-1");
        transfer(from, to, "1.00", "INR", "two", "vel-2");
        JsonNode third = transfer(from, to, "1.00", "INR", "three", "vel-3");

        assertThat(third.get("riskFlags").toString()).contains("VELOCITY").doesNotContain("NEW_BENEFICIARY");
    }

    @Test
    void concurrentDebitsCannotOverdrawTheAccount() throws Exception {
        UUID from = openAccount("Contended", "100.00");
        List<UUID> destinations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            destinations.add(openAccount("Dest " + i, "0.00"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch ready = new CountDownLatch(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                UUID destination = destinations.get(i);
                String key = "race-" + i;
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    HttpResult result = transferService.transfer(key, new TransferRequest(
                            from, destination, new BigDecimal("30.00"), "INR", "race"));
                    return result.status();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(3);
            assertThat(statuses.stream().filter(status -> status == 422).count()).isEqualTo(7);
            assertThat(balance(from)).isEqualByComparingTo("10.00");
            assertThat(reconcile().get("balanced").asBoolean()).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    private UUID openAccount(String owner, String balance) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"%s","currency":"INR","initialBalance":%s}
                                """.formatted(owner, balance)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("id").asText());
    }

    private BigDecimal balance(UUID accountId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isOk())
                .andReturn();
        return new BigDecimal(objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("balance").asText());
    }

    private JsonNode transfer(UUID from, UUID to, String amount, String currency, String reference, String key) throws Exception {
        return transferRaw(from, to, amount, currency, reference, key, 201);
    }

    private JsonNode transferRaw(UUID from, UUID to, String amount, String currency, String reference, String key, int status)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(from, to, amount, currency, reference)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String transferJson(UUID from, UUID to, String amount, String currency, String reference) {
        return """
                {"fromAccountId":"%s","toAccountId":"%s","amount":%s,"currency":"%s","reference":"%s"}
                """.formatted(from, to, amount, currency, reference);
    }

    private JsonNode reconcile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/ledger/reconciliation"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.get("issues")).isEmpty();
        return body;
    }

    private String readEvents(UUID transferId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "ledger-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        StringBuilder found = new StringBuilder();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of("payments.events"));
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));
                records.forEach(record -> {
                    if (record.value().contains(transferId.toString())) {
                        found.append(record.value()).append('\n');
                    }
                });
                if (found.indexOf("PaymentCompleted") >= 0 || found.indexOf("PaymentFailed") >= 0) {
                    break;
                }
            }
        }
        return found.toString();
    }
}
