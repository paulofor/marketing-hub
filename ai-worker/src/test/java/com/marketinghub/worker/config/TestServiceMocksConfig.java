package com.marketinghub.worker.config;

import com.marketinghub.audience.service.AudienceService;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.deliverable.service.DeliverableService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestServiceMocksConfig {

    @Bean
    @Primary
    public AudienceService audienceServiceMock() {
        return Mockito.mock(AudienceService.class);
    }

    @Bean
    @Primary
    public CreativeService creativeServiceMock() {
        return Mockito.mock(CreativeService.class);
    }

    @Bean
    @Primary
    public DeliverableService deliverableServiceMock() {
        return Mockito.mock(DeliverableService.class);
    }
}
