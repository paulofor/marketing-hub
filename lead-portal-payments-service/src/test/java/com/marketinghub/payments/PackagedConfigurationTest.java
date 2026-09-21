package com.marketinghub.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/** Comprova a inicialização real com o YAML do módulo e a página entregue, sem integrações pagas. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:configuration-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.profiles.active=test", "delivery.enabled=false",
    "digital-product.delivery.email.enabled=false", "product-ai.delivery.enabled=false",
    "pde.entitlement.enabled=false", "mercado-pago.access-token=local-test",
    "mercado-pago.base-url=http://127.0.0.1:9", "agenda-cheia.production.openai-api-key=",
    "agenda-cheia.production.openai-base-url=http://127.0.0.1:9",
    "logging.file.name=target/configuration-test.log"
})
class PackagedConfigurationTest {
    @Autowired private TestRestTemplate client;

    /** Exige saúde e recursos comerciais servidos pelo Spring, carregando o YAML real. */
    @Test
    void bootsAndServesThePostPurchaseContract() {
        assertThat(client.getForEntity("/actuator/health", String.class).getBody()).contains("UP");
        var page = client.getForEntity("/agenda-cheia/obrigado.html", String.class);
        assertThat(page.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(page.getBody()).contains("até 3 dias úteis", "contato@digicomdigital.com.br", "kit-title");
        assertThat(client.getForObject("/agenda-cheia/obrigado.js", String.class))
            .contains("ENTREGUE", "Não foi possível confirmar o envio");
    }
}
