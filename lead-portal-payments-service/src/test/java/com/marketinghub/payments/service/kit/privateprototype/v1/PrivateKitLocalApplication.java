package com.marketinghub.payments.service.kit.privateprototype.v1;

import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.repository.AgendaCheiaDeliveryRepository;
import com.marketinghub.payments.service.*;
import com.marketinghub.payments.service.kit.PrivateKitIllustrations;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Responsabilidade: executar o compositor real localmente sem cadastro de pagamentos ou envio
 * externo.
 */
@TestConfiguration
@EnableAutoConfiguration(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableScheduling
@Import(PrivateKitCompositionWorker.class)
public class PrivateKitLocalApplication {
  /** Inicia o consumidor oficial conectado exclusivamente ao backend local. */
  public static void main(String[] args) {
    new SpringApplicationBuilder(PrivateKitLocalApplication.class)
        .web(WebApplicationType.NONE)
        .run(
            "--spring.config.location=optional:classpath:private-kit-local/no-production.properties",
            "--product-ai.delivery.backend-base-url=http://127.0.0.1:57282",
            "--payments.admin-auth-token=kit-local-payments-only",
            "--agenda-cheia.production.storage-root=" + args[0],
            "--logging.level.root=WARN",
            "--logging.level.com.marketinghub.payments=INFO");
  }

  /**
   * Reutiliza o motor produtivo com contatos, dependências externas e imagens de teste segregados.
   */
  @Bean
  AgendaCheiaKitProductionService composer(org.springframework.core.env.Environment env) {
    return new AgendaCheiaKitProductionService(
        mock(AgendaCheiaDeliveryRepository.class),
        mock(DigitalProductPostPurchaseEmailService.class),
        new ObjectMapper(),
        new PrivateKitIllustrations(),
        env.getRequiredProperty("agenda-cheia.production.storage-root"),
        "http://127.0.0.1:57282");
  }

  /** Publica o serializador dos contratos do worker sem dependência de configuração de produção. */
  @Bean
  ObjectMapper json() {
    return new ObjectMapper();
  }
}
