package com.marketinghub.journey;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.journey.dto.JourneyMetricsResponse;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStatus;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.repository.jpa.journey.JourneyRepository;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
import com.marketinghub.journey.service.JourneyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AdsServiceApplication.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
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

        JourneyMetricsResponse initialMetrics = journeyService.metrics();

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

        assertThat(metrics.totalJourneys()).isEqualTo(initialMetrics.totalJourneys() + 3);
        assertThat(metrics.statusBreakdown())
                .containsEntry(JourneyStatus.ACTIVE, initialMetrics.statusBreakdown().getOrDefault(JourneyStatus.ACTIVE, 0L) + 1)
                .containsEntry(JourneyStatus.DRAFT, initialMetrics.statusBreakdown().getOrDefault(JourneyStatus.DRAFT, 0L) + 1)
                .containsEntry(JourneyStatus.PAUSED, initialMetrics.statusBreakdown().getOrDefault(JourneyStatus.PAUSED, 0L) + 1);
        for (JourneyStatus status : JourneyStatus.values()) {
            assertThat(metrics.statusBreakdown()).containsKey(status);
        }
    }
}
