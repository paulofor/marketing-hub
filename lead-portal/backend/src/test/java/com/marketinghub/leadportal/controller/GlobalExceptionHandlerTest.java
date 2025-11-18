package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.exception.FlowNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundWhenFlowIsMissing() {
        FlowNotFoundException exception = new FlowNotFoundException("missing-flow");

        ResponseEntity<Map<String, String>> response = handler.handleFlowNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals(exception.getMessage(), body.get("error"));
    }
}
