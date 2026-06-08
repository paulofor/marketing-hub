package com.marketinghub.worker.architecture;

import com.marketinghub.worker.pipeline.StageProcessor;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Guarda arquitetural do motor genérico com.marketinghub.worker.pipeline. */
@AnalyzeClasses(
        packages = "com.marketinghub.worker",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class PipelineArchitectureTest {
    private static final String PIPELINE_ROOT = "com.marketinghub.worker.pipeline";

    private static final DescribedPredicate<JavaClass> CLASSES_DE_ETAPA =
            new DescribedPredicate<>("classes dentro de pipeline.<etapa>") {
                /** Identifica classes que pertencem a subpacotes concretos do pipeline. */
                @Override
                public boolean test(JavaClass javaClass) {
                    String packageName = javaClass.getPackageName();
                    return packageName.startsWith(PIPELINE_ROOT + ".");
                }
            };

    @ArchTest
    static final ArchRule pacote_pipeline_raiz_nao_deve_depender_de_etapas =
            noClasses()
                    .that()
                    .resideInAPackage(PIPELINE_ROOT)
                    .should()
                    .dependOnClassesThat(CLASSES_DE_ETAPA)
                    .because("[ARQUITETURA] o pacote pipeline é o núcleo genérico e não pode conhecer etapas concretas");

    @ArchTest
    static final ArchRule etapas_nao_devem_depender_umas_das_outras =
            slices()
                    .matching("com.marketinghub.worker.pipeline.(*)..")
                    .should()
                    .notDependOnEachOther()
                    .because("[ARQUITETURA] cada pipeline.<etapa> deve ser independente das outras etapas");

    @ArchTest
    static final ArchRule etapas_nao_devem_ter_ciclos =
            slices()
                    .matching("com.marketinghub.worker.pipeline.(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because("[ARQUITETURA] etapas do pipeline não devem formar ciclos de dependência");

    @ArchTest
    static final ArchRule processors_de_etapa_devem_implementar_stage_processor =
            classes()
                    .that()
                    .resideInAPackage("com.marketinghub.worker.pipeline..")
                    .and()
                    .resideOutsideOfPackage(PIPELINE_ROOT)
                    .and()
                    .haveSimpleNameEndingWith("Processor")
                    .should()
                    .implement(StageProcessor.class)
                    .because("[ARQUITETURA] toda etapa concreta deve entrar no pipeline através do contrato StageProcessor");

    @ArchTest
    static final ArchRule pipeline_raiz_nao_deve_depender_de_tecnologias_concretas =
            noClasses()
                    .that()
                    .resideInAPackage(PIPELINE_ROOT)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.web.reactive.function.client..",
                            "org.jsoup..",
                            "com.microsoft.playwright..",
                            "software.amazon.awssdk..",
                            "okhttp3..")
                    .because("[ARQUITETURA] o núcleo do pipeline deve depender de abstrações, não de tecnologias concretas");
}
