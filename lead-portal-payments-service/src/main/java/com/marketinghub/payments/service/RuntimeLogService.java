package com.marketinghub.payments.service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RuntimeLogService {

    private static final int DEFAULT_LINES = 200;
    private static final int MAX_LINES = 1000;

    private final String loggingFileName;

    public RuntimeLogService(@Value("${logging.file.name:}") String loggingFileName) {
        this.loggingFileName = loggingFileName;
    }

    public RuntimeLogTail readLastLines(int requestedLines) {
        int lines = Math.max(1, Math.min(requestedLines, MAX_LINES));
        Path logPath = resolveLogPath();

        if (logPath == null || !Files.exists(logPath) || !Files.isRegularFile(logPath)) {
            return RuntimeLogTail.missing(lines);
        }

        try {
            String content = tail(logPath, lines);
            return RuntimeLogTail.available(logPath, lines, content);
        } catch (IOException ex) {
            return RuntimeLogTail.error(logPath, lines, ex.getMessage());
        }
    }

    public int defaultLines() {
        return DEFAULT_LINES;
    }

    private Path resolveLogPath() {
        if (!StringUtils.hasText(loggingFileName)) {
            return null;
        }

        Path configured = Paths.get(loggingFileName.trim());
        if (configured.isAbsolute()) {
            return configured;
        }
        return Paths.get("").toAbsolutePath().resolve(configured).normalize();
    }

    private String tail(Path logPath, int lines) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(logPath.toFile(), "r")) {
            long length = file.length();
            if (length == 0) {
                return "";
            }

            long pointer = length - 1;
            int lineBreakCount = 0;

            while (pointer >= 0 && lineBreakCount < lines) {
                file.seek(pointer);
                int currentByte = file.read();
                if (currentByte == '\n' && pointer < length - 1) {
                    lineBreakCount++;
                }
                pointer--;
            }

            long startPosition = Math.max(0, pointer + 1);
            if (startPosition < length) {
                file.seek(startPosition);
                if (file.read() == '\n') {
                    startPosition++;
                }
            }
            file.seek(startPosition);

            byte[] bytes = new byte[(int) (length - startPosition)];
            file.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public record RuntimeLogTail(boolean available, Path path, int lines, String content, String errorMessage) {

        private static RuntimeLogTail available(Path path, int lines, String content) {
            return new RuntimeLogTail(true, path, lines, content, null);
        }

        private static RuntimeLogTail missing(int lines) {
            return new RuntimeLogTail(false, null, lines, "", "Arquivo de log não encontrado");
        }

        private static RuntimeLogTail error(Path path, int lines, String errorMessage) {
            return new RuntimeLogTail(false, path, lines, "", errorMessage);
        }
    }
}
