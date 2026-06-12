package com.marketinghub.oprmcoletormei;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderScheduler;
import com.marketinghub.nichocnae.nicheresearchseedbuilder.web.NicheResearchSeedBuilderController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Valida que o contexto Spring do coletor OPRM sobe sem disparar integrações externas durante testes.
 */
@SpringBootTest(properties = "oprm.cnae-enrichment.startup-catch-up.enabled=false")
class OprmColetorMeiApplicationTests {
    @Autowired private ApplicationContext applicationContext;
    @Autowired private Environment environment;

    /** Confirma o carregamento mínimo do ApplicationContext do coletor OPRM. */
    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    /** Confirma que o pacote nichocnae entra no component scan do coletor publicado. */
    @Test
    void contextLoadsNichoCnaePackageBeans() {
        assertThat(applicationContext.getBean(NicheResearchSeedBuilderScheduler.class)).isNotNull();
        assertThat(applicationContext.getBean(NicheResearchSeedBuilderController.class)).isNotNull();
    }

    /** Confirma que a URL operacional padrão do backend usa a porta HTTP publicada para o Codex/coletor. */
    @Test
    void backendBaseUrlDefaultsToOperationalHttpPort() {
        assertThat(environment.getProperty("oprm.market-import.collector.backend-base-url"))
                .isEqualTo("http://191.252.181.168");
    }
}
