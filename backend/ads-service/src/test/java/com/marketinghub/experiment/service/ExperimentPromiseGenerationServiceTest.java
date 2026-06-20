package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.openai.OpenAiBatchClient;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a geração de opções de promessa única para experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentPromiseGenerationServiceTest {
    @Mock
    private MarketNicheRepository nicheRepository;
    @Mock
    private HypothesisRepository hypothesisRepository;
    @Mock
    private OpenAiBatchClient openAiBatchClient;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private AiWorkerGenerationService generationService;
    @Mock
    private OpenAiPricingService pricingService;
    @InjectMocks
    private ExperimentPromiseGenerationService service;

    /** Deve bloquear geração sem nicho porque a IA precisa de contexto comercial mínimo. */
    @Test
    void shouldRejectRequestWithoutNiche() {
        assertThatThrownBy(() -> service.generate(new GenerateExperimentPromiseOptionsRequest(
                null, null, null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Selecione um nicho");
    }

    /** Deve retornar exatamente três opções completas e registrar a geração para auditoria. */
    @Test
    void shouldGenerateThreePromiseOptions() {
        MarketNiche niche = MarketNiche.builder().id(7L).name("Salões de beleza").build();
        when(nicheRepository.findById(7L)).thenReturn(Optional.of(niche));
        when(openAiBatchClient.executeSingle(any(), anyString())).thenReturn(new OpenAiResponse(
                "resp-1",
                "{\"options\":["
                        + "{\"singlePain\":\"Agenda vazia\",\"freeReward\":\"Checklist de reagendamento\",\"funnelPromise\":\"Receber o checklist\",\"primaryCta\":\"Receber checklist\",\"reason\":\"Direta\"},"
                        + "{\"singlePain\":\"Clientes somem\",\"freeReward\":\"3 mensagens prontas\",\"funnelPromise\":\"Receber as mensagens\",\"primaryCta\":\"Quero as mensagens\",\"reason\":\"Emocional\"},"
                        + "{\"singlePain\":\"Faltas no horário\",\"freeReward\":\"Roteiro de confirmação\",\"funnelPromise\":\"Receber o roteiro\",\"primaryCta\":\"Usar roteiro\",\"reason\":\"Operacional\"}]} ",
                null,
                new OpenAiResponse.OpenAiUsage(10, 20, null, null, 30),
                null,
                "completed"));
        when(pricingService.estimateStandardCost(anyString(), any())).thenReturn(BigDecimal.ZERO);

        var response = service.generate(new GenerateExperimentPromiseOptionsRequest(
                7L, null, "Hipótese", null, null, null, null));

        assertThat(response.options()).hasSize(3);
        assertThat(response.options().getFirst().singlePain()).isEqualTo("Agenda vazia");
        verify(generationService).recordGeneration(any());
    }
}
