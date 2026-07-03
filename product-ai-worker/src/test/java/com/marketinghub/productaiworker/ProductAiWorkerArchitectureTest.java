package com.marketinghub.productaiworker;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Responsabilidade: proteger isolamento arquitetural do Product AI Worker. */
@AnalyzeClasses(packages = "com.marketinghub.productaiworker", importOptions = ImportOption.DoNotIncludeTests.class)
class ProductAiWorkerArchitectureTest {

    /** Garante que o worker não acessa banco diretamente. */
    @ArchTest
    static final ArchRule workerMustNotUseDatabase =
            noClasses()
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..",
                            "javax.persistence..",
                            "org.springframework.jdbc..",
                            "org.springframework.data.jpa..")
                    .because("[ARQUITETURA] Product AI Worker deve consumir apenas APIs do backend, nunca banco direto");

    /** Garante que prompt/schema não sejam carregados de arquivo local. */
    @ArchTest
    static final ArchRule workerMustNotLoadLocalPromptSchema =
            noClasses()
                    .that().resideInAPackage("..personalizedsample..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.core.io..",
                            "java.nio.file..")
                    .because("[ARQUITETURA] Prompt/schema do Product AI Worker devem vir do backend pelo pending");

    /** Garante que o núcleo genérico não conhece a etapa concreta nem OpenAI. */
    @ArchTest
    static final ArchRule coreMustNotDependOnConcreteStageOrOpenAi =
            noClasses()
                    .that().resideInAPackage("..core..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..personalizedsample..",
                            "..infra..")
                    .because("[ARQUITETURA] Núcleo do Product AI Worker deve permanecer genérico e plugável");
}
