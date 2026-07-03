package com.marketinghub.mois.bibliotecapaginavenda.worker.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageProcessor;
import com.marketinghub.pipelines.salespagepatterns.v1.SalesPagePatternsPipelineDefinition;
import com.marketinghub.pipelines.warmupecosystem.v1.WarmupEcosystemPipelineDefinition;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Garante contratos estruturais de rastreabilidade OpenAI consumidos pelo PromptBuilder. */
@AnalyzeClasses(packages = {"com.marketinghub.mois", "com.marketinghub.pipelines"}, importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String PIPELINE_ROOT = "com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline";
    private static final String DOSSIER_V1_PIPELINE_ROOT = "com.marketinghub.pipelines.dossie.v1";
    private static final String DOSSIE_PRODUTO_V1_PIPELINE_ROOT = "com.marketinghub.pipelines.dossieproduto.v1";
    private static final String SALES_PAGE_PATTERNS_V1_ROOT = "com.marketinghub.pipelines.salespagepatterns.v1";
    private static final String WARMUP_ECOSYSTEM_V1_ROOT = "com.marketinghub.pipelines.warmupecosystem.v1";
    private static final List<String> DOSSIE_PRODUTO_STAGE_SUFFIXES = List.of(
            "BackendClient",
            "ExecutionScheduler",
            "Input",
            "Output",
            "PromptBuilder",
            "ResponseHandler",
            "ResponseValidator",
            "WorkerConfiguration",
            "WorkerProperties");

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

    /** Garante que o núcleo genérico do dossiê MOIS v1 não importe etapas concretas. */
    @ArchTest
    static final ArchRule dossie_v1_pipeline_raiz_nao_deve_depender_de_etapas = classes()
            .that()
            .resideInAPackage(DOSSIER_V1_PIPELINE_ROOT)
            .should(notDependOnConcreteDossierV1Stages())
            .because("[ARQUITETURA] o núcleo do dossiê MOIS v1 não pode conhecer etapas concretas");

    /** Garante independência plugável entre etapas concretas do dossiê MOIS v1. */
    @ArchTest
    static final ArchRule dossie_v1_etapas_nao_devem_depender_umas_das_outras = classes()
            .that(resideInDossierV1ConcreteStage())
            .should(notDependOnAnotherDossierV1ConcreteStage())
            .because("[ARQUITETURA] cada etapa concreta de com.marketinghub.pipelines.dossie.v1.<etapa> deve ser independente");

    /** Garante que as etapas concretas do dossiê MOIS v1 não formem ciclos. */
    @ArchTest
    static final ArchRule dossie_v1_etapas_nao_devem_ter_ciclos = slices()
            .matching("com.marketinghub.pipelines.dossie.v1.(*)..")
            .should()
            .beFreeOfCycles()
            .because("[ARQUITETURA] etapas concretas do dossiê MOIS v1 não podem formar ciclos");

    /** Garante que processors concretos do dossiê MOIS v1 implementem o contrato de etapa. */
    @ArchTest
    static final ArchRule dossie_v1_processors_devem_implementar_stage_processor = classes()
            .that()
            .resideInAPackage("com.marketinghub.pipelines.dossie.v1..")
            .and()
            .resideOutsideOfPackage(DOSSIER_V1_PIPELINE_ROOT)
            .and()
            .haveSimpleNameEndingWith("Processor")
            .should()
            .implement(com.marketinghub.pipelines.dossie.v1.StageProcessor.class)
            .because("[ARQUITETURA] toda etapa concreta do dossiê MOIS v1 deve implementar StageProcessor");

    /** Garante que o núcleo do dossiê MOIS v1 não dependa de tecnologias concretas. */
    @ArchTest
    static final ArchRule dossie_v1_pipeline_raiz_nao_deve_depender_de_tecnologias_concretas = classes()
            .that()
            .resideInAPackage(DOSSIER_V1_PIPELINE_ROOT)
            .should(notDependOnConcreteTechnologies())
            .because("[ARQUITETURA] o núcleo do dossiê MOIS v1 deve depender de abstrações, não de tecnologias concretas");

    /** Garante que os dois pipelines de dossiê da biblioteca existem com nomes versionados e comunicáveis. */
    @ArchTest
    static void dois_pipelines_da_biblioteca_devem_ter_codigos_canonicos(JavaClasses classes) {
        boolean hasSalesPagePatterns = classes.stream()
                .anyMatch(javaClass -> javaClass.getName().equals(SalesPagePatternsPipelineDefinition.class.getName()));
        boolean hasWarmupEcosystem = classes.stream()
                .anyMatch(javaClass -> javaClass.getName().equals(WarmupEcosystemPipelineDefinition.class.getName()));
        if (!hasSalesPagePatterns || !hasWarmupEcosystem) {
            throw new AssertionError("[ARQUITETURA] a biblioteca deve declarar os pipelines salespagepatterns.v1 e warmupecosystem.v1");
        }
        if (!"salespagepatterns.v1".equals(SalesPagePatternsPipelineDefinition.CODE)) {
            throw new AssertionError("[ARQUITETURA] código canônico do pipeline de padrões deve ser salespagepatterns.v1");
        }
        if (!"warmupecosystem.v1".equals(WarmupEcosystemPipelineDefinition.CODE)) {
            throw new AssertionError("[ARQUITETURA] código canônico do pipeline de aquecimento deve ser warmupecosystem.v1");
        }
    }

    /** Garante desacoplamento direto entre os dois pipelines de dossiê da biblioteca. */
    @ArchTest
    static final ArchRule salespagepatterns_nao_deve_depender_de_warmupecosystem = classes()
            .that()
            .resideInAPackage(SALES_PAGE_PATTERNS_V1_ROOT + "..")
            .should(notDependOnPackage(WARMUP_ECOSYSTEM_V1_ROOT))
            .because("[ARQUITETURA] salespagepatterns.v1 deve ser coeso e não depender do pipeline warmupecosystem.v1");

    /** Garante desacoplamento direto entre os dois pipelines de dossiê da biblioteca. */
    @ArchTest
    static final ArchRule warmupecosystem_nao_deve_depender_de_salespagepatterns = classes()
            .that()
            .resideInAPackage(WARMUP_ECOSYSTEM_V1_ROOT + "..")
            .should(notDependOnPackage(SALES_PAGE_PATTERNS_V1_ROOT))
            .because("[ARQUITETURA] warmupecosystem.v1 deve ser coeso e não depender do pipeline salespagepatterns.v1");

    /** Garante que prompts/schemas dos dossiês sejam recebidos do backend, nunca carregados de arquivos locais. */
    @ArchTest
    static final ArchRule pipelines_de_dossie_nao_devem_carregar_prompt_schema_local = classes()
            .that()
            .resideInAnyPackage(DOSSIER_V1_PIPELINE_ROOT + "..", SALES_PAGE_PATTERNS_V1_ROOT + "..")
            .should(notDependOnLocalPromptSchemaLoaders())
            .because("[ARQUITETURA] prompts e schemas dos dossiês devem vir do backend pelo contrato pending");


    /** Garante que cada subpacote de dossieproduto.v1 tenha somente as 9 classes canônicas da etapa. */
    @ArchTest
    static void dossie_produto_v1_subpacotes_devem_ter_apenas_nove_classes_canonicas(JavaClasses classes) {
        Map<String, Set<String>> classesByStagePackage = new LinkedHashMap<>();
        for (JavaClass javaClass : classes) {
            String packageName = javaClass.getPackageName();
            if (isDirectDossieProdutoV1StagePackage(packageName)) {
                classesByStagePackage
                        .computeIfAbsent(packageName, ignored -> new LinkedHashSet<>())
                        .add(javaClass.getSimpleName());
            }
        }

        for (Map.Entry<String, Set<String>> entry : classesByStagePackage.entrySet()) {
            String packageName = entry.getKey();
            Set<String> classNames = entry.getValue();
            Set<String> expectedClassNames = expectedDossieProdutoV1ClassNames(classNames);
            if (!classNames.equals(expectedClassNames)) {
                throw new AssertionError("[ARQUITETURA] " + packageName
                        + " deve conter apenas 9 classes canônicas no formato <nome-etapa>BackendClient, "
                        + "<nome-etapa>ExecutionScheduler, <nome-etapa>Input, <nome-etapa>Output, "
                        + "<nome-etapa>PromptBuilder, <nome-etapa>ResponseHandler, <nome-etapa>ResponseValidator, "
                        + "<nome-etapa>WorkerConfiguration e <nome-etapa>WorkerProperties. Esperado: "
                        + expectedClassNames + ". Encontrado: " + classNames);
            }
        }
    }

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

    private static DescribedPredicate<JavaClass> resideInDossierV1ConcreteStage() {
        return new DescribedPredicate<>("[ARQUITETURA] classes dentro de pipelines.dossie.v1.<etapa>") {
            @Override
            public boolean test(JavaClass javaClass) {
                return concreteDossierV1StageName(javaClass) != null;
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnConcreteDossierV1Stages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de pipelines.dossie.v1.<etapa>") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean valid = concreteDossierV1StageName(target) == null;
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " não pode depender da etapa concreta de dossiê "
                                    + target.getName()));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnAnotherDossierV1ConcreteStage() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outra etapa concreta do dossiê MOIS v1") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String sourceStage = concreteDossierV1StageName(javaClass);
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetStage = concreteDossierV1StageName(target);
                    boolean valid = targetStage == null || targetStage.equals(sourceStage);
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " da etapa " + sourceStage
                                    + " não pode depender de " + target.getName() + " da etapa " + targetStage));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnPackage(String forbiddenPackagePrefix) {
        return new ArchCondition<>("[ARQUITETURA] não depender de " + forbiddenPackagePrefix) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean valid = !target.getPackageName().startsWith(forbiddenPackagePrefix);
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " não pode depender de " + target.getName()));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnLocalPromptSchemaLoaders() {
        return new ArchCondition<>("[ARQUITETURA] não carregar prompt/schema local") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    String targetName = dependency.getTargetClass().getName();
                    boolean forbidden = targetName.equals("org.springframework.core.io.ClassPathResource")
                            || targetName.equals("java.nio.file.Files")
                            || targetName.equals("java.nio.file.Path")
                            || targetName.equals("java.nio.file.Paths");
                    events.add(new SimpleConditionEvent(dependency, !forbidden,
                            "[ARQUITETURA] " + javaClass.getName()
                                    + " não pode carregar prompt/schema local via " + targetName));
                }
            }
        };
    }

    private static String concreteDossierV1StageName(JavaClass javaClass) {
        String prefix = DOSSIER_V1_PIPELINE_ROOT + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String suffix = packageName.substring(prefix.length());
        int dot = suffix.indexOf('.');
        return dot < 0 ? suffix : suffix.substring(0, dot);
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


    private static boolean isDirectDossieProdutoV1StagePackage(String packageName) {
        String prefix = DOSSIE_PRODUTO_V1_PIPELINE_ROOT + ".";
        if (!packageName.startsWith(prefix)) {
            return false;
        }
        String suffix = packageName.substring(prefix.length());
        return !suffix.isBlank() && !suffix.contains(".");
    }

    private static Set<String> expectedDossieProdutoV1ClassNames(Set<String> classNames) {
        String stageClassPrefix = dossieProdutoV1StageClassPrefix(classNames);
        Set<String> expectedClassNames = new LinkedHashSet<>();
        DOSSIE_PRODUTO_STAGE_SUFFIXES.forEach(suffix -> expectedClassNames.add(stageClassPrefix + suffix));
        return expectedClassNames;
    }

    private static String dossieProdutoV1StageClassPrefix(Set<String> classNames) {
        return classNames.stream()
                .map(ArquiteturaTest::dossieProdutoV1StageClassPrefix)
                .filter(prefix -> !prefix.isBlank())
                .findFirst()
                .orElse("<nome-etapa>");
    }

    private static String dossieProdutoV1StageClassPrefix(String className) {
        return DOSSIE_PRODUTO_STAGE_SUFFIXES.stream()
                .filter(className::endsWith)
                .map(suffix -> className.substring(0, className.length() - suffix.length()))
                .findFirst()
                .orElse("");
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
