package com.marketinghub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlAssembler;
import com.marketinghub.geralanding.wireframe.WireframeProvisionalHtmlAssembler;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Garante padrão arquitetural de uso e localização dos assemblers de etapas do GeraLanding.
 */
@AnalyzeClasses(packages = "com.marketinghub")
class GeraLandingAssemblerArchitectureTest {

    @ArchTest
    static final ArchRule stageExecutionServiceMustNotCallLegacyTwoArgDesignPresetAssembler = noClasses()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    DesignPresetProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class,
                    String.class)
            .because("o serviço deve usar o contrato padronizado com os novos parâmetros do design-preset");


    @ArchTest
    static final ArchRule stageExecutionServiceMustNotCallLegacyOneArgWireframeAssembler = noClasses()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    WireframeProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class)
            .because("o serviço deve usar o contrato padronizado assemble(modelResponse, jobId) para wireframe");

    @ArchTest
    static final ArchRule stageExecutionServiceMustNotCallLegacyTwoArgCopyAssembler = noClasses()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    com.marketinghub.geralanding.copy.CopyProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class,
                    String.class)
            .because("o serviço deve usar o contrato padronizado assemble(copyModelResponse, wireframeModelResponse, jobId) para copy");



    @ArchTest
    static final ArchRule stageExecutionServiceMustCallStandardWireframeAssembler = classes()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    WireframeProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class,
                    String.class)
            .because("quando STAGE_WIREFRAME for processado, o assembler canônico de wireframe deve ser usado");

    @ArchTest
    static final ArchRule stageExecutionServiceMustCallStandardDesignPresetAssembler = classes()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    DesignPresetProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class)
            .because("quando STAGE_DESIGN_PRESET for processado, o assembler canônico de design preset com novos parâmetros deve ser usado");

    @ArchTest
    static final ArchRule stageExecutionServiceMustCallStandardCopyAssembler = classes()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    com.marketinghub.geralanding.copy.CopyProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class,
                    String.class,
                    String.class)
            .because("quando STAGE_COPY for processado, o assembler canônico de copy deve ser usado");

    @ArchTest
    static final ArchRule wireframeAssemblerMustResideInWireframePackage = classes()
            .that()
            .haveSimpleName("WireframeProvisionalHtmlAssembler")
            .should()
            .resideInAPackage("..geralanding.wireframe..")
            .because("o assembler de wireframe deve ficar no pacote geralanding.wireframe");

    @ArchTest
    static final ArchRule designPresetAssemblerMustResideInDesignPresetPackage = classes()
            .that()
            .haveSimpleName("DesignPresetProvisionalHtmlAssembler")
            .should()
            .resideInAPackage("..geralanding.designpreset..")
            .because("o assembler de design preset deve ficar no pacote geralanding.designpreset");

    @ArchTest
    static final ArchRule wireframePackageMustContainCanonicalAssemblerType = classes()
            .that()
            .haveNameMatching(".*WireframeProvisionalHtmlAssembler")
            .should()
            .beAssignableTo(WireframeProvisionalHtmlAssembler.class)
            .because("o tipo canônico do assembler de wireframe deve existir e permanecer estável");
}
