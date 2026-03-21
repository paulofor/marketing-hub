package com.marketinghub.facebookadsworker.runtimelog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

@Service
public class RuntimeLogService {
    private final Path logFilePath;

    public RuntimeLogService(@Value("${logging.file.name:${LOGGING_FILE_NAME:/var/log/facebook-ads-worker/application.log}}") String logFilePath) {
        this.logFilePath = Path.of(logFilePath);
    }

    public RuntimeLogTail readLastLines(int requestedLines) {
        int linesToRead = Math.max(1, Math.min(requestedLines, 5000));

        if (!Files.exists(logFilePath)) {
            return RuntimeLogTail.unavailable(logFilePath.toString(), "Arquivo de log não encontrado");
        }

        ArrayDeque<String> tail = new ArrayDeque<>(linesToRead);
        try (BufferedReader reader = Files.newBufferedReader(logFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (tail.size() == linesToRead) {
                    tail.removeFirst();
                }
                tail.addLast(line);
            }
        } catch (IOException ex) {
            return RuntimeLogTail.unavailable(logFilePath.toString(), "Erro ao ler arquivo de log: " + ex.getMessage());
        }

        return RuntimeLogTail.available(logFilePath.toString(), List.copyOf(tail), Instant.now());
    }

    public record RuntimeLogTail(String logFilePath,
                                 boolean available,
                                 String errorMessage,
                                 List<String> lines,
                                 Instant generatedAt) {

        static RuntimeLogTail available(String logFilePath, List<String> lines, Instant generatedAt) {
            return new RuntimeLogTail(logFilePath, true, null, lines, generatedAt);
        }

        static RuntimeLogTail unavailable(String logFilePath, String errorMessage) {
            return new RuntimeLogTail(logFilePath, false, errorMessage, List.of(), Instant.now());
        }
    }
}
