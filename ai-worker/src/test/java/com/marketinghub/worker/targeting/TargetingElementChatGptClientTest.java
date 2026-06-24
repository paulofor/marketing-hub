package com.marketinghub.worker.targeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Testes do cliente que gera e filtra públicos de Meta Ads para nichos. */
class TargetingElementChatGptClientTest {

    /** Deve montar prompt com curadoria comercial e contexto do nicho. */
    @Test
    void shouldBuildPromptWithCommercialCurationRules() throws Exception {
        TargetingElementChatGptClient client = newClient();
        MarketNiche niche = MarketNiche.builder()
                .id(24L)
                .name("Promoção de vendas")
                .description("Estratégias promocionais para aumentar vendas")
                .build();

        Object promptData = ReflectionTestUtils.invokeMethod(
                client,
                "buildPrompt",
                niche,
                TargetingElementType.INTEREST,
                3);
        Method promptMethod = promptData.getClass().getDeclaredMethod("prompt");
        promptMethod.setAccessible(true);
        String prompt = (String) promptMethod.invoke(promptData);

        assertThat(prompt)
                .contains("confidence >= 0.75")
                .contains("Reprove públicos genéricos")
                .contains("Promoção de vendas");
    }

    /** Deve descartar candidatos que o modelo avaliou com baixa aderência comercial. */
    @Test
    void shouldDiscardLowConfidenceCandidates() {
        TargetingElementChatGptClient client = newClient();
        MarketNiche niche = MarketNiche.builder().id(24L).name("Promoção de vendas").build();
        TargetingElementChatGptClient.TargetingBatchRequest request =
                new TargetingElementChatGptClient.TargetingBatchRequest(
                        niche,
                        TargetingElementType.INTEREST,
                        2,
                        "gpt-5.5");
        Object promptData = ReflectionTestUtils.invokeMethod(
                client,
                "buildPrompt",
                niche,
                TargetingElementType.INTEREST,
                2);
        Object context = new RequestContextAccessor().create(request, promptData, "gpt-5.5");

        @SuppressWarnings("unchecked")
        List<CreateTargetingElementRequest> parsed = ReflectionTestUtils.invokeMethod(
                client,
                "parseContent",
                "{\"items\":["
                        + "{\"term\":\"Facebook access (mobile): tablets\",\"description\":\"genérico\",\"confidence\":0.62},"
                        + "{\"term\":\"Digital marketing (marketing)\",\"description\":\"compatível\",\"confidence\":0.91}"
                        + "]}",
                context,
                null);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).getTerm()).isEqualTo("Digital marketing (marketing)");
    }

    /** Cria o cliente isolado para validar montagem de prompt e filtragem sem chamar OpenAI. */
    private TargetingElementChatGptClient newClient() {
        return new TargetingElementChatGptClient(
                WebClient.builder(),
                new ObjectMapper(),
                mock(AiGenerationRecorder.class),
                "test-key",
                "https://api.openai.com/v1",
                "gpt-5.5",
                Duration.ofMillis(50),
                Duration.ofSeconds(1));
    }

    /** Acessa o record privado de contexto via reflexão para testar o parser isoladamente. */
    private static class RequestContextAccessor {
        Object create(
                TargetingElementChatGptClient.TargetingBatchRequest request,
                Object prompt,
                String model) {
            try {
                Class<?> type = Class.forName(
                        "com.marketinghub.worker.targeting.TargetingElementChatGptClient$RequestContext");
                var constructor = type.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                return constructor.newInstance(request, prompt, java.util.Map.of(), model);
            } catch (Exception ex) {
                throw new IllegalStateException("Não foi possível montar o contexto de teste", ex);
            }
        }
    }
}
