package com.marketinghub.journey;

import com.marketinghub.journey.dto.JourneyMetricsResponse;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStatus;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.journey.service.JourneyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JourneyServiceTest {

    @Autowired
    private JourneyService journeyService;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private JourneyTemplateRepository journeyTemplateRepository;

    @Test
    void metricsAggregatesCountsPerStatus() {
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder()
                .name("Onboarding Experience")
                .build());

        journeyRepository.save(Journey.builder()
                .template(template)
                .name("Warm-up")
                .status(JourneyStatus.DRAFT)
                .build());

        journeyRepository.save(Journey.builder()
                .template(template)
                .name("Activation")
                .status(JourneyStatus.ACTIVE)
                .build());

        journeyRepository.save(Journey.builder()
                .template(template)
                .name("Re-engagement")
                .status(JourneyStatus.PAUSED)
                .build());

        JourneyMetricsResponse metrics = journeyService.metrics();

        assertThat(metrics.totalJourneys()).isEqualTo(3);
        assertThat(metrics.statusBreakdown())
                .containsEntry(JourneyStatus.ACTIVE, 1L)
                .containsEntry(JourneyStatus.DRAFT, 1L)
                .containsEntry(JourneyStatus.PAUSED, 1L);
        for (JourneyStatus status : JourneyStatus.values()) {
            assertThat(metrics.statusBreakdown()).containsKey(status);
        }
    }
}
