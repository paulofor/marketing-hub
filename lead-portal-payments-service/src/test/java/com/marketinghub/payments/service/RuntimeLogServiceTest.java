package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeLogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnOnlyLastRequestedLines() throws IOException {
        Path logFile = tempDir.resolve("runtime.log");
        Files.writeString(logFile, "line-1\nline-2\nline-3\nline-4\n");

        RuntimeLogService service = new RuntimeLogService(logFile.toString());

        RuntimeLogService.RuntimeLogTail tail = service.readLastLines(2);

        assertThat(tail.available()).isTrue();
        assertThat(tail.content()).isEqualTo("line-3\nline-4\n");
    }

    @Test
    void shouldCapRequestedLinesToMaximum() throws IOException {
        Path logFile = tempDir.resolve("runtime.log");
        Files.writeString(logFile, "a\nb\nc\n");

        RuntimeLogService service = new RuntimeLogService(logFile.toString());

        RuntimeLogService.RuntimeLogTail tail = service.readLastLines(5000);

        assertThat(tail.available()).isTrue();
        assertThat(tail.lines()).isEqualTo(1000);
        assertThat(tail.content()).isEqualTo("a\nb\nc\n");
    }

    @Test
    void shouldReturnMissingWhenLogFileDoesNotExist() {
        RuntimeLogService service = new RuntimeLogService(tempDir.resolve("missing.log").toString());

        RuntimeLogService.RuntimeLogTail tail = service.readLastLines(20);

        assertThat(tail.available()).isFalse();
        assertThat(tail.errorMessage()).isEqualTo("Arquivo de log não encontrado");
    }
}
