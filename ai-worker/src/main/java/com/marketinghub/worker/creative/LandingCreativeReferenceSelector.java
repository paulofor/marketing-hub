package com.marketinghub.worker.creative;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

/** Responsabilidade: selecionar exemplos visuais reais da landing para orientar criativos de anúncio. */
final class LandingCreativeReferenceSelector {
    private static final int MAX_REFERENCES = 3;
    private static final List<String> PRODUCT_PROOF_TERMS = List.of(
            "post", "story", "legenda", "produto", "exemplo", "mockup", "template", "resultado");

    private final ObjectMapper objectMapper;

    /** Inicializa o seletor com o parser usado para ler o manifesto persistido pelo backend. */
    LandingCreativeReferenceSelector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Seleciona até três imagens concluídas, priorizando provas concretas do produto digital. */
    List<ReferenceImage> select(String manifest) {
        if (!StringUtils.hasText(manifest)) {
            return List.of();
        }
        try {
            JsonNode images = objectMapper.readTree(manifest).path("images");
            if (!images.isArray()) {
                return List.of();
            }
            List<ReferenceImage> candidates = new ArrayList<>();
            for (JsonNode image : images) {
                String url = firstText(image, "resolvedUrl", "webUrl", "sourceUrl");
                if (!StringUtils.hasText(url) || !isCompleted(image.path("status").asText())) {
                    continue;
                }
                String label = String.join(" ",
                        image.path("planningItemKey").asText(""),
                        image.path("sectionName").asText(""),
                        image.path("elementId").asText(""),
                        image.path("prompt").asText(""));
                candidates.add(new ReferenceImage(url.trim(), label.trim(), proofScore(label)));
            }
            return candidates.stream()
                    .sorted(Comparator.comparingInt(ReferenceImage::proofScore).reversed())
                    .limit(MAX_REFERENCES)
                    .toList();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Manifesto de imagens da landing inválido", ex);
        }
    }

    /** Seleciona referências aprovadas do kit visual canônico do plano comercial. */
    List<ReferenceImage> selectCommercialKit(String manifest) {
        if (!StringUtils.hasText(manifest)) {
            return List.of();
        }
        try {
            JsonNode assets = objectMapper.readTree(manifest).path("assets");
            if (!assets.isArray()) {
                return List.of();
            }
            List<ReferenceImage> references = new ArrayList<>();
            for (JsonNode asset : assets) {
                String url = asset.path("url").asText(null);
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                String label = String.join(" ", asset.path("label").asText(""), asset.path("purpose").asText(""));
                references.add(new ReferenceImage(url.trim(), label.trim(), proofScore(label) + 10));
            }
            return references.stream()
                    .sorted(Comparator.comparingInt(ReferenceImage::proofScore).reversed())
                    .limit(MAX_REFERENCES)
                    .toList();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Kit visual do plano comercial inválido", ex);
        }
    }

    /** Reconhece estados que comprovam a existência efetiva do arquivo visual. */
    private boolean isCompleted(String status) {
        return "COMPLETED".equalsIgnoreCase(status)
                || "CONCLUIDO".equalsIgnoreCase(status)
                || "READY".equalsIgnoreCase(status);
    }

    /** Pontua referências que representam demonstrações reais do produto em vez de decoração. */
    private int proofScore(String label) {
        String normalized = label == null ? "" : label.toLowerCase(Locale.ROOT);
        return (int) PRODUCT_PROOF_TERMS.stream().filter(normalized::contains).count();
    }

    /** Retorna o primeiro campo textual preenchido da imagem do manifesto. */
    private String firstText(JsonNode image, String... fields) {
        for (String field : fields) {
            String value = image.path(field).asText(null);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /** Referência selecionada com contexto auditável e pontuação de prova do produto. */
    record ReferenceImage(String url, String label, int proofScore) {
    }
}
