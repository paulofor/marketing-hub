package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Carrega o JSON Schema versionado da qualificação semântica de fontes. */
@Component
public class SourceSearcherSchemaLoader {
    private static final String SCHEMA_PATH = "prompts/nichocnaev3/source-searcher-schema.json";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper;

    /** Inicializa o carregador com ObjectMapper compartilhado. */
    public SourceSearcherSchemaLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Lê o schema do classpath como mapa para a Responses API. */
    public Map<String, Object> load() {
        try {
            return objectMapper.readValue(new ClassPathResource(SCHEMA_PATH).getInputStream(), MAP_TYPE);
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao carregar schema da etapa source-searcher.", ex);
        }
    }
}
