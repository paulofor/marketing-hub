package com.marketinghub.nichocnae.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

/** Garante por ArchUnit que o núcleo genérico do pipeline nichocnae não depende de etapas concretas. */
class NichoCnaePipelineArchitectureTest {

    /** Valida o isolamento do pacote raiz pipeline conforme o padrão canônico de etapas. */
    @Test
    void pipelineCoreShouldNotDependOnConcreteStages() {
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.marketinghub.nichocnae");

        classes()
                .that().resideInAPackage("com.marketinghub.nichocnae.pipeline..")
                .should(notDependOnConcreteNichoCnaeStages())
                .because("[ARQUITETURA] o núcleo pipeline deve conhecer apenas contratos genéricos, nunca etapas concretas")
                .check(importedClasses);
    }

    /** Cria a condição explícita de dependência para evitar falso positivo em regras genéricas de pacotes. */
    private ArchCondition<JavaClass> notDependOnConcreteNichoCnaeStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de pacotes concretos com.marketinghub.nichocnae.<etapa>") {
            /** Verifica chamadas de métodos e construtores originadas no núcleo do pipeline. */
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getMethodCallsFromSelf().stream()
                        .map(JavaMethodCall::getTargetOwner)
                        .filter(NichoCnaePipelineArchitectureTest.this::isConcreteStage)
                        .forEach(target -> events.add(SimpleConditionEvent.violated(
                                item,
                                "[ARQUITETURA] " + item.getName() + " depende da etapa concreta " + target.getName())));
                item.getConstructorCallsFromSelf().stream()
                        .map(JavaConstructorCall::getTargetOwner)
                        .filter(NichoCnaePipelineArchitectureTest.this::isConcreteStage)
                        .forEach(target -> events.add(SimpleConditionEvent.violated(
                                item,
                                "[ARQUITETURA] " + item.getName() + " instancia a etapa concreta " + target.getName())));
            }
        };
    }

    /** Identifica pacotes concretos abaixo de nichocnae que não pertencem ao núcleo pipeline. */
    private boolean isConcreteStage(JavaClass target) {
        String packageName = target.getPackageName();
        return packageName.startsWith("com.marketinghub.nichocnae.")
                && !packageName.startsWith("com.marketinghub.nichocnae.pipeline");
    }
}
