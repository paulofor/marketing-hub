package com.marketinghub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Garante isolamento arquitetural entre módulos/pacotes internos do backend.
 */
@AnalyzeClasses(packages = "com.marketinghub")
class ModuleIsolationArchitectureTest {

    private static final String MOIS_SALES_LIBRARY_PACKAGE = "com.marketinghub.mois.bibliotecapaginavenda.worker.v1";
    private static final String GERALANDING_WIREFRAME_PACKAGE = "com.marketinghub.geralanding.wireframe";
    private static final String GERALANDING_COPY_PACKAGE = "com.marketinghub.geralanding.copy";
    private static final String GERALANDING_IMAGEPLANNING_PACKAGE = "com.marketinghub.geralanding.imageplanning";
    private static final String GERALANDING_PRESETDESIGN_PACKAGE = "com.marketinghub.geralanding.designpreset";

    @ArchTest
    static final ArchRule moisMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.mois..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept("com.marketinghub.mois"))
            .because("o módulo MOIS não deve depender de outros pacotes internos do sistema");

    @ArchTest
    static final ArchRule oprmMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.oprm..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept("com.marketinghub.oprm"))
            .because("o módulo OPRM não deve depender de outros pacotes internos do sistema");

    @ArchTest
    static final ArchRule moisSalesLibraryPackageMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept(MOIS_SALES_LIBRARY_PACKAGE))
            .because("o pacote de biblioteca de páginas de vendas do MOIS deve ficar isolado dos demais pacotes internos");

    @ArchTest
    static final ArchRule otherMarketingHubPackagesMustNotDependOnMoisSalesLibraryPackage = noClasses()
            .that()
            .resideInAPackage("com.marketinghub..")
            .and()
            .resideOutsideOfPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .because("nenhum outro pacote interno deve depender da biblioteca de páginas de vendas do MOIS");

    @ArchTest
    static final ArchRule geralandingWireframeMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_WIREFRAME_PACKAGE,
            "o subpacote geralanding.wireframe deve ficar independente dos demais pacotes internos");

    @ArchTest
    static final ArchRule geralandingCopyMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_COPY_PACKAGE,
            "o subpacote geralanding.copy deve ficar independente dos demais pacotes internos");

    @ArchTest
    static final ArchRule geralandingImagePlanningMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_IMAGEPLANNING_PACKAGE,
            "o subpacote geralanding.imageplanning deve ficar independente dos demais pacotes internos");

    @ArchTest
    static final ArchRule geralandingPresetDesignMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_PRESETDESIGN_PACKAGE,
            "o subpacote geralanding.designpreset deve ficar independente dos demais pacotes internos");

    /**
     * Constrói regra para impedir dependências para outros pacotes internos fora do prefixo permitido.
     */
    private static ArchRule isolatedFromOtherMarketingHubPackages(String allowedPackagePrefix, String reason) {
        return noClasses().that().resideInAPackage(allowedPackagePrefix + "..")
                .should()
                .dependOnClassesThat(otherMarketingHubPackagesExcept(allowedPackagePrefix))
                .because(reason);
    }

    /**
     * Retorna predicado que identifica classes internas fora do pacote permitido.
     */
    private static DescribedPredicate<JavaClass> otherMarketingHubPackagesExcept(String allowedPackagePrefix) {
        return new DescribedPredicate<>("other com.marketinghub packages except " + allowedPackagePrefix) {
            @Override
            public boolean test(JavaClass input) {
                String packageName = input.getPackageName();
                return packageName.startsWith("com.marketinghub") && !packageName.startsWith(allowedPackagePrefix);
            }
        };
    }
}
