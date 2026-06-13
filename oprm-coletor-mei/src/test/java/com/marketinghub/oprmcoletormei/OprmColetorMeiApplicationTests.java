package com.marketinghub.oprmcoletormei;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderScheduler;
import com.marketinghub.nichocnae.nicheresearchseedbuilder.web.NicheResearchSeedBuilderController;
import com.marketinghub.oprmcoletormei.opportunity.service.OprmCnaeEnrichmentScheduler;
import com.marketinghub.oprmcoletormei.opportunity.service.OprmCnaeOpportunityScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;

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

    /** Confirma que os ciclos CNAE automáticos ficam desligados por padrão para evitar execuções sem necessidade operacional. */
    @Test
    void cnaeAutomaticCyclesAreDisabledByDefault() {
        assertThat(applicationContext.getBeanNamesForType(OprmCnaeOpportunityScheduler.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(OprmCnaeEnrichmentScheduler.class)).isEmpty();
        assertThat(environment.getProperty("oprm.cnae-opportunity.scheduler.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("oprm.cnae-enrichment.scheduler.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("oprm.cnae-enrichment.startup-catch-up.enabled")).isEqualTo("false");
    }

    /** Confirma que os executores CNAE não possuem gatilhos automáticos por cron ou evento de inicialização. */
    @Test
    void cnaeExecutorsDoNotDeclareAutomaticTriggers() {
        assertThat(OprmCnaeOpportunityScheduler.class.getDeclaredMethods())
                .noneMatch(method -> method.isAnnotationPresent(Scheduled.class));
        assertThat(OprmCnaeEnrichmentScheduler.class.getDeclaredMethods())
                .noneMatch(method -> method.isAnnotationPresent(Scheduled.class))
                .noneMatch(method -> method.isAnnotationPresent(EventListener.class));
    }
}
