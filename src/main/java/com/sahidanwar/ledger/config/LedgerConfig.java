package com.sahidanwar.ledger.config;

import com.sahidanwar.ledger.domain.AnomalyDetector;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RiskProperties.class)
public class LedgerConfig {

    @Bean
    AnomalyDetector anomalyDetector(RiskProperties properties) {
        return new AnomalyDetector(
                properties.getUnusualAmount(),
                properties.getVelocityLimit(),
                properties.getAverageMultiplier(),
                properties.getMinimumSample()
        );
    }

    @Bean
    OpenAPI ledgerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ledger Payments API")
                .version("1.0.0")
                .description("Double-entry payments ledger. Balances are decimal money, never floating point."));
    }
}
