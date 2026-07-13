package com.marketinghub.scientificresearch.architecture;

import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageProcessor;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Protege o protocolo padrão módulo aplicado ao worker de pesquisa científica.
 */
class ScientificResearchArchitectureTest {

    private static final String ROOT = "com.marketinghub.scientificresearch";
    private static final String PIPELINE = ROOT + ".productevidence.v1.pipeline..";

    private final JavaClasses importedClasses = new ClassFileImporter().importPath(Path.of("target/classes"));

    /**
     * Garante que o núcleo genérico não conheça etapas concretas.
     */
    @Test
    void coreMustNotDependOnConcreteStages() {
        classes()
                .that()
                .resideInAPackage(PIPELINE)
                .should(notDependOnConcreteStage("[ARQUITETURA] Núcleo do pipeline não pode depender de etapa concreta."))
                .check(importedClasses);
    }

    /**
     * Garante que uma etapa concreta não importe outra etapa concreta.
     */
    @Test
    void concreteStagesMustNotDependOnEachOther() {
        classes()
                .that()
                .resideInAPackage(ROOT + ".productevidence.v1..")
                .and()
                .resideOutsideOfPackage(PIPELINE)
                .should(notDependOnSiblingStage("[ARQUITETURA] Etapa concreta não pode depender diretamente de outra etapa concreta."))
                .check(importedClasses);
    }

    /**
     * Garante que processors concretos implementem o contrato canônico.
     */
    @Test
    void concreteProcessorsMustImplementStageProcessor() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Processor")
                .and()
                .resideOutsideOfPackage(PIPELINE)
                .should()
                .implement(StageProcessor.class)
                .because("[ARQUITETURA] Processor concreto deve implementar StageProcessor.")
                .check(importedClasses);
    }

    /**
     * Bloqueia dependências diretas de banco de dados no worker.
     */
    @Test
    void workerMustNotUseDatabaseDependencies() {
        classes()
                .that()
                .resideInAPackage(ROOT + "..")
                .should(notDependOnDatabase("[ARQUITETURA] Scientific Research Worker não pode acessar banco diretamente."))
                .check(importedClasses);
    }

    /**
     * Confirma que o catálogo v1 possui as três etapas operacionais esperadas.
     */
    @Test
    void pipelineMustHaveThreeConcreteProcessors() {
        long processorCount = importedClasses.stream()
                .filter(javaClass -> javaClass.getSimpleName().endsWith("Processor"))
                .filter(javaClass -> !javaClass.getPackageName().endsWith(".pipeline"))
                .count();
        assertThat(processorCount)
                .as("[ARQUITETURA] Pipeline product-evidence v1 deve ter processors para descoberta, síntese e entregável.")
                .isEqualTo(3);
    }

    /**
     * Cria condição que bloqueia dependência do núcleo em etapa concreta.
     */
    private ArchCondition<JavaClass> notDependOnConcreteStage(String message) {
        return new ArchCondition<>(message) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String target = dependency.getTargetClass().getPackageName();
                    if (isConcreteStagePackage(target)) {
                        events.add(SimpleConditionEvent.violated(
                                item,
                                message + " Classe " + item.getName() + " depende de " + dependency.getTargetClass().getName()));
                    }
                });
            }
        };
    }

    /**
     * Cria condição que bloqueia dependência entre etapas irmãs.
     */
    private ArchCondition<JavaClass> notDependOnSiblingStage(String message) {
        return new ArchCondition<>(message) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String ownStage = stageSegment(item.getPackageName());
                if (ownStage == null) {
                    return;
                }
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetStage = stageSegment(dependency.getTargetClass().getPackageName());
                    if (targetStage != null && !ownStage.equals(targetStage)) {
                        events.add(SimpleConditionEvent.violated(
                                item,
                                message + " Classe " + item.getName() + " depende de etapa " + targetStage));
                    }
                });
            }
        };
    }

    /**
     * Cria condição que bloqueia dependências de persistência local.
     */
    private ArchCondition<JavaClass> notDependOnDatabase(String message) {
        return new ArchCondition<>(message) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String target = dependency.getTargetClass().getName();
                    if (target.startsWith("jakarta.persistence")
                            || target.startsWith("javax.persistence")
                            || target.startsWith("org.springframework.data")
                            || target.contains(".jdbc.")
                            || target.contains(".r2dbc.")) {
                        events.add(SimpleConditionEvent.violated(
                                item,
                                message + " Classe " + item.getName() + " depende de " + target));
                    }
                });
            }
        };
    }

    /**
     * Identifica se o pacote pertence a uma etapa concreta.
     */
    private boolean isConcreteStagePackage(String packageName) {
        return stageSegment(packageName) != null;
    }

    /**
     * Extrai o segmento da etapa concreta dentro do pipeline v1.
     */
    private String stageSegment(String packageName) {
        String prefix = ROOT + ".productevidence.v1.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String rest = packageName.substring(prefix.length());
        if (rest.startsWith("pipeline") || rest.startsWith("backend")) {
            return null;
        }
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
