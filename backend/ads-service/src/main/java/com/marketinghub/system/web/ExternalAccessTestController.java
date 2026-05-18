package com.marketinghub.system.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class ExternalAccessTestController {

    @GetMapping("/external-access-test")
    public Map<String, String> externalAccessTest() {
        return Map.of(
                "status", "ok",
                "message", "external access test endpoint is reachable",
                "timestamp", Instant.now().toString());
    }
}
