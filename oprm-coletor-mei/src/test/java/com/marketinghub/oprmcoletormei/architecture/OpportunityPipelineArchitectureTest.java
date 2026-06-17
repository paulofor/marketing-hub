package com.marketinghub.oprmcoletormei.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageProcessor;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Garante que o fluxo opportunity do coletor OPRM segue o protocolo padrão módulo com núcleo genérico e etapas plugáveis.
 */
class OpportunityPipelineArchitectureTest {
    private static final String BASE_PACKAGE = "com.marketinghub.oprmcoletormei.opportunity";
    private static final String PIPELINE_PACKAGE = BASE_PACKAGE + ".pipeline";
    private static final Set<String> CONCRETE_STAGES = Set.of("score", "enrichment");
    private static final Set<String> FORBIDDEN_CORE_TECH_PACKAGES = Set.of(
            "org.springframework",
            "org.jsoup",
            "com.microsoft.playwright",
            "org.openqa.selenium",
            "software.amazon.awssdk",
            "com.openai");

    private static final DescribedPredicate<JavaClass> ARE_IN_CONCRETE_STAGE =
            new DescribedPredicate<>("[ARQUITETURA] classes de etapas concretas abaixo de opportunity.<etapa>") {
                /** Identifica se a classe pertence a uma etapa concreta do fluxo opportunity. */
                @Override
                public boolean test(JavaClass input) {
                    return stageNameOf(input) != null;
                }
            };

    /** Valida que o núcleo pipeline não depende de etapas concretas do fluxo opportunity. */
    @Test
    void pipelineCoreShouldNotDependOnConcreteStages() {
        JavaClasses importedClasses = importOpportunityProductionClasses();

        classes()
                .that().resideInAPackage(PIPELINE_PACKAGE + "..")
                .should(notDependOnConcreteOpportunityStages())
                .because("[ARQUITETURA] o núcleo pipeline opportunity deve conhecer apenas contratos genéricos")
                .check(importedClasses);
    }

    /** Valida que etapas concretas não dependem diretamente umas das outras. */
    @Test
    void concreteStagesShouldNotDependOnOtherConcreteStages() {
        JavaClasses importedClasses = importOpportunityProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .should(notDependOnOtherConcreteStages())
                .because("[ARQUITETURA] etapas opportunity devem ser removíveis e substituíveis sem acoplamento cruzado")
                .check(importedClasses);
    }

    /** Valida que processors concretos implementam o contrato genérico StageProcessor. */
    @Test
    void concreteStageProcessorsShouldImplementGenericStageProcessor() {
        JavaClasses importedClasses = importOpportunityProductionClasses();

        classes()
                .that(ARE_IN_CONCRETE_STAGE)
                .and().haveSimpleNameEndingWith("Processor")
                .should().beAssignableTo(StageProcessor.class)
                .because("[ARQUITETURA] processors opportunity devem implementar o contrato genérico StageProcessor")
                .check(importedClasses);
    }

    /** Valida que o núcleo genérico não conhece tecnologias concretas de execução ou integração externa. */
    @Test
    void pipelineCoreShouldNotDependOnConcreteTechnologies() {
        JavaClasses importedClasses = importOpportunityProductionClasses();

        classes()
                .that().resideInAPackage(PIPELINE_PACKAGE + "..")
                .should(notDependOnConcreteTechnologies())
                .because("[ARQUITETURA] tecnologias concretas devem ficar nas etapas ou infraestrutura compartilhada, nunca no núcleo")
                .check(importedClasses);
    }

    /** Importa somente classes de produção do pacote opportunity para reduzir ruído arquitetural. */
    private JavaClasses importOpportunityProductionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

    /** Cria condição explícita que bloqueia dependência do núcleo em etapas concretas. */
    private ArchCondition<JavaClass> notDependOnConcreteOpportunityStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de pacotes concretos " + BASE_PACKAGE + ".<etapa>") {
            /** Verifica dependências diretas saindo do núcleo pipeline. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    if (stageNameOf(target) != null) {
                        String message = "[ARQUITETURA] " + source.getName()
                                + " está no núcleo pipeline mas depende da etapa concreta "
                                + target.getName() + " via: " + dependency.getDescription();
                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    /** Cria condição explícita que bloqueia dependência direta entre etapas concretas diferentes. */
    private ArchCondition<JavaClass> notDependOnOtherConcreteStages() {
        return new ArchCondition<>("[ARQUITETURA] não depender de outra etapa concreta opportunity") {
            /** Verifica dependências diretas entre pacotes de etapas concretas. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceStage = stageNameOf(source);
                if (sourceStage == null) {
                    return;
                }
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetStage = stageNameOf(target);
                    if (targetStage != null && !sourceStage.equals(targetStage)) {
                        String message = "[ARQUITETURA] " + source.getName()
                                + " pertence à etapa '" + sourceStage + "' mas depende da etapa '"
                                + targetStage + "' via: " + dependency.getDescription();
                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    /** Cria condição explícita que bloqueia tecnologia concreta no núcleo pipeline. */
    private ArchCondition<JavaClass> notDependOnConcreteTechnologies() {
        return new ArchCondition<>("[ARQUITETURA] não depender de tecnologia concreta no núcleo opportunity.pipeline") {
            /** Verifica dependências diretas contra pacotes de tecnologia concreta. */
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    boolean forbidden = FORBIDDEN_CORE_TECH_PACKAGES.stream().anyMatch(targetPackage::startsWith);
                    if (forbidden) {
                        String message = "[ARQUITETURA] " + source.getName()
                                + " está no núcleo pipeline mas depende de tecnologia concreta "
                                + dependency.getTargetClass().getName() + " via: " + dependency.getDescription();
                        events.add(SimpleConditionEvent.violated(source, message));
                    }
                }
            }
        };
    }

    /** Extrai o nome da etapa concreta quando a classe está em pacote opportunity.<etapa>. */
    private static String stageNameOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(BASE_PACKAGE + ".")) {
            return null;
        }
        String remainder = packageName.substring((BASE_PACKAGE + ".").length());
        String topLevelPackage = remainder.contains(".") ? remainder.substring(0, remainder.indexOf('.')) : remainder;
        return CONCRETE_STAGES.contains(topLevelPackage) ? topLevelPackage : null;
    }
}
