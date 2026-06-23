package com.marketinghub.worker.targetingrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Testes do cliente que monta solicitações OpenAI para geração de targeting Meta Ads.
 */
class TargetingRequestChatGptClientTest {

    /**
     * Deve usar GPT-5.5 e modo flex no payload enviado para a Responses API.
     */
    @Test
    void shouldBuildResponsesPayloadWithGpt55AndFlexTier() throws Exception {
        TargetingRequestChatGptClient client = newClient("gpt-5.5");
        TargetingRequestDto request = new TargetingRequestDto(
                UUID.fromString("13de8684-4162-4654-b677-e9e824a19054"),
                "Nicho Comércio varejista de artigos do vestuário e acessórios",
                "pt_BR",
                "BR",
                TargetingAudienceType.PROSPECT,
                "PENDING_AI",
                90
        );

        String prompt = (String) ReflectionTestUtils.invokeMethod(client, "buildPrompt", request);
        Object context = ReflectionTestUtils.invokeMethod(client, "buildContext", request, prompt);
        Method payloadMethod = context.getClass().getDeclaredMethod("payload");
        payloadMethod.setAccessible(true);
        Map<?, ?> payload = (Map<?, ?>) payloadMethod.invoke(context);

        assertThat(payload.get("model")).isEqualTo("gpt-5.5");
        assertThat(payload.get("service_tier")).isEqualTo("flex");
    }

    /**
     * Deve orientar o modelo a gerar seeds com maior chance de resolução oficial na Meta.
     */
    @Test
    void shouldBuildPromptWithMetaTargetingSearchGuidance() {
        TargetingRequestChatGptClient client = newClient("gpt-5.5");
        TargetingRequestDto request = new TargetingRequestDto(
                UUID.randomUUID(),
                "Nicho Comércio varejista de artigos do vestuário e acessórios",
                "pt_BR",
                "BR",
                TargetingAudienceType.PROSPECT,
                "PENDING_AI",
                90
        );

        String prompt = (String) ReflectionTestUtils.invokeMethod(client, "buildPrompt", request);

        assertThat(prompt)
                .contains("taxonomia oficial do Meta Ads")
                .contains("adinterest/adTargetingCategory")
                .contains("adworkposition")
                .contains("Facebook Ads Worker na API da Meta")
                .contains("Nicho Comércio varejista de artigos do vestuário e acessórios");
    }

    /**
     * Cria o cliente com dependências isoladas para validar apenas montagem de payload e prompt.
     */
    private TargetingRequestChatGptClient newClient(String model) {
        return new TargetingRequestChatGptClient(
                WebClient.builder(),
                new ObjectMapper(),
                mock(AiGenerationRecorder.class),
                "test-key",
                "https://api.openai.com/v1",
                model,
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofMillis(50)
        );
    }
}
