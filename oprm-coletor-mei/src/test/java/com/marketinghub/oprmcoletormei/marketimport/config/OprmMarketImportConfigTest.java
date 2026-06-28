package com.marketinghub.oprmcoletormei.marketimport.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Valida a configuração HTTP operacional compartilhada pelo coletor OPRM. */
class OprmMarketImportConfigTest {

    /** Garante timeout suficiente para chamadas OpenAI em modo Flex sem quedas prematuras. */
    @Test
    void requestFactoryUsesExtendedTimeoutsForLongOpenAiCalls() {
        SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) new OprmMarketImportConfig().requestFactory();
        DirectFieldAccessor accessor = new DirectFieldAccessor(requestFactory);

        assertThat(accessor.getPropertyValue("connectTimeout")).isEqualTo(30_000);
        assertThat(accessor.getPropertyValue("readTimeout")).isEqualTo(300_000);
    }

    /** Garante que a importação antiga CNPJ/CNAE não volte a rodar automaticamente por padrão. */
    @Test
    void marketImportScheduleIsDisabledByDefault() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));

        assertThat(factory.getObject())
                .containsEntry("oprm.market-import.schedule.enabled", "${OPRM_MARKET_IMPORT_SCHEDULE_ENABLED:false}");
    }
}
