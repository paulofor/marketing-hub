package com.marketinghub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.marketinghub")
class ArquiteturaTest {

    private static final String MOIS_SALES_LIBRARY_PACKAGE = "com.marketinghub.mois.bibliotecapaginavenda.worker.v1";

    @ArchTest
    static final ArchRule moisSalesLibraryPackageMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept(MOIS_SALES_LIBRARY_PACKAGE))
            .because("[ARQUITETURA] o pacote de biblioteca de páginas de vendas do MOIS deve ficar isolado dos demais pacotes internos");

    @ArchTest
    static final ArchRule otherMarketingHubPackagesMustNotDependOnMoisSalesLibraryPackage = noClasses()
            .that()
            .resideInAPackage("com.marketinghub..")
            .and()
            .resideOutsideOfPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .because("[ARQUITETURA] nenhum outro pacote interno deve depender da biblioteca de páginas de vendas do MOIS");

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
