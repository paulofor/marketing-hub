package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Protege a biblioteca MOIS backend para manter execução de IA no worker externo. */
@AnalyzeClasses(packages = "com.marketinghub", importOptions = ImportOption.DoNotIncludeTests.class)
class MoisSalesLibraryArchitectureTest {

    /** Garante que o backend da biblioteca não vire executor runtime de OpenAI. */
    @ArchTest
    static final ArchRule backend_mois_sales_library_nao_deve_chamar_openai_diretamente = classes()
            .that()
            .resideInAPackage("..mois.bibliotecapaginavenda.worker.v1..")
            .should(notDependOnOpenAiRuntimeClients())
            .because("[ARQUITETURA] backend MOIS deve persistir contratos/custos; OpenAI deve rodar no mois-sales-library-worker");

    private static ArchCondition<JavaClass> notDependOnOpenAiRuntimeClients() {
        return new ArchCondition<>("[ARQUITETURA] não depender de clients runtime OpenAI") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String packageName = target.getPackageName();
                    boolean invalid = packageName.startsWith("okhttp3")
                            || packageName.startsWith("org.springframework.web.reactive.function.client")
                            || target.getName().contains("OpenAiClient");
                    events.add(new SimpleConditionEvent(dependency, !invalid,
                            "[ARQUITETURA] " + javaClass.getName()
                                    + " não pode depender de client runtime OpenAI " + target.getName()));
                }
            }
        };
    }
}
