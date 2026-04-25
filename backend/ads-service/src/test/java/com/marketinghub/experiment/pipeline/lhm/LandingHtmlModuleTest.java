package com.marketinghub.experiment.pipeline.lhm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import org.junit.jupiter.api.Test;

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
}
