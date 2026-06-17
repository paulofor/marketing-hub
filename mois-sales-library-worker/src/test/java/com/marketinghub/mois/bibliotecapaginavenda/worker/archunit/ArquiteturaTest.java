package com.marketinghub.mois.bibliotecapaginavenda.worker.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageProcessor;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Garante contratos estruturais de rastreabilidade OpenAI consumidos pelo PromptBuilder. */
@AnalyzeClasses(packages = "com.marketinghub.mois.bibliotecapaginavenda.worker", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String PIPELINE_ROOT = "com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline";

    private static final DescribedPredicate<JavaClass> CLASSES_DE_ETAPA =
            new DescribedPredicate<>("[ARQUITETURA] classes dentro de pipeline.<etapa>") {
                @Override
                public boolean test(JavaClass javaClass) {
                    String packageName = javaClass.getPackageName();
                    return packageName.startsWith(PIPELINE_ROOT + ".");
                }
            };

    /** Garante que o núcleo genérico do pipeline não importe uma etapa concreta. */
    @ArchTest
    static final ArchRule pacote_pipeline_raiz_nao_deve_depender_de_etapas = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should(notDependOnConcretePipelineStages())
            .because("[ARQUITETURA] o pacote pipeline é núcleo genérico e não pode conhecer etapas concretas");

    /** Garante independência plugável entre etapas concretas do pipeline. */
    @ArchTest
    static final ArchRule etapas_nao_devem_depender_umas_das_outras = classes()
            .that(CLASSES_DE_ETAPA)
            .should(notDependOnAnotherConcreteStage())
            .because("[ARQUITETURA] cada pipeline.<etapa> deve ser independente das outras etapas");

    /** Garante que etapas concretas sejam acíclicas para permitir remoção/substituição segura. */
    @ArchTest
    static final ArchRule etapas_nao_devem_ter_ciclos = slices()
            .matching("com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.(*)..")
            .should()
            .beFreeOfCycles()
            .because("[ARQUITETURA] etapas concretas do pipeline não podem formar ciclos");

    /** Garante entrada de etapas concretas exclusivamente pelo contrato StageProcessor. */
    @ArchTest
    static final ArchRule processors_de_etapa_devem_implementar_stage_processor = classes()
            .that()
            .resideInAPackage("com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline..")
            .and()
            .resideOutsideOfPackage(PIPELINE_ROOT)
            .and()
            .haveSimpleNameEndingWith("Processor")
            .should()
            .implement(StageProcessor.class)
            .because("[ARQUITETURA] toda etapa concreta deve entrar no pipeline através do contrato StageProcessor");

    /** Garante que tecnologias concretas de captura não vazem para o núcleo do pipeline. */
    @ArchTest
    static final ArchRule pipeline_raiz_nao_deve_depender_de_tecnologias_concretas = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should(notDependOnConcreteTechnologies())
            .because("[ARQUITETURA] o núcleo do pipeline deve depender de abstrações, não de tecnologias concretas");

    /** Valida assinatura obrigatória com 4 parâmetros para uso direto do PromptBuilder. */
    @ArchTest
    static final ArchRule should_have_prompt_builder_record_method = classes()
            .that()
            .haveSimpleName("OpenAiPromptResultRecorder")
            .and()
            .resideInAPackage("..mois.bibliotecapaginavenda.worker.v1.service")
            .should(haveRequiredRecordMethod());

    /** Valida assinatura obrigatória com 5 parâmetros para inserção no backend com jobId Marketing Hub. */
    @ArchTest
    static final ArchRule should_have_backend_insert_method = classes()
            .that()
            .haveSimpleName("OpenAiPromptResultRecorder")
            .and()
            .resideInAPackage("..mois.bibliotecapaginavenda.worker.v1.service")
            .should(haveRequiredInsertMethod());

    private static ArchCondition<JavaClass> notDependOnConcretePipelineStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de pipeline.<etapa>") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean valid = !CLASSES_DE_ETAPA.test(target);
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " não pode depender da etapa concreta " + target.getName()));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnAnotherConcreteStage() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outra etapa concreta") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String sourceStage = concreteStageName(javaClass);
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetStage = concreteStageName(target);
                    boolean valid = targetStage == null || targetStage.equals(sourceStage);
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " da etapa " + sourceStage
                                    + " não pode depender de " + target.getName() + " da etapa " + targetStage));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnConcreteTechnologies() {
        return new ArchCondition<>("[ARQUITETURA] não depender de Jsoup, RestClient ou OkHttp") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetPackage = target.getPackageName();
                    boolean concreteTechnology = targetPackage.startsWith("org.jsoup")
                            || targetPackage.startsWith("org.springframework.web.client")
                            || targetPackage.startsWith("okhttp3");
                    events.add(new SimpleConditionEvent(dependency, !concreteTechnology,
                            "[ARQUITETURA] " + javaClass.getName() + " não pode depender da tecnologia concreta " + target.getName()));
                }
            }
        };
    }

    private static String concreteStageName(JavaClass javaClass) {
        String prefix = PIPELINE_ROOT + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String suffix = packageName.substring(prefix.length());
        int dot = suffix.indexOf('.');
        return dot < 0 ? suffix : suffix.substring(0, dot);
    }

    private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass> haveRequiredRecordMethod() {
        return new ArchCondition<>("[ARQUITETURA] have method recordPromptBuilderOpenAiResult(String, String, String, String)") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getMethods().stream()
                        .filter(method -> method.getName().equals("recordPromptBuilderOpenAiResult"))
                        .anyMatch(ArquiteturaTest::hasFourStringParameters);
                events.add(new SimpleConditionEvent(javaClass, hasMethod,
                        "[ARQUITETURA] " + javaClass.getName() + " must declare method recordPromptBuilderOpenAiResult(String, String, String, String)"));
            }
        };
    }

    private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass> haveRequiredInsertMethod() {
        return new ArchCondition<>("[ARQUITETURA] have method insertOpenAiIntegrationRecord(String, String, String, String, Long)") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getMethods().stream()
                        .filter(method -> method.getName().equals("insertOpenAiIntegrationRecord"))
                        .anyMatch(ArquiteturaTest::hasExpectedInsertParameters);
                events.add(new SimpleConditionEvent(javaClass, hasMethod,
                        "[ARQUITETURA] " + javaClass.getName() + " must declare method insertOpenAiIntegrationRecord(String, String, String, String, Long)"));
            }
        };
    }

    private static boolean hasFourStringParameters(JavaMethod method) {
        return method.getRawParameterTypes().size() == 4
                && method.getRawParameterTypes().stream().allMatch(type -> type.getName().equals(String.class.getName()));
    }

    private static boolean hasExpectedInsertParameters(JavaMethod method) {
        return method.getRawParameterTypes().size() == 5
                && method.getRawParameterTypes().get(0).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(1).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(2).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(3).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(4).getName().equals(Long.class.getName());
    }
}
