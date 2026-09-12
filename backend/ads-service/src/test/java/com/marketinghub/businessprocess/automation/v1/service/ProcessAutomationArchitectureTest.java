package com.marketinghub.businessprocess.automation.v1.service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.*;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que o backend assuma polling ou tecnologias dos agentes executores. */
class ProcessAutomationArchitectureTest {
  /**
   * O coordenador conhece contratos de domínio e persistência, nunca rotinas operacionais de
   * execução.
   */
  @Test
  void backendCoordinatesOnlyCanonicalContracts() {
    var imported =
        new ClassFileImporter().importPackages("com.marketinghub.businessprocess.automation.v1");
    classes()
        .that()
        .resideInAPackage("com.marketinghub.businessprocess.automation.v1..")
        .should(
            new ArchCondition<JavaClass>(
                "[ARQUITETURA] preservar coordenação no backend e polling no executor") {
              /** Relata a dependência ou agendamento indevido com a classe de origem. */
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                for (var dependency : item.getDirectDependenciesFromSelf()) {
                  String target = dependency.getTargetClass().getName();
                  if (target.startsWith("java.net.http.")
                      || target.contains("WebClient")
                      || target.contains("RestTemplate")
                      || target.contains("playwright")
                      || target.startsWith("com.openai."))
                    events.add(
                        SimpleConditionEvent.violated(
                            item,
                            "[ARQUITETURA] "
                                + item.getName()
                                + " depende de tecnologia externa: "
                                + target));
                }
                for (var method : item.getMethods())
                  if (method.isAnnotatedWith("org.springframework.scheduling.annotation.Scheduled"))
                    events.add(
                        SimpleConditionEvent.violated(
                            method,
                            "[ARQUITETURA] polling deve ficar no process-execution-worker: "
                                + method.getFullName()));
              }
            })
        .check(imported);
  }
}
