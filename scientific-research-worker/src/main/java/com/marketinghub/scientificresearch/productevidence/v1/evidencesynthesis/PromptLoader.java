package com.marketinghub.scientificresearch.productevidence.v1.evidencesynthesis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Carrega prompts e schemas versionados do classpath.
 */
@Component
public class PromptLoader {

    /**
     * Lê um recurso textual obrigatório do classpath.
     */
    public String load(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível carregar recurso: " + path, ex);
        }
    }
}
