package com.marketinghub.system;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints mínimos apenas para validar o deploy na VPS.
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemController.class);

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "timestamp", Instant.now().toString()));
    }

    @PostMapping("/restart")
    public ResponseEntity<Map<String, String>> restart() {
        LOGGER.info("Requisição de reinício recebida");
        return ResponseEntity.accepted().body(
            Map.of(
                "message",
                "Reinício agendado. O serviço será reiniciado via systemd após a publicação do novo artefato."
            )
        );
    }
}
