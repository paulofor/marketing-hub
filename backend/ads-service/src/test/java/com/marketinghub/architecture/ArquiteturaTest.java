package com.marketinghub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlAssembler;
import com.marketinghub.geralanding.wireframe.provisorio.WireframeProvisionalHtmlAssembler;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Garante isolamento arquitetural entre módulos/pacotes internos do backend.
 */
@AnalyzeClasses(packages = "com.marketinghub")
class ArquiteturaTest {

    private static final String MOIS_SALES_LIBRARY_PACKAGE = "com.marketinghub.mois.bibliotecapaginavenda.worker.v1";
    private static final String EXPERIMENT_CLASS = "com.marketinghub.experiment.Experiment";
    private static final String EXPERIMENT_REPOSITORY_CLASS = "com.marketinghub.experiment.repository.ExperimentRepository";
    private static final String GERALANDING_STAGE_EXECUTION_CLASS = "com.marketinghub.geralanding.GeraLandingStageExecution";
    private static final String GERALANDING_STAGE_EXECUTION_REPOSITORY_CLASS =
            "com.marketinghub.geralanding.GeraLandingStageExecutionRepository";
    private static final String GERALANDING_STAGE_EXECUTION_BUILDER_CLASS =
            "com.marketinghub.geralanding.GeraLandingStageExecution$GeraLandingStageExecutionBuilder";
    private static final Pattern SALES_LIBRARY_LAYER_PATTERN = Pattern.compile(
            "^com\\.marketinghub\\.mois\\.bibliotecapaginavenda\\.([a-zA-Z0-9_]+)\\.(v\\d+)\\.(web|service|repository)(?:\\..*)?$");

    @ArchTest
    static final ArchRule moisMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.mois..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept("com.marketinghub.mois"))
            .because("[ARQUITETURA][MOIS] o módulo MOIS não deve depender de outros pacotes internos do sistema");

    @ArchTest
    static final ArchRule oprmMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.oprm..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept("com.marketinghub.oprm"))
            .because("[ARQUITETURA][OPRM] o módulo OPRM não deve depender de outros pacotes internos do sistema");

    @ArchTest
    static final ArchRule moisSalesLibraryPackageMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept(MOIS_SALES_LIBRARY_PACKAGE))
            .because("[ARQUITETURA][MOIS] o pacote de biblioteca de páginas de vendas do MOIS deve ficar isolado dos demais pacotes internos");

    @ArchTest
    static final ArchRule otherMarketingHubPackagesMustNotDependOnMoisSalesLibraryPackage = noClasses()
            .that()
            .resideInAPackage("com.marketinghub..")
            .and()
            .resideOutsideOfPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .because("[ARQUITETURA][MOIS] nenhum outro pacote interno deve depender da biblioteca de páginas de vendas do MOIS");

    @ArchTest
    static final ArchRule geralandingWebPackagesShouldOnlyDependOnWebOrServiceInSameStage =
            classes().that().resideInAPackage("com.marketinghub.geralanding.*.web..")
                    .should(onlyDependOnWebOrServiceWithinSameStage())
                    .because("[ARQUITETURA][BACKEND][GeraLanding] web em geralanding.*.web só pode acessar classes de geralanding.*.web e geralanding.*.service da mesma etapa");

    @ArchTest
    static final ArchRule geralandingProvisorioPackagesShouldOnlyDependOnOwnProvisorioPackage =
            classes().that().resideInAPackage("com.marketinghub.geralanding.*.provisorio..")
                    .should(onlyDependOnProvisorioWithinSameStage())
                    .because("[ARQUITETURA][BACKEND][GeraLanding] provisorio em geralanding.*.provisorio só pode acessar classes do próprio pacote geralanding.*.provisorio");

    @ArchTest
    static final ArchRule geralandingServicePackagesShouldOnlyAccessAllowedMarketingHubClasses =
            classes().that().resideInAPackage("com.marketinghub.geralanding..service..")
                    .should(onlyDependOnAllowedMarketingHubClasses())
                    .because("[ARQUITETURA] [BACKEND][GeraLanding] serviços em geralanding.*.service só podem acessar classes permitidas dentro de com.marketinghub");

    @ArchTest
    static final ArchRule backendStageControllersMustExposePendingMethod =
            classes().that().resideInAPackage("com.marketinghub.geralanding.*.web..")
                    .and().haveNameMatching(".*\\.Backend[A-Za-z0-9]+Controller")
                    .should(havePendingMethodReturningStagePendingRecord())
                    .because("[ARQUITETURA] [BACKEND][GeraLanding] toda classe Backend<Etapa>Controller deve expor pending retornando List<Record<Etapa>Pending> para a fila interna da etapa");

    @ArchTest
    static final ArchRule backendWireframeControllerMustExposeRecebePromptMethod = classes()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.wireframe.web.BackendWireframeController")
            .should(haveRecebePromptMethod())
            .because("[ARQUITETURA] [BACKEND][GeraLanding][Wireframe] BackendWireframeController deve expor recebePrompt(String idJob, RecebePromptRequest payload) para receber o prompt enviado à IA");

    @ArchTest
    static final ArchRule moisSalesLibraryWebShouldOnlyDependOnServiceLayer =
            classes().that(classesBelongingToLayer("web")).should(onlyDependOnLayer("service"));

    @ArchTest
    static final ArchRule moisSalesLibraryServiceShouldOnlyDependOnRepositoryLayer =
            classes().that(classesBelongingToLayer("service")).should(onlyDependOnLayer("repository"));



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
            .because("[ARQUITETURA][BACKEND][GeraLandingStageExecutionService] o serviço deve usar o contrato padronizado com os novos parâmetros do design-preset");

    @ArchTest
    static final ArchRule stageExecutionServiceMustNotCallLegacyOneArgWireframeAssembler = noClasses()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    WireframeProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class)
            .because("[ARQUITETURA][BACKEND][GeraLandingStageExecutionService] o serviço deve usar o contrato padronizado assemble(modelResponse, jobId) para wireframe");

    @ArchTest
    static final ArchRule stageExecutionServiceMustNotCallLegacyTwoArgCopyAssembler = noClasses()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    com.marketinghub.geralanding.copy.provisorio.CopyProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class,
                    String.class)
            .because("[ARQUITETURA][BACKEND][GeraLandingStageExecutionService] o serviço deve usar o contrato padronizado assemble(copyModelResponse, wireframeModelResponse, jobId) para copy");

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
            .because("[ARQUITETURA][BACKEND][GeraLandingStageExecutionService] quando STAGE_WIREFRAME for processado, o assembler canônico de wireframe deve ser usado");

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
            .because("[ARQUITETURA][BACKEND][GeraLandingStageExecutionService] quando STAGE_DESIGN_PRESET for processado, o assembler canônico de design preset com novos parâmetros deve ser usado");

    @ArchTest
    static final ArchRule stageExecutionServiceMustCallStandardCopyAssembler = classes()
            .that()
            .haveFullyQualifiedName("com.marketinghub.geralanding.GeraLandingStageExecutionService")
            .should()
            .callMethod(
                    com.marketinghub.geralanding.copy.provisorio.CopyProvisionalHtmlAssembler.class,
                    "assemble",
                    String.class,
                    String.class,
                    String.class)
            .because("[ARQUITETURA][BACKEND][GeraLandingStageExecutionService] quando STAGE_COPY for processado, o assembler canônico de copy deve ser usado");

    @ArchTest
    static final ArchRule wireframeAssemblerMustResideInWireframePackage = classes()
            .that()
            .haveSimpleName("WireframeProvisionalHtmlAssembler")
            .should()
            .resideInAPackage("..geralanding.wireframe..")
            .because("[ARQUITETURA][BACKEND][WireframeProvisionalHtmlAssembler] o assembler de wireframe deve ficar no pacote geralanding.wireframe");

    @ArchTest
    static final ArchRule designPresetAssemblerMustResideInDesignPresetPackage = classes()
            .that()
            .haveSimpleName("DesignPresetProvisionalHtmlAssembler")
            .should()
            .resideInAPackage("..geralanding.designpreset..")
            .because("[ARQUITETURA][BACKEND][DesignPresetProvisionalHtmlAssembler] o assembler de design preset deve ficar no pacote geralanding.designpreset");

    @ArchTest
    static final ArchRule wireframePackageMustContainCanonicalAssemblerType = classes()
            .that()
            .haveNameMatching(".*WireframeProvisionalHtmlAssembler")
            .should()
            .beAssignableTo(WireframeProvisionalHtmlAssembler.class)
            .because("[ARQUITETURA][BACKEND][WireframeProvisionalHtmlAssembler] o tipo canônico do assembler de wireframe deve existir e permanecer estável");

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
     * Garante que controllers internos por etapa exponham pending com o record canônico da etapa.
     */
    private static ArchCondition<JavaClass> havePendingMethodReturningStagePendingRecord() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][GeraLanding] Backend<Etapa>Controller.pending returns List<Record<Etapa>Pending>") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Optional<JavaMethod> pendingMethod = item.getMethods().stream()
                        .filter(method -> method.getName().equals("pending"))
                        .findFirst();
                if (pendingMethod.isEmpty()) {
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding] classe=" + item.getName()
                            + " deve declarar método pending para expor a fila interna da etapa";
                    events.add(SimpleConditionEvent.violated(item, message));
                    return;
                }

                String stageName = extractBackendStageName(item);
                String expectedRecordName = "Record" + stageName + "Pending";
                Type returnType = pendingMethod.get().reflect().getGenericReturnType();
                boolean validReturnType = isListOfExpectedRecord(returnType, expectedRecordName);
                if (!validReturnType) {
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding] classe=" + item.getName()
                            + " método=pending deve retornar List<" + expectedRecordName + ">"
                            + " conforme o padrão Backend<Etapa>Controller.pending -> List<Record<Etapa>Pending>"
                            + "; retorno atual=" + returnType.getTypeName();
                    events.add(SimpleConditionEvent.violated(item, message));
                }
            }
        };
    }

    /**
     * Garante que BackendWireframeController exponha o método interno recebePrompt recebendo jobId e payload.
     */
    private static ArchCondition<JavaClass> haveRecebePromptMethod() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][GeraLanding][Wireframe] BackendWireframeController.recebePrompt(String, RecebePromptRequest)") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Optional<JavaMethod> method = item.getMethods().stream()
                        .filter(candidate -> candidate.getName().equals("recebePrompt"))
                        .findFirst();
                if (method.isEmpty()) {
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding][Wireframe] classe=" + item.getName()
                            + " deve declarar método recebePrompt para receber o prompt enviado à IA";
                    events.add(SimpleConditionEvent.violated(item, message));
                    return;
                }

                Class<?>[] parameterTypes = method.get().reflect().getParameterTypes();
                boolean validSignature = parameterTypes.length == 2
                        && String.class.equals(parameterTypes[0])
                        && "com.marketinghub.geralanding.wireframe.service.recebeprompt.RecebePromptRequest"
                                .equals(parameterTypes[1].getName());
                if (!validSignature) {
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding][Wireframe] classe=" + item.getName()
                            + " método=recebePrompt deve receber exatamente String idJob e RecebePromptRequest payload";
                    events.add(SimpleConditionEvent.violated(item, message));
                }
            }
        };
    }

    /**
     * Extrai o nome da etapa do controller Backend<Etapa>Controller.
     */
    private static String extractBackendStageName(JavaClass item) {
        Matcher matcher = Pattern.compile("^Backend([A-Za-z0-9]+)Controller$").matcher(item.getSimpleName());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Verifica se o tipo de retorno é exatamente List<Record<Etapa>Pending>.
     */
    private static boolean isListOfExpectedRecord(Type returnType, String expectedRecordName) {
        if (!(returnType instanceof ParameterizedType parameterizedType)) {
            return false;
        }
        if (!List.class.equals(parameterizedType.getRawType())) {
            return false;
        }
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length != 1) {
            return false;
        }
        String actualTypeName = typeArguments[0].getTypeName();
        return actualTypeName.endsWith("." + expectedRecordName);
    }

    /**
     * Garante que classes web dependam apenas de web/service da mesma etapa geralanding.
     */
    private static ArchCondition<JavaClass> onlyDependOnWebOrServiceWithinSameStage() {
        return new ArchCondition<>("[ARQUITETURA][BACKEND][GeraLanding] web only depends on same-stage web/service") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetName = dependency.getTargetClass().getName();
                    if (!targetName.startsWith("com.marketinghub.geralanding.")) {
                        return;
                    }
                    String sourceStage = extractGeraLandingStage(item.getPackageName());
                    String targetStage = extractGeraLandingStage(dependency.getTargetClass().getPackageName());
                    String targetLayer = extractGeraLandingLayer(dependency.getTargetClass().getPackageName());
                    if (sourceStage == null) {
                        return;
                    }
                    boolean valid = sourceStage.equals(targetStage)
                            && ("web".equals(targetLayer) || "service".equals(targetLayer));
                    if (!valid) {
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: geralanding.*.web só pode acessar geralanding.*.web e geralanding.*.service da mesma etapa";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que classes provisorio dependam apenas de provisorio da mesma etapa geralanding.
     */
    private static ArchCondition<JavaClass> onlyDependOnProvisorioWithinSameStage() {
        return new ArchCondition<>("[ARQUITETURA][BACKEND][GeraLanding] provisorio only depends on same-stage provisorio") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetName = dependency.getTargetClass().getName();
                    if (!targetName.startsWith("com.marketinghub.geralanding.")) {
                        return;
                    }
                    String sourceStage = extractGeraLandingStage(item.getPackageName());
                    String targetStage = extractGeraLandingStage(dependency.getTargetClass().getPackageName());
                    String targetLayer = extractGeraLandingLayer(dependency.getTargetClass().getPackageName());
                    if (sourceStage == null) {
                        return;
                    }
                    boolean valid = sourceStage.equals(targetStage) && "provisorio".equals(targetLayer);
                    if (!valid) {
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: geralanding.*.provisorio só pode acessar geralanding.*.provisorio da mesma etapa";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que serviços geralanding acessem serviços internos da mesma etapa e classes compartilhadas permitidas.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedMarketingHubClasses() {
        return new ArchCondition<>("[ARQUITETURA] [BACKEND][GeraLanding] depend only on same-stage service packages and explicit allowed classes") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    if (isSameStageServiceDependency(item, targetClass)
                            || targetName.equals(EXPERIMENT_CLASS)
                            || targetName.equals(EXPERIMENT_REPOSITORY_CLASS)
                            || targetName.equals(GERALANDING_STAGE_EXECUTION_CLASS)
                            || targetName.equals(GERALANDING_STAGE_EXECUTION_REPOSITORY_CLASS)
                            || targetName.equals(GERALANDING_STAGE_EXECUTION_BUILDER_CLASS)) {
                        return;
                    }
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: serviços em geralanding.*.service só podem acessar "
                            + "pacotes internos geralanding.<etapa>.service.* da mesma etapa, "
                            + EXPERIMENT_CLASS + ", "
                            + EXPERIMENT_REPOSITORY_CLASS + ", "
                            + GERALANDING_STAGE_EXECUTION_CLASS + ", "
                            + GERALANDING_STAGE_EXECUTION_REPOSITORY_CLASS + " e "
                            + GERALANDING_STAGE_EXECUTION_BUILDER_CLASS;
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Verifica se a dependência alvo pertence à árvore service da mesma etapa GeraLanding.
     */
    private static boolean isSameStageServiceDependency(JavaClass sourceClass, JavaClass targetClass) {
        String sourceStage = extractGeraLandingStage(sourceClass.getPackageName());
        String targetStage = extractGeraLandingStage(targetClass.getPackageName());
        String targetLayer = extractGeraLandingLayer(targetClass.getPackageName());
        return sourceStage != null && sourceStage.equals(targetStage) && "service".equals(targetLayer);
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
        return new ArchCondition<>("[ARQUITETURA][MOIS] depend only on " + allowedTargetLayer + " in same x/vN") {
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
                        String message = "[ARQUITETURA] [MOIS] classe " + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + dependency.getTargetClass().getName() + ")"
                                + " | regra: só pode depender de pacote ." + allowedTargetLayer
                                + " com mesmo x/vN";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Extrai a etapa geralanding (subpacote após geralanding).
     */
    private static String extractGeraLandingStage(String packageName) {
        String marker = "com.marketinghub.geralanding.";
        if (!packageName.startsWith(marker)) {
            return null;
        }
        String remainder = packageName.substring(marker.length());
        int idx = remainder.indexOf('.');
        return idx > 0 ? remainder.substring(0, idx) : null;
    }

    /**
     * Extrai o layer principal da etapa geralanding (web/service/provisorio).
     */
    private static String extractGeraLandingLayer(String packageName) {
        String stage = extractGeraLandingStage(packageName);
        if (stage == null) {
            return null;
        }
        String prefix = "com.marketinghub.geralanding." + stage + ".";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        int idx = remainder.indexOf('.');
        return idx > 0 ? remainder.substring(0, idx) : remainder;
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
