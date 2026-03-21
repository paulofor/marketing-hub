package com.marketinghub.facebookadsworker.runtimelog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeLogControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnNotFoundWhenEndpointIsDisabled() throws Exception {
        Path logFile = tempDir.resolve("application.log");
        Files.writeString(logFile, "a\nb\n");

        RuntimeLogController controller = new RuntimeLogController(
            new RuntimeLogService(logFile.toString()),
            false
        );

        var response = controller.tail(100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldReturnTailWhenEndpointIsEnabled() throws Exception {
        Path logFile = tempDir.resolve("application.log");
        Files.writeString(logFile, "x\ny\nz\n");

        RuntimeLogController controller = new RuntimeLogController(
            new RuntimeLogService(logFile.toString()),
            true
        );

        var response = controller.tail(2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().available()).isTrue();
        assertThat(response.getBody().lineCount()).isEqualTo(2);
        assertThat(response.getBody().lines()).containsExactly("y", "z");
    }
}
