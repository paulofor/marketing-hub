package com.marketinghub.metaadapproverworker;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a independência e os limites do Aprovador Meta. */
class ArchitectureTest {
  /** Impede acesso direto a banco, OpenAI e publicadores de mídia. */
  @Test
  void forbidsDatabaseOpenAiAndPublicationDependencies() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("com.marketinghub.metaadapproverworker");

    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "jakarta.persistence..",
            "org.springframework.data..",
            "java.sql..",
            "com.openai..",
            "com.marketinghub.facebook..")
        .because(
            "[ARQUITETURA] o Aprovador usa Codex/MCP e o backend, sem banco, OpenAI direto ou publicação")
        .check(classes);
  }

  /** Confirma que o módulo possui exatamente um scheduler e um runner Codex. */
  @Test
  void hasSingleOperationalEntryAndCodexRunner() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("com.marketinghub.metaadapproverworker");
    long schedulers =
        classes.stream().filter(type -> type.getSimpleName().endsWith("Scheduler")).count();
    long runners =
        classes.stream().filter(type -> type.getSimpleName().endsWith("CodexRunner")).count();

    assertThat(schedulers)
        .as("[ARQUITETURA] deve existir um único scheduler canônico")
        .isEqualTo(1);
    assertThat(runners)
        .as("[ARQUITETURA] deve existir um único runner Codex canônico")
        .isEqualTo(1);
  }

  /** Impede construtores concorrentes de tornarem ambígua a injeção do scheduler pelo Spring. */
  @Test
  void schedulerHasSingleCanonicalConstructor() {
    assertThat(MetaAdApproverScheduler.class.getDeclaredConstructors())
        .as("[ARQUITETURA] o scheduler deve possuir um único construtor canônico para o Spring")
        .hasSize(1);
  }
}
