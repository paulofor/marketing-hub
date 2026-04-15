package com.marketinghub.oprm.web;

import com.marketinghub.oprm.dto.OprmHeartbeatRequestDto;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm/heartbeat")
@Slf4j
public class OprmHeartbeatController {

    @PostMapping
    public ResponseEntity<Void> heartbeat(@Valid @RequestBody OprmHeartbeatRequestDto request) {
        log.info("oprm-heartbeat workerId={} workerVersion={} sentAt={} counters={}",
                request.workerId(), request.workerVersion(), request.sentAt(), request.counters());
        return ResponseEntity.accepted().build();
    }
}

