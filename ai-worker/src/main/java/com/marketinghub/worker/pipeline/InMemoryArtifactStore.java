package com.marketinghub.worker.pipeline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Responsabilidade: manter referências auditáveis em memória para artefatos já persistidos nos contratos do backend. */
public class InMemoryArtifactStore implements ArtifactStore {
    private final Map<String, String> contentByStorageKey = new ConcurrentHashMap<>();

    /** Calcula hash, guarda o conteúdo em memória e retorna uma referência auditável para o ciclo atual. */
    @Override
    public StageArtifact save(String type, String name, String contentType, String content, Map<String, Object> metadata) {
        String safeContent = content == null ? "" : content;
        String sha256 = sha256(safeContent);
        String storageKey = "memory://pipeline/" + type + "/" + sha256 + "/" + name;
        contentByStorageKey.put(storageKey, safeContent);
        return new StageArtifact(type, name, contentType, storageKey, sha256, metadata);
    }

    /** Calcula o SHA-256 textual usado para rastrear igualdade entre artefatos do pipeline. */
    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível para ArtifactStore", ex);
        }
    }
}
