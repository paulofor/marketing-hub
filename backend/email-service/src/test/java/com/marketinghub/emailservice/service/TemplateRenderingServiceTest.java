package com.marketinghub.emailservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateRenderingServiceTest {

    private final TemplateRenderingService templateRenderingService = new TemplateRenderingService();

    @Test
    void shouldRenderTemplateWithCurlyPlaceholders() {
        String template = "Olá {{nome}}, sua campanha {{campanha}} foi aprovada!";
        Map<String, Object> variables = Map.of("nome", "Ana", "campanha", "Promoção Dia das Mães");

        String rendered = templateRenderingService.render(template, variables);

        assertThat(rendered).isEqualTo("Olá Ana, sua campanha Promoção Dia das Mães foi aprovada!");
    }

    @Test
    void shouldRenderTemplateWithDefaultPlaceholders() {
        String template = "Olá ${nome}, o número do pedido é ${pedido}";
        Map<String, Object> variables = Map.of("nome", "Carlos", "pedido", "#123");

        String rendered = templateRenderingService.render(template, variables);

        assertThat(rendered).isEqualTo("Olá Carlos, o número do pedido é #123");
    }

    @Test
    void shouldIgnoreMissingVariables() {
        String template = "Olá {{nome}}, seu cupom é {{cupom}}";

        String rendered = templateRenderingService.render(template, Map.of("nome", "Laura"));

        assertThat(rendered).isEqualTo("Olá Laura, seu cupom é {{cupom}}");
    }
}
