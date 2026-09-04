package com.marketinghub.harnesslibraryapi.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.harnesslibraryapi.HarnessLibraryApiApplication;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/** Impede o gateway externo de virar uma segunda fonte de verdade ou fragmentar sua API. */
class ArchitectureTest {
  private static JavaClasses classes;

  /** Importa somente o módulo independente analisado. */
  @BeforeAll
  static void importClasses() {
    classes = new ClassFileImporter().importPackagesOf(HarnessLibraryApiApplication.class);
  }

  /** Proíbe qualquer tecnologia de banco ou repository dentro do gateway. */
  @Test
  void shouldNotAccessDatabase() {
    classes()
        .should(notDependOnDatabaseTechnology())
        .because("[ARQUITETURA] somente o backend principal pode acessar o banco")
        .check(classes);
  }

  /** Garante uma única superfície HTTP e uma única orquestração funcional. */
  @Test
  void shouldKeepSingleControllerAndService() {
    long controllers =
        classes.stream().filter(type -> type.isAnnotatedWith(RestController.class)).count();
    long services = classes.stream().filter(type -> type.isAnnotatedWith(Service.class)).count();

    assertThat(controllers)
        .as("[ARQUITETURA] o módulo deve possuir exatamente um controller")
        .isEqualTo(1);
    assertThat(services)
        .as("[ARQUITETURA] o módulo deve possuir exatamente um service")
        .isEqualTo(1);
  }

  /** Seleciona todas as classes do módulo para aplicar condições explícitas. */
  private com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction classes() {
    return com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
        .that()
        .resideInAPackage("com.marketinghub.harnesslibraryapi..");
  }

  /** Detecta dependências diretas em APIs de persistência com mensagem rastreável. */
  private ArchCondition<JavaClass> notDependOnDatabaseTechnology() {
    Set<String> forbiddenPrefixes =
        Set.of(
            "jakarta.persistence.",
            "javax.persistence.",
            "javax.sql.",
            "org.springframework.data.jpa.",
            "org.springframework.jdbc.");
    return new ArchCondition<>("[ARQUITETURA] não depender de tecnologia de banco") {
      /** Inspeciona dependências de cada classe e relata origem e destino proibidos. */
      @Override
      public void check(JavaClass item, com.tngtech.archunit.lang.ConditionEvents events) {
        for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
          String target = dependency.getTargetClass().getName();
          if (forbiddenPrefixes.stream().anyMatch(target::startsWith)) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    "[ARQUITETURA] "
                        + item.getName()
                        + " depende de tecnologia de banco proibida: "
                        + target));
          }
        }
      }
    };
  }
}
