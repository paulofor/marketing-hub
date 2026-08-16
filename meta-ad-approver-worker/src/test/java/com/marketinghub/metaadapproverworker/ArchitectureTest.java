package com.marketinghub.metaadapproverworker;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

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

  /** Confirma as duas entradas isoladas e preserva um único runner Codex revisor. */
  @Test
  void hasOneOperationalEntryPerContainerRoleAndSingleCodexRunner() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("com.marketinghub.metaadapproverworker");
    long schedulers =
        classes.stream().filter(type -> type.getSimpleName().endsWith("Scheduler")).count();
    long runners =
        classes.stream().filter(type -> type.getSimpleName().endsWith("CodexRunner")).count();

    assertThat(schedulers)
        .as("[ARQUITETURA] devem existir apenas os schedulers de revisão e produção")
        .isEqualTo(2);
    assertThat(runners)
        .as("[ARQUITETURA] deve existir um único runner Codex canônico")
        .isEqualTo(1);
  }

  /** Impede construtores concorrentes de tornarem ambígua a injeção dos schedulers pelo Spring. */
  @Test
  void schedulersHaveSingleCanonicalConstructor() {
    assertThat(MetaAdApproverScheduler.class.getDeclaredConstructors())
        .as("[ARQUITETURA] o scheduler revisor deve possuir um único construtor canônico")
        .hasSize(1);
    assertThat(TemisImageStudioScheduler.class.getDeclaredConstructors())
        .as("[ARQUITETURA] o scheduler produtor deve possuir um único construtor canônico")
        .hasSize(1);
  }

  /** Garante que cada scheduler seja ativado somente no papel do seu próprio container. */
  @Test
  void schedulersAreSegregatedByExecutionRole() {
    ConditionalOnProperty review =
        MetaAdApproverScheduler.class.getAnnotation(ConditionalOnProperty.class);
    ConditionalOnProperty studio =
        TemisImageStudioScheduler.class.getAnnotation(ConditionalOnProperty.class);

    assertThat(review).as("[ARQUITETURA] o revisor deve declarar condição de papel").isNotNull();
    assertThat(review.havingValue()).isEqualTo("review");
    assertThat(studio).as("[ARQUITETURA] o produtor deve declarar condição de papel").isNotNull();
    assertThat(studio.havingValue()).isEqualTo("image-studio");
    assertThat(Arrays.stream(MetaAdApproverScheduler.class.getDeclaredFields()))
        .as("[ARQUITETURA] o revisor não pode receber processors produtivos")
        .noneMatch(
            field ->
                field.getType().equals(TemisImageStudioProcessor.class)
                    || field.getType().equals(TemisCreativeImprovementProcessor.class));
  }
}
