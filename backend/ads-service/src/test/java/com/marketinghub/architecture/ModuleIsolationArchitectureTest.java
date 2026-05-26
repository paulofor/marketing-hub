package com.marketinghub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern SALES_LIBRARY_LAYER_PATTERN = Pattern.compile(
            "^com\\.marketinghub\\.mois\\.bibliotecapaginavenda\\.([a-zA-Z0-9_]+)\\.(v\\d+)\\.(web|service|repository)(?:\\..*)?$");

    @ArchTest
    static final ArchRule moisMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.mois..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept("com.marketinghub.mois"))
            .because("[ARQUITETURA] o módulo MOIS não deve depender de outros pacotes internos do sistema");

    @ArchTest
    static final ArchRule oprmMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.oprm..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept("com.marketinghub.oprm"))
            .because("[ARQUITETURA] o módulo OPRM não deve depender de outros pacotes internos do sistema");

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

    @ArchTest
    static final ArchRule geralandingWireframeMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_WIREFRAME_PACKAGE,
            "[ARQUITETURA] o subpacote geralanding.wireframe deve ficar independente dos demais pacotes internos");

    @ArchTest
    static final ArchRule geralandingCopyMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_COPY_PACKAGE,
            "[ARQUITETURA] o subpacote geralanding.copy deve ficar independente dos demais pacotes internos");

    @ArchTest
    static final ArchRule geralandingImagePlanningMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_IMAGEPLANNING_PACKAGE,
            "[ARQUITETURA] o subpacote geralanding.imageplanning deve ficar independente dos demais pacotes internos");

    @ArchTest
    static final ArchRule geralandingPresetDesignMustBeIsolated = isolatedFromOtherMarketingHubPackages(
            GERALANDING_PRESETDESIGN_PACKAGE,
            "[ARQUITETURA] o subpacote geralanding.designpreset deve ficar independente dos demais pacotes internos");

    @ArchTest
    static final ArchRule moisSalesLibraryWebShouldOnlyDependOnServiceLayer =
            classes().that(classesBelongingToLayer("web")).should(onlyDependOnLayer("service"));

    @ArchTest
    static final ArchRule moisSalesLibraryServiceShouldOnlyDependOnRepositoryLayer =
            classes().that(classesBelongingToLayer("service")).should(onlyDependOnLayer("repository"));

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

    /**
     * Retorna classes de um layer específico da biblioteca de páginas de venda do MOIS.
     */
    private static DescribedPredicate<JavaClass> classesBelongingToLayer(String expectedLayer) {
        return new DescribedPredicate<>("classes in " + expectedLayer + " layer of mois sales library") {
            @Override
            public boolean test(JavaClass input) {
                return extractSalesLibraryPackageInfo(input.getPackageName())
                        .map(packageInfo -> packageInfo.layer().equals(expectedLayer) && !input.getSimpleName().endsWith("Test"))
                        .orElse(false);
            }
        };
    }

    /**
     * Valida que classes de um layer dependem apenas do layer permitido com mesmo x e vN.
     */
    private static ArchCondition<JavaClass> onlyDependOnLayer(String allowedTargetLayer) {
        return new ArchCondition<>("[ARQUITETURA] depend only on " + allowedTargetLayer + " in same x/vN") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Optional<SalesLibraryPackageInfo> sourceInfo = extractSalesLibraryPackageInfo(item.getPackageName());
                if (sourceInfo.isEmpty()) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetPackageName = dependency.getTargetClass().getPackageName();
                    Optional<SalesLibraryPackageInfo> targetInfo = extractSalesLibraryPackageInfo(targetPackageName);
                    if (targetInfo.isEmpty()) {
                        return;
                    }
                    boolean sameNamespace = sourceInfo.get().namespace().equals(targetInfo.get().namespace())
                            && sourceInfo.get().version().equals(targetInfo.get().version());
                    boolean allowedLayer = targetInfo.get().layer().equals(allowedTargetLayer)
                            || targetInfo.get().layer().equals(sourceInfo.get().layer());
                    if (!sameNamespace || !allowedLayer) {
                        String message = "[ARQUITETURA] " + item.getName() + " depende de " + dependency.getTargetClass().getName()
                                + " mas só pode depender de pacote ." + allowedTargetLayer
                                + " com mesmo x/vN";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Extrai namespace (x), versão (vN) e layer dos pacotes da biblioteca de páginas de venda.
     */
    private static Optional<SalesLibraryPackageInfo> extractSalesLibraryPackageInfo(String packageName) {
        Matcher matcher = SALES_LIBRARY_LAYER_PATTERN.matcher(packageName);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new SalesLibraryPackageInfo(matcher.group(1), matcher.group(2), matcher.group(3)));
    }

    /**
     * Representa a estrutura do pacote da biblioteca de páginas de venda do MOIS.
     */
    private record SalesLibraryPackageInfo(String namespace, String version, String layer) {}
}
