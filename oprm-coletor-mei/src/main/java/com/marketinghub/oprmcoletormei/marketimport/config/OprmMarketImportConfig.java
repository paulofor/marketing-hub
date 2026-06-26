package com.marketinghub.oprmcoletormei.marketimport.config;

import com.marketinghub.nichocnae.meiaudiencesegmenter.MeiAudienceSegmenterOpenAiProperties;
import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderOpenAiProperties;
import com.marketinghub.nichocnae.sourcesearcher.GoogleCustomSearchProperties;
import com.marketinghub.nichocnaev3.pipeline.personacandidategenerator.PersonaCandidateOpenAiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Centraliza beans e propriedades operacionais usados pelo coletor OPRM. */
@Configuration
@EnableConfigurationProperties({
        OprmMarketImportScheduleProperties.class,
        OprmMarketImportCollectorProperties.class,
        NicheResearchSeedBuilderOpenAiProperties.class,
        MeiAudienceSegmenterOpenAiProperties.class,
        PersonaCandidateOpenAiProperties.class,
        GoogleCustomSearchProperties.class
})
public class OprmMarketImportConfig {

    /** Cria o cliente HTTP compartilhado para integrações externas do coletor. */
    @Bean
    RestClient restClient() {
        return RestClient.builder().build();
    }
}
