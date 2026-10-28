package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Responsabilidade: detectar dependências ausentes na fixture antes da rodada MySQL completa. */
class ProcessAutomationFixtureWiringTest {
  /** Confere os imports e beans das duas aplicações locais contra o construtor real do motor. */
  @Test
  void fixturesProvideEveryRequiredExecutionDependency() {
    for (Class<?> fixture :
        java.util.List.of(
            ProcessAutomationLocalApplication.class,
            com.marketinghub.businessprocesschain.learningcycle.v1.service
                .LearningCycleLocalApplication.class)) {
      var available =
          new HashSet<Class<?>>(Arrays.asList(fixture.getAnnotation(Import.class).value()));
      Arrays.stream(fixture.getDeclaredMethods())
          .filter(method -> method.isAnnotationPresent(Bean.class))
          .map(java.lang.reflect.Method::getReturnType)
          .forEach(available::add);
      for (Class<?> dependency : ProcessRunService.class.getConstructors()[0].getParameterTypes()) {
        if (!dependency.getName().startsWith("com.marketinghub.")) continue;
        assertThat(available.stream().anyMatch(dependency::isAssignableFrom))
            .as(
                "[ARQUITETURA] %s deve fornecer %s ao motor real",
                fixture.getSimpleName(), dependency.getSimpleName())
            .isTrue();
      }
    }
  }

  /** Inicializa o contexto real com os beans declarados pela fixture e JDBC sem acesso externo. */
  @Test
  void fixtureProvidesEveryRequiredContextDependency() throws ReflectiveOperationException {
    var required = Arrays.asList(ProcessRunContext.class.getConstructors()[0].getParameterTypes());
    var fixture = new ProcessAutomationLocalApplication();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
      for (var method : ProcessAutomationLocalApplication.class.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(Bean.class) || !required.contains(method.getReturnType()))
          continue;
        Object[] arguments =
            Arrays.stream(method.getParameterTypes()).map(type -> mock(type)).toArray();
        register(context, method.getReturnType(), method.invoke(fixture, arguments));
      }
      context.registerBean(ProcessRunContext.class);
      context.refresh();
      assertThat(context.getBean(ProcessRunContext.class)).isNotNull();
    }
  }

  /**
   * Registra a instância criada pelo próprio método da fixture, preservando seu tipo de contrato.
   */
  private static <T> void register(
      AnnotationConfigApplicationContext context, Class<T> type, Object value) {
    context.getBeanFactory().registerSingleton(type.getName(), type.cast(value));
  }
}
