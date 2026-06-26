package com.marketinghub.worker.architecture;

import com.marketinghub.worker.pipeline.StageProcessor;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Guarda arquitetural do pipeline executor GeraAnuncio v2 no AI Worker. */
@AnalyzeClasses(packages = "com.marketinghub.worker", importOptions = ImportOption.DoNotIncludeTests.class)
class GeraAnuncioV2ArchitectureTest {
    private static final String PIPELINE_ROOT = "com.marketinghub.worker.geraanunciov2.pipeline";

    @ArchTest
    static final ArchRule nucleo_geraanuncio_v2_nao_deve_depender_de_etapas = noClasses()
            .that()
            .resideInAnyPackage(PIPELINE_ROOT, PIPELINE_ROOT + ".model..", PIPELINE_ROOT + ".port..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PIPELINE_ROOT + ".criativo..")
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] o núcleo genérico não pode conhecer etapas concretas");

    @ArchTest
    static final ArchRule etapas_geraanuncio_v2_nao_devem_depender_umas_das_outras = slices()
            .matching("com.marketinghub.worker.geraanunciov2.pipeline.(*)..")
            .should()
            .notDependOnEachOther()
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] etapas concretas não podem depender umas das outras");

    @ArchTest
    static final ArchRule etapas_geraanuncio_v2_nao_devem_ter_ciclos = slices()
            .matching("com.marketinghub.worker.geraanunciov2.pipeline.(*)..")
            .should()
            .beFreeOfCycles()
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] etapas do pipeline não podem formar ciclos");

    @ArchTest
    static final ArchRule processors_geraanuncio_v2_devem_implementar_stage_processor = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT + "..")
            .and()
            .haveSimpleNameEndingWith("Processor")
            .should()
            .implement(StageProcessor.class)
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] processors concretos devem implementar StageProcessor");

    @ArchTest
    static final ArchRule nucleo_geraanuncio_v2_nao_deve_depender_de_tecnologias_concretas = noClasses()
            .that()
            .resideInAnyPackage(PIPELINE_ROOT, PIPELINE_ROOT + ".model..", PIPELINE_ROOT + ".port..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web.reactive.function.client..", "org.jsoup..", "com.microsoft.playwright..", "software.amazon.awssdk..", "okhttp3..")
            .because("[ARQUITETURA] [AI Worker][GeraAnuncio v2] o núcleo deve depender de contratos, não de tecnologias concretas");
}
