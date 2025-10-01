package com.marketinghub.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
        ResponseStatusException exception,
        HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", exception.getStatusCode().value());
        String error = exception.getStatusCode() instanceof org.springframework.http.HttpStatus httpStatus
            ? httpStatus.getReasonPhrase()
            : exception.getStatusCode().toString();
        body.put("error", error);
        body.put("message", exception.getReason());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }
}
