package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Valida a seleção das provas visuais reais produzidas para a landing. */
class LandingCreativeReferenceSelectorTest {

    /** Prioriza post, story e legenda concluídos e ignora itens sem arquivo materializado. */
    @Test
    void selectsCompletedProductProofBeforeDecorativeImages() {
        String manifest = """
                {"images":[
                  {"planningItemKey":"hero-decoration","status":"COMPLETED","resolvedUrl":"https://cdn/hero.png"},
                  {"planningItemKey":"real-post-example","sectionName":"Exemplo de post","status":"COMPLETED","resolvedUrl":"https://cdn/post.png"},
                  {"planningItemKey":"real-story-example","prompt":"Story real do produto","status":"READY","webUrl":"https://cdn/story.png"},
                  {"planningItemKey":"caption-example","prompt":"Legenda e resultado","status":"CONCLUIDO","sourceUrl":"https://cdn/caption.png"},
                  {"planningItemKey":"planned-proof","status":"PLANNED","resolvedUrl":"https://cdn/missing.png"}
                ]}
                """;

        var selected = new LandingCreativeReferenceSelector(new ObjectMapper()).select(manifest);

        assertThat(selected).extracting(LandingCreativeReferenceSelector.ReferenceImage::url)
                .containsExactly("https://cdn/post.png", "https://cdn/story.png", "https://cdn/caption.png");
    }

    /** Expõe manifesto inválido como falha funcional em vez de gerar anúncio sem referência. */
    @Test
    void rejectsInvalidManifest() {
        assertThatThrownBy(() -> new LandingCreativeReferenceSelector(new ObjectMapper()).select("{invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manifesto de imagens da landing inválido");
    }
}
