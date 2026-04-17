package com.marketinghub.salesvideo.service;

import com.marketinghub.product.Product;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.CreateSalesVideoConversionEventRequest;
import com.marketinghub.salesvideo.dto.SalesVideoPerformanceSummaryDto;
import com.marketinghub.salesvideo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SalesVideoCommercialInsightsServiceTest {

    @Mock
    private SalesVideoProfileRepository profileRepository;
    @Mock
    private SalesVideoJobRepository jobRepository;
    @Mock
    private SalesVideoScriptRepository scriptRepository;
    @Mock
    private SalesVideoCommercialPlaybookRepository playbookRepository;
    @Mock
    private SalesVideoConversionEventRepository conversionEventRepository;

    private SalesVideoCommercialInsightsService service;

    @BeforeEach
    void setUp() {
        service = new SalesVideoCommercialInsightsService(
                profileRepository,
                jobRepository,
                scriptRepository,
                playbookRepository,
                conversionEventRepository
        );
    }

    @Test
    void shouldCreateConversionEventBindingScriptFromJob() {
        SalesVideoProfile profile = profile();
        SalesVideoScript script = SalesVideoScript.builder().id(77L).profile(profile).build();
        SalesVideoJob job = SalesVideoJob.builder()
                .id(11L)
                .profile(profile)
                .tenantId("tenant-a")
                .script(script)
                .providerName("video-management-service")
                .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                .jobType(SalesVideoJobType.RENDER)
                .status(SalesVideoStatus.VIDEO_READY)
                .build();
        CreateSalesVideoConversionEventRequest request = new CreateSalesVideoConversionEventRequest();
        request.setJobId(11L);
        request.setEventType(SalesVideoConversionEventType.PURCHASE);
        request.setEventValue(new BigDecimal("197.90"));

        given(profileRepository.findById(7L)).willReturn(Optional.of(profile));
        given(jobRepository.findById(11L)).willReturn(Optional.of(job));
        given(conversionEventRepository.save(any(SalesVideoConversionEvent.class)))
                .willAnswer(invocation -> {
                    SalesVideoConversionEvent event = invocation.getArgument(0);
                    event.setId(900L);
                    return event;
                });

        var response = service.createConversionEvent(7L, request);

        assertThat(response.getId()).isEqualTo(900L);
        assertThat(response.getScriptId()).isEqualTo(77L);
        assertThat(response.getEventType()).isEqualTo(SalesVideoConversionEventType.PURCHASE);
        assertThat(response.getEventValue()).isEqualByComparingTo("197.90");
    }

    @Test
    void shouldSummarizePerformanceByScriptAndProvider() {
        SalesVideoProfile profile = profile();
        SalesVideoScript script = SalesVideoScript.builder().id(3L).profile(profile).build();
        SalesVideoJob job = SalesVideoJob.builder()
                .id(21L)
                .profile(profile)
                .script(script)
                .providerName("provider-real")
                .tenantId("tenant-a")
                .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                .jobType(SalesVideoJobType.RENDER)
                .status(SalesVideoStatus.VIDEO_READY)
                .build();
        SalesVideoConversionEvent lead = SalesVideoConversionEvent.builder()
                .id(1L)
                .profile(profile)
                .job(job)
                .script(script)
                .tenantId("tenant-a")
                .eventType(SalesVideoConversionEventType.LEAD)
                .occurredAt(Instant.parse("2026-04-17T10:00:00Z"))
                .build();
        SalesVideoConversionEvent purchase = SalesVideoConversionEvent.builder()
                .id(2L)
                .profile(profile)
                .job(job)
                .script(script)
                .tenantId("tenant-a")
                .eventType(SalesVideoConversionEventType.PURCHASE)
                .eventValue(new BigDecimal("399.00"))
                .occurredAt(Instant.parse("2026-04-17T10:30:00Z"))
                .build();

        given(profileRepository.findById(7L)).willReturn(Optional.of(profile));
        given(conversionEventRepository.findByProfileIdAndTenantIdOrderByOccurredAtDesc(7L, "tenant-a"))
                .willReturn(List.of(purchase, lead));
        given(playbookRepository.findByProfileIdAndTenantIdOrderByCreatedAtDesc(7L, "tenant-a"))
                .willReturn(List.of());

        SalesVideoPerformanceSummaryDto response = service.summarizePerformance(7L, null, null);

        assertThat(response.getTotalEvents()).isEqualTo(2);
        assertThat(response.getTotalLeads()).isEqualTo(1);
        assertThat(response.getTotalPurchases()).isEqualTo(1);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("399.00");
        assertThat(response.getVariants()).hasSize(1);
        assertThat(response.getVariants().get(0).getScriptId()).isEqualTo(3L);
        assertThat(response.getVariants().get(0).getProviderName()).isEqualTo("provider-real");
    }

    private static SalesVideoProfile profile() {
        return SalesVideoProfile.builder()
                .id(7L)
                .tenantId("tenant-a")
                .title("Avatar Hero")
                .videoKind(SalesVideoKind.HERO)
                .status(SalesVideoStatus.SCRIPT_READY)
                .product(Product.builder().id(99L).build())
                .build();
    }
}
