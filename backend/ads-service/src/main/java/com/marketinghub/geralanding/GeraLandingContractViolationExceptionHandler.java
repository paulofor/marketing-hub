package com.marketinghub.geralanding;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GeraLandingContractViolationExceptionHandler {

    @ExceptionHandler(GeraLandingContractViolationException.class)
    public ResponseEntity<Map<String, Object>> handle(GeraLandingContractViolationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "GERALANDING_CONTRACT_VIOLATION");
        body.put("message", ex.toString());
        body.put("operation", ex.getOperation());
        body.put("endpoint", ex.getEndpoint());
        body.put("expectedContract", ex.getExpectedContract());
        body.put("receivedPayload", ex.getReceivedPayload());
        body.put("upstreamError", ex.getUpstreamError());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatusCode.valueOf(ex.getHttpStatusCode())).body(body);
    }
}
