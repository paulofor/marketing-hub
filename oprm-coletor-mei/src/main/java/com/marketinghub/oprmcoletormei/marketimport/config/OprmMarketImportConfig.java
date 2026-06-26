package com.marketinghub.oprmcoletormei.marketimport.config;

import com.marketinghub.nichocnae.meiaudiencesegmenter.MeiAudienceSegmenterOpenAiProperties;
import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderOpenAiProperties;
import com.marketinghub.nichocnae.sourcesearcher.GoogleCustomSearchProperties;
import com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator.PersonaCandidateOpenAiProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration HTTP_READ_TIMEOUT = Duration.ofMinutes(5);

    /** Cria o cliente HTTP compartilhado para integrações externas do coletor com timeout ampliado. */
    @Bean
    RestClient restClient(ClientHttpRequestFactory requestFactory) {
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    /** Define timeouts HTTP para chamadas externas longas, especialmente OpenAI em modo Flex. */
    @Bean
    ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(HTTP_READ_TIMEOUT);
        return requestFactory;
    }
}
