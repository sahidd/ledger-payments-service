package com.sahidanwar.ledger.api;

import com.sahidanwar.ledger.application.ProblemResponses;
import com.sahidanwar.ledger.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemResponses problems;

    public GlobalExceptionHandler(ProblemResponses problems) {
        this.problems = problems;
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException exception, HttpServletRequest request) {
        return problems.problem(exception, URI.create(request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        detail.setTitle("Validation failed");
        detail.setType(URI.create("https://ledger.sahidanwar.dev/problems/validation"));
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ProblemDetail unreadable(Exception exception, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body or a required header is missing or malformed.");
        detail.setTitle("Malformed request");
        detail.setType(URI.create("https://ledger.sahidanwar.dev/problems/malformed-request"));
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), exception);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error.");
        detail.setTitle("Unexpected error");
        detail.setType(URI.create("https://ledger.sahidanwar.dev/problems/unexpected"));
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
