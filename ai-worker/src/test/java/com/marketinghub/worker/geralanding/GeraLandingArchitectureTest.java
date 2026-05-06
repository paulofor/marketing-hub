package com.marketinghub.worker.geralanding;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.marketinghub.worker", importOptions = ImportOption.DoNotIncludeTests.class)
class GeraLandingArchitectureTest {

    @ArchTest
    static final ArchRule geralanding_must_not_depend_on_experimentpipeline = noClasses()
            .that()
            .resideInAPackage("..geralanding..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..experimentpipeline..");
}
