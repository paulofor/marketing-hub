package com.marketinghub.worker.pipeline.gerasalespagev1;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/** Protege o isolamento arquitetural do executor GeraSalesPage v1. */
@AnalyzeClasses(packages = "com.marketinghub.worker", importOptions = ImportOption.DoNotIncludeTests.class)
class GeraSalesPageArchitectureTest {
    private static final String PACKAGE = "com.marketinghub.worker.pipeline.gerasalespagev1";

    /** Garante que o GeraSalesPage v1 não depende do GeraLanding. */
    @ArchTest
    static final ArchRule gerasalespage_nao_deve_depender_de_geralanding = classes()
            .that()
            .resideInAPackage("..pipeline.gerasalespagev1..")
            .should(notDependOnPackages(Set.of(".geralanding.", ".openai.core.wireframe.", ".openai.core.copy.")))
            .because("[ARQUITETURA] GeraSalesPage v1 deve ser independente do pipeline antigo GeraLanding");

    /** Garante que prompt e schema do GeraSalesPage v1 não sejam carregados de arquivos locais. */
    @ArchTest
    static final ArchRule gerasalespage_nao_deve_carregar_prompt_schema_do_classpath = classes()
            .that()
            .resideInAPackage("..pipeline.gerasalespagev1..")
            .should(notDependOnPackages(Set.of("org.springframework.core.io.")))
            .because("[ARQUITETURA] GeraSalesPage v1 deve receber prompt/schema do backend, persistidos no banco");

    /** Garante que somente o processor canônico possa acionar modelos de IA. */
    @ArchTest
    static final ArchRule somente_processor_deve_acessar_modelo = classes()
            .that()
            .resideInAPackage("..pipeline.gerasalespagev1..")
            .should(accessOpenAiOnlyFromProcessor())
            .because("[ARQUITETURA] páginas novas devem reutilizar o processor canônico e seus gates auditáveis");

    /** Garante um único processor, evitando implementações paralelas sem gates. */
    @ArchTest
    static void deve_existir_um_unico_processor_canonico(JavaClasses classes) {
        long processors = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().equals(PACKAGE))
                .filter(javaClass -> javaClass.getSimpleName().endsWith("Processor"))
                .count();
        if (processors != 1) {
            throw new AssertionError("[ARQUITETURA] GeraSalesPage v1 deve possuir exatamente um processor canônico; encontrado="
                    + processors);
        }
    }

    /** Bloqueia acesso ao cliente de IA fora do processor que aplica validação e auditoria. */
    private static ArchCondition<JavaClass> accessOpenAiOnlyFromProcessor() {
        return new ArchCondition<>("[ARQUITETURA] acessar OpenAI somente pelo GeraSalesPageProcessor") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean accessesOpenAi = javaClass.getMethodCallsFromSelf().stream()
                        .anyMatch(call -> call.getTargetOwner().isAssignableTo(OpenAiClientPort.class));
                if (accessesOpenAi && !javaClass.getSimpleName().equals("GeraSalesPageProcessor")) {
                    events.add(SimpleConditionEvent.violated(
                            javaClass,
                            "[ARQUITETURA] " + javaClass.getName()
                                    + " acessa OpenAI fora do processor canônico e pode ignorar os gates"));
                }
            }
        };
    }

    /** Cria condição explícita para impedir dependências em pacotes proibidos. */
    private static ArchCondition<JavaClass> notDependOnPackages(Set<String> forbiddenFragments) {
        return new ArchCondition<>("[ARQUITETURA] não depender de pacotes proibidos " + forbiddenFragments) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> forbiddenFragments.stream()
                                .anyMatch(fragment -> dependency.getTargetClass().getPackageName().contains(fragment)))
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(
                                javaClass,
                                "[ARQUITETURA] " + javaClass.getName() + " depende de "
                                        + dependency.getTargetClass().getName())));
            }
        };
    }
}
