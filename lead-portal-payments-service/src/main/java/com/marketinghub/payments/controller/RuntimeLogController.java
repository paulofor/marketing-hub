package com.marketinghub.payments.controller;

import com.marketinghub.payments.service.RuntimeLogService;
import com.marketinghub.payments.service.RuntimeLogService.RuntimeLogTail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class RuntimeLogController {

    private final RuntimeLogService runtimeLogService;

    public RuntimeLogController(RuntimeLogService runtimeLogService) {
        this.runtimeLogService = runtimeLogService;
    }

    @GetMapping(value = "/runtime", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> runtimeLog(@RequestParam(name = "lines", required = false) Integer lines) {
        int desiredLines = lines != null ? lines : runtimeLogService.defaultLines();
        RuntimeLogTail tail = runtimeLogService.readLastLines(desiredLines);

        if (!tail.available()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(tail.errorMessage());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.TEXT_PLAIN)
                .body(tail.content());
    }
}
