package com.marketinghub.pdemonitor.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Protege a independência arquitetural do monitor dedicado de PDEs. */
class ArquiteturaTest {

    @Test
    /** Garante que o módulo não cria dependência de classes do backend principal. */
    void monitorPdeNaoDeveDependerDoBackendPrincipal() {
        var classes = new ClassFileImporter().importPackages("com.marketinghub.pdemonitor");

        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.marketinghub.opsmonitor..", "com.marketinghub.experiment..")
                .because("[ARQUITETURA] O pde-monitor-worker deve ser independente do backend principal para operar 24/7.")
                .check(classes);
    }
}
