package com.marketinghub.nichocnaev2.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
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

/** Garante que o pipeline NichoCNAE versão 2 siga o protocolo padrão módulo no executor OPRM. */
class NichoCnaeV2PipelineArchitectureTest {
    private static final String INITIAL_VERSION_PACKAGE = "com.marketinghub.nichocnae";
    private static final String BASE_PACKAGE = "com.marketinghub.nichocnaev2";
    private static final String PIPELINE_PACKAGE = BASE_PACKAGE + ".pipeline";

    private static final DescribedPredicate<JavaClass> ARE_IN_CONCRETE_STAGE =
            new DescribedPredicate<>("[ARQUITETURA] classes de etapas concretas em nichocnaev2.pipeline.<etapa>") {
                /** Identifica classes de etapa concreta abaixo do pacote pipeline. */
                @Override
                public boolean test(JavaClass input) {
                    return stageNameOf(input) != null;
                }
            };

    /** Valida que o núcleo genérico não conhece nenhuma etapa concreta do pipeline versão 2. */
    @Test
    void pipelineCoreShouldNotDependOnConcreteStages() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that().resideInAPackage(PIPELINE_PACKAGE)
                .should(notDependOnConcreteStages())
                .because("[ARQUITETURA] o núcleo nichocnaev2.pipeline deve conhecer apenas contratos genéricos")
                .check(importedClasses);
    }

    /** Valida que uma etapa concreta da versão 2 não importa outra etapa concreta. */
    @Test
    void concreteStagesShouldNotDependOnOtherConcreteStages() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .should(notDependOnOtherConcreteStages())
                .because("[ARQUITETURA] etapas NichoCNAE v2 devem ser plugáveis e removíveis")
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
                .because("[ARQUITETURA] processors NichoCNAE v2 devem implementar StageProcessor")
                .check(importedClasses);
    }

    /** Valida que a versão 2 não conhece classes da versão inicial do pipeline NichoCNAE. */
    @Test
    void versionTwoShouldNotDependOnInitialVersion() {
        JavaClasses importedClasses = importProductionClasses();

        classes()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should(notDependOnInitialVersion())
                .because("[ARQUITETURA] a versão 2 nichocnae deve permanecer independente da versão inicial")
                .check(importedClasses);
    }

    /** Importa classes de produção do pipeline versão 2. */
    private JavaClasses importProductionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE, INITIAL_VERSION_PACKAGE);
    }

    /** Cria condição explícita que bloqueia dependência do núcleo para etapa concreta. */
    private ArchCondition<JavaClass> notDependOnConcreteStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de nichocnaev2.pipeline.<etapa>") {
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

    /** Cria condição explícita que bloqueia dependência direta da versão 2 para a versão inicial. */
    private ArchCondition<JavaClass> notDependOnInitialVersion() {
        return new ArchCondition<>("[ARQUITETURA] não depender de " + INITIAL_VERSION_PACKAGE) {
            /** Verifica dependências diretas originadas na versão 2 para classes da versão inicial. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    if (target.getPackageName().equals(INITIAL_VERSION_PACKAGE)
                            || target.getPackageName().startsWith(INITIAL_VERSION_PACKAGE + ".")) {
                        events.add(SimpleConditionEvent.violated(source, "[ARQUITETURA] " + source.getName()
                                + " pertence à versão 2 mas depende da versão inicial "
                                + target.getName() + " via: " + dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** Extrai a etapa concreta de pacotes no formato nichocnaev2.pipeline.<etapa>. */
    private static String stageNameOf(JavaClass javaClass) {
        String prefix = PIPELINE_PACKAGE + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        return remainder.contains(".") ? remainder.substring(0, remainder.indexOf('.')) : remainder;
    }
}
