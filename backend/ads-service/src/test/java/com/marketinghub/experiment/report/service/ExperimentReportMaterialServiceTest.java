package com.marketinghub.experiment.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.creative.Creative;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCaptureDestinationType;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.CreativeVariantRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o material consolidado usado nos relatórios de experimento. */
@ExtendWith(MockitoExtension.class)
class ExperimentReportMaterialServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private CreativeRepository creativeRepository;

    @Mock
    private CreativeVariantRepository creativeVariantRepository;

    @Mock
    private LandingPageRepository landingPageRepository;

    @Mock
    private LeadPortalFlowRepository leadPortalFlowRepository;

    @Mock
    private LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;

    @Mock
    private ExperimentFunnelService experimentFunnelService;

    private ExperimentReportMaterialService service;

    /** Garante que o relatório exponha o destino de captura e metadados do Instant Form. */
    @Test
    void shouldExposeCaptureDestinationAndInstantFormMetadataInMaterial() {
        service = new ExperimentReportMaterialService(
                experimentRepository,
                creativeRepository,
                creativeVariantRepository,
                landingPageRepository,
                leadPortalFlowRepository,
                leadPortalPublicUrlResolver,
                experimentFunnelService,
                new ObjectMapper());
        FacebookInstantForm instantForm = FacebookInstantForm.builder()
                .id(33L)
                .name("Formulário nativo Meta")
                .formId("1234567890")
                .status("ACTIVE")
                .shareLink("https://fb.me/form/1234567890")
                .followUpActionUrl("https://example.com/obrigado")
                .privacyPolicyUrl("https://example.com/privacy")
                .approved(true)
                .published(true)
                .build();
        Experiment experiment = Experiment.builder()
                .id(10L)
                .name("Experimento com Instant Form")
                .captureDestinationType(ExperimentCaptureDestinationType.META_INSTANT_FORM)
                .facebookInstantForm(instantForm)
                .build();

        when(experimentRepository.findById(10L)).thenReturn(Optional.of(experiment));
        when(creativeRepository.findByExperimentId(10L)).thenReturn(List.<Creative>of());
        when(creativeVariantRepository.findByExperimentId(10L)).thenReturn(List.of());
        when(landingPageRepository.findByExperimentId(10L)).thenReturn(List.of());
        when(leadPortalFlowRepository.findAllByExperimentIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(experimentFunnelService.summarize(10L)).thenReturn(List.of());

        ExperimentReportMaterialDto material = service.build(10L);

        assertThat(material.getExperiment().getCaptureDestinationType()).isEqualTo("META_INSTANT_FORM");
        assertThat(material.getInstantForm().getFacebookFormId()).isEqualTo("1234567890");
        assertThat(material.getInstantForm().isApproved()).isTrue();
        assertThat(material.getInstantForm().isPublished()).isTrue();
    }
}
