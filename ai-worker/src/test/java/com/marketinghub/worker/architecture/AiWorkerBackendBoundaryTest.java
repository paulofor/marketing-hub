package com.marketinghub.worker.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Guarda arquitetural para impedir que o AI Worker volte a acessar banco ou services de persistência. */
@AnalyzeClasses(
        packages = "com.marketinghub.worker",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class AiWorkerBackendBoundaryTest {
    @ArchTest
    static final ArchRule ai_worker_nao_deve_depender_de_repositories_ou_jpa =
            noClasses()
                    .that()
                    .resideInAnyPackage("com.marketinghub.worker..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.marketinghub.repository..",
                            "jakarta.persistence..",
                            "javax.persistence..",
                            "org.springframework.data.jpa..")
                    .because("[ARQUITETURA] AI Worker deve ler e escrever dados somente por endpoints do backend, nunca por JPA/repository/banco direto");

    @ArchTest
    static final ArchRule ai_worker_nao_deve_consumir_services_backend =
            noClasses()
                    .that()
                    .resideInAnyPackage("com.marketinghub.worker..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("com.marketinghub..service..")
                    .because("[ARQUITETURA] AI Worker deve consumir contratos HTTP do backend, sem injetar services internos que encapsulam persistência");
}
