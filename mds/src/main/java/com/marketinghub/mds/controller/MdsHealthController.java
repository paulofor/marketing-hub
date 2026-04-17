package com.marketinghub.mds.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/mechanism-discovery")
public class MdsHealthController {
    @GetMapping("/actuator/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "mds");
    }
}
