package com.marketinghub.worker.openai.core;

import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.openai.core.port.StageBackendPort;
import com.marketinghub.worker.openai.core.port.StagePromptBuilder;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Guarda arquitetural do pacote com.marketinghub.worker.openai.core.
 *
 * Objetivo:
 * - manter o core genérico livre de dependências das etapas concretas;
 * - impedir que novas etapas quebrem o padrão de ports/adapters;
 * - garantir configuração condicional por etapa;
 * - evitar @Component/@Service soltos nas etapas;
 * - evitar @Value espalhado;
 * - impedir dependência entre etapas concretas;
 * - manter o fluxo evolutivo sob controle.
 */
@AnalyzeClasses(
        packages = "com.marketinghub.worker.openai.core",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArquiteturaCoreTest {

    private static final String BASE_PACKAGE = "com.marketinghub.worker.openai.core";

    private static final Set<String> INTERNAL_CORE_PACKAGES = Set.of(
            "exception",
            "model",
            "openai",
            "port"
    );

    private static final DescribedPredicate<JavaClass> ARE_IN_CORE_ROOT_PACKAGE =
            new DescribedPredicate<>("classes diretamente no pacote raiz do core") {
                @Override
                public boolean test(JavaClass input) {
                    return BASE_PACKAGE.equals(input.getPackageName());
                }
            };

    private static final DescribedPredicate<JavaClass> ARE_IN_CORE_SUPPORT_PACKAGES =
            new DescribedPredicate<>("classes de suporte interno do core: model, port ou exception") {
                @Override
                public boolean test(JavaClass input) {
                    String packageName = input.getPackageName();
                    return packageName.startsWith(BASE_PACKAGE + ".model")
                            || packageName.startsWith(BASE_PACKAGE + ".port")
                            || packageName.startsWith(BASE_PACKAGE + ".exception");
                }
            };

    private static final DescribedPredicate<JavaClass> ARE_IN_STAGE_PACKAGE =
            new DescribedPredicate<>("classes de etapas concretas abaixo de openai.core.<stage>") {
                @Override
                public boolean test(JavaClass input) {
                    return stageNameOf(input) != null;
                }
            };

    private static final DescribedPredicate<JavaClass> ARE_NOT_WORKER_CONFIGURATION =
            new DescribedPredicate<>("classes que não terminam com WorkerConfiguration") {
                @Override
                public boolean test(JavaClass input) {
                    return !input.getSimpleName().endsWith("WorkerConfiguration");
                }
            };

    @ArchTest
    static final ArchRule core_generico_nao_deve_depender_de_frameworks =
            noClasses()
                    .that(ARE_IN_CORE_ROOT_PACKAGE.or(ARE_IN_CORE_SUPPORT_PACKAGES))
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "javax.persistence..",
                            "com.fasterxml.jackson..",
                            "reactor.."
                    )
                    .because("[ARQUITETURA] o core genérico, ports, models e exceptions devem continuar independentes de frameworks");

    @ArchTest
    static final ArchRule core_generico_nao_deve_depender_de_etapas_concretas =
            noClasses()
                    .that(ARE_IN_CORE_ROOT_PACKAGE.or(ARE_IN_CORE_SUPPORT_PACKAGES))
                    .should()
                    .dependOnClassesThat(ARE_IN_STAGE_PACKAGE)
                    .because("[ARQUITETURA] o core deve ser reutilizável e não pode conhecer wireframe, copy, designpreset ou etapas futuras");

    @ArchTest
    static final ArchRule pacote_openai_nao_deve_depender_de_etapas_concretas =
            noClasses()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".openai..")
                    .should()
                    .dependOnClassesThat(ARE_IN_STAGE_PACKAGE)
                    .because("[ARQUITETURA] a integração OpenAI deve servir para todas as etapas, não apenas para uma etapa específica");

    @ArchTest
    static final ArchRule etapas_concretas_nao_devem_depender_de_outras_etapas =
            classes()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .should(notDependOnOtherStagePackages())
                    .because("[ARQUITETURA] cada etapa deve ser plugável e independente das demais");

    @ArchTest
    static final ArchRule pacotes_do_core_nao_devem_ter_ciclos =
            slices()
                    .matching(BASE_PACKAGE + ".(*)..")
                    .should()
                    .beFreeOfCycles();

    @ArchTest
    static final ArchRule backend_client_da_etapa_deve_implementar_stage_backend_port =
            classes()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .and()
                    .haveSimpleNameEndingWith("BackendClient")
                    .should()
                    .beAssignableTo(StageBackendPort.class)
                    .because("[ARQUITETURA] o acesso ao backend deve passar pelo port StageBackendPort");

    @ArchTest
    static final ArchRule prompt_builder_da_etapa_deve_implementar_stage_prompt_builder =
            classes()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .and()
                    .haveSimpleNameEndingWith("PromptBuilder")
                    .should()
                    .beAssignableTo(StagePromptBuilder.class)
                    .because("[ARQUITETURA] a montagem de prompt/request deve passar pelo port StagePromptBuilder");

    @ArchTest
    static final ArchRule response_validator_da_etapa_deve_implementar_stage_response_validator =
            classes()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .and()
                    .haveSimpleNameEndingWith("ResponseValidator")
                    .should()
                    .beAssignableTo(StageResponseValidator.class)
                    .because("[ARQUITETURA] a validação da resposta do modelo deve ser explícita por etapa");

    @ArchTest
    static final ArchRule response_handler_da_etapa_deve_implementar_stage_response_handler =
            classes()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .and()
                    .haveSimpleNameEndingWith("ResponseHandler")
                    .should()
                    .beAssignableTo(StageResponseHandler.class)
                    .because("[ARQUITETURA] hooks de sucesso/falha devem seguir o port StageResponseHandler");

    @ArchTest
    static final ArchRule clients_openai_devem_implementar_openai_client_port =
            classes()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + ".openai..")
                    .and()
                    .haveSimpleNameEndingWith("OpenAiClient")
                    .should()
                    .beAssignableTo(OpenAiClientPort.class)
                    .because("[ARQUITETURA] toda chamada OpenAI deve ser feita por trás do port OpenAiClientPort");

    @ArchTest
    static final ArchRule configuracoes_de_etapa_devem_ser_condicionais =
            classes()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .and()
                    .haveSimpleNameEndingWith("WorkerConfiguration")
                    .should()
                    .beAnnotatedWith(Configuration.class)
                    .andShould()
                    .beAnnotatedWith(EnableConfigurationProperties.class)
                    .andShould()
                    .beAnnotatedWith(ConditionalOnProperty.class)
                    .because("[ARQUITETURA] cada etapa deve subir somente quando <stage>.worker.enabled=true");

    @ArchTest
    static final ArchRule properties_de_etapa_devem_ser_tipadas_e_validadas =
            classes()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .and()
                    .haveSimpleNameEndingWith("WorkerProperties")
                    .should()
                    .beAnnotatedWith(ConfigurationProperties.class)
                    .andShould()
                    .beAnnotatedWith(Validated.class)
                    .because("[ARQUITETURA] configuração da etapa deve ser tipada, validada e centralizada");

    @ArchTest
    static final ArchRule classes_de_etapa_nao_devem_usar_component_service_ou_configuration_fora_da_configuracao =
            noClasses()
                    .that(ARE_IN_STAGE_PACKAGE)
                    .and(ARE_NOT_WORKER_CONFIGURATION)
                    .should()
                    .beAnnotatedWith(Component.class)
                    .orShould()
                    .beAnnotatedWith(Service.class)
                    .orShould()
                    .beAnnotatedWith(Configuration.class)
                    .because("[ARQUITETURA] beans da etapa devem ser controlados pela configuração condicional da etapa");

    @ArchTest
    static final ArchRule nao_usar_value_no_core_openai =
            noFields()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(BASE_PACKAGE + "..")
                    .should()
                    .beAnnotatedWith(Value.class)
                    .because("[ARQUITETURA] configuração deve ser tipada com @ConfigurationProperties, não espalhada com @Value");

    @ArchTest
    static final ArchRule nao_usar_field_injection_no_core_openai =
            noFields()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(BASE_PACKAGE + "..")
                    .should()
                    .beAnnotatedWith(Autowired.class)
                    .because("[ARQUITETURA] usar injeção por construtor ou criação explícita por @Bean");

    @ArchTest
    static final ArchRule scheduled_deve_usar_cron_externalizado =
            methods()
                    .that()
                    .areAnnotatedWith(Scheduled.class)
                    .should(useExternalizedCronExpression())
                    .because("[ARQUITETURA] frequência operacional deve ser configurável por ambiente");

    @ArchTest
    static final ArchRule metodos_publicos_em_worker_configuration_devem_ser_bean =
            methods()
                    .that()
                    .arePublic()
                    .and()
                    .areDeclaredInClassesThat()
                    .haveSimpleNameEndingWith("WorkerConfiguration")
                    .should()
                    .beAnnotatedWith(Bean.class)
                    .because("[ARQUITETURA] a configuração da etapa deve criar beans explicitamente");

    @ArchTest
    static final ArchRule core_openai_nao_deve_depender_de_pacotes_legados_de_worker =
            noClasses()
                    .that()
                    .resideInAPackage(BASE_PACKAGE + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.marketinghub.worker.geralanding.wireframe..",
                            "com.marketinghub.worker.geralanding.copy..",
                            "com.marketinghub.worker.geralanding.designpreset..",
                            "com.marketinghub.worker.geralanding.imageplanning.."
                    )
                    .because("[ARQUITETURA] o novo core OpenAI deve substituir o acoplamento com implementações antigas");

    private static ArchCondition<JavaClass> notDependOnOtherStagePackages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outra etapa concreta") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceStage = stageNameOf(source);

                if (sourceStage == null) {
                    return;
                }

                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetStage = stageNameOf(target);

                    if (targetStage != null && !sourceStage.equals(targetStage)) {
                        String message = "[ARQUITETURA] " + source.getName()
                                + " pertence à etapa '" + sourceStage + "' mas depende da etapa '"
                                + targetStage + "' via: " + dependency.getDescription();

                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> useExternalizedCronExpression() {
        return new ArchCondition<>("[ARQUITETURA] usar cron externalizado por placeholder de propriedade") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Scheduled scheduled = method.getAnnotationOfType(Scheduled.class);
                String cron = scheduled.cron();

                boolean valid = cron != null
                        && cron.contains("${")
                        && cron.contains("}");

                if (!valid) {
                    String message = "[ARQUITETURA] " + method.getFullName()
                            + " usa @Scheduled com cron fixo. Use formato externalizado, por exemplo: "
                            + "@Scheduled(cron = \"${wireframe.worker.cron:0 */5 * * * *}\")";

                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }

    private static String stageNameOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();

        if (!packageName.startsWith(BASE_PACKAGE + ".")) {
            return null;
        }

        String remainder = packageName.substring((BASE_PACKAGE + ".").length());
        String topLevelPackage = remainder.contains(".")
                ? remainder.substring(0, remainder.indexOf('.'))
                : remainder;

        if (INTERNAL_CORE_PACKAGES.contains(topLevelPackage)) {
            return null;
        }

        return topLevelPackage;
    }
}
