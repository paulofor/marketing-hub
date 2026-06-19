package com.marketinghub.architecture;

import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.frameworkimage.FrameworkImageGenerationJobRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.marketinghub.geralanding.presetdesign.provisorio.DesignPresetProvisionalHtmlAssembler;
import com.marketinghub.geralanding.wireframe.provisorio.WireframeProvisionalHtmlAssembler;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Garante isolamento arquitetural entre módulos/pacotes internos do backend.
 */
@AnalyzeClasses(packages = "com.marketinghub")
class ArquiteturaTest {

    private static final String MOIS_SALES_LIBRARY_PACKAGE = "com.marketinghub.mois.bibliotecapaginavenda.worker.v1";
    private static final String EXPERIMENT_CLASS = "com.marketinghub.experiment.Experiment";
    private static final String EXPERIMENT_REPOSITORY_CLASS = ExperimentRepository.class.getName();
    private static final String HYPOTHESIS_REPOSITORY_CLASS = HypothesisRepository.class.getName();
    private static final String FRAMEWORK_IMAGE_GENERATION_JOB_REPOSITORY_CLASS =
            FrameworkImageGenerationJobRepository.class.getName();
    private static final String GERALANDING_STAGE_EXECUTION_CLASS = "com.marketinghub.geralanding.GeraLandingStageExecution";
    private static final String GERALANDING_STAGE_EXECUTION_REPOSITORY_CLASS =
            GeraLandingStageExecutionRepository.class.getName();
    private static final String GERALANDING_STAGE_EXECUTION_BUILDER_CLASS =
            "com.marketinghub.geralanding.GeraLandingStageExecution$GeraLandingStageExecutionBuilder";
    private static final String OPRM_ENRICHED_NICHE_MATERIALIZER_SERVICE =
            "com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.BackendEnrichedNicheMaterializerService";
    private static final String OPRM_ENRICHED_NICHE_MATERIALIZER_SERVICE_TEST =
            OPRM_ENRICHED_NICHE_MATERIALIZER_SERVICE + "Test";
    private static final List<String> ALLOWED_OPRM_ENRICHED_NICHE_MATERIALIZER_CLASSES = List.of(
            "com.marketinghub.niche.MarketNiche",
            "com.marketinghub.niche.MarketNicheEnrichmentProfile",
            "com.marketinghub.repository.jpa.niche.MarketNicheRepository",
            "com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository");
    private static final Pattern SALES_LIBRARY_LAYER_PATTERN = Pattern.compile(
            "^com\\.marketinghub\\.mois\\.bibliotecapaginavenda\\.([a-zA-Z0-9_]+)\\.(v\\d+)\\.(web|service|dto|repository)(?:\\..*)?$");
    private static final String MOIS_SALES_LIBRARY_WEB_PACKAGE = MOIS_SALES_LIBRARY_PACKAGE + ".web";
    private static final String MOIS_SALES_LIBRARY_SERVICE_PACKAGE = MOIS_SALES_LIBRARY_PACKAGE + ".service";
    private static final String MOIS_SALES_LIBRARY_DTO_PACKAGE = MOIS_SALES_LIBRARY_PACKAGE + ".dto";
    private static final String MOIS_SALES_LIBRARY_REPOSITORY_PACKAGE =
            "com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1";
    private static final Pattern BACKEND_STAGE_WEB_PACKAGE_PATTERN =
            Pattern.compile("^com\\.marketinghub\\.geralanding\\.[a-zA-Z0-9_]+\\.web$");
    private static final Pattern BACKEND_STAGE_SERVICE_PACKAGE_PATTERN =
            Pattern.compile("^com\\.marketinghub\\.geralanding\\.[a-zA-Z0-9_]+\\.service$");
    private static final String HYPOTHESIS_PAIN_STAGE_EXECUTION_REPOSITORY_CLASS =
            "com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository";
    private static final String MARKET_NICHE_REPOSITORY_CLASS =
            "com.marketinghub.repository.jpa.niche.MarketNicheRepository";
    private static final String HYPOTHESIS_CLASS = "com.marketinghub.hypothesis.Hypothesis";
    private static final String MARKET_NICHE_CLASS = "com.marketinghub.niche.MarketNiche";
    private static final List<String> ALLOWED_HYPOTHESIS_STAGE_SERVICE_REPOSITORIES = List.of(
            HYPOTHESIS_PAIN_STAGE_EXECUTION_REPOSITORY_CLASS,
            MARKET_NICHE_REPOSITORY_CLASS);
    private static final Pattern BACKEND_CONTROLLER_NAME_PATTERN = Pattern.compile("^Backend([A-Za-z0-9]+)Controller$");
    private static final Pattern BACKEND_SERVICE_NAME_PATTERN = Pattern.compile("^Backend([A-Za-z0-9]+)Service$");
    private static final Pattern EPM_CONTROLLER_PACKAGE_PATTERN = Pattern.compile("^com\\.marketinghub\\.epm\\.controller$");
    private static final Pattern EPM_SERVICE_PACKAGE_PATTERN = Pattern.compile("^com\\.marketinghub\\.epm\\.service$");
    private static final String PIPELINE_WEB_PACKAGE = "com.marketinghub.pipeline.web";
    private static final String PIPELINE_SERVICE_PACKAGE = "com.marketinghub.pipeline.service";
    private static final String SALES_VIDEO_PACKAGE = "com.marketinghub.salesvideo";
    private static final String SALES_VIDEO_CONTROLLER_PACKAGE = SALES_VIDEO_PACKAGE + ".controller";
    private static final String SALES_VIDEO_SERVICE_PACKAGE = SALES_VIDEO_PACKAGE + ".service";
    private static final String SALES_VIDEO_DTO_PACKAGE = SALES_VIDEO_PACKAGE + ".dto";
    private static final String SALES_VIDEO_REPOSITORY_PACKAGE = "com.marketinghub.repository.jpa.salesvideo";
    private static final Pattern FACEBOOK_ADS_STAGE_CONTROLLER_PACKAGE_PATTERN =
            Pattern.compile("^com\\.marketinghub\\.facebookads\\.stage\\.([a-zA-Z0-9_]+)\\.controller$");
    private static final Pattern FACEBOOK_ADS_STAGE_SERVICE_PACKAGE_PATTERN =
            Pattern.compile("^com\\.marketinghub\\.facebookads\\.stage\\.([a-zA-Z0-9_]+)\\.service$");
    private static final String FACEBOOK_ADS_STAGE_PACKAGE_PREFIX = "com.marketinghub.facebookads.stage.";
    private static final Pattern OPRM_NICHO_CNAE_STAGE_CONTROLLER_PACKAGE_PATTERN = Pattern.compile(
            "^com\\.marketinghub\\.oprm\\.nichocnae\\.([a-zA-Z0-9_]+)\\.(web|controller)$");
    private static final Pattern OPRM_NICHO_CNAE_STAGE_SERVICE_PACKAGE_PATTERN = Pattern.compile(
            "^com\\.marketinghub\\.oprm\\.nichocnae\\.([a-zA-Z0-9_]+)\\.service$");
    private static final String OPRM_NICHO_CNAE_STAGE_PACKAGE_PREFIX = "com.marketinghub.oprm.nichocnae.";
    private static final String OPRM_CNAE_PACKAGE = "com.marketinghub.oprm.cnae";
    private static final String OPRM_CNAE_WEB_PACKAGE = OPRM_CNAE_PACKAGE + ".web";
    private static final String OPRM_CNAE_SERVICE_PACKAGE = OPRM_CNAE_PACKAGE + ".service";
    private static final String OPRM_CNAE_DTO_PACKAGE = OPRM_CNAE_PACKAGE + ".dto";
    private static final List<String> REQUIRED_BACKEND_SERVICE_SUBPACKAGES = List.of(
            "detailStageExecution", "listStageExecutions", "pending", "recebePrompt", "recebeResposta");
    private static final List<String> REQUIRED_EPM_SERVICE_SUBPACKAGES = List.of(
            "createExperimentBudget",
            "createExperimentDecision",
            "createExperimentMetric",
            "createFinancialPlan",
            "createPlanHypothesis",
            "createPlanNiche",
            "createProductPriceScenario",
            "getExperimentBudget",
            "getFinancialPlan",
            "getFinancialPlanSummary",
            "getLatestExperimentMetric",
            "getPlanHypothesis",
            "getPlanNiche",
            "listExperimentBudgets",
            "listExperimentDecisions",
            "listFinancialPlans",
            "listPlanHypotheses",
            "listPlanNiches",
            "listProductPriceScenarios",
            "updateExperimentBudget",
            "updateFinancialPlan");
    private static final List<String> REQUIRED_BACKEND_CONTROLLER_METHODS = List.of(
            "start", "listStageExecutions", "pending", "recebePrompt", "recebeResposta", "detailStageExecution");
    private static final List<String> ALLOWED_GERALANDING_SERVICE_REPOSITORIES = List.of(
            EXPERIMENT_REPOSITORY_CLASS,
            HYPOTHESIS_REPOSITORY_CLASS,
            FRAMEWORK_IMAGE_GENERATION_JOB_REPOSITORY_CLASS,
            GERALANDING_STAGE_EXECUTION_REPOSITORY_CLASS);

    @ArchTest
    static final ArchRule moisMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.mois..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept(
                    "com.marketinghub.mois", "com.marketinghub.repository.jpa.mois"))
            .because("[ARQUITETURA][MOIS] o módulo MOIS não deve depender de outros pacotes internos do sistema");

    @ArchTest
    static final ArchRule oprmMustNotDependOnOtherMarketingHubPackages = classes()
            .that()
            .resideInAPackage("com.marketinghub.oprm..")
            .should(onlyDependOnAllowedOprmMarketingHubClasses())
            .because("[ARQUITETURA][OPRM] o módulo OPRM não deve depender de outros pacotes internos do sistema");

    @ArchTest
    static final ArchRule epmMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.epm..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept(
                    "com.marketinghub.epm", "com.marketinghub.repository.jpa.epm"))
            .because("[ARQUITETURA] [EPM] o módulo EPM não deve depender de outros pacotes internos além dos seus repositories canônicos");

    @ArchTest
    static final ArchRule otherPackagesMustNotDependOnFacebookAdsControllers = noClasses()
            .that()
            .resideInAPackage("com.marketinghub..")
            .and()
            .haveNameNotMatching(".*Test")
            .and()
            .resideOutsideOfPackage("com.marketinghub.facebookads.controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub.facebookads.controller..")
            .because("[ARQUITETURA] [BACKEND][FacebookAds] controllers de Facebook Ads são borda HTTP "
                    + "do próprio pacote e não devem ser consumidos por outros pacotes");

    @ArchTest
    static final ArchRule facebookAdsControllersMustNotDependOnOtherControllers = classes()
            .that()
            .resideInAPackage("com.marketinghub.facebookads.controller..")
            .should(notDependOnOtherModuleControllers())
            .because("[ARQUITETURA] [BACKEND][FacebookAds] controllers de Facebook Ads não devem depender "
                    + "de controllers de outros módulos");

    @ArchTest
    static final ArchRule facebookAdsMustNotDependOnUnapprovedMarketingHubPackages = classes()
            .that()
            .resideInAPackage("com.marketinghub.facebookads..")
            .should(onlyDependOnAllowedFacebookAdsMarketingHubClasses())
            .because("[ARQUITETURA] [BACKEND][FacebookAds] o pacote Facebook Ads só deve depender "
                    + "dos contratos internos aprovados para publicação, mensuração e segmentação de campanhas");

    @ArchTest
    static final ArchRule facebookAdsServicesMustNotDependOnControllers = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.facebookads..service..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.marketinghub..controller..")
            .because("[ARQUITETURA] [BACKEND][FacebookAds] services de Facebook Ads não devem depender "
                    + "de controllers; regra de negócio deve ficar abaixo da borda HTTP");

    @ArchTest
    static final ArchRule onlyApprovedPackagesMustDependOnFacebookAdsImplementation = classes()
            .that()
            .resideInAPackage("com.marketinghub..")
            .should(onlyApprovedPackagesDependOnFacebookAdsClasses())
            .because("[ARQUITETURA] [BACKEND][FacebookAds] somente o próprio pacote, experimento e "
                    + "repositories canônicos de experimento/Facebook Ads podem depender das classes de Facebook Ads");

    @ArchTest
    static final ArchRule facebookAdsStageControllersShouldOnlyDependOnSameStageControllerOrService =
            classes().that().resideInAPackage("com.marketinghub.facebookads.stage.*.controller..")
                    .should(onlyDependOnFacebookAdsControllerOrServiceWithinSameStage())
                    .allowEmptyShould(true)
                    .because("[ARQUITETURA] [BACKEND][FacebookAds] controller em facebookads.stage.<etapa>.controller "
                            + "só pode acessar controller/service da mesma etapa");

    @ArchTest
    static final ArchRule facebookAdsStageServicesShouldOnlyDependOnSameStageServiceOrApprovedClasses =
            classes().that().resideInAPackage("com.marketinghub.facebookads.stage.*.service..")
                    .should(onlyDependOnAllowedFacebookAdsStageServiceClasses())
                    .allowEmptyShould(true)
                    .because("[ARQUITETURA] [BACKEND][FacebookAds] service em facebookads.stage.<etapa>.service "
                            + "só pode acessar service/DTOs da mesma etapa e contratos aprovados de Facebook Ads");

    @ArchTest
    static final ArchRule moisSalesLibraryPackageMustNotDependOnOtherMarketingHubPackages = noClasses()
            .that()
            .resideInAPackage(MOIS_SALES_LIBRARY_PACKAGE + "..")
            .should()
            .dependOnClassesThat(otherMarketingHubPackagesExcept(
                    MOIS_SALES_LIBRARY_PACKAGE, "com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1"))
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
    static final ArchRule springDataJpaRepositoriesMustResideInRepositoryJpaSubpackages = classes()
            .that()
            .areAssignableTo(JpaRepository.class)
            .should(resideInRepositoryJpaSubpackage())
            .because("[ARQUITETURA] [BACKEND][Repository] todo repository Spring Data JPA deve ficar em subpacote de com.marketinghub.repository.jpa");

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
    static final ArchRule hypothesisControllerPackagesShouldOnlyDependOnControllerOrServiceInSameStage =
            classes().that().resideInAPackage("com.marketinghub.hypothesis.*.controller..")
                    .should(onlyDependOnHypothesisControllerOrServiceWithinSameStage())
                    .because("[ARQUITETURA] [BACKEND][Hypothesis] controller em hypothesis.*.controller só pode acessar classes de hypothesis.*.controller e hypothesis.*.service da mesma etapa");

    @ArchTest
    static final ArchRule hypothesisProvisorioPackagesShouldOnlyDependOnOwnProvisorioPackage =
            classes().that().resideInAPackage("com.marketinghub.hypothesis.*.provisorio..")
                    .should(onlyDependOnHypothesisProvisorioWithinSameStage())
                    .allowEmptyShould(true)
                    .because("[ARQUITETURA] [BACKEND][Hypothesis] provisorio em hypothesis.*.provisorio só pode acessar classes do próprio pacote hypothesis.*.provisorio");

    @ArchTest
    static final ArchRule hypothesisServicePackagesShouldOnlyAccessAllowedMarketingHubClasses =
            classes().that().resideInAPackage("com.marketinghub.hypothesis.*.service..")
                    .should(onlyDependOnAllowedHypothesisStageMarketingHubClasses())
                    .because("[ARQUITETURA] [BACKEND][Hypothesis] serviços em hypothesis.*.service só podem acessar classes permitidas dentro de com.marketinghub");

    @ArchTest
    static final ArchRule epmControllerPackagesShouldOnlyDependOnControllerOrService =
            classes().that().resideInAPackage("com.marketinghub.epm.controller..")
                    .should(onlyDependOnEpmControllerOrService())
                    .because("[ARQUITETURA] [BACKEND][EPM] controller em epm.controller só pode acessar epm.controller e epm.service");

    @ArchTest
    static final ArchRule epmServicePackagesShouldOnlyAccessAllowedMarketingHubClasses =
            classes().that().resideInAPackage("com.marketinghub.epm..service..")
                    .should(onlyDependOnAllowedEpmMarketingHubClasses())
                    .because("[ARQUITETURA] [BACKEND][EPM] service em epm.service só pode acessar domínio EPM, subpacotes service e repositories EPM");

    @ArchTest
    static final ArchRule pipelineWebPackagesShouldOnlyDependOnPipelineContractsAndServices =
            classes().that().resideInAPackage("com.marketinghub.pipeline.web..")
                    .should(onlyDependOnAllowedPipelineWebMarketingHubClasses())
                    .because("[ARQUITETURA] [BACKEND][Pipeline] web em pipeline.web só pode acessar domínio, DTOs, mapper e services do próprio pacote pipeline");

    @ArchTest
    static final ArchRule pipelineServicePackagesShouldOnlyAccessAllowedMarketingHubClasses =
            classes().that().resideInAPackage("com.marketinghub.pipeline.service..")
                    .should(onlyDependOnAllowedPipelineServiceMarketingHubClasses())
                    .because("[ARQUITETURA] [BACKEND][Pipeline] service em pipeline.service só pode acessar pipeline, repositories canônicos de pipeline e exceção explícita do modelo OpenAI");

    @ArchTest
    static final ArchRule salesVideoControllersShouldOnlyAccessModuleContractsAndFacade =
            classes().that().resideInAPackage("com.marketinghub.salesvideo.controller..")
                    .should(onlyDependOnAllowedSalesVideoControllerMarketingHubClasses())
                    .because("[ARQUITETURA] [BACKEND][SalesVideo] controller deve acessar somente contratos do módulo, fachada de service e contratos de mídia necessários");

    @ArchTest
    static final ArchRule salesVideoServicesShouldOnlyAccessAllowedMarketingHubClasses =
            classes().that().resideInAPackage("com.marketinghub.salesvideo.service..")
                    .should(onlyDependOnAllowedSalesVideoServiceMarketingHubClasses())
                    .because("[ARQUITETURA] [BACKEND][SalesVideo] services devem acessar somente domínio do módulo, DTOs, serviços internos e repositories canônicos externos");

    @ArchTest
    static final ArchRule salesVideoDtosShouldOnlyDependOnSalesVideoContracts =
            classes().that().resideInAPackage("com.marketinghub.salesvideo.dto..")
                    .should(onlyDependOnSalesVideoDtoContracts())
                    .because("[ARQUITETURA] [BACKEND][SalesVideo] DTOs devem permanecer contratos simples, dependentes apenas de DTOs e tipos de domínio do próprio SalesVideo");

    @ArchTest
    static final ArchRule salesVideoFunctionalPackageMustNotContainRepositories = noClasses()
            .that()
            .resideInAPackage("com.marketinghub.salesvideo..")
            .should()
            .beAssignableTo(JpaRepository.class)
            .because("[ARQUITETURA] [BACKEND][SalesVideo] repositories devem ficar fora do pacote funcional, em com.marketinghub.repository.jpa.salesvideo");

    @ArchTest
    static final ArchRule salesVideoRepositoriesShouldOnlyAccessSalesVideoDomain =
            classes().that().resideInAPackage("com.marketinghub.repository.jpa.salesvideo..")
                    .should(onlyDependOnSalesVideoRepositoryContracts())
                    .because("[ARQUITETURA] [BACKEND][SalesVideo] repositories externos de SalesVideo devem acessar apenas domínio SalesVideo e contratos do próprio pacote repository");

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
    static final ArchRule backendStageControllersMustExposeOnlyCanonicalMethodsAndDelegateToService =
            classes().that().resideInAPackage("com.marketinghub.geralanding.*.web..")
                    .and().haveNameMatching(".*\\.Backend[A-Za-z0-9]+Controller")
                    .should(haveOnlyCanonicalControllerMethodsCallingService())
                    .because("[ARQUITETURA] [BACKEND][GeraLanding] Backend<Etapa>Controller deve expor exatamente start, listStageExecutions, pending, recebePrompt, recebeResposta e detailStageExecution, sempre delegando ao Backend<Etapa>Service");


    /**
     * Garante que o módulo SalesVideo tenha uma única borda HTTP no pacote controller.
     */
    @ArchTest
    static void salesVideoControllerPackageMustHaveSingleAnnotatedController(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> controllerPackageClasses = directPackageClasses(importedClasses, SALES_VIDEO_CONTROLLER_PACKAGE);
        List<JavaClass> controllers = controllerPackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("SalesVideoController"))
                .toList();
        if (controllerPackageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][SalesVideo] pacote=" + SALES_VIDEO_CONTROLLER_PACKAGE
                    + " deve conter apenas SalesVideoController; classes=" + simpleNames(controllerPackageClasses));
        }
        if (controllers.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][SalesVideo] pacote=" + SALES_VIDEO_CONTROLLER_PACKAGE
                    + " deve conter exatamente uma classe SalesVideoController; controllers=" + simpleNames(controllers));
        } else if (!controllers.get(0).isAnnotatedWith(RestController.class)) {
            violations.add("[ARQUITETURA] [BACKEND][SalesVideo] classe=" + controllers.get(0).getName()
                    + " deve possuir @RestController");
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que o módulo SalesVideo tenha somente a fachada principal anotada como @Service.
     */
    @ArchTest
    static void salesVideoServicePackageMustHaveSingleServiceFacade(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> servicePackageClasses = directPackageClasses(importedClasses, SALES_VIDEO_SERVICE_PACKAGE);
        List<JavaClass> facades = servicePackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("SalesVideoService"))
                .toList();
        long annotatedServices = servicePackageClasses.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(Service.class))
                .count();
        if (facades.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][SalesVideo] pacote=" + SALES_VIDEO_SERVICE_PACKAGE
                    + " deve conter exatamente uma fachada SalesVideoService; facades=" + simpleNames(facades));
        } else if (!facades.get(0).isAnnotatedWith(Service.class)) {
            violations.add("[ARQUITETURA] [BACKEND][SalesVideo] classe=" + facades.get(0).getName()
                    + " deve possuir @Service para marcar a fachada única do módulo");
        }
        if (annotatedServices != 1) {
            violations.add("[ARQUITETURA] [BACKEND][SalesVideo] pacote=" + SALES_VIDEO_SERVICE_PACKAGE
                    + " deve possuir exatamente um @Service principal; quantidade=" + annotatedServices);
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que os repositories externos de SalesVideo estejam no pacote canônico e sejam interfaces JPA.
     */
    @ArchTest
    static void salesVideoRepositoriesMustStayInExternalRepositoryPackage(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> repositoryClasses = directPackageClasses(importedClasses, SALES_VIDEO_REPOSITORY_PACKAGE);
        if (repositoryClasses.isEmpty()) {
            violations.add("[ARQUITETURA] [BACKEND][SalesVideo] pacote=" + SALES_VIDEO_REPOSITORY_PACKAGE
                    + " deve conter os repositories externos do módulo");
        }
        repositoryClasses.stream()
                .filter(javaClass -> !javaClass.isAssignableTo(JpaRepository.class))
                .forEach(javaClass -> violations.add("[ARQUITETURA] [BACKEND][SalesVideo] classe="
                        + javaClass.getName() + " deve estender JpaRepository no pacote externo "
                        + SALES_VIDEO_REPOSITORY_PACKAGE));
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que cada pacote web backend de etapa tenha somente o controller canônico anotado.
     */
    @ArchTest
    static void backendStageWebPackagesMustHaveSingleAnnotatedBackendController(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        backendStageWebPackages(importedClasses).forEach((packageName, packageClasses) -> {
            List<JavaClass> controllers = packageClasses.stream()
                    .filter(ArquiteturaTest::isBackendControllerClass)
                    .toList();
            if (controllers.isEmpty()) {
                return;
            }
            if (packageClasses.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][GeraLanding] pacote=" + packageName
                        + " deve conter apenas uma classe Backend<Etapa>Controller; classes="
                        + simpleNames(packageClasses));
            }
            if (controllers.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][GeraLanding] pacote=" + packageName
                        + " deve conter exatamente uma classe Backend<Etapa>Controller; controllers="
                        + simpleNames(controllers));
                return;
            }
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][GeraLanding] classe=" + controller.getName()
                        + " deve possuir @RestController");
            }
            if (!hasRequestMappingApi(controller)) {
                violations.add("[ARQUITETURA] [BACKEND][GeraLanding] classe=" + controller.getName()
                        + " deve possuir @RequestMapping(\"/api\")");
            }
        });
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que cada pacote service backend de etapa tenha o serviço canônico anotado e subpacotes obrigatórios.
     */
    @ArchTest
    static void backendStageServicePackagesMustHaveCanonicalServiceAndRequiredSubpackages(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        backendStageServicePackages(importedClasses).forEach((packageName, packageClasses) -> {
            List<JavaClass> services = packageClasses.stream()
                    .filter(ArquiteturaTest::isBackendServiceClass)
                    .toList();
            if (services.isEmpty()) {
                return;
            }
            if (services.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][GeraLanding] pacote=" + packageName
                        + " deve conter exatamente uma classe Backend<Etapa>Service; services="
                        + simpleNames(services));
                return;
            }
            JavaClass serviceClass = services.get(0);
            if (!serviceClass.isAnnotatedWith(Service.class)) {
                violations.add("[ARQUITETURA] [BACKEND][GeraLanding] classe=" + serviceClass.getName()
                        + " deve possuir @Service");
            }
            REQUIRED_BACKEND_SERVICE_SUBPACKAGES.forEach(requiredSubpackage -> {
                String requiredPackage = packageName + "." + requiredSubpackage;
                boolean exists = importedClasses.stream()
                        .filter(ArquiteturaTest::isProductionTopLevelClass)
                        .anyMatch(candidate -> candidate.getPackageName().equals(requiredPackage));
                if (!exists) {
                    violations.add("[ARQUITETURA] [BACKEND][GeraLanding] pacote=" + packageName
                            + " deve possuir subpacote obrigatório " + requiredSubpackage);
                }
            });
        });
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que os subpacotes obrigatórios de service contenham apenas records.
     */
    @ArchTest
    static void backendStageServiceRequiredSubpackagesMustContainOnlyRecords(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        backendStageServicePackages(importedClasses).keySet().forEach(servicePackage ->
                REQUIRED_BACKEND_SERVICE_SUBPACKAGES.forEach(requiredSubpackage -> {
                    String requiredPackage = servicePackage + "." + requiredSubpackage;
                    importedClasses.stream()
                            .filter(ArquiteturaTest::isProductionTopLevelClass)
                            .filter(candidate -> candidate.getPackageName().equals(requiredPackage))
                            .filter(candidate -> !candidate.reflect().isRecord())
                            .forEach(candidate -> violations.add(
                                    "[ARQUITETURA] [BACKEND][GeraLanding] classe=" + candidate.getName()
                                            + " está no subpacote obrigatório " + requiredSubpackage
                                            + " e deve ser declarada como record"));
                }));
        failWithArchitectureViolations(violations);
    }


    /**
     * Garante que o módulo OPRM CNAE tenha uma única borda HTTP canônica.
     */
    @ArchTest
    static void oprmCnaePackageMustHaveSingleCanonicalController(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> webPackageClasses = directPackageClasses(importedClasses, OPRM_CNAE_WEB_PACKAGE);
        List<JavaClass> controllers = webPackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("OprmCnaeOpportunityController"))
                .toList();
        if (webPackageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] pacote=" + OPRM_CNAE_WEB_PACKAGE
                    + " deve conter apenas OprmCnaeOpportunityController; classes=" + simpleNames(webPackageClasses));
        }
        if (controllers.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] pacote=" + OPRM_CNAE_WEB_PACKAGE
                    + " deve conter exatamente uma classe OprmCnaeOpportunityController; controllers="
                    + simpleNames(controllers));
        } else {
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] classe=" + controller.getName()
                        + " deve possuir @RestController");
            }
            if (!hasRequestMapping(controller, "/api/oprm")) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] classe=" + controller.getName()
                        + " deve possuir @RequestMapping(\"/api/oprm\")");
            }
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que o módulo OPRM CNAE tenha uma única fachada de service canônica.
     */
    @ArchTest
    static void oprmCnaePackageMustHaveSingleCanonicalService(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> servicePackageClasses = directPackageClasses(importedClasses, OPRM_CNAE_SERVICE_PACKAGE);
        List<JavaClass> services = servicePackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("OprmCnaeOpportunityPersistenceService"))
                .toList();
        if (servicePackageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] pacote=" + OPRM_CNAE_SERVICE_PACKAGE
                    + " deve conter apenas OprmCnaeOpportunityPersistenceService na raiz; classes="
                    + simpleNames(servicePackageClasses));
        }
        if (services.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] pacote=" + OPRM_CNAE_SERVICE_PACKAGE
                    + " deve conter exatamente uma classe OprmCnaeOpportunityPersistenceService; services="
                    + simpleNames(services));
        } else if (!services.get(0).isAnnotatedWith(Service.class)) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] classe=" + services.get(0).getName()
                    + " deve possuir @Service");
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que contratos HTTP do módulo OPRM CNAE permaneçam imutáveis.
     */
    @ArchTest
    static void oprmCnaeDtosMustBeRecords(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        directPackageClasses(importedClasses, OPRM_CNAE_DTO_PACKAGE).stream()
                .filter(javaClass -> !javaClass.reflect().isRecord())
                .forEach(javaClass -> violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] classe="
                        + javaClass.getName() + " deve ser declarada como record por representar contrato HTTP"));
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que o pacote funcional OPRM CNAE não contenha repositories ou gateways de banco.
     */
    @ArchTest
    static void oprmCnaeFunctionalPackageMustNotContainRepositories(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> javaClass.getPackageName().startsWith(OPRM_CNAE_PACKAGE))
                .filter(javaClass -> javaClass.getPackageName().contains(".repository"))
                .forEach(javaClass -> violations.add("[ARQUITETURA] [BACKEND][OPRM][CNAE] classe="
                        + javaClass.getName()
                        + " não deve ficar no pacote funcional; use com.marketinghub.repository.*"));
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que a etapa inicial do pipeline NichoCNAE v2 tenha controller canônico com endpoint pending.
     */
    @ArchTest
    static void oprmNichoCnaeV2CandidateGeneratorMustExposeCanonicalPendingEndpoint(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        String controllerPackage = "com.marketinghub.oprm.nichocnae.v2.candidategenerator.controller";
        List<JavaClass> packageClasses = directPackageClasses(importedClasses, controllerPackage);
        List<JavaClass> controllers = packageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("BackendCandidateGeneratorController"))
                .toList();
        if (packageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] pacote=" + controllerPackage
                    + " deve conter apenas BackendCandidateGeneratorController; classes=" + simpleNames(packageClasses));
        }
        if (controllers.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] pacote=" + controllerPackage
                    + " deve conter exatamente uma classe BackendCandidateGeneratorController; controllers="
                    + simpleNames(controllers));
        } else {
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] classe=" + controller.getName()
                        + " deve possuir @RestController");
            }
            if (!hasRequestMapping(controller,
                    "/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions")) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] classe=" + controller.getName()
                        + " deve possuir @RequestMapping do endpoint interno pending canônico");
            }
            boolean hasPending = controller.getMethods().stream()
                    .anyMatch(method -> method.getName().equals("pending"));
            if (!hasPending) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] classe=" + controller.getName()
                        + " deve expor método pending como ponto inicial canônico do executor");
            }
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que a etapa inicial do pipeline NichoCNAE v2 tenha service canônico único.
     */
    @ArchTest
    static void oprmNichoCnaeV2CandidateGeneratorMustHaveCanonicalService(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        String servicePackage = "com.marketinghub.oprm.nichocnae.v2.candidategenerator.service";
        List<JavaClass> packageClasses = directPackageClasses(importedClasses, servicePackage);
        List<JavaClass> services = packageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("BackendCandidateGeneratorService"))
                .toList();
        if (packageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] pacote=" + servicePackage
                    + " deve conter apenas BackendCandidateGeneratorService na raiz; classes="
                    + simpleNames(packageClasses));
        }
        if (services.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] pacote=" + servicePackage
                    + " deve conter exatamente uma classe BackendCandidateGeneratorService; services="
                    + simpleNames(services));
        } else if (!services.get(0).isAnnotatedWith(Service.class)) {
            violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] classe=" + services.get(0).getName()
                    + " deve possuir @Service");
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que contratos de service da etapa NichoCNAE v2 sejam records imutáveis.
     */
    @ArchTest
    static void oprmNichoCnaeV2CandidateGeneratorServiceContractsMustBeRecords(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> javaClass.getPackageName()
                        .startsWith("com.marketinghub.oprm.nichocnae.v2.candidategenerator.service."))
                .filter(javaClass -> !javaClass.reflect().isRecord())
                .forEach(javaClass -> violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE-v2] classe="
                        + javaClass.getName() + " está em subpacote de service e deve ser record"));
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que cada etapa do OPRM nicho CNAE tenha um único controller canônico anotado.
     */
    @ArchTest
    static void oprmNichoCnaeStageControllerPackagesMustHaveSingleCanonicalController(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        oprmNichoCnaeStageControllerPackages(importedClasses).forEach((packageName, packageClasses) -> {
            List<JavaClass> controllers = packageClasses.stream()
                    .filter(ArquiteturaTest::isBackendControllerClass)
                    .toList();
            if (packageClasses.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE] pacote=" + packageName
                        + " deve conter apenas uma classe Backend<Etapa>Controller; classes="
                        + simpleNames(packageClasses));
            }
            if (controllers.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE] pacote=" + packageName
                        + " deve conter exatamente uma classe canônica Backend<Etapa>Controller; controllers="
                        + simpleNames(controllers));
                return;
            }
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE] classe=" + controller.getName()
                        + " deve possuir @RestController");
            }
        });
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que cada etapa do OPRM nicho CNAE tenha um service backend canônico anotado.
     */
    @ArchTest
    static void oprmNichoCnaeStageServicePackagesMustHaveCanonicalBackendService(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        oprmNichoCnaeStageServicePackages(importedClasses).forEach((packageName, packageClasses) -> {
            List<JavaClass> services = packageClasses.stream()
                    .filter(ArquiteturaTest::isCanonicalOprmNichoCnaeBackendServiceClass)
                    .toList();
            if (services.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE] pacote=" + packageName
                        + " deve conter exatamente uma classe canônica Backend<Etapa>Service; services="
                        + simpleNames(services));
                return;
            }
            if (!services.get(0).isAnnotatedWith(Service.class)) {
                violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE] classe=" + services.get(0).getName()
                        + " deve possuir @Service");
            }
        });
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que contratos de etapas OPRM nicho CNAE em subpacotes de service sejam records.
     */
    @ArchTest
    static void oprmNichoCnaeStageServiceSubpackagesMustContainOnlyRecords(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(ArquiteturaTest::isOprmNichoCnaeStageServiceSubpackageClass)
                .filter(candidate -> !candidate.reflect().isRecord())
                .forEach(candidate -> violations.add("[ARQUITETURA] [BACKEND][OPRM][NichoCNAE] classe="
                        + candidate.getName()
                        + " está em subpacote de service de etapa OPRM nicho CNAE e deve ser declarada como record"));
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que cada etapa nova de Facebook Ads tenha somente um controller canônico.
     */
    @ArchTest
    static void facebookAdsStageControllerPackagesMustHaveSingleCanonicalController(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        facebookAdsStageControllerPackages(importedClasses).forEach((packageName, packageClasses) -> {
            String stage = extractFacebookAdsStage(packageName);
            String expectedControllerName = canonicalFacebookAdsStageTypeName(stage, "Controller");
            List<JavaClass> controllers = packageClasses.stream()
                    .filter(javaClass -> javaClass.getSimpleName().equals(expectedControllerName))
                    .toList();
            if (packageClasses.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][FacebookAds] pacote=" + packageName
                        + " deve conter apenas " + expectedControllerName + "; classes="
                        + simpleNames(packageClasses));
            }
            if (controllers.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][FacebookAds] pacote=" + packageName
                        + " deve conter exatamente uma classe canônica " + expectedControllerName
                        + "; controllers=" + simpleNames(controllers));
                return;
            }
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][FacebookAds] classe=" + controller.getName()
                        + " deve possuir @RestController");
            }
            if (!hasRequestMapping(controller, "/api/facebook-ads/" + stage)) {
                violations.add("[ARQUITETURA] [BACKEND][FacebookAds] classe=" + controller.getName()
                        + " deve possuir @RequestMapping(\"/api/facebook-ads/" + stage + "\")");
            }
        });
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que cada etapa nova de Facebook Ads tenha somente um service canônico.
     */
    @ArchTest
    static void facebookAdsStageServicePackagesMustHaveSingleCanonicalService(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        facebookAdsStageServicePackages(importedClasses).forEach((packageName, packageClasses) -> {
            String stage = extractFacebookAdsStage(packageName);
            String expectedServiceName = canonicalFacebookAdsStageTypeName(stage, "Service");
            List<JavaClass> services = packageClasses.stream()
                    .filter(javaClass -> javaClass.getSimpleName().equals(expectedServiceName))
                    .toList();
            if (packageClasses.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][FacebookAds] pacote=" + packageName
                        + " deve conter apenas " + expectedServiceName + " na raiz; classes="
                        + simpleNames(packageClasses));
            }
            if (services.size() != 1) {
                violations.add("[ARQUITETURA] [BACKEND][FacebookAds] pacote=" + packageName
                        + " deve conter exatamente uma classe canônica " + expectedServiceName
                        + "; services=" + simpleNames(services));
                return;
            }
            if (!services.get(0).isAnnotatedWith(Service.class)) {
                violations.add("[ARQUITETURA] [BACKEND][FacebookAds] classe=" + services.get(0).getName()
                        + " deve possuir @Service");
            }
        });
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que DTOs de etapas novas de Facebook Ads fiquem em subpacotes de service e sejam records.
     */
    @ArchTest
    static void facebookAdsStageServiceSubpackagesMustContainOnlyRecords(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(ArquiteturaTest::isFacebookAdsStageServiceSubpackageClass)
                .filter(candidate -> !candidate.reflect().isRecord())
                .forEach(candidate -> violations.add("[ARQUITETURA] [BACKEND][FacebookAds] classe="
                        + candidate.getName()
                        + " está em subpacote de service de uma etapa Facebook Ads e deve ser declarada como record"));
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que o pacote pipeline possua a borda HTTP e o service principal no padrão backend.
     */
    @ArchTest
    static void pipelinePackageMustHaveSingleCanonicalControllerAndService(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> webPackageClasses = pipelineDirectPackageClasses(importedClasses, PIPELINE_WEB_PACKAGE);
        List<JavaClass> controllers = webPackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("PipelineController"))
                .toList();
        if (webPackageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][Pipeline] pacote=" + PIPELINE_WEB_PACKAGE
                    + " deve conter apenas PipelineController; classes=" + simpleNames(webPackageClasses));
        }
        if (controllers.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][Pipeline] pacote=" + PIPELINE_WEB_PACKAGE
                    + " deve conter exatamente uma classe PipelineController; controllers=" + simpleNames(controllers));
        } else {
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][Pipeline] classe=" + controller.getName()
                        + " deve possuir @RestController");
            }
            if (!hasRequestMapping(controller, "/api/pipelines")) {
                violations.add("[ARQUITETURA] [BACKEND][Pipeline] classe=" + controller.getName()
                        + " deve possuir @RequestMapping(\"/api/pipelines\")");
            }
        }

        List<JavaClass> servicePackageClasses = pipelineDirectPackageClasses(importedClasses, PIPELINE_SERVICE_PACKAGE);
        List<JavaClass> services = servicePackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("PipelineService"))
                .toList();
        long annotatedServices = servicePackageClasses.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(Service.class))
                .count();
        if (services.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][Pipeline] pacote=" + PIPELINE_SERVICE_PACKAGE
                    + " deve conter exatamente uma classe PipelineService; services=" + simpleNames(services));
        } else if (!services.get(0).isAnnotatedWith(Service.class)) {
            violations.add("[ARQUITETURA] [BACKEND][Pipeline] classe=" + services.get(0).getName()
                    + " deve possuir @Service");
        }
        if (annotatedServices != 1) {
            violations.add("[ARQUITETURA] [BACKEND][Pipeline] pacote=" + PIPELINE_SERVICE_PACKAGE
                    + " deve possuir exatamente um @Service principal; quantidade=" + annotatedServices);
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que o pacote controller do EPM tenha somente o controller canônico anotado.
     */
    @ArchTest
    static void epmControllerPackageMustHaveSingleAnnotatedController(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> controllerPackageClasses = epmControllerPackageClasses(importedClasses);
        List<JavaClass> controllers = controllerPackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("EpmController"))
                .toList();
        if (controllerPackageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][EPM] pacote=com.marketinghub.epm.controller "
                    + "deve conter apenas EpmController; classes=" + simpleNames(controllerPackageClasses));
        }
        if (controllers.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][EPM] pacote=com.marketinghub.epm.controller "
                    + "deve conter exatamente uma classe EpmController; controllers=" + simpleNames(controllers));
        } else {
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][EPM] classe=" + controller.getName()
                        + " deve possuir @RestController");
            }
            if (!hasRequestMapping(controller, "/api/epm")) {
                violations.add("[ARQUITETURA] [BACKEND][EPM] classe=" + controller.getName()
                        + " deve possuir @RequestMapping(\"/api/epm\")");
            }
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que o pacote service do EPM tenha somente o service canônico e os subpacotes por operação.
     */
    @ArchTest
    static void epmServicePackageMustHaveCanonicalServiceAndOperationSubpackages(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> servicePackageClasses = epmServicePackageClasses(importedClasses);
        List<JavaClass> services = servicePackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("EpmService"))
                .toList();
        if (servicePackageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][EPM] pacote=com.marketinghub.epm.service "
                    + "deve conter apenas EpmService na raiz; classes=" + simpleNames(servicePackageClasses));
        }
        if (services.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][EPM] pacote=com.marketinghub.epm.service "
                    + "deve conter exatamente uma classe EpmService; services=" + simpleNames(services));
        } else if (!services.get(0).isAnnotatedWith(Service.class)) {
            violations.add("[ARQUITETURA] [BACKEND][EPM] classe=" + services.get(0).getName()
                    + " deve possuir @Service");
        }
        REQUIRED_EPM_SERVICE_SUBPACKAGES.forEach(requiredSubpackage -> {
            String requiredPackage = "com.marketinghub.epm.service." + requiredSubpackage;
            boolean exists = importedClasses.stream()
                    .filter(ArquiteturaTest::isProductionTopLevelClass)
                    .anyMatch(candidate -> candidate.getPackageName().equals(requiredPackage));
            if (!exists) {
                violations.add("[ARQUITETURA] [BACKEND][EPM] pacote=com.marketinghub.epm.service "
                        + "deve possuir subpacote de operação " + requiredSubpackage);
            }
        });
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que os subpacotes de operação do service EPM contenham apenas records e nomes canônicos.
     */
    @ArchTest
    static void epmServiceOperationSubpackagesMustBeCanonicalAndContainOnlyRecords(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(candidate -> candidate.getPackageName().startsWith("com.marketinghub.epm.service."))
                .forEach(candidate -> {
                    String subpackage = extractDirectSubpackage(candidate.getPackageName(), "com.marketinghub.epm.service.");
                    if (!REQUIRED_EPM_SERVICE_SUBPACKAGES.contains(subpackage)) {
                        violations.add("[ARQUITETURA] [BACKEND][EPM] classe=" + candidate.getName()
                                + " está em subpacote de service não canônico " + subpackage
                                + "; use um dos subpacotes por operação " + REQUIRED_EPM_SERVICE_SUBPACKAGES);
                    }
                    if (!candidate.reflect().isRecord()) {
                        violations.add("[ARQUITETURA] [BACKEND][EPM] classe=" + candidate.getName()
                                + " está em subpacote de operação do service EPM e deve ser declarada como record");
                    }
                });
        failWithArchitectureViolations(violations);
    }

    @ArchTest
    static final ArchRule moisSalesLibraryWebShouldOnlyDependOnServiceLayer =
            classes().that(classesBelongingToLayer("web")).should(onlyDependOnLayer("service"));

    @ArchTest
    static final ArchRule moisSalesLibraryServiceShouldOnlyDependOnRepositoryLayer =
            classes().that(classesBelongingToLayer("service")).should(onlyDependOnLayer("repository"));




    /**
     * Garante que a Biblioteca de Páginas de Vendas do MOIS tenha controller único e canônico.
     */
    @ArchTest
    static void moisSalesLibraryPackageMustHaveSingleCanonicalController(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> webPackageClasses = directPackageClasses(importedClasses, MOIS_SALES_LIBRARY_WEB_PACKAGE);
        List<JavaClass> controllers = webPackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("MoisSalesLibraryController"))
                .toList();
        if (webPackageClasses.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] pacote="
                    + MOIS_SALES_LIBRARY_WEB_PACKAGE
                    + " deve conter apenas MoisSalesLibraryController; classes=" + simpleNames(webPackageClasses));
        }
        if (controllers.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] pacote="
                    + MOIS_SALES_LIBRARY_WEB_PACKAGE
                    + " deve conter exatamente uma classe MoisSalesLibraryController; controllers="
                    + simpleNames(controllers));
        } else {
            JavaClass controller = controllers.get(0);
            if (!controller.isAnnotatedWith(RestController.class)) {
                violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] classe="
                        + controller.getName() + " deve possuir @RestController");
            }
            if (!hasRequestMapping(controller, "/api/mois/sales-library")) {
                violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] classe="
                        + controller.getName()
                        + " deve possuir @RequestMapping(\"/api/mois/sales-library\")");
            }
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que a Biblioteca de Páginas de Vendas do MOIS possua a fachada de service canônica.
     */
    @ArchTest
    static void moisSalesLibraryPackageMustHaveCanonicalServiceFacade(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> servicePackageClasses = directPackageClasses(importedClasses, MOIS_SALES_LIBRARY_SERVICE_PACKAGE);
        List<JavaClass> facades = servicePackageClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().equals("MoisSalesLibraryService"))
                .toList();
        if (facades.size() != 1) {
            violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] pacote="
                    + MOIS_SALES_LIBRARY_SERVICE_PACKAGE
                    + " deve conter exatamente uma fachada MoisSalesLibraryService; facades="
                    + simpleNames(facades));
        } else if (!facades.get(0).isAnnotatedWith(Service.class)) {
            violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] classe="
                    + facades.get(0).getName()
                    + " deve possuir @Service para marcar a fachada canônica do módulo");
        }
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que os contratos da Biblioteca de Páginas de Vendas permaneçam imutáveis.
     */
    @ArchTest
    static void moisSalesLibraryDtosMustBeRecords(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        directPackageClasses(importedClasses, MOIS_SALES_LIBRARY_DTO_PACKAGE).stream()
                .filter(javaClass -> !javaClass.getSimpleName().equals("MoisSalesLibraryDtos"))
                .filter(javaClass -> !javaClass.reflect().isRecord())
                .forEach(javaClass -> violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] classe="
                        + javaClass.getName()
                        + " deve ser declarada como record quando representar contrato HTTP"));
        failWithArchitectureViolations(violations);
    }

    /**
     * Garante que repositories da Biblioteca de Páginas de Vendas fiquem no pacote canônico externo.
     */
    @ArchTest
    static void moisSalesLibraryRepositoriesMustStayInCanonicalRepositoryPackage(JavaClasses importedClasses) {
        List<String> violations = new ArrayList<>();
        List<JavaClass> repositoryClasses = directPackageClasses(importedClasses, MOIS_SALES_LIBRARY_REPOSITORY_PACKAGE);
        if (repositoryClasses.isEmpty()) {
            violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] pacote="
                    + MOIS_SALES_LIBRARY_REPOSITORY_PACKAGE
                    + " deve conter os gateways/repositories canônicos do módulo");
        }
        repositoryClasses.stream()
                .filter(javaClass -> !javaClass.isInterface())
                .forEach(javaClass -> violations.add("[ARQUITETURA] [BACKEND][MOIS][BibliotecaPaginaVenda] classe="
                        + javaClass.getName() + " deve permanecer como contrato/interface de persistência no pacote "
                        + MOIS_SALES_LIBRARY_REPOSITORY_PACKAGE));
        failWithArchitectureViolations(violations);
    }


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
    static final ArchRule designPresetAssemblerMustResideInPresetDesignProvisorioPackage = classes()
            .that()
            .haveSimpleName("DesignPresetProvisionalHtmlAssembler")
            .should()
            .resideInAPackage("..geralanding.presetdesign.provisorio..")
            .because("[ARQUITETURA] [BACKEND][DesignPresetProvisionalHtmlAssembler] o assembler de design preset deve ficar no pacote geralanding.presetdesign.provisorio");

    @ArchTest
    static final ArchRule wireframePackageMustContainCanonicalAssemblerType = classes()
            .that()
            .haveNameMatching(".*WireframeProvisionalHtmlAssembler")
            .should()
            .beAssignableTo(WireframeProvisionalHtmlAssembler.class)
            .because("[ARQUITETURA][BACKEND][WireframeProvisionalHtmlAssembler] o tipo canônico do assembler de wireframe deve existir e permanecer estável");


    /**
     * Agrupa classes de produção dos pacotes web diretos que seguem o padrão de etapa backend.
     */
    private static Map<String, List<JavaClass>> backendStageWebPackages(JavaClasses importedClasses) {
        Map<String, List<JavaClass>> packages = new LinkedHashMap<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> BACKEND_STAGE_WEB_PACKAGE_PATTERN.matcher(javaClass.getPackageName()).matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .forEach(javaClass -> packages
                        .computeIfAbsent(javaClass.getPackageName(), ignored -> new ArrayList<>())
                        .add(javaClass));
        return packages;
    }

    /**
     * Agrupa classes de produção dos pacotes service diretos que seguem o padrão de etapa backend.
     */
    private static Map<String, List<JavaClass>> backendStageServicePackages(JavaClasses importedClasses) {
        Map<String, List<JavaClass>> packages = new LinkedHashMap<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> BACKEND_STAGE_SERVICE_PACKAGE_PATTERN.matcher(javaClass.getPackageName()).matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .forEach(javaClass -> packages
                        .computeIfAbsent(javaClass.getPackageName(), ignored -> new ArrayList<>())
                        .add(javaClass));
        return packages;
    }

    /**
     * Agrupa classes diretas de controller/web das etapas OPRM nicho CNAE.
     */
    private static Map<String, List<JavaClass>> oprmNichoCnaeStageControllerPackages(JavaClasses importedClasses) {
        Map<String, List<JavaClass>> packages = new LinkedHashMap<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> OPRM_NICHO_CNAE_STAGE_CONTROLLER_PACKAGE_PATTERN
                        .matcher(javaClass.getPackageName())
                        .matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .forEach(javaClass -> packages
                        .computeIfAbsent(javaClass.getPackageName(), ignored -> new ArrayList<>())
                        .add(javaClass));
        return packages;
    }

    /**
     * Agrupa classes diretas de service das etapas OPRM nicho CNAE.
     */
    private static Map<String, List<JavaClass>> oprmNichoCnaeStageServicePackages(JavaClasses importedClasses) {
        Map<String, List<JavaClass>> packages = new LinkedHashMap<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> OPRM_NICHO_CNAE_STAGE_SERVICE_PACKAGE_PATTERN
                        .matcher(javaClass.getPackageName())
                        .matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .forEach(javaClass -> packages
                        .computeIfAbsent(javaClass.getPackageName(), ignored -> new ArrayList<>())
                        .add(javaClass));
        return packages;
    }

    /**
     * Verifica se a classe é service backend canônico da etapa OPRM nicho CNAE.
     */
    private static boolean isCanonicalOprmNichoCnaeBackendServiceClass(JavaClass javaClass) {
        return isBackendServiceClass(javaClass) && !javaClass.getSimpleName().contains("StallGuard");
    }

    /**
     * Verifica se a classe está em subpacote de service de etapa OPRM nicho CNAE.
     */
    private static boolean isOprmNichoCnaeStageServiceSubpackageClass(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(OPRM_NICHO_CNAE_STAGE_PACKAGE_PREFIX)) {
            return false;
        }
        String remainder = packageName.substring(OPRM_NICHO_CNAE_STAGE_PACKAGE_PREFIX.length());
        String[] parts = remainder.split("\\.");
        return parts.length >= 3 && "service".equals(parts[1]);
    }

    /**
     * Agrupa classes diretas de controller das etapas novas de Facebook Ads.
     */
    private static Map<String, List<JavaClass>> facebookAdsStageControllerPackages(JavaClasses importedClasses) {
        Map<String, List<JavaClass>> packages = new LinkedHashMap<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> FACEBOOK_ADS_STAGE_CONTROLLER_PACKAGE_PATTERN
                        .matcher(javaClass.getPackageName())
                        .matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .forEach(javaClass -> packages
                        .computeIfAbsent(javaClass.getPackageName(), ignored -> new ArrayList<>())
                        .add(javaClass));
        return packages;
    }

    /**
     * Agrupa classes diretas de service das etapas novas de Facebook Ads.
     */
    private static Map<String, List<JavaClass>> facebookAdsStageServicePackages(JavaClasses importedClasses) {
        Map<String, List<JavaClass>> packages = new LinkedHashMap<>();
        importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> FACEBOOK_ADS_STAGE_SERVICE_PACKAGE_PATTERN
                        .matcher(javaClass.getPackageName())
                        .matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .forEach(javaClass -> packages
                        .computeIfAbsent(javaClass.getPackageName(), ignored -> new ArrayList<>())
                        .add(javaClass));
        return packages;
    }

    /**
     * Lista classes diretas de produção em um pacote específico.
     */
    private static List<JavaClass> directPackageClasses(JavaClasses importedClasses, String packageName) {
        return importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> javaClass.getPackageName().equals(packageName))
                .sorted(Comparator.comparing(JavaClass::getName))
                .toList();
    }

    /**
     * Lista classes diretas de produção em um pacote do módulo pipeline.
     */
    private static List<JavaClass> pipelineDirectPackageClasses(JavaClasses importedClasses, String packageName) {
        return directPackageClasses(importedClasses, packageName);
    }

    /**
     * Lista classes diretas de produção no pacote controller do EPM.
     */
    private static List<JavaClass> epmControllerPackageClasses(JavaClasses importedClasses) {
        return importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> EPM_CONTROLLER_PACKAGE_PATTERN.matcher(javaClass.getPackageName()).matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .toList();
    }

    /**
     * Lista classes diretas de produção no pacote service do EPM.
     */
    private static List<JavaClass> epmServicePackageClasses(JavaClasses importedClasses) {
        return importedClasses.stream()
                .filter(ArquiteturaTest::isProductionTopLevelClass)
                .filter(javaClass -> EPM_SERVICE_PACKAGE_PATTERN.matcher(javaClass.getPackageName()).matches())
                .sorted(Comparator.comparing(JavaClass::getName))
                .toList();
    }

    /**
     * Identifica classes de produção top-level, removendo testes e classes internas importadas pelo ArchUnit.
     */
    private static boolean isProductionTopLevelClass(JavaClass javaClass) {
        return !javaClass.getSimpleName().endsWith("Test")
                && !javaClass.getName().contains("$")
                && !javaClass.getPackageName().startsWith("com.marketinghub.architecture");
    }

    /**
     * Verifica se a classe segue o padrão de nome Backend<Etapa>Controller.
     */
    private static boolean isBackendControllerClass(JavaClass javaClass) {
        return BACKEND_CONTROLLER_NAME_PATTERN.matcher(javaClass.getSimpleName()).matches();
    }

    /**
     * Verifica se a classe segue o padrão de nome Backend<Etapa>Service.
     */
    private static boolean isBackendServiceClass(JavaClass javaClass) {
        return BACKEND_SERVICE_NAME_PATTERN.matcher(javaClass.getSimpleName()).matches();
    }

    /**
     * Verifica se o controller usa @RequestMapping com valor ou path exatamente /api.
     */
    private static boolean hasRequestMappingApi(JavaClass javaClass) {
        RequestMapping requestMapping = javaClass.reflect().getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return false;
        }
        return containsOnlyApi(requestMapping.value()) || containsOnlyApi(requestMapping.path());
    }

    /**
     * Verifica se o controller usa @RequestMapping com valor ou path exatamente igual ao mapeamento esperado.
     */
    private static boolean hasRequestMapping(JavaClass javaClass, String expectedMapping) {
        RequestMapping requestMapping = javaClass.reflect().getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return false;
        }
        return containsOnlyMapping(requestMapping.value(), expectedMapping)
                || containsOnlyMapping(requestMapping.path(), expectedMapping);
    }

    /**
     * Verifica se o atributo da anotação possui somente o mapeamento /api.
     */
    private static boolean containsOnlyApi(String[] mappings) {
        return mappings.length == 1 && "/api".equals(mappings[0]);
    }

    /**
     * Verifica se o atributo da anotação possui somente o mapeamento esperado.
     */
    private static boolean containsOnlyMapping(String[] mappings, String expectedMapping) {
        return mappings.length == 1 && expectedMapping.equals(mappings[0]);
    }

    /**
     * Retorna os nomes simples das classes para mensagens de falha determinísticas.
     */
    private static List<String> simpleNames(List<JavaClass> classes) {
        return classes.stream()
                .map(JavaClass::getSimpleName)
                .sorted()
                .toList();
    }

    /**
     * Falha o teste de arquitetura com mensagens iniciadas pelo prefixo obrigatório.
     */
    private static void failWithArchitectureViolations(List<String> violations) {
        if (!violations.isEmpty()) {
            violations.sort(String::compareTo);
            throw new AssertionError(String.join(System.lineSeparator(), violations));
        }
    }

    /**
     * Garante que controllers Facebook Ads não consumam controllers de outros módulos.
     */
    private static ArchCondition<JavaClass> notDependOnOtherModuleControllers() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][FacebookAds] não depende de controllers de outros módulos") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getSimpleName().endsWith("Test")) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetPackage = targetClass.getPackageName();
                    if (!targetPackage.startsWith("com.marketinghub.")) {
                        return;
                    }
                    if (!targetPackage.contains(".controller")
                            || targetPackage.startsWith("com.marketinghub.facebookads.controller")) {
                        return;
                    }
                    String message = "[ARQUITETURA] [BACKEND][FacebookAds] classe-origem=" + item.getName()
                            + " possui dependência para controller externo: " + dependency.getDescription()
                            + " (alvo: " + targetClass.getName() + ")"
                            + " | regra: a borda HTTP de Facebook Ads deve delegar para services/contratos, "
                            + "não para controllers de outros módulos.";
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Garante que Facebook Ads consuma apenas contratos internos aprovados do backend.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedFacebookAdsMarketingHubClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][FacebookAds] depende apenas de pacotes aprovados") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getSimpleName().endsWith("Test")) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    if (isAllowedFacebookAdsDependency(targetClass)) {
                        return;
                    }
                    String message = "[ARQUITETURA] [BACKEND][FacebookAds] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: facebookads só pode acessar o próprio pacote, ads, experiment, "
                            + "creative, journey, leadportal, niche, settings, targeting e repositories canônicos "
                            + "necessários para campanhas.";
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Garante que apenas pacotes aprovados consumam classes internas de Facebook Ads.
     */
    private static ArchCondition<JavaClass> onlyApprovedPackagesDependOnFacebookAdsClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][FacebookAds] somente pacotes aprovados dependem de Facebook Ads") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getSimpleName().endsWith("Test")) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.facebookads.")) {
                        return;
                    }
                    String sourcePackage = item.getPackageName();
                    boolean valid = sourcePackage.startsWith("com.marketinghub.facebookads")
                            || sourcePackage.startsWith("com.marketinghub.experiment")
                            || sourcePackage.startsWith("com.marketinghub.repository.jpa.experiment")
                            || sourcePackage.startsWith("com.marketinghub.repository.jpa.facebookads");
                    if (valid) {
                        return;
                    }
                    String message = "[ARQUITETURA] [BACKEND][FacebookAds] classe-origem=" + item.getName()
                            + " possui dependência não aprovada para Facebook Ads: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: somente facebookads, experiment, repository.jpa.experiment e "
                            + "repository.jpa.facebookads podem depender diretamente das classes de Facebook Ads.";
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Verifica se a dependência de Facebook Ads aponta para pacote interno aprovado.
     */
    private static boolean isAllowedFacebookAdsDependency(JavaClass targetClass) {
        String targetPackage = targetClass.getPackageName();
        return targetPackage.startsWith("com.marketinghub.facebookads")
                || targetPackage.startsWith("com.marketinghub.ads")
                || targetPackage.startsWith("com.marketinghub.creative")
                || targetPackage.startsWith("com.marketinghub.experiment")
                || targetPackage.startsWith("com.marketinghub.hypothesis")
                || targetPackage.startsWith("com.marketinghub.journey")
                || targetPackage.startsWith("com.marketinghub.leadportal")
                || targetPackage.startsWith("com.marketinghub.niche")
                || targetPackage.startsWith("com.marketinghub.settings")
                || targetPackage.startsWith("com.marketinghub.targeting")
                || targetPackage.startsWith("com.marketinghub.repository.jpa.ads")
                || targetPackage.startsWith("com.marketinghub.repository.jpa.creative")
                || targetPackage.startsWith("com.marketinghub.repository.jpa.experiment")
                || targetPackage.startsWith("com.marketinghub.repository.jpa.facebookads")
                || targetPackage.startsWith("com.marketinghub.repository.jpa.hypothesis")
                || targetPackage.startsWith("com.marketinghub.repository.jpa.targeting");
    }

    /**
     * Garante que controllers de etapas novas de Facebook Ads dependam apenas da mesma etapa.
     */
    private static ArchCondition<JavaClass> onlyDependOnFacebookAdsControllerOrServiceWithinSameStage() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][FacebookAds] controller depends only on same-stage controller/service") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith(FACEBOOK_ADS_STAGE_PACKAGE_PREFIX)) {
                        return;
                    }
                    String sourceStage = extractFacebookAdsStage(item.getPackageName());
                    String targetStage = extractFacebookAdsStage(targetClass.getPackageName());
                    String targetLayer = extractFacebookAdsStageLayer(targetClass.getPackageName());
                    boolean valid = sourceStage != null
                            && sourceStage.equals(targetStage)
                            && ("controller".equals(targetLayer) || "service".equals(targetLayer));
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][FacebookAds] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: facebookads.<etapa>.controller só pode acessar "
                                + "facebookads.<etapa>.controller e facebookads.<etapa>.service da mesma etapa.";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que services de etapas novas de Facebook Ads usem a mesma etapa e contratos aprovados.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedFacebookAdsStageServiceClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][FacebookAds] service depends on same-stage service and approved contracts") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    if (isSameFacebookAdsStageServiceDependency(item, targetClass)
                            || isAllowedFacebookAdsDependency(targetClass)) {
                        return;
                    }
                    String message = "[ARQUITETURA] [BACKEND][FacebookAds] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: facebookads.<etapa>.service só pode acessar service/DTOs da mesma etapa "
                            + "e contratos internos aprovados para Facebook Ads.";
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Verifica se a dependência alvo pertence à árvore service da mesma etapa Facebook Ads.
     */
    private static boolean isSameFacebookAdsStageServiceDependency(JavaClass sourceClass, JavaClass targetClass) {
        String sourceStage = extractFacebookAdsStage(sourceClass.getPackageName());
        String targetStage = extractFacebookAdsStage(targetClass.getPackageName());
        String targetLayer = extractFacebookAdsStageLayer(targetClass.getPackageName());
        return sourceStage != null && sourceStage.equals(targetStage) && "service".equals(targetLayer);
    }

    /**
     * Garante que o OPRM dependa apenas do próprio módulo, repositories OPRM e da exceção nominal do materializador.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedOprmMarketingHubClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA][OPRM] depende apenas de OPRM, repositories OPRM e exceção nominal do materializador") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    if (isAllowedOprmDependency(item, targetClass)) {
                        return;
                    }
                    String message = "[ARQUITETURA][OPRM] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: OPRM só pode acessar com.marketinghub.oprm, "
                            + "com.marketinghub.repository.jpa.oprm, com.marketinghub.repository.jdbc.oprm e, especificamente para "
                            + "BackendEnrichedNicheMaterializerService, apenas as quatro classes autorizadas de nicho.";
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Verifica se a dependência OPRM está dentro dos pacotes canônicos ou na exceção nominal liberada.
     */
    private static boolean isAllowedOprmDependency(JavaClass sourceClass, JavaClass targetClass) {
        String targetPackage = targetClass.getPackageName();
        if (targetPackage.startsWith("com.marketinghub.oprm")
                || targetPackage.startsWith("com.marketinghub.repository.jpa.oprm")
                || targetPackage.startsWith("com.marketinghub.repository.jdbc.oprm")) {
            return true;
        }
        return isEnrichedNicheMaterializerException(sourceClass, targetClass);
    }

    /**
     * Autoriza somente o materializador enriquecido do OPRM, e seu teste, a acessar as quatro classes de nicho.
     */
    private static boolean isEnrichedNicheMaterializerException(JavaClass sourceClass, JavaClass targetClass) {
        String sourceName = sourceClass.getName();
        return (OPRM_ENRICHED_NICHE_MATERIALIZER_SERVICE.equals(sourceName)
                        || OPRM_ENRICHED_NICHE_MATERIALIZER_SERVICE_TEST.equals(sourceName))
                && ALLOWED_OPRM_ENRICHED_NICHE_MATERIALIZER_CLASSES.contains(targetClass.getName());
    }

    /**
     * Retorna predicado que identifica classes internas fora dos pacotes permitidos.
     */
    private static DescribedPredicate<JavaClass> otherMarketingHubPackagesExcept(String... allowedPackagePrefixes) {
        return new DescribedPredicate<>("other com.marketinghub packages except allowed prefixes") {
            @Override
            public boolean test(JavaClass input) {
                String packageName = input.getPackageName();
                if (!packageName.startsWith("com.marketinghub")) {
                    return false;
                }
                for (String allowedPackagePrefix : allowedPackagePrefixes) {
                    if (packageName.startsWith(allowedPackagePrefix)) {
                        return false;
                    }
                }
                return true;
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
                        && "com.marketinghub.geralanding.wireframe.service.recebePrompt.RecebePromptRequest"
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
     * Garante que controllers backend tenham apenas os métodos públicos canônicos e deleguem ao service.
     */
    private static ArchCondition<JavaClass> haveOnlyCanonicalControllerMethodsCallingService() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][GeraLanding] Backend<Etapa>Controller exposes only canonical public methods and delegates to service") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                List<JavaMethod> publicMethods = item.getMethods().stream()
                        .filter(method -> method.getOwner().equals(item))
                        .filter(method -> Modifier.isPublic(method.reflect().getModifiers()))
                        .filter(method -> !method.reflect().isSynthetic())
                        .sorted(Comparator.comparing(JavaMethod::getName))
                        .toList();
                List<String> actualMethodNames = publicMethods.stream()
                        .map(JavaMethod::getName)
                        .toList();
                REQUIRED_BACKEND_CONTROLLER_METHODS.stream()
                        .filter(requiredMethod -> !actualMethodNames.contains(requiredMethod))
                        .forEach(requiredMethod -> events.add(SimpleConditionEvent.violated(
                                item,
                                "[ARQUITETURA] [BACKEND][GeraLanding] classe=" + item.getName()
                                        + " deve declarar o método público obrigatório " + requiredMethod
                                        + " e expor somente os métodos canônicos "
                                        + REQUIRED_BACKEND_CONTROLLER_METHODS)));
                actualMethodNames.stream()
                        .filter(actualMethod -> !REQUIRED_BACKEND_CONTROLLER_METHODS.contains(actualMethod))
                        .forEach(extraMethod -> events.add(SimpleConditionEvent.violated(
                                item,
                                "[ARQUITETURA] [BACKEND][GeraLanding] classe=" + item.getName()
                                        + " declara método público extra " + extraMethod
                                        + "; controllers Backend<Etapa>Controller devem expor somente "
                                        + REQUIRED_BACKEND_CONTROLLER_METHODS)));
                REQUIRED_BACKEND_CONTROLLER_METHODS.forEach(requiredMethod -> {
                    long occurrences = actualMethodNames.stream()
                            .filter(requiredMethod::equals)
                            .count();
                    if (occurrences > 1) {
                        events.add(SimpleConditionEvent.violated(
                                item,
                                "[ARQUITETURA] [BACKEND][GeraLanding] classe=" + item.getName()
                                        + " declara overload do método público " + requiredMethod
                                        + "; cada método canônico deve existir exatamente uma vez"));
                    }
                });

                String expectedServiceClassName = expectedBackendServiceClassName(item);
                publicMethods.stream()
                        .filter(method -> REQUIRED_BACKEND_CONTROLLER_METHODS.contains(method.getName()))
                        .filter(method -> !callsExpectedBackendService(method, expectedServiceClassName))
                        .forEach(method -> events.add(SimpleConditionEvent.violated(
                                item,
                                "[ARQUITETURA] [BACKEND][GeraLanding] classe=" + item.getName()
                                        + " método=" + method.getName()
                                        + " deve chamar o service canônico " + expectedServiceClassName
                                        + " para manter o controller sem regra de negócio")));
            }
        };
    }

    /**
     * Monta o nome totalmente qualificado do service canônico esperado para o controller da etapa.
     */
    private static String expectedBackendServiceClassName(JavaClass controllerClass) {
        String stagePackage = extractGeraLandingStage(controllerClass.getPackageName());
        Matcher matcher = BACKEND_CONTROLLER_NAME_PATTERN.matcher(controllerClass.getSimpleName());
        String stageName = matcher.matches() ? matcher.group(1) : "";
        return "com.marketinghub.geralanding." + stagePackage + ".service.Backend" + stageName + "Service";
    }

    /**
     * Verifica se o método do controller realiza chamada direta ao service canônico da etapa.
     */
    private static boolean callsExpectedBackendService(JavaMethod method, String expectedServiceClassName) {
        return method.getMethodCallsFromSelf().stream()
                .anyMatch(call -> call.getTargetOwner().getName().equals(expectedServiceClassName));
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
     * Garante que repositories Spring Data JPA residam em subpacotes da raiz canônica de JPA.
     */
    private static ArchCondition<JavaClass> resideInRepositoryJpaSubpackage() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][Repository] reside in com.marketinghub.repository.jpa subpackage") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getPackageName().startsWith("com.marketinghub.repository.jpa.")) {
                    return;
                }
                String message = "[ARQUITETURA] [BACKEND][Repository] classe=" + item.getName()
                        + " acessa JPA via Spring Data JpaRepository e deve ficar em algum subpacote dentro de "
                        + "com.marketinghub.repository.jpa";
                events.add(SimpleConditionEvent.violated(item, message));
            }
        };
    }

    /**
     * Garante que a borda HTTP do pipeline dependa somente de contratos e orquestrações do próprio pipeline.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedPipelineWebMarketingHubClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][Pipeline] web only depends on pipeline contracts/services") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getSimpleName().endsWith("Test")) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    String targetPackage = targetClass.getPackageName();
                    boolean valid = targetPackage.equals("com.marketinghub.pipeline")
                            || targetPackage.startsWith("com.marketinghub.pipeline.web")
                            || targetPackage.startsWith("com.marketinghub.pipeline.service")
                            || targetPackage.startsWith("com.marketinghub.pipeline.dto")
                            || targetPackage.startsWith("com.marketinghub.pipeline.mapper");
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][Pipeline] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: pipeline.web só pode acessar domínio, DTOs, mapper e services "
                                + "do próprio pacote com.marketinghub.pipeline.";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que services do pipeline usem somente domínio próprio e repositories centralizados permitidos.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedPipelineServiceMarketingHubClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][Pipeline] service only depends on pipeline/openai/repository contracts") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getSimpleName().endsWith("Test")) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    String targetPackage = targetClass.getPackageName();
                    boolean valid = targetPackage.equals("com.marketinghub.pipeline")
                            || targetPackage.startsWith("com.marketinghub.pipeline.service")
                            || targetPackage.startsWith("com.marketinghub.pipeline.definition")
                            || targetPackage.startsWith("com.marketinghub.pipeline.dto")
                            || targetPackage.startsWith("com.marketinghub.repository.jpa.pipeline")
                            || targetPackage.startsWith("com.marketinghub.openai")
                            || targetPackage.startsWith("com.marketinghub.repository.jpa.openai");
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][Pipeline] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: pipeline.service só pode acessar domínio/definition/DTOs do pipeline, "
                                + "repositories canônicos de pipeline e a exceção explícita OpenAI usada na configuração de modelo.";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que controllers EPM dependam somente do próprio pacote e da árvore service do EPM.
     */
    private static ArchCondition<JavaClass> onlyDependOnEpmControllerOrService() {
        return new ArchCondition<>("[ARQUITETURA] [BACKEND][EPM] controller only depends on EPM controller/service") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.epm.")) {
                        return;
                    }
                    boolean valid = targetClass.getPackageName().startsWith("com.marketinghub.epm.controller")
                            || targetClass.getPackageName().startsWith("com.marketinghub.epm.service");
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][EPM] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: epm.controller só pode acessar epm.controller e epm.service";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que services EPM acessem domínio EPM, DTOs por operação e repositories EPM.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedEpmMarketingHubClasses() {
        return new ArchCondition<>("[ARQUITETURA] [BACKEND][EPM] service only depends on EPM domain/service/repository") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    String targetPackage = targetClass.getPackageName();
                    boolean valid = targetPackage.equals("com.marketinghub.epm")
                            || targetPackage.startsWith("com.marketinghub.epm.service")
                            || targetPackage.startsWith("com.marketinghub.repository.jpa.epm");
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][EPM] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: epm.service só pode acessar domínio com.marketinghub.epm, "
                                + "subpacotes com.marketinghub.epm.service.* e repositories "
                                + "com.marketinghub.repository.jpa.epm";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que controllers SalesVideo dependam apenas da fachada, DTOs, domínio e contratos de mídia liberados.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedSalesVideoControllerMarketingHubClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][SalesVideo] controller only depends on SalesVideo facade/contracts") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    String targetPackage = targetClass.getPackageName();
                    boolean valid = targetPackage.equals(SALES_VIDEO_PACKAGE)
                            || targetPackage.startsWith(SALES_VIDEO_CONTROLLER_PACKAGE)
                            || targetPackage.startsWith(SALES_VIDEO_DTO_PACKAGE)
                            || targetName.equals("com.marketinghub.salesvideo.service.SalesVideoService")
                            || targetPackage.equals("com.marketinghub.media")
                            || targetPackage.startsWith("com.marketinghub.media.dto")
                            || targetPackage.startsWith("com.marketinghub.media.mapper");
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][SalesVideo] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: salesvideo.controller só pode acessar domínio/DTOs do SalesVideo, "
                                + "a fachada SalesVideoService e contratos de mídia necessários à borda HTTP.";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que services SalesVideo usem somente domínio próprio e repositories centralizados permitidos.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedSalesVideoServiceMarketingHubClasses() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][SalesVideo] service only depends on SalesVideo and approved repositories") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    String targetPackage = targetClass.getPackageName();
                    boolean valid = targetPackage.equals(SALES_VIDEO_PACKAGE)
                            || targetPackage.startsWith(SALES_VIDEO_SERVICE_PACKAGE)
                            || targetPackage.startsWith(SALES_VIDEO_DTO_PACKAGE)
                            || targetPackage.startsWith("com.marketinghub.salesvideo.mapper")
                            || targetPackage.startsWith("com.marketinghub.salesvideo.tenant")
                            || targetPackage.startsWith("com.marketinghub.salesvideo.exception")
                            || targetPackage.startsWith(SALES_VIDEO_REPOSITORY_PACKAGE)
                            || targetPackage.equals("com.marketinghub.media")
                            || targetPackage.startsWith("com.marketinghub.repository.jpa.media")
                            || targetPackage.equals("com.marketinghub.storage")
                            || targetPackage.equals("com.marketinghub.experiment")
                            || targetPackage.startsWith("com.marketinghub.repository.jpa.experiment")
                            || targetPackage.equals("com.marketinghub.product")
                            || targetPackage.startsWith("com.marketinghub.repository.jpa.product");
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][SalesVideo] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: salesvideo.service só pode acessar domínio/DTOs/services internos, "
                                + "repositories externos canônicos de SalesVideo e contratos auxiliares aprovados "
                                + "de mídia, storage, experiment e product.";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que DTOs SalesVideo não acessem camadas executáveis ou persistência.
     */
    private static ArchCondition<JavaClass> onlyDependOnSalesVideoDtoContracts() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][SalesVideo] dto only depends on SalesVideo contracts") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    String targetPackage = targetClass.getPackageName();
                    boolean valid = targetPackage.equals(SALES_VIDEO_PACKAGE)
                            || targetPackage.startsWith(SALES_VIDEO_DTO_PACKAGE);
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][SalesVideo] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: salesvideo.dto só pode depender de DTOs e tipos de domínio/enums "
                                + "do próprio SalesVideo.";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que repositories externos SalesVideo dependam apenas do domínio do módulo.
     */
    private static ArchCondition<JavaClass> onlyDependOnSalesVideoRepositoryContracts() {
        return new ArchCondition<>(
                "[ARQUITETURA] [BACKEND][SalesVideo] repository only depends on SalesVideo domain") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    String targetPackage = targetClass.getPackageName();
                    boolean valid = targetPackage.equals(SALES_VIDEO_PACKAGE)
                            || targetPackage.startsWith(SALES_VIDEO_REPOSITORY_PACKAGE);
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][SalesVideo] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: repository.jpa.salesvideo é externo ao pacote funcional e deve depender "
                                + "apenas do domínio SalesVideo e de contratos do próprio pacote repository.";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Extrai o primeiro subpacote abaixo de uma raiz de pacote.
     */
    private static String extractDirectSubpackage(String packageName, String packagePrefix) {
        String remainder = packageName.substring(packagePrefix.length());
        int idx = remainder.indexOf('.');
        return idx > 0 ? remainder.substring(0, idx) : remainder;
    }

    /**
     * Garante que controllers dependam apenas de controller/service da mesma etapa hypothesis.
     */
    private static ArchCondition<JavaClass> onlyDependOnHypothesisControllerOrServiceWithinSameStage() {
        return new ArchCondition<>("[ARQUITETURA] [BACKEND][Hypothesis] controller only depends on same-stage controller/service") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetName = dependency.getTargetClass().getName();
                    if (!targetName.startsWith("com.marketinghub.hypothesis.")) {
                        return;
                    }
                    String sourceStage = extractHypothesisStage(item.getPackageName());
                    String targetStage = extractHypothesisStage(dependency.getTargetClass().getPackageName());
                    String targetLayer = extractHypothesisLayer(dependency.getTargetClass().getPackageName());
                    if (sourceStage == null) {
                        return;
                    }
                    boolean valid = sourceStage.equals(targetStage)
                            && ("controller".equals(targetLayer) || "service".equals(targetLayer));
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][Hypothesis] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: hypothesis.*.controller só pode acessar hypothesis.*.controller "
                                + "e hypothesis.*.service da mesma etapa";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que classes provisorio dependam apenas de provisorio da mesma etapa hypothesis.
     */
    private static ArchCondition<JavaClass> onlyDependOnHypothesisProvisorioWithinSameStage() {
        return new ArchCondition<>("[ARQUITETURA] [BACKEND][Hypothesis] provisorio only depends on same-stage provisorio") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetName = dependency.getTargetClass().getName();
                    if (!targetName.startsWith("com.marketinghub.hypothesis.")) {
                        return;
                    }
                    String sourceStage = extractHypothesisStage(item.getPackageName());
                    String targetStage = extractHypothesisStage(dependency.getTargetClass().getPackageName());
                    String targetLayer = extractHypothesisLayer(dependency.getTargetClass().getPackageName());
                    if (sourceStage == null) {
                        return;
                    }
                    boolean valid = sourceStage.equals(targetStage) && "provisorio".equals(targetLayer);
                    if (!valid) {
                        String message = "[ARQUITETURA] [BACKEND][Hypothesis] classe-origem=" + item.getName()
                                + " possui import/dependência violadora: " + dependency.getDescription()
                                + " (alvo: " + targetName + ")"
                                + " | regra: hypothesis.*.provisorio só pode acessar hypothesis.*.provisorio da mesma etapa";
                        events.add(SimpleConditionEvent.violated(item, message));
                    }
                });
            }
        };
    }

    /**
     * Garante que services hypothesis acessem services/provisórios internos da mesma etapa e classes compartilhadas permitidas.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedHypothesisStageMarketingHubClasses() {
        return new ArchCondition<>("[ARQUITETURA] [BACKEND][Hypothesis] depend only on same-stage service/provisorio packages and explicit allowed classes") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    if (isSameStageHypothesisServiceDependency(item, targetClass)
                            || isSameStageHypothesisProvisorioDependency(item, targetClass)
                            || isSameStageHypothesisDomainDependency(item, targetClass)
                            || targetName.equals(HYPOTHESIS_CLASS)
                            || targetName.equals(MARKET_NICHE_CLASS)
                            || isAllowedHypothesisStageServiceRepository(targetName)) {
                        return;
                    }
                    String message = "[ARQUITETURA] [BACKEND][Hypothesis] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: serviços em hypothesis.*.service só podem acessar "
                            + "pacotes internos hypothesis.<etapa>.service.*, hypothesis.<etapa>.provisorio.* "
                            + "e entidades da mesma etapa, "
                            + HYPOTHESIS_CLASS + ", "
                            + MARKET_NICHE_CLASS
                            + " e somente repositories canônicos explicitamente liberados: "
                            + ALLOWED_HYPOTHESIS_STAGE_SERVICE_REPOSITORIES;
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Verifica se a dependência alvo é um repository liberado para services do Hypothesis.
     */
    private static boolean isAllowedHypothesisStageServiceRepository(String targetName) {
        return ALLOWED_HYPOTHESIS_STAGE_SERVICE_REPOSITORIES.contains(targetName);
    }

    /**
     * Verifica se a dependência alvo pertence à árvore service da mesma etapa Hypothesis.
     */
    private static boolean isSameStageHypothesisServiceDependency(JavaClass sourceClass, JavaClass targetClass) {
        String sourceStage = extractHypothesisStage(sourceClass.getPackageName());
        String targetStage = extractHypothesisStage(targetClass.getPackageName());
        String targetLayer = extractHypothesisLayer(targetClass.getPackageName());
        return sourceStage != null && sourceStage.equals(targetStage) && "service".equals(targetLayer);
    }

    /**
     * Verifica se a dependência alvo pertence à árvore provisorio da mesma etapa Hypothesis.
     */
    private static boolean isSameStageHypothesisProvisorioDependency(JavaClass sourceClass, JavaClass targetClass) {
        String sourceStage = extractHypothesisStage(sourceClass.getPackageName());
        String targetStage = extractHypothesisStage(targetClass.getPackageName());
        String targetLayer = extractHypothesisLayer(targetClass.getPackageName());
        return sourceStage != null && sourceStage.equals(targetStage) && "provisorio".equals(targetLayer);
    }

    /**
     * Verifica se a dependência alvo pertence ao domínio direto da mesma etapa Hypothesis.
     */
    private static boolean isSameStageHypothesisDomainDependency(JavaClass sourceClass, JavaClass targetClass) {
        String sourceStage = extractHypothesisStage(sourceClass.getPackageName());
        String targetStage = extractHypothesisStage(targetClass.getPackageName());
        String targetLayer = extractHypothesisLayer(targetClass.getPackageName());
        return sourceStage != null && sourceStage.equals(targetStage) && targetLayer == null;
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
     * Garante que serviços geralanding acessem serviços/provisórios internos da mesma etapa e classes compartilhadas permitidas.
     */
    private static ArchCondition<JavaClass> onlyDependOnAllowedMarketingHubClasses() {
        return new ArchCondition<>("[ARQUITETURA] [BACKEND][GeraLanding] depend only on same-stage service/provisorio packages and explicit allowed classes") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetName = targetClass.getName();
                    if (!targetName.startsWith("com.marketinghub.")) {
                        return;
                    }
                    if (isSameStageServiceDependency(item, targetClass)
                            || isSameStageProvisorioDependency(item, targetClass)
                            || targetName.equals(EXPERIMENT_CLASS)
                            || isAllowedGeraLandingServiceRepository(targetName)
                            || targetName.equals(GERALANDING_STAGE_EXECUTION_CLASS)
                            || targetName.equals(GERALANDING_STAGE_EXECUTION_BUILDER_CLASS)) {
                        return;
                    }
                    String message = "[ARQUITETURA] [BACKEND][GeraLanding] classe-origem=" + item.getName()
                            + " possui import/dependência violadora: " + dependency.getDescription()
                            + " (alvo: " + targetName + ")"
                            + " | regra: serviços em geralanding.*.service só podem acessar "
                            + "pacotes internos geralanding.<etapa>.service.* e geralanding.<etapa>.provisorio.* da mesma etapa, "
                            + EXPERIMENT_CLASS + ", "
                            + GERALANDING_STAGE_EXECUTION_CLASS + ", "
                            + GERALANDING_STAGE_EXECUTION_BUILDER_CLASS
                            + " e somente repositories das tabelas experiment, hypothesis, framework_image_generation_job e gera_landing_stage_execution: "
                            + ALLOWED_GERALANDING_SERVICE_REPOSITORIES;
                    events.add(SimpleConditionEvent.violated(item, message));
                });
            }
        };
    }

    /**
     * Verifica se a dependência alvo é um repository liberado para services do GeraLanding.
     */
    private static boolean isAllowedGeraLandingServiceRepository(String targetName) {
        return ALLOWED_GERALANDING_SERVICE_REPOSITORIES.contains(targetName);
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
     * Verifica se a dependência alvo pertence à árvore provisorio da mesma etapa GeraLanding.
     */
    private static boolean isSameStageProvisorioDependency(JavaClass sourceClass, JavaClass targetClass) {
        String sourceStage = extractGeraLandingStage(sourceClass.getPackageName());
        String targetStage = extractGeraLandingStage(targetClass.getPackageName());
        String targetLayer = extractGeraLandingLayer(targetClass.getPackageName());
        return sourceStage != null && sourceStage.equals(targetStage) && "provisorio".equals(targetLayer);
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
     * Extrai a etapa hypothesis (subpacote após hypothesis).
     */
    private static String extractHypothesisStage(String packageName) {
        String marker = "com.marketinghub.hypothesis.";
        if (!packageName.startsWith(marker)) {
            return null;
        }
        String remainder = packageName.substring(marker.length());
        int idx = remainder.indexOf('.');
        return idx > 0 ? remainder.substring(0, idx) : remainder;
    }

    /**
     * Extrai o layer principal da etapa hypothesis (controller/service/provisorio).
     */
    private static String extractHypothesisLayer(String packageName) {
        String stage = extractHypothesisStage(packageName);
        if (stage == null) {
            return null;
        }
        String stagePackage = "com.marketinghub.hypothesis." + stage;
        if (packageName.equals(stagePackage)) {
            return null;
        }
        String prefix = stagePackage + ".";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        int idx = remainder.indexOf('.');
        return idx > 0 ? remainder.substring(0, idx) : remainder;
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
     * Extrai a etapa Facebook Ads no padrão facebookads.<etapa>.<camada>.
     */
    private static String extractFacebookAdsStage(String packageName) {
        if (!packageName.startsWith(FACEBOOK_ADS_STAGE_PACKAGE_PREFIX)) {
            return null;
        }
        String remainder = packageName.substring(FACEBOOK_ADS_STAGE_PACKAGE_PREFIX.length());
        int idx = remainder.indexOf('.');
        return idx > 0 ? remainder.substring(0, idx) : null;
    }

    /**
     * Extrai a camada principal da etapa Facebook Ads (controller/service).
     */
    private static String extractFacebookAdsStageLayer(String packageName) {
        String stage = extractFacebookAdsStage(packageName);
        if (stage == null) {
            return null;
        }
        String prefix = FACEBOOK_ADS_STAGE_PACKAGE_PREFIX + stage + ".";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String remainder = packageName.substring(prefix.length());
        int idx = remainder.indexOf('.');
        return idx > 0 ? remainder.substring(0, idx) : remainder;
    }

    /**
     * Verifica se a classe está em subpacote de service de etapa nova de Facebook Ads.
     */
    private static boolean isFacebookAdsStageServiceSubpackageClass(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        String stage = extractFacebookAdsStage(packageName);
        String layer = extractFacebookAdsStageLayer(packageName);
        if (stage == null || !"service".equals(layer)) {
            return false;
        }
        String servicePackage = FACEBOOK_ADS_STAGE_PACKAGE_PREFIX + stage + ".service";
        return packageName.startsWith(servicePackage + ".");
    }

    /**
     * Converte o nome do pacote de etapa em nome canônico de tipo Java.
     */
    private static String canonicalFacebookAdsStageTypeName(String stage, String suffix) {
        if (stage == null || stage.isBlank()) {
            return suffix;
        }
        String[] parts = stage.split("[_-]");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            name.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1));
        }
        return name.append(suffix).toString();
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
