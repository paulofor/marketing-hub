package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.promise.ExperimentPromiseGenerationRequest;
import com.marketinghub.experiment.promise.ExperimentPromiseGenerationRequestStatus;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentPromiseGenerationRequestRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar o registro assíncrono de opções de promessa única para experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentPromiseGenerationServiceTest {
    @Mock
    private MarketNicheRepository nicheRepository;
    @Mock
    private HypothesisRepository hypothesisRepository;
    @Mock
    private ExperimentPromiseGenerationRequestRepository requestRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
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

    /** Deve bloquear geração sem hipótese porque a IA precisa do pipeline completo da hipótese. */
    @Test
    void shouldRejectRequestWithoutHypothesis() {
        assertThatThrownBy(() -> service.generate(new GenerateExperimentPromiseOptionsRequest(
                7L, null, null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Selecione uma hipótese");
    }

    /** Deve registrar solicitação pendente sem acessar OpenAI diretamente pelo backend. */
    @Test
    void shouldRegisterPendingPromiseOptionsRequest() {
        UUID hypothesisId = UUID.randomUUID();
        MarketNiche niche = MarketNiche.builder()
                .id(7L)
                .name("Salões de beleza")
                .description("Nicho de profissionais locais")
                .promises("Promessa validada")
                .offers("Oferta validada")
                .build();
        Hypothesis hypothesis = Hypothesis.builder()
                .id(hypothesisId)
                .title("Hipótese de agenda")
                .problem("{\"summary\":\"Clientes somem depois do atendimento\",\"evidenceSignals\":[\"evidência extensa que não deve entrar no prompt\"]}")
                .promise("Agenda mais previsível")
                .mechanism("Régua de manutenção")
                .uniqueMechanism("Fluxo de manutenção guiada")
                .entrega("Mensagens prontas")
                .frameworkJson("{\"pain\":\"clientes somem\"}")
                .prompt("Prompt bruto antigo que não deve entrar")
                .build();
        when(nicheRepository.findById(7L)).thenReturn(Optional.of(niche));
        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.of(hypothesis));
        when(requestRepository.save(any())).thenAnswer(invocation -> {
            ExperimentPromiseGenerationRequest saved = invocation.getArgument(0);
            saved.setId(123L);
            return saved;
        });

        var response = service.generate(new GenerateExperimentPromiseOptionsRequest(
                7L, hypothesisId, "dor digitada", "recompensa digitada", "promessa digitada", "cta digitado", null));

        assertThat(response.requestId()).isEqualTo(123L);
        assertThat(response.status()).isEqualTo(ExperimentPromiseGenerationRequestStatus.PENDING.name());
        assertThat(response.options()).isEmpty();
        ArgumentCaptor<ExperimentPromiseGenerationRequest> requestCaptor = ArgumentCaptor.forClass(
                ExperimentPromiseGenerationRequest.class);
        verify(requestRepository).save(requestCaptor.capture());
        ExperimentPromiseGenerationRequest persisted = requestCaptor.getValue();
        assertThat(persisted.getPrompt())
                .contains("Nicho selecionado", "Hipótese selecionada", "Clientes somem depois do atendimento")
                .doesNotContain("evidência extensa", "Prompt bruto antigo", "dor digitada", "recompensa digitada");
        assertThat(persisted.getPrompt().length()).isLessThan(8_000);
        assertThat(persisted.getStatus()).isEqualTo(ExperimentPromiseGenerationRequestStatus.PENDING);
    }

    /** Deve retornar o status persistido para a tela acompanhar a solicitação até a conclusão. */
    @Test
    void shouldGetPersistedPromiseOptionsRequestStatus() {
        ExperimentPromiseGenerationRequest request = ExperimentPromiseGenerationRequest.builder()
                .status(ExperimentPromiseGenerationRequestStatus.PROCESSING)
                .build();
        request.setId(456L);
        when(requestRepository.findById(456L)).thenReturn(Optional.of(request));

        var response = service.get(456L);

        assertThat(response.requestId()).isEqualTo(456L);
        assertThat(response.status()).isEqualTo(ExperimentPromiseGenerationRequestStatus.PROCESSING.name());
        assertThat(response.options()).isEmpty();
    }

    /** Deve retornar o rascunho mais recente pelo backend, sem depender de armazenamento no navegador. */
    @Test
    void shouldGetLatestDraftFromBackend() {
        UUID hypothesisId = UUID.randomUUID();
        MarketNiche niche = MarketNiche.builder()
                .id(7L)
                .name("Manicure")
                .build();
        Hypothesis hypothesis = Hypothesis.builder()
                .id(hypothesisId)
                .title("Agenda irregular")
                .build();
        ExperimentPromiseGenerationRequest request = ExperimentPromiseGenerationRequest.builder()
                .niche(niche)
                .hypothesis(hypothesis)
                .status(ExperimentPromiseGenerationRequestStatus.PROCESSING)
                .currentSinglePain("agenda quebra")
                .currentFreeReward("mensagens prontas")
                .currentFunnelPromise("organizar agenda")
                .currentPrimaryCta("receber mensagens")
                .build();
        request.setId(789L);
        when(requestRepository.findFirstByStatusInOrderByCreatedAtDesc(List.of(
                ExperimentPromiseGenerationRequestStatus.PENDING,
                ExperimentPromiseGenerationRequestStatus.PROCESSING,
                ExperimentPromiseGenerationRequestStatus.COMPLETED))).thenReturn(Optional.of(request));

        var response = service.latestDraft();

        assertThat(response).isPresent();
        assertThat(response.get().requestId()).isEqualTo(789L);
        assertThat(response.get().nicheId()).isEqualTo(7L);
        assertThat(response.get().hypothesisId()).isEqualTo(hypothesisId);
        assertThat(response.get().currentSinglePain()).isEqualTo("agenda quebra");
    }

    /** Deve descartar o rascunho retomável para não manter atalho antigo após salvar o teste. */
    @Test
    void shouldDismissDraftAfterExperimentCreation() {
        ExperimentPromiseGenerationRequest request = ExperimentPromiseGenerationRequest.builder()
                .status(ExperimentPromiseGenerationRequestStatus.COMPLETED)
                .build();
        request.setId(987L);
        when(requestRepository.findById(987L)).thenReturn(Optional.of(request));

        service.dismiss(987L);

        assertThat(request.getStatus()).isEqualTo(ExperimentPromiseGenerationRequestStatus.DISMISSED);
        assertThat(request.getFinishedAt()).isNotNull();
    }

}
