package com.marketinghub.mois.bibliotecapaginavenda.worker.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Garante contratos estruturais de rastreabilidade OpenAI consumidos pelo PromptBuilder. */
@AnalyzeClasses(packages = "com.marketinghub.mois.bibliotecapaginavenda.worker")
class ArquiteturaTest {

    /** Valida assinatura obrigatória com 4 parâmetros para uso direto do PromptBuilder. */
    @ArchTest
    static final ArchRule should_have_prompt_builder_record_method = classes()
            .that()
            .haveSimpleName("OpenAiPromptResultRecorder")
            .and()
            .resideInAPackage("..mois.bibliotecapaginavenda.worker.v1.service")
            .should(haveRequiredRecordMethod());

    /** Valida assinatura obrigatória com 5 parâmetros para inserção no backend com jobId Marketing Hub. */
    @ArchTest
    static final ArchRule should_have_backend_insert_method = classes()
            .that()
            .haveSimpleName("OpenAiPromptResultRecorder")
            .and()
            .resideInAPackage("..mois.bibliotecapaginavenda.worker.v1.service")
            .should(haveRequiredInsertMethod());

    private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass> haveRequiredRecordMethod() {
        return new ArchCondition<>("[ARQUITETURA] have method recordPromptBuilderOpenAiResult(String, String, String, String)") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getMethods().stream()
                        .filter(method -> method.getName().equals("recordPromptBuilderOpenAiResult"))
                        .anyMatch(ArquiteturaTest::hasFourStringParameters);
                events.add(new SimpleConditionEvent(javaClass, hasMethod,
                        "[ARQUITETURA] " + javaClass.getName() + " must declare method recordPromptBuilderOpenAiResult(String, String, String, String)"));
            }
        };
    }

    private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass> haveRequiredInsertMethod() {
        return new ArchCondition<>("[ARQUITETURA] have method insertOpenAiIntegrationRecord(String, String, String, String, Long)") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getMethods().stream()
                        .filter(method -> method.getName().equals("insertOpenAiIntegrationRecord"))
                        .anyMatch(ArquiteturaTest::hasExpectedInsertParameters);
                events.add(new SimpleConditionEvent(javaClass, hasMethod,
                        "[ARQUITETURA] " + javaClass.getName() + " must declare method insertOpenAiIntegrationRecord(String, String, String, String, Long)"));
            }
        };
    }

    private static boolean hasFourStringParameters(JavaMethod method) {
        return method.getRawParameterTypes().size() == 4
                && method.getRawParameterTypes().stream().allMatch(type -> type.getName().equals(String.class.getName()));
    }

    private static boolean hasExpectedInsertParameters(JavaMethod method) {
        return method.getRawParameterTypes().size() == 5
                && method.getRawParameterTypes().get(0).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(1).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(2).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(3).getName().equals(String.class.getName())
                && method.getRawParameterTypes().get(4).getName().equals(Long.class.getName());
    }
}
