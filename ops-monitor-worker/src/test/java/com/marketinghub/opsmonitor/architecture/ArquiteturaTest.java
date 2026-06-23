package com.marketinghub.opsmonitor.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.marketinghub.opsmonitor.pipeline.StageProcessor;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

class ArquiteturaTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.marketinghub.opsmonitor");

    @Test
    void nucleoNaoDeveDependerDeEtapasConcretas() {
        noClasses().that().resideInAPackage("..pipeline")
                .should().dependOnClassesThat().resideInAnyPackage("..pipeline.healthcheck..", "..pipeline.availability..", "..pipeline.logscan..")
                .because("[ARQUITETURA] O núcleo genérico não pode conhecer etapas concretas.")
                .check(classes);
    }

    @Test
    void etapasNaoDevemDependerEntreSi() {
        classes().that().resideInAnyPackage("..pipeline.healthcheck..", "..pipeline.availability..", "..pipeline.logscan..")
                .should(notDependOnOtherConcreteStages())
                .because("[ARQUITETURA] Etapas concretas devem ser plugáveis e independentes.")
                .check(classes);
    }

    @Test
    void processorsConcretosDevemImplementarContratoDeEtapa() {
        classes().that().haveSimpleNameEndingWith("Processor").and().resideInAnyPackage("..pipeline.healthcheck..", "..pipeline.availability..", "..pipeline.logscan..")
                .should().beAssignableTo(StageProcessor.class)
                .because("[ARQUITETURA] Processors concretos precisam implementar StageProcessor.")
                .check(classes);
    }

    @Test
    void nucleoNaoDeveDependerDeTecnologiasConcretas() {
        noClasses().that().resideInAPackage("..pipeline")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "reactor..", "com.fasterxml..", "software.amazon..", "com.microsoft.playwright..")
                .because("[ARQUITETURA] O núcleo do pipeline não deve depender de tecnologias concretas de execução.")
                .check(classes);
    }

    private ArchCondition<JavaClass> notDependOnOtherConcreteStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outra etapa concreta") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String ownStage = stageOf(item.getPackageName());
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetStage = stageOf(dependency.getTargetClass().getPackageName());
                    if (targetStage != null && ownStage != null && !ownStage.equals(targetStage)) {
                        events.add(SimpleConditionEvent.violated(item, "[ARQUITETURA] " + item.getName() + " depende da etapa " + targetStage));
                    }
                });
            }
        };
    }

    private String stageOf(String packageName) {
        if (packageName.contains(".pipeline.healthcheck")) return "healthcheck";
        if (packageName.contains(".pipeline.availability")) return "availability";
        if (packageName.contains(".pipeline.logscan")) return "logscan";
        return null;
    }
}
