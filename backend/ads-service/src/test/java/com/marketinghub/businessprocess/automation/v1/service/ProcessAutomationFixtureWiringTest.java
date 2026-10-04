package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/** Responsabilidade: detectar dependências ausentes na fixture antes da rodada MySQL completa. */
class ProcessAutomationFixtureWiringTest {
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
