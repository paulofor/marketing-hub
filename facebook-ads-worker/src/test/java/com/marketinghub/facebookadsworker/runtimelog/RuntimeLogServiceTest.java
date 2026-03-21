package com.marketinghub.facebookadsworker.runtimelog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeLogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadOnlyTailLines() throws Exception {
        Path logFile = tempDir.resolve("application.log");
        Files.writeString(logFile, "l1\nl2\nl3\nl4\n");
        RuntimeLogService service = new RuntimeLogService(logFile.toString());

        RuntimeLogService.RuntimeLogTail tail = service.readLastLines(2);

        assertThat(tail.available()).isTrue();
        assertThat(tail.lines()).containsExactly("l3", "l4");
        assertThat(tail.errorMessage()).isNull();
    }

    @Test
    void shouldReturnUnavailableWhenFileDoesNotExist() {
        RuntimeLogService service = new RuntimeLogService(tempDir.resolve("missing.log").toString());

        RuntimeLogService.RuntimeLogTail tail = service.readLastLines(50);

        assertThat(tail.available()).isFalse();
        assertThat(tail.lines()).isEmpty();
        assertThat(tail.errorMessage()).isEqualTo("Arquivo de log não encontrado");
    }
}
