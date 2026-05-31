package com.marketinghub.worker.geralanding;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;


import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.marketinghub.worker")
class ArquiteturaTest {

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
            .matching("com.marketinghub.worker.geralanding.(copy|imageplanning|presetdesign|deliverables)..")
            .should()
            .notDependOnEachOther()
            .allowEmptyShould(true);

    /** Garante que copy só acesse classes do próprio pacote ou geralanding.comum dentro da aplicação. */
    @ArchTest
    static final ArchRule geralanding_copy_must_only_access_own_or_comum = noClasses()
            .that()
            .resideInAPackage("..geralanding.copy..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub..")
            .andShould()
            .resideOutsideOfPackages("..geralanding.copy..", "..geralanding.comum..");

    /** Garante que presetdesign só acesse classes do próprio pacote ou geralanding.comum dentro da aplicação. */
    @ArchTest
    static final ArchRule geralanding_presetdesign_must_only_access_own_or_comum = noClasses()
            .that()
            .resideInAPackage("..geralanding.presetdesign..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub..")
            .andShould()
            .resideOutsideOfPackages("..geralanding.presetdesign..", "..geralanding.comum..");

    /** Garante que stage só acesse classes do próprio pacote ou geralanding.comum dentro da aplicação. */
    @ArchTest
    static final ArchRule geralanding_stage_must_only_access_own_or_comum = noClasses()
            .that()
            .resideInAPackage("..geralanding.stage..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub..")
            .andShould()
            .resideOutsideOfPackages("..geralanding.stage..", "..geralanding.comum..");

    /** Garante que deliverables só acesse classes do próprio pacote ou geralanding.comum dentro da aplicação. */
    @ArchTest
    static final ArchRule geralanding_deliverables_must_only_access_own_or_comum = noClasses()
            .that()
            .resideInAPackage("..geralanding.deliverables..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub..")
            .andShould()
            .resideOutsideOfPackages("..geralanding.deliverables..", "..geralanding.comum..");

    /** Garante que imageplanning só acesse classes do próprio pacote ou geralanding.comum dentro da aplicação. */
    @ArchTest
    static final ArchRule geralanding_imageplanning_must_only_access_own_or_comum = noClasses()
            .that()
            .resideInAPackage("..geralanding.imageplanning..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub..")
            .andShould()
            .resideOutsideOfPackages("..geralanding.imageplanning..", "..geralanding.comum..");

    /** Garante que geralanding.comum só acesse classes do próprio pacote dentro da aplicação. */
    @ArchTest
    static final ArchRule geralanding_comum_must_only_access_its_own_package = noClasses()
            .that()
            .resideInAPackage("..geralanding.comum..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub..")
            .andShould()
            .resideOutsideOfPackages("..geralanding.comum..");

}
