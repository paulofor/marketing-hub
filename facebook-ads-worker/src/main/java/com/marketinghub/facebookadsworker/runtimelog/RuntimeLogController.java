package com.marketinghub.facebookadsworker.runtimelog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/public/runtime-logs")
public class RuntimeLogController {
    private final RuntimeLogService runtimeLogService;
    private final boolean publicEndpointEnabled;

    public RuntimeLogController(RuntimeLogService runtimeLogService,
                                @Value("${runtime.logs.public.enabled:false}") boolean publicEndpointEnabled) {
        this.runtimeLogService = runtimeLogService;
        this.publicEndpointEnabled = publicEndpointEnabled;
    }

    @GetMapping("/tail")
    public ResponseEntity<RuntimeLogTailResponse> tail(@RequestParam(name = "lines", defaultValue = "200") int lines) {
        if (!publicEndpointEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        RuntimeLogService.RuntimeLogTail tail = runtimeLogService.readLastLines(lines);
        if (!tail.available()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RuntimeLogTailResponse.error(tail.errorMessage(), tail.logFilePath(), tail.generatedAt()));
        }

        return ResponseEntity.ok(RuntimeLogTailResponse.success(tail.logFilePath(), tail.lines(), tail.generatedAt()));
    }

    public record RuntimeLogTailResponse(boolean available,
                                         String errorMessage,
                                         String logFilePath,
                                         int lineCount,
                                         List<String> lines,
                                         Instant generatedAt) {
        static RuntimeLogTailResponse success(String logFilePath, List<String> lines, Instant generatedAt) {
            return new RuntimeLogTailResponse(true, null, logFilePath, lines.size(), lines, generatedAt);
        }

        static RuntimeLogTailResponse error(String errorMessage, String logFilePath, Instant generatedAt) {
            return new RuntimeLogTailResponse(false, errorMessage, logFilePath, 0, List.of(), generatedAt);
        }
    }
}
