package com.marketinghub.nichocnae.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

/**
 * Garante por ArchUnit que o núcleo genérico do pipeline nichocnae fica desacoplado e que etapas concretas
 * permanecem plugáveis sem dependência cruzada.
 */
class NichoCnaePipelineArchitectureTest {
    private static final String BASE_PACKAGE = "com.marketinghub.nichocnae";
    private static final String V2_PACKAGE = "com.marketinghub.nichocnaev2";
    private static final String PIPELINE_PACKAGE = BASE_PACKAGE + ".pipeline";

    private static final DescribedPredicate<JavaClass> ARE_IN_CONCRETE_STAGE =
            new DescribedPredicate<>("[ARQUITETURA] classes de etapas concretas abaixo de nichocnae.<etapa>") {
                /** Identifica se a classe pertence a uma etapa concreta fora do núcleo pipeline. */
                @Override
                public boolean test(JavaClass input) {
                    return stageNameOf(input) != null;
                }
            };

    /** Valida o isolamento do pacote raiz pipeline conforme o padrão canônico de etapas. */
    @Test
    void pipelineCoreShouldNotDependOnConcreteStages() {
        JavaClasses importedClasses = importNichoCnaeProductionClasses();

        classes()
                .that().resideInAPackage(PIPELINE_PACKAGE + "..")
                .should(notDependOnConcreteNichoCnaeStages())
                .because("[ARQUITETURA] o núcleo pipeline deve conhecer apenas contratos genéricos, nunca etapas concretas")
                .check(importedClasses);
    }

    /** Valida que uma etapa concreta não conhece implementação, DTO, controller ou serviço de outra etapa. */
    @Test
    void concreteStagesShouldNotDependOnOtherConcreteStages() {
        JavaClasses importedClasses = importNichoCnaeProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .should(notDependOnOtherConcreteStages())
                .because("[ARQUITETURA] cada etapa nichocnae deve ser plugável e removível sem quebrar as demais")
                .check(importedClasses);
    }

    /** Valida que processors de etapas concretas seguem o contrato genérico executado pelo pipeline. */
    @Test
    void concreteStageProcessorsShouldImplementGenericStageProcessor() {
        JavaClasses importedClasses = importNichoCnaeProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .and().haveSimpleNameEndingWith("Processor")
                .should().beAssignableTo(StageProcessor.class)
                .because("[ARQUITETURA] processors de etapas concretas devem usar o contrato genérico StageProcessor")
                .check(importedClasses);
    }

    /** Valida que a versão inicial não conhece classes da versão 2 do pipeline NichoCNAE. */
    @Test
    void initialVersionShouldNotDependOnVersionTwo() {
        JavaClasses importedClasses = importNichoCnaeProductionClasses();

        classes()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should(notDependOnNichoCnaeVersionTwo())
                .because("[ARQUITETURA] a versão inicial nichocnae deve permanecer independente da versão 2")
                .check(importedClasses);
    }

    /** Importa somente classes de produção do contexto nichocnae para evitar ruído de testes. */
    private JavaClasses importNichoCnaeProductionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE, V2_PACKAGE);
    }

    /** Cria a condição explícita de dependência para evitar falso positivo em regras genéricas de pacotes. */
    private ArchCondition<JavaClass> notDependOnConcreteNichoCnaeStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de pacotes concretos " + BASE_PACKAGE + ".<etapa>") {
            /** Verifica qualquer dependência direta originada no núcleo do pipeline. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    if (isConcreteStage(target)) {
                        String message = "[ARQUITETURA] " + source.getName()
                                + " está no núcleo pipeline mas depende da etapa concreta "
                                + target.getName() + " via: " + dependency.getDescription();
                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    /** Cria a condição explícita que bloqueia acoplamento cruzado entre etapas concretas. */
    private ArchCondition<JavaClass> notDependOnOtherConcreteStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outra etapa concreta nichocnae") {
            /** Verifica qualquer dependência direta entre pacotes de etapas diferentes. */
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

    /** Cria condição explícita que bloqueia dependência direta da versão inicial para a versão 2. */
    private ArchCondition<JavaClass> notDependOnNichoCnaeVersionTwo() {
        return new ArchCondition<>("[ARQUITETURA] não depender de " + V2_PACKAGE) {
            /** Verifica qualquer dependência direta da versão inicial para classes do pacote v2. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    if (target.getPackageName().equals(V2_PACKAGE)
                            || target.getPackageName().startsWith(V2_PACKAGE + ".")) {
                        String message = "[ARQUITETURA] " + source.getName()
                                + " pertence à versão inicial mas depende da versão 2 "
                                + target.getName() + " via: " + dependency.getDescription();
                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    /** Identifica se uma classe pertence a um pacote concreto abaixo de nichocnae fora do núcleo pipeline. */
    private boolean isConcreteStage(JavaClass target) {
        return stageNameOf(target) != null;
    }

    /** Extrai o nome da etapa concreta a partir do primeiro pacote após nichocnae. */
    private static String stageNameOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(BASE_PACKAGE + ".")) {
            return null;
        }

        String remainder = packageName.substring((BASE_PACKAGE + ".").length());
        String topLevelPackage = remainder.contains(".")
                ? remainder.substring(0, remainder.indexOf('.'))
                : remainder;

        if ("pipeline".equals(topLevelPackage)) {
            return null;
        }

        return topLevelPackage;
    }
}
