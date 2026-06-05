package com.marketinghub.geralanding.publiclanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.publiclanding.service.BackendPublicLandingService;
import com.marketinghub.geralanding.publiclanding.service.approveEndPublish.PublicLandingLeadPortalPublishRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Valida a orquestração da publicação da landing pública do GeraLanding. */
@ExtendWith(MockitoExtension.class)
class BackendPublicLandingServiceTest {
    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private RestTemplate restTemplate;

    /** Deve publicar html_geralanding com tracking, controles de funil, pixel e URL standalone oficial. */
    @Test
    void approveEndPublishShouldPublishGeraLandingHtmlAndPersistStandaloneUrl() {
        BackendPublicLandingService service = new BackendPublicLandingService(
                experimentRepository,
                restTemplate,
                "http://lead-portal");
        MarketNiche niche = new MarketNiche();
        niche.setFacebookPixelId("123456789");
        Experiment experiment = new Experiment();
        experiment.setId(35L);
        experiment.setName("Experimento 35");
        experiment.setNiche(niche);
        experiment.setHtmlGeraLanding("<html><head><title>LP</title></head><body><section id=\"hero\">Oferta</section></body></html>");
        when(experimentRepository.findById(35L)).thenReturn(Optional.of(experiment));

        var response = service.approveEndPublish(35L);

        assertEquals(35L, response.experimentId());
        assertEquals("http://lead-portal/api/public/flows/exp-35-landing-geralanding", response.iframeUrl());
        assertEquals("http://lead-portal/api/flows/exp-35-landing-geralanding/page", response.standaloneUrl());
        assertEquals(response.standaloneUrl(), experiment.getFollowUpActionUrl());
        assertTrue(experiment.getLandingPageHtml().contains("data-mh-funnel-tracking"));
        assertTrue(experiment.getLandingPageHtml().contains("data-mh-funnel-controls"));
        assertTrue(experiment.getLandingPageHtml().contains("data-mh-facebook-pixel"));
        assertTrue(experiment.getLandingPageHtml().contains("data-track-section=\"hero\""));
        verify(experimentRepository).save(experiment);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<PublicLandingLeadPortalPublishRequest>> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).put(uriCaptor.capture(), entityCaptor.capture());
        assertEquals(URI.create("http://lead-portal/api/flows/exp-35-landing-geralanding"), uriCaptor.getValue());
        PublicLandingLeadPortalPublishRequest payload = entityCaptor.getValue().getBody();
        assertNotNull(payload);
        assertEquals("exp-35-landing-geralanding", payload.slug());
        assertEquals("Landing GeraLanding - Experimento 35", payload.name());
        assertTrue(payload.customFormHtml().contains("data-mh-facebook-pixel"));
    }

    /** Deve injetar envio canônico quando a landing possui campos de lead sem contrato de submissão. */
    @Test
    void approveEndPublishShouldInjectLeadSubmissionContractWhenFormControlsExist() {
        BackendPublicLandingService service = new BackendPublicLandingService(
                experimentRepository,
                restTemplate,
                "http://lead-portal");
        Experiment experiment = new Experiment();
        experiment.setId(37L);
        experiment.setName("Experimento 37");
        experiment.setHtmlGeraLanding("""
                <html><head><title>LP</title></head><body>
                <section id="formulario">
                  <input id="input-nome" type="text" name="nome" required>
                  <input id="input-email" type="email" name="email" required>
                  <button id="form-submit" type="button">Receber minha prévia</button>
                </section>
                </body></html>
                """);
        when(experimentRepository.findById(37L)).thenReturn(Optional.of(experiment));

        service.approveEndPublish(37L);

        String finalHtml = experiment.getLandingPageHtml();
        assertTrue(finalHtml.contains("lead-portal-submission-engagement.v1"));
        assertTrue(finalHtml.contains("/api/public/lead-portal/flows/"));
        assertTrue(finalHtml.contains("/submission"));
        assertTrue(finalHtml.contains("contato: {nome: nome, email: email}"));
        assertTrue(finalHtml.contains("button.addEventListener('click', submitLead)"));
        assertTrue(finalHtml.contains("data-track-section=\"formulario\""));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<PublicLandingLeadPortalPublishRequest>> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).put(any(URI.class), entityCaptor.capture());
        PublicLandingLeadPortalPublishRequest payload = entityCaptor.getValue().getBody();
        assertNotNull(payload);
        assertTrue(payload.customFormHtml().contains("lead-portal-submission-engagement.v1"));
    }

    /** Deve bloquear publicação quando o experimento ainda não possui HTML de landing. */
    @Test
    void approveEndPublishShouldThrowConflictWhenLandingHtmlIsMissing() {
        BackendPublicLandingService service = new BackendPublicLandingService(
                experimentRepository,
                restTemplate,
                "http://lead-portal");
        Experiment experiment = new Experiment();
        experiment.setId(88L);
        experiment.setName("Sem HTML");
        when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> service.approveEndPublish(88L));

        assertEquals(409, exception.getStatusCode().value());
        verify(restTemplate, org.mockito.Mockito.never()).put(any(URI.class), any());
    }
}
