package com.marketinghub.metaaudience;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

/** Protege o pacote MetaAudience para permanecer como leitura/escrita no backend. */
@AnalyzeClasses(packages = "com.marketinghub")
class MetaAudienceArchitectureTest {
    private static final String META_AUDIENCE_PACKAGE = "com.marketinghub.metaaudience";
    private static final String META_AUDIENCE_EXECUTOR_MODULE = "facebook-ads-worker";

    @ArchTest
    static final ArchRule metaAudienceBackendMustRemainReadWriteOnly = classes()
            .that()
            .resideInAPackage(META_AUDIENCE_PACKAGE + "..")
            .should(notAssumeOperationalExecutionResponsibilityForMetaAudience())
            .because("[ARQUITETURA] [BACKEND][MetaAudience] o backend deve registrar planos CNAE, expor pending e receber "
                    + "resultado; a criação/sincronização operacional de públicos pertence ao módulo "
                    + META_AUDIENCE_EXECUTOR_MODULE);

    /** Garante que o backend MetaAudience não assuma execução operacional do Facebook Ads Worker. */
    private static ArchCondition<JavaClass> notAssumeOperationalExecutionResponsibilityForMetaAudience() {
        List<String> forbiddenNames = List.of("Scheduled", "PipelineWorker", "StageProcessor", "StageContext",
                "StageResult", "StageArtifact", "WebClient", "RestTemplate", "Jsoup", "Playwright", "Selenium");
        return new ArchCondition<>("[ARQUITETURA] manter MetaAudience backend como leitura/escrita") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> forbiddenNames.stream().anyMatch(name ->
                                dependency.getTargetClass().getName().contains(name)))
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(item,
                                "[ARQUITETURA] [BACKEND][MetaAudience] " + item.getName()
                                        + " depende de " + dependency.getTargetClass().getName()
                                        + "; criação de público, polling e integração externa pertencem ao "
                                        + META_AUDIENCE_EXECUTOR_MODULE)));
            }
        };
    }
}
