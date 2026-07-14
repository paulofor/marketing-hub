package com.marketinghub.mds.productevidence.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.mds.productevidence.v1.MdsProductEvidenceStageExecution;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.mds.MdsProductEvidenceStageExecutionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar o workflow backend do pacote científico de produto. */
@ExtendWith(MockitoExtension.class)
class ProductEvidenceWorkflowServiceTest {
    @Mock
    private MdsProductEvidenceStageExecutionRepository executionRepository;

    @Mock
    private HypothesisPainStageExecutionRepository hypothesisExecutionRepository;

    @Mock
    private HypothesisRepository hypothesisRepository;

    @Mock
    private MarketNicheRepository marketNicheRepository;

    private ProductEvidenceWorkflowService service;

    /** Prepara o serviço de evidência científica com dependências isoladas. */
    @BeforeEach
    void setup() {
        service = new ProductEvidenceWorkflowService(
                executionRepository,
                hypothesisExecutionRepository,
                hypothesisRepository,
                marketNicheRepository,
                new ObjectMapper());
    }

    /** Deve iniciar a pesquisa científica e bloquear a oferta quando ainda não há pacote aprovado. */
    @Test
    void requireApprovedEvidencePackStartsScientificResearchAndBlocksOffer() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        niche.setName("Manicure em domicílio");
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByCreatedAtDesc(
                        18L,
                        "deliverable-composer",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());
        when(executionRepository.findTopByMarketNicheIdAndStatusInOrderByCreatedAtDesc(
                        any(),
                        any()))
                .thenReturn(Optional.empty());
        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        when(hypothesisRepository.findByMarketNicheId(18L)).thenReturn(List.of());
        mockStage("hypothesis-pain", "agenda instável");
        mockStage("hypothesis-result", "agenda previsível");
        mockStage("hypothesis-mechanism", "confirmação e regras");
        mockStage("hypothesis-proof", "diagnóstico de 7 dias");

        assertThatThrownBy(() -> service.requireApprovedEvidencePack(18L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base científica");

        ArgumentCaptor<MdsProductEvidenceStageExecution> captor =
                ArgumentCaptor.forClass(MdsProductEvidenceStageExecution.class);
        verify(executionRepository).save(captor.capture());
        assertThat(captor.getValue().getStageCode()).isEqualTo("source-discovery");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDENTE");
        assertThat(captor.getValue().getScientificQuestion()).contains("confirmação e regras");
    }

    /** Deve usar a hipótese manual mais recente quando o pipeline de hipótese ainda não gerou etapas. */
    @Test
    void ensureProductEvidenceStartedFallsBackToLatestHypothesisForManualFlow() {
        MarketNiche niche = new MarketNiche();
        niche.setId(31L);
        niche.setName("Mulheres urbanas sofisticação acessível");
        Hypothesis hypothesis = Hypothesis.builder()
                .title("MUSA-H001")
                .persona("Mulher urbana de 25 a 45 anos")
                .problem("Quer parecer sofisticada sem gastar com luxo.")
                .promise("Parecer mais elegante com escolhas simples de beleza e imagem.")
                .mechanism("Metodo Elegancia Acessivel")
                .entrega("Checklist gratuito de sinais de sofisticação.")
                .createdAt(Instant.parse("2026-07-14T10:00:00Z"))
                .build();
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByCreatedAtDesc(
                        31L,
                        "deliverable-composer",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());
        when(executionRepository.findTopByMarketNicheIdAndStatusInOrderByCreatedAtDesc(
                        any(),
                        any()))
                .thenReturn(Optional.empty());
        when(marketNicheRepository.findById(31L)).thenReturn(Optional.of(niche));
        when(hypothesisRepository.findByMarketNicheId(31L)).thenReturn(List.of(hypothesis));
        mockEmptyStages(31L);

        service.ensureProductEvidenceStarted(31L);

        ArgumentCaptor<MdsProductEvidenceStageExecution> captor =
                ArgumentCaptor.forClass(MdsProductEvidenceStageExecution.class);
        verify(executionRepository).save(captor.capture());
        assertThat(captor.getValue().getProductIdea()).contains("Parecer mais elegante");
        assertThat(captor.getValue().getScientificQuestion()).contains("Metodo Elegancia Acessivel");
        assertThat(captor.getValue().getInputPayload()).contains("MUSA-H001");
        assertThat(captor.getValue().getInputPayload()).contains("Quer parecer sofisticada");
    }

    /** Deve entregar pendência no contrato esperado pelo scientific-research-worker. */
    @Test
    void listPendingMarksExecutionAsProcessingAndReturnsWorkerContract() {
        MdsProductEvidenceStageExecution execution = execution(
                77L,
                "source-discovery",
                "PENDENTE",
                "{\"pain\":\"agenda instável\"}");
        when(executionRepository.findByStageCodeAndStatusOrderByCreatedAtAsc(
                        any(),
                        any(),
                        any(Pageable.class)))
                .thenReturn(List.of(execution));

        List<ProductEvidenceStagePendingResponse> pending = service.listPending("SOURCE_DISCOVERY", 5);

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().executionId()).isEqualTo("77");
        assertThat(pending.getFirst().callbackUrl()).contains("/source-discovery/stage-executions/77/callback");
        assertThat(pending.getFirst().input()).containsEntry("pain", "agenda instável");
        assertThat(execution.getStatus()).isEqualTo("PROCESSANDO");
        assertThat(execution.getProcessingStartedAt()).isNotNull();
    }

    /** Deve persistir resultado e enfileirar a próxima etapa quando o worker concluir com sucesso. */
    @Test
    void receiveCallbackCreatesNextStageFromEnumStageCode() {
        MdsProductEvidenceStageExecution execution = execution(
                77L,
                "source-discovery",
                "PROCESSANDO",
                "{\"pain\":\"agenda instável\"}");
        when(executionRepository.findByIdAndStageCode(77L, "source-discovery")).thenReturn(Optional.of(execution));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusInOrderByCreatedAtDesc(
                        18L,
                        "evidence-synthesis",
                        List.of("PENDENTE", "PROCESSANDO", "CONCLUIDO", "BLOQUEADO", "FALHA")))
                .thenReturn(Optional.empty());

        service.receiveCallback(
                "source-discovery",
                77L,
                new ProductEvidenceStageCallbackRequest(
                        "job-18",
                        "77",
                        "COMPLETED",
                        Map.of("sources", List.of(Map.of("title", "Review"))),
                        List.of(Map.of("fileName", "scientific-source-candidates.json")),
                        null,
                        null,
                        null,
                        "EVIDENCE_SYNTHESIS",
                        null,
                        null));

        ArgumentCaptor<MdsProductEvidenceStageExecution> captor =
                ArgumentCaptor.forClass(MdsProductEvidenceStageExecution.class);
        verify(executionRepository).save(captor.capture());
        assertThat(execution.getStatus()).isEqualTo("CONCLUIDO");
        assertThat(execution.getOutputPayload()).contains("sources");
        assertThat(captor.getValue().getStageCode()).isEqualTo("evidence-synthesis");
        assertThat(captor.getValue().getInputPayload()).contains("Review");
    }

    /** Deve tratar callback repetido como idempotente e não duplicar a próxima etapa. */
    @Test
    void receiveCallbackDoesNotDuplicateNextStageWhenAlreadyCreated() {
        MdsProductEvidenceStageExecution execution = execution(
                77L,
                "source-discovery",
                "CONCLUIDO",
                "{\"pain\":\"agenda instável\"}");
        MdsProductEvidenceStageExecution next = execution(
                78L,
                "evidence-synthesis",
                "PENDENTE",
                "{\"sources\":[]}");
        when(executionRepository.findByIdAndStageCode(77L, "source-discovery")).thenReturn(Optional.of(execution));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusInOrderByCreatedAtDesc(
                        18L,
                        "evidence-synthesis",
                        List.of("PENDENTE", "PROCESSANDO", "CONCLUIDO", "BLOQUEADO", "FALHA")))
                .thenReturn(Optional.of(next));

        service.receiveCallback(
                "source-discovery",
                77L,
                new ProductEvidenceStageCallbackRequest(
                        "job-18",
                        "77",
                        "COMPLETED",
                        Map.of("sources", List.of()),
                        List.of(),
                        null,
                        null,
                        null,
                        "EVIDENCE_SYNTHESIS",
                        null,
                        null));

        verify(executionRepository, never()).save(any(MdsProductEvidenceStageExecution.class));
    }

    /** Simula uma resposta concluída da etapa de hipótese usada como contexto científico. */
    private void mockStage(String stageCode, String response) {
        HypothesisPainStageExecution execution = HypothesisPainStageExecution.builder()
                .stageCode(stageCode)
                .modelResponse(response)
                .build();
        when(hypothesisExecutionRepository
                        .findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
                                18L,
                                stageCode,
                                "CONCLUIDO"))
                .thenReturn(Optional.of(execution));
    }

    /** Simula ausência de etapas concluídas do pipeline de hipótese. */
    private void mockEmptyStages(Long marketNicheId) {
        for (String stageCode : List.of(
                "hypothesis-pain",
                "hypothesis-result",
                "hypothesis-mechanism",
                "hypothesis-proof")) {
            when(hypothesisExecutionRepository
                            .findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
                                    marketNicheId,
                                    stageCode,
                                    "CONCLUIDO"))
                    .thenReturn(Optional.empty());
        }
    }

    /** Cria uma execução científica mínima para os testes do workflow. */
    private MdsProductEvidenceStageExecution execution(
            Long id,
            String stageCode,
            String status,
            String inputPayload) {
        MdsProductEvidenceStageExecution execution = new MdsProductEvidenceStageExecution();
        execution.setId(id);
        execution.setMarketNicheId(18L);
        execution.setJobId("job-18");
        execution.setStageCode(stageCode);
        execution.setStatus(status);
        execution.setProductIdea("Produto digital para agenda previsível");
        execution.setScientificQuestion("Quais evidências sustentam confirmação e regras?");
        execution.setInputPayload(inputPayload);
        execution.setCreatedAt(Instant.now());
        return execution;
    }
}
