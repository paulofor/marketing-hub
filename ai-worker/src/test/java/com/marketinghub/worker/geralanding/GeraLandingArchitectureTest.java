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

    /** Garante que o módulo copy só acesse o próprio pacote e geralanding.comum dentro do domínio geralanding. */
    @ArchTest
    static final ArchRule geralanding_copy_must_not_depend_on_other_modules = noClasses()
            .that()
            .resideInAPackage("..geralanding.copy..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..geralanding.presetdesign..",
                    "..geralanding.stage..",
                    "..geralanding.wireframe..",
                    "..geralanding.deliverables..",
                    "..geralanding.imageplanning..");

    /** Garante que o módulo presetdesign só acesse o próprio pacote e geralanding.comum dentro do domínio geralanding. */
    @ArchTest
    static final ArchRule geralanding_presetdesign_must_not_depend_on_other_modules = noClasses()
            .that()
            .resideInAPackage("..geralanding.presetdesign..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..geralanding.copy..",
                    "..geralanding.stage..",
                    "..geralanding.wireframe..",
                    "..geralanding.deliverables..",
                    "..geralanding.imageplanning..");

    /** Garante que o módulo stage só acesse o próprio pacote e geralanding.comum dentro do domínio geralanding. */
    @ArchTest
    static final ArchRule geralanding_stage_must_not_depend_on_other_modules = noClasses()
            .that()
            .resideInAPackage("..geralanding.stage..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..geralanding.copy..",
                    "..geralanding.presetdesign..",
                    "..geralanding.wireframe..",
                    "..geralanding.deliverables..",
                    "..geralanding.imageplanning..");

    /** Garante que o módulo wireframe só acesse o próprio pacote e geralanding.comum dentro do domínio geralanding. */
    @ArchTest
    static final ArchRule geralanding_wireframe_must_not_depend_on_other_modules = noClasses()
            .that()
            .resideInAPackage("..geralanding.wireframe..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..geralanding.copy..",
                    "..geralanding.presetdesign..",
                    "..geralanding.stage..",
                    "..geralanding.deliverables..",
                    "..geralanding.imageplanning..");

    /** Garante que o módulo deliverables só acesse o próprio pacote e geralanding.comum dentro do domínio geralanding. */
    @ArchTest
    static final ArchRule geralanding_deliverables_must_not_depend_on_other_modules = noClasses()
            .that()
            .resideInAPackage("..geralanding.deliverables..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..geralanding.copy..",
                    "..geralanding.presetdesign..",
                    "..geralanding.stage..",
                    "..geralanding.wireframe..",
                    "..geralanding.imageplanning..");

    /** Garante que o módulo imageplanning só acesse o próprio pacote e geralanding.comum dentro do domínio geralanding. */
    @ArchTest
    static final ArchRule geralanding_imageplanning_must_not_depend_on_other_modules = noClasses()
            .that()
            .resideInAPackage("..geralanding.imageplanning..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..geralanding.copy..",
                    "..geralanding.presetdesign..",
                    "..geralanding.stage..",
                    "..geralanding.wireframe..",
                    "..geralanding.deliverables..");

    /** Garante que geralanding.comum só acesse classes do próprio pacote dentro do domínio geralanding. */
    @ArchTest
    static final ArchRule geralanding_comum_must_not_depend_on_other_modules = noClasses()
            .that()
            .resideInAPackage("..geralanding.comum..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..geralanding.copy..",
                    "..geralanding.presetdesign..",
                    "..geralanding.stage..",
                    "..geralanding.wireframe..",
                    "..geralanding.deliverables..",
                    "..geralanding.imageplanning..");
}
