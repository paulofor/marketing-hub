package com.marketinghub.feo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.marketinghub.feo.fabricacaov1.pipeline.StageProcessor;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/**
 * Protege o protocolo padrao modulo aplicado ao executor FEO.
 */
@AnalyzeClasses(packages = "com.marketinghub.feo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String PIPELINE_ROOT = "com.marketinghub.feo.fabricacaov1.pipeline";
    private static final Set<String> STAGE_PACKAGES = Set.of(
            "com.marketinghub.feo.fabricacaov1.planejamentoentregaveis",
            "com.marketinghub.feo.fabricacaov1.redacaoentregaveis",
            "com.marketinghub.feo.fabricacaov1.geracaoativosvisuais",
            "com.marketinghub.feo.fabricacaov1.montagempacote");

    private static final DescribedPredicate<JavaClass> CLASSES_DE_ETAPA =
            new DescribedPredicate<>("[ARQUITETURA] classes das etapas concretas da FEO") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return STAGE_PACKAGES.stream().anyMatch(pkg -> javaClass.getPackageName().startsWith(pkg));
                }
            };

    /**
     * Garante que o nucleo generico nao conhece etapas concretas.
     */
    @ArchTest
    static final ArchRule pipeline_raiz_nao_deve_depender_de_etapas = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should(notDependOnConcreteStages())
            .because("[ARQUITETURA] o nucleo da FEO deve ser generico e nao importar etapas concretas");

    /**
     * Garante que uma etapa concreta nao conhece outra etapa concreta.
     */
    @ArchTest
    static final ArchRule etapas_nao_devem_depender_umas_das_outras = classes()
            .that(CLASSES_DE_ETAPA)
            .should(notDependOnAnotherStage())
            .because("[ARQUITETURA] etapas da FEO devem ser plugaveis e independentes");

    /**
     * Garante que etapas concretas nao formem ciclos.
     */
    @ArchTest
    static final ArchRule etapas_nao_devem_ter_ciclos = slices()
            .matching("com.marketinghub.feo.fabricacaov1.(*)..")
            .should()
            .beFreeOfCycles()
            .because("[ARQUITETURA] etapas da FEO nao podem formar ciclos entre pacotes");

    /**
     * Garante que processors concretos implementem o contrato da etapa.
     */
    @ArchTest
    static final ArchRule processors_devem_implementar_stage_processor = classes()
            .that()
            .haveSimpleNameEndingWith("Processor")
            .and(CLASSES_DE_ETAPA)
            .should()
            .implement(StageProcessor.class)
            .because("[ARQUITETURA] todo processor concreto da FEO deve implementar StageProcessor");

    /**
     * Garante que tecnologias concretas nao vazem para o nucleo.
     */
    @ArchTest
    static final ArchRule pipeline_raiz_nao_deve_depender_de_tecnologias_concretas = classes()
            .that()
            .resideInAPackage(PIPELINE_ROOT)
            .should(notDependOnConcreteTechnologies())
            .because("[ARQUITETURA] o nucleo da FEO nao pode depender de tecnologias concretas de execucao");

    /**
     * Cria condicao para impedir dependencia do nucleo em etapa concreta.
     */
    private static ArchCondition<JavaClass> notDependOnConcreteStages() {
        return new ArchCondition<>("[ARQUITETURA] nao depender de etapas concretas") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean valid = !CLASSES_DE_ETAPA.test(target);
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " nao pode depender de " + target.getName()));
                }
            }
        };
    }

    /**
     * Cria condicao para impedir dependencia direta entre etapas.
     */
    private static ArchCondition<JavaClass> notDependOnAnotherStage() {
        return new ArchCondition<>("[ARQUITETURA] nao depender de outra etapa concreta") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String sourceStage = stagePackage(javaClass);
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetStage = stagePackage(target);
                    boolean valid = targetStage == null || targetStage.equals(sourceStage);
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " da etapa " + sourceStage
                                    + " nao pode depender de " + target.getName() + " da etapa " + targetStage));
                }
            }
        };
    }

    /**
     * Cria condicao para bloquear tecnologias concretas no nucleo.
     */
    private static ArchCondition<JavaClass> notDependOnConcreteTechnologies() {
        return new ArchCondition<>("[ARQUITETURA] nao depender de WebClient, OpenAI, PDF, ZIP ou storage concreto") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    String target = dependency.getTargetClass().getName();
                    boolean valid = !target.contains("WebClient")
                            && !target.contains("PdfRendererBuilder")
                            && !target.contains("ZipOutputStream")
                            && !target.toLowerCase().contains("openai")
                            && !target.toLowerCase().contains("s3");
                    events.add(new SimpleConditionEvent(dependency, valid,
                            "[ARQUITETURA] " + javaClass.getName() + " nao pode depender de tecnologia concreta " + target));
                }
            }
        };
    }

    /**
     * Identifica a etapa concreta de uma classe, quando existir.
     */
    private static String stagePackage(JavaClass javaClass) {
        return STAGE_PACKAGES.stream()
                .filter(pkg -> javaClass.getPackageName().startsWith(pkg))
                .findFirst()
                .orElse(null);
    }
}
