package com.marketinghub.worker.geralanding;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.marketinghub.worker", importOptions = ImportOption.DoNotIncludeTests.class)
class GeraLandingArchitectureTest {

    /** Garante que o módulo de geração de landing não dependa do pipeline legado de experimentos. */
    @ArchTest
    static final ArchRule geralanding_must_not_depend_on_experimentpipeline = noClasses()
            .that()
            .resideInAPackage("..geralanding..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..experimentpipeline..");

    /** Garante independência entre subpacotes do GeraLanding para evitar acoplamento cruzado. */
    @ArchTest
    static final ArchRule geralanding_subpackages_must_be_independent = slices()
            .matching("com.marketinghub.worker.geralanding.(wireframe|copy|imageplanning|presetdesign)..")
            .should()
            .notDependOnEachOther()
            .allowEmptyShould(true);
}
