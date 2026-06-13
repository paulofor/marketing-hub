package com.marketinghub.oprmcoletormei;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
@SpringBootTest
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

    /** Confirma que os ciclos CNAE automáticos foram removidos do coletor para evitar religamento por configuração. */
    @Test
    void cnaeAutomaticCyclesAreRemovedFromCollector() {
        assertThat(environment.getProperty("oprm.cnae-opportunity.scheduler.enabled")).isNull();
        assertThat(environment.getProperty("oprm.cnae-enrichment.scheduler.enabled")).isNull();
        assertThat(environment.getProperty("oprm.cnae-enrichment.startup-catch-up.enabled")).isNull();
    }

    /** Confirma que as classes antigas de scheduler CNAE não existem mais no artefato do coletor. */
    @Test
    void cnaeSchedulerClassesAreAbsentFromCollectorArtifact() {
        assertThatThrownBy(() -> Class.forName("com.marketinghub.oprmcoletormei.opportunity.service.OprmCnaeOpportunityScheduler"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.marketinghub.oprmcoletormei.opportunity.service.OprmCnaeEnrichmentScheduler"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
