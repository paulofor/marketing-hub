package com.marketinghub.oprmcoletormei.marketimport.config;

import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderOpenAiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        OprmMarketImportScheduleProperties.class,
        OprmMarketImportCollectorProperties.class,
        NicheResearchSeedBuilderOpenAiProperties.class
})
public class OprmMarketImportConfig {

    @Bean
    RestClient restClient() {
        return RestClient.builder().build();
    }
}
