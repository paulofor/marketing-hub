package com.marketinghub.pipelines.nichocnae.v3.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Garante que o pipeline NichoCNAE versão 3 siga o protocolo padrão módulo no executor OPRM. */
class NichoCnaeV3PipelineArchitectureTest {
    private static final String BASE_PACKAGE = "com.marketinghub.pipelines.nichocnae.v3";
    private static final String CORE_PACKAGE = BASE_PACKAGE + ".core";
    private static final Set<String> SUPPORT_PACKAGES = Set.of("core", "execution", "architecture");
    private static final Set<String> REQUIRED_STAGE_CLASS_SUFFIXES = Set.of(
            "BackendClient",
            "ExecutionScheduler",
            "Input",
            "Output",
            "PromptBuilder",
            "ResponseHandler",
            "ResponseValidator",
            "WorkerConfiguration",
            "WorkerProperties");
    private static final Set<String> FORBIDDEN_CORE_TECH_PACKAGES = Set.of(
            "org.springframework",
            "org.jsoup",
            "com.microsoft.playwright",
            "org.openqa.selenium",
            "software.amazon.awssdk",
            "com.openai",
            "java.net.http");

    private static final DescribedPredicate<JavaClass> ARE_IN_CONCRETE_STAGE =
            new DescribedPredicate<>("[ARQUITETURA] classes de etapas concretas em pipelines.nichocnae.v3.<etapa>") {
                /** Identifica classes de etapa concreta abaixo da versão do pipeline. */
                @Override
                public boolean test(JavaClass input) {
                    return stageNameOf(input) != null;
                }
            };

    /** Valida que o núcleo genérico não conhece nenhuma etapa concreta do pipeline versão 3. */
    @Test
    void pipelineCoreShouldNotDependOnConcreteStages() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that().resideInAPackage(CORE_PACKAGE)
                .should(notDependOnConcreteStages())
                .because("[ARQUITETURA] o núcleo pipelines.nichocnae.v3.core deve conhecer apenas contratos genéricos")
                .check(importedClasses);
    }

    /** Valida que uma etapa concreta da versão 3 não importa outra etapa concreta. */
    @Test
    void concreteStagesShouldNotDependOnOtherConcreteStages() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .should(notDependOnOtherConcreteStages())
                .because("[ARQUITETURA] etapas NichoCNAE v3 devem ser plugáveis e removíveis")
                .check(importedClasses);
    }


    /** Valida que cada etapa concreta da v3 possui somente as 9 classes canônicas do worker. */
    @Test
    void concreteStagePackagesShouldHaveOnlyCanonicalWorkerClasses() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .should(haveOnlyCanonicalWorkerClassesByStagePackage(importedClasses))
                .because("[ARQUITETURA] cada subpacote de etapa NichoCNAE v3 deve ter apenas as 9 classes canônicas do worker")
                .check(importedClasses);
    }

    /** Valida que processors concretos implementam o contrato genérico StageProcessor. */
    @Test
    void concreteProcessorsShouldImplementStageProcessor() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .and().haveSimpleNameEndingWith("Processor")
                .should().beAssignableTo(StageProcessor.class)
                .because("[ARQUITETURA] processors NichoCNAE v3 devem implementar StageProcessor")
                .check(importedClasses);
    }

    /** Valida que o núcleo genérico não depende de tecnologias concretas de execução ou integração. */
    @Test
    void pipelineCoreShouldNotDependOnConcreteTechnologies() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that().resideInAPackage(CORE_PACKAGE)
                .should(notDependOnConcreteTechnologies())
                .because("[ARQUITETURA] tecnologias concretas ficam em etapas, execution ou infraestrutura compartilhada, nunca no núcleo")
                .check(importedClasses);
    }

    /** Valida que os pacotes da v3 não formam ciclos de dependência entre etapas e núcleo. */
    @Test
    void nichoCnaeV3PackagesShouldBeFreeOfCycles() {
        JavaClasses importedClasses = importProductionClasses();

        slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .because("[ARQUITETURA] pacotes NichoCNAE v3 devem permanecer plugáveis e sem ciclos")
                .check(importedClasses);
    }

    /** Importa classes de produção do pipeline versão 3. */
    private JavaClasses importProductionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }


    /** Cria condição explícita que valida o conjunto exato de classes por subpacote de etapa. */
    private ArchCondition<JavaClass> haveOnlyCanonicalWorkerClassesByStagePackage(JavaClasses importedClasses) {
        Map<String, Set<String>> classesByStage = classesByStage(importedClasses);
        return new ArchCondition<>("[ARQUITETURA] ter somente as 9 classes canônicas por etapa NichoCNAE v3") {
            /** Valida se o pacote da etapa possui exatamente os nomes esperados pelo padrão canônico. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String stage = stageNameOf(source);
                if (stage == null) {
                    return;
                }
                Set<String> actualClasses = classesByStage.getOrDefault(stage, Set.of());
                String expectedPrefix = stageClassPrefix(stage);
                Set<String> expectedClasses = new TreeSet<>();
                for (String suffix : REQUIRED_STAGE_CLASS_SUFFIXES) {
                    expectedClasses.add(expectedPrefix + suffix);
                }
                if (!usesCanonicalWorkerClassPattern(actualClasses, expectedPrefix)) {
                    return;
                }
                if (!actualClasses.equals(expectedClasses)) {
                    Set<String> missingClasses = new TreeSet<>(expectedClasses);
                    missingClasses.removeAll(actualClasses);
                    Set<String> extraClasses = new TreeSet<>(actualClasses);
                    extraClasses.removeAll(expectedClasses);
                    events.add(SimpleConditionEvent.violated(source, "[ARQUITETURA] etapa NichoCNAE v3 '" + stage
                            + "' deve conter exatamente 9 classes canônicas " + expectedClasses
                            + ", mas encontrou " + actualClasses
                            + ". Faltando: " + missingClasses + ". Extras: " + extraClasses));
                }
            }
        };
    }

    /** Identifica subpacotes que já adotaram pelo menos uma classe do padrão canônico do worker com o prefixo da etapa. */
    private static boolean usesCanonicalWorkerClassPattern(Set<String> actualClasses, String expectedPrefix) {
        return actualClasses.stream().anyMatch(className -> REQUIRED_STAGE_CLASS_SUFFIXES.stream()
                .map(expectedPrefix::concat)
                .anyMatch(className::equals));
    }

    /** Agrupa classes de produção por subpacote de etapa concreta. */
    private static Map<String, Set<String>> classesByStage(JavaClasses importedClasses) {
        Map<String, Set<String>> classesByStage = new HashMap<>();
        for (JavaClass javaClass : importedClasses) {
            String stage = stageNameOf(javaClass);
            if (stage != null) {
                classesByStage.computeIfAbsent(stage, ignored -> new TreeSet<>()).add(javaClass.getSimpleName());
            }
        }
        return classesByStage;
    }

    /** Converte o nome do pacote da etapa em prefixo PascalCase esperado para as classes. */
    private static String stageClassPrefix(String stage) {
        StringBuilder prefix = new StringBuilder();
        boolean capitalizeNext = true;
        for (char character : stage.toCharArray()) {
            if (character == '-' || character == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                prefix.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else {
                prefix.append(character);
            }
        }
        return prefix.toString();
    }

    /** Cria condição explícita que bloqueia dependência do núcleo para etapa concreta. */
    private ArchCondition<JavaClass> notDependOnConcreteStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de pipelines.nichocnae.v3.<etapa>") {
            /** Verifica dependências diretas originadas no núcleo genérico. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    if (stageNameOf(dependency.getTargetClass()) != null) {
                        events.add(SimpleConditionEvent.violated(source, "[ARQUITETURA] " + source.getName()
                                + " está no núcleo mas depende de " + dependency.getTargetClass().getName()));
                    }
                }
            }
        };
    }

    /** Cria condição explícita que bloqueia dependência cruzada entre etapas concretas. */
    private ArchCondition<JavaClass> notDependOnOtherConcreteStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outra etapa concreta") {
            /** Verifica dependências diretas entre etapas concretas diferentes. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceStage = stageNameOf(source);
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    String targetStage = stageNameOf(dependency.getTargetClass());
                    if (targetStage != null && !targetStage.equals(sourceStage)) {
                        events.add(SimpleConditionEvent.violated(source, "[ARQUITETURA] " + source.getName()
                                + " pertence à etapa " + sourceStage + " mas depende da etapa " + targetStage));
                    }
                }
            }
        };
    }

    /** Cria condição explícita que bloqueia tecnologia concreta dentro do núcleo genérico. */
    private ArchCondition<JavaClass> notDependOnConcreteTechnologies() {
        return new ArchCondition<>("[ARQUITETURA] não depender de tecnologia concreta no núcleo NichoCNAE v3") {
            /** Verifica dependências diretas do núcleo contra pacotes de tecnologia concreta. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    boolean forbidden = FORBIDDEN_CORE_TECH_PACKAGES.stream().anyMatch(targetPackage::startsWith);
                    if (forbidden) {
                        events.add(SimpleConditionEvent.violated(source, "[ARQUITETURA] " + source.getName()
                                + " está no núcleo NichoCNAE v3 mas depende de tecnologia concreta "
                                + dependency.getTargetClass().getName() + " via: " + dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** Extrai a etapa concreta de pacotes no formato pipelines.nichocnae.v3.<etapa>. */
    private static String stageNameOf(JavaClass javaClass) {
        String prefix = BASE_PACKAGE + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        String firstSegment = remainder.contains(".") ? remainder.substring(0, remainder.indexOf('.')) : remainder;
        if (SUPPORT_PACKAGES.contains(firstSegment)) {
            return null;
        }
        return firstSegment;
    }
}
