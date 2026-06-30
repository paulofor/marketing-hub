package com.marketinghub.worker.pipeline.gerasalespagev1;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Protege o isolamento arquitetural do executor GeraSalesPage v1. */
@AnalyzeClasses(packages = "com.marketinghub.worker", importOptions = ImportOption.DoNotIncludeTests.class)
class GeraSalesPageArchitectureTest {
    /** Garante que o GeraSalesPage v1 não depende do GeraLanding. */
    @ArchTest
    static final ArchRule gerasalespage_nao_deve_depender_de_geralanding = noClasses()
            .that()
            .resideInAPackage("..pipeline.gerasalespagev1..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..geralanding..", "..openai.core.wireframe..", "..openai.core.copy..")
            .because("[ARQUITETURA] GeraSalesPage v1 deve ser independente do pipeline antigo GeraLanding");

    /** Garante que prompt e schema do GeraSalesPage v1 não sejam carregados de arquivos locais. */
    @ArchTest
    static final ArchRule gerasalespage_nao_deve_carregar_prompt_schema_do_classpath = noClasses()
            .that()
            .resideInAPackage("..pipeline.gerasalespagev1..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.core.io..")
            .because("[ARQUITETURA] GeraSalesPage v1 deve receber prompt/schema do backend, persistidos no banco");
}
