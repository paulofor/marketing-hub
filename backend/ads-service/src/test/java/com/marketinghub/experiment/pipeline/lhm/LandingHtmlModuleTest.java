package com.marketinghub.experiment.pipeline.lhm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingHtmlModuleTest {

    private final LandingHtmlModule module = new LandingHtmlModule(new ObjectMapper());

    @Test
    void assembleHtmlDocumentIncludesCanonicalAsyncSubmitRuntime() {
        Experiment experiment = new Experiment();
        experiment.setName("Teste LHM");
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "sectionId": "hero",
                        "sectionName": "Hero",
                        "contentType": "form",
                        "surfaceSpec": {
                          "surfaceToken": "surface-hero",
                          "style": "band",
                          "contrastMode": "normal"
                        }
                      }
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "submitTarget": "/api/flows/submissions",
                      "submitLabel": "Enviar",
                      "fields": [
                        {"name": "nome", "type": "text", "required": true},
                        {"name": "email", "type": "email", "required": true}
                      ]
                    }
                  }
                }
                """);
        experiment.setLandingPageCopy("""
                {
                  "landingPageCopy": {
                    "headline": "Headline de teste",
                    "summary": "Resumo de teste"
                  }
                }
                """);

        String html = module.assembleHtmlDocument(experiment);

        assertTrue(html.contains("addEventListener('submit'"));
        assertTrue(html.contains("event.preventDefault()"));
        assertTrue(html.contains("fetch(form.action"));
        assertTrue(html.contains("new FormData(form)"));
        assertTrue(html.contains("submitButton.disabled = true"));
        assertTrue(html.contains("submitButton.disabled = false"));
        assertTrue(html.toLowerCase().contains("checkvalidity"));
        assertTrue(html.toLowerCase().contains("reportvalidity"));
        assertTrue(html.toLowerCase().contains("success"));
    }

    @Test
    void assembleHtmlDocumentKeepsExactSurfaceContractFromWireframe() {
        Experiment experiment = new Experiment();
        experiment.setName("Teste LHM Surface");
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "sectionId": "benefits",
                        "sectionName": "Benefícios",
                        "contentType": "split",
                        "surfaceSpec": {
                          "surfaceToken": "surface-benefits",
                          "style": "solid",
                          "contrastMode": "high"
                        }
                      }
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "submitTarget": "/api/flows/submissions",
                      "submitLabel": "Enviar",
                      "fields": [
                        {"name": "nome", "type": "text", "required": true}
                      ]
                    }
                  }
                }
                """);

        String html = module.assembleHtmlDocument(experiment);

        assertTrue(html.contains("data-section-id=\"benefits\""));
        assertTrue(html.contains("class=\"card surface-solid contrast-high\""));
        assertTrue(html.contains("data-surface-token=\"surface-benefits\""));
        assertTrue(html.contains("data-surface-style=\"solid\""));
        assertTrue(html.contains("data-surface-contrast=\"high\""));
        assertFalse(html.contains("data-section-id=\"hero\""));
        assertFalse(html.contains("data-section-id=\"form\""));
    }

    @Test
    void assembleHtmlDocumentRendersBodyFaqAndCtaFromLandingCopy() {
        Experiment experiment = new Experiment();
        experiment.setName("Teste LHM Copy Completa");
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {"sectionId": "hero", "sectionName": "Hero", "contentType": "split", "surfaceSpec": {"surfaceToken": "surface-hero", "style": "band", "contrastMode": "high"}},
                      {"sectionId": "mechanism_01", "sectionName": "Mecanismo", "contentType": "split", "surfaceSpec": {"surfaceToken": "surface-mech", "style": "solid", "contrastMode": "normal"}},
                      {"sectionId": "faq_01", "sectionName": "FAQ", "contentType": "faq", "surfaceSpec": {"surfaceToken": "surface-faq", "style": "band", "contrastMode": "soft"}},
                      {"sectionId": "cta_01_final", "sectionName": "CTA Final", "contentType": "cta", "surfaceSpec": {"surfaceToken": "surface-cta", "style": "solid", "contrastMode": "high"}}
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "submitTarget": "/api/flows/submissions",
                      "submitLabel": "Enviar",
                      "fields": [{"name": "nome", "type": "text", "required": true}]
                    }
                  }
                }
                """);
        experiment.setLandingPageCopy("""
                {
                  "landingPageCopy": {
                    "hero": {
                      "promise": "Promessa principal",
                      "supportingCopy": "Resumo do valor",
                      "ctaLabel": "Quero agora",
                      "ctaUrl": "https://example.com/cta"
                    },
                    "bodySections": [
                      {
                        "sectionId": "mechanism_01",
                        "summary": "Resumo mecanismo",
                        "copy": "Copy mecanismo",
                        "bullets": ["Passo 1", "Passo 2", "Passo 3"],
                        "ctaSupport": "Apoio do CTA"
                      }
                    ],
                    "faq": [
                      {"question": "Dúvida 1", "answer": "Resposta 1"}
                    ],
                    "ctaBlocks": [
                      {"placement": "final", "ctaVariant": "final", "ctaLabel": "Final CTA", "ctaUrl": "https://example.com/final", "ctaSupport": "Suporte final"}
                    ]
                  }
                }
                """);

        String html = module.assembleHtmlDocument(experiment);

        assertTrue(html.contains("Promessa principal"));
        assertTrue(html.contains("Resumo mecanismo"));
        assertTrue(html.contains("Passo 1"));
        assertTrue(html.contains("<details><summary>Dúvida 1"));
        assertTrue(html.contains("Final CTA"));
    }

    @Test
    void assembleHtmlDocumentUsesDesignPresetForStyleAndContrastMode() {
        Experiment experiment = new Experiment();
        experiment.setName("Teste LHM Surface Preset");
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "sectionId": "proof",
                        "sectionName": "Prova",
                        "contentType": "proof",
                        "surfaceSpec": {
                          "surfaceToken": "surface-proof",
                          "style": "band",
                          "contrastMode": "soft"
                        }
                      }
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "submitTarget": "/api/flows/submissions",
                      "submitLabel": "Enviar",
                      "fields": [{"name": "nome", "type": "text", "required": true}]
                    }
                  }
                }
                """);
        experiment.setLandingPageDesignPreset("""
                {
                  "landingPageDesignPreset": {
                    "sectionPresets": [
                      {
                        "sectionId": "proof",
                        "surfaceStyle": "solid",
                        "contrastMode": "high"
                      }
                    ]
                  }
                }
                """);

        String html = module.assembleHtmlDocument(experiment);

        assertTrue(html.contains("data-section-id=\"proof\""));
        assertTrue(html.contains("class=\"card surface-solid contrast-high\""));
        assertTrue(html.contains("data-surface-token=\"surface-proof\""));
        assertTrue(html.contains("data-surface-style=\"solid\""));
        assertTrue(html.contains("data-surface-contrast=\"high\""));
        assertFalse(html.contains("data-surface-style=\"band\""));
        assertFalse(html.contains("data-surface-contrast=\"soft\""));
    }

    @Test
    void assembleHtmlDocumentUsesHeroSectionInsteadOfFirstWireframeSectionForH1() {
        Experiment experiment = new Experiment();
        experiment.setName("Teste LHM Hero");
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {"sectionId": "nav-identity", "sectionName": "Topo", "contentType": "hero", "surfaceSpec": {"surfaceToken": "surface-nav", "style": "band", "contrastMode": "normal"}},
                      {"sectionId": "hero-split-form", "sectionName": "Hero", "contentType": "split", "surfaceSpec": {"surfaceToken": "surface-hero", "style": "solid", "contrastMode": "high"}}
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "submitTarget": "/api/flows/submissions",
                      "submitLabel": "Enviar",
                      "fields": [{"name": "nome", "type": "text", "required": true}]
                    }
                  }
                }
                """);
        experiment.setLandingPageCopy("""
                {
                  "landingPageCopy": {
                    "hero": {
                      "headline": "Headline principal bem mais curta",
                      "supportingCopy": "Resumo de apoio"
                    }
                  }
                }
                """);

        String html = module.assembleHtmlDocument(experiment);

        assertTrue(html.contains("data-section-id=\"hero-split-form\""));
        assertTrue(html.contains("<h1>Headline principal bem mais curta</h1>"));
        assertTrue(html.contains("data-section-id=\"nav-identity\""));
        assertTrue(html.contains("<h2>Topo</h2>"));
    }

    @Test
    void assembleHtmlDocumentAvoidsRepeatingPromiseWhenDuplicateOfHeroHeadline() {
        Experiment experiment = new Experiment();
        experiment.setName("Teste LHM Dedup");
        experiment.setLandingPageWireframe("""
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {"sectionId": "hero", "sectionName": "Hero", "contentType": "split", "surfaceSpec": {"surfaceToken": "surface-hero", "style": "band", "contrastMode": "normal"}}
                    ],
                    "formSpec": {
                      "formId": "lead-capture-primary",
                      "submitTarget": "/api/flows/submissions",
                      "submitLabel": "Enviar",
                      "fields": [{"name": "nome", "type": "text", "required": true}]
                    }
                  }
                }
                """);
        experiment.setLandingPageCopy("""
                {
                  "landingPageCopy": {
                    "hero": {
                      "headline": "Mesma mensagem da promessa",
                      "promise": "Mesma mensagem da promessa",
                      "supportingCopy": "Apoio adicional"
                    }
                  }
                }
                """);

        String html = module.assembleHtmlDocument(experiment);

        assertTrue(html.contains("<h1>Mesma mensagem da promessa</h1>"));
        assertFalse(html.contains("<p class=\"section-objective\">Mesma mensagem da promessa</p>"));
    }
}
