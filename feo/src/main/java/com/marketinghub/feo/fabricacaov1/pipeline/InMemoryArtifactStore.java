package com.marketinghub.feo.fabricacaov1.pipeline;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Mantem artefatos em memoria ate que o resultado seja publicado no backend.
 */
@Component
public class InMemoryArtifactStore implements ArtifactStore {

    /**
     * Cria o artefato com hash para rastreabilidade.
     */
    @Override
    public StageArtifact store(String type, String name, String contentType, byte[] content) {
        return new StageArtifact(type, name, contentType, content, sha256(content), Map.of("storage", "backend-callback"));
    }

    /**
     * Calcula SHA-256 do conteudo gerado.
     */
    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }
}
