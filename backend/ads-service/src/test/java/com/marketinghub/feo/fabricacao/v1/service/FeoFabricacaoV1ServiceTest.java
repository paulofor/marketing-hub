package com.marketinghub.feo.fabricacao.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageExecution;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1CompleteRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.feo.fabricacao.v1.FeoFabricacaoV1StageExecutionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar o contrato mínimo da fila backend da FEO v1. */
class FeoFabricacaoV1ServiceTest {

    private final ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
    private final DeliverableRepository deliverableRepository = mock(DeliverableRepository.class);
    private final DeliverablePackageRepository deliverablePackageRepository = mock(DeliverablePackageRepository.class);
    private final FeoFabricacaoV1StageExecutionRepository executionRepository =
            mock(FeoFabricacaoV1StageExecutionRepository.class);
    private final FeoFabricacaoV1Service service = new FeoFabricacaoV1Service(
            experimentRepository,
            deliverableRepository,
            deliverablePackageRepository,
            executionRepository,
            new ObjectMapper());

    /** Deve criar uma pendência FEO com contexto do pacote de entregáveis já vinculado ao experimento. */
    @Test
    void shouldStartFabricationFromExperimentPackage() {
        Experiment experiment = experiment();
        Deliverable deliverable = Deliverable.builder().title("Checklist elegante").build();
        DeliverablePackage pack = DeliverablePackage.builder()
                .name("Método MUSA")
                .deliverables(new java.util.LinkedHashSet<>(List.of(deliverable)))
                .build();
        when(experimentRepository.findById(66L)).thenReturn(Optional.of(experiment));
        when(executionRepository.existsByExperimentIdAndStageCodeAndStatusIn(eq(66L), eq("planejamento-entregaveis"), any()))
                .thenReturn(false);
        when(deliverablePackageRepository.findByExperimentIdOrderByCreatedAtDesc(66L)).thenReturn(List.of(pack));
        when(executionRepository.save(any())).thenAnswer(invocation -> {
            FeoFabricacaoV1StageExecution execution = invocation.getArgument(0);
            execution.setId(10L);
            return execution;
        });

        var response = service.startForExperiment(66L);

        assertThat(response.executionId()).isEqualTo(10L);
        assertThat(response.stageCode()).isEqualTo("planejamento-entregaveis");
        assertThat(response.status()).isEqualTo("PENDING");
        ArgumentCaptor<FeoFabricacaoV1StageExecution> captor =
                ArgumentCaptor.forClass(FeoFabricacaoV1StageExecution.class);
        verify(executionRepository).save(captor.capture());
        assertThat(captor.getValue().getInputPayload())
                .contains("Método MUSA")
                .contains("Checklist elegante")
                .contains("Arquitetura de Presença Elegante Acessível");
    }

    /** Deve marcar pending como running e entregar o contrato esperado pelo worker FEO. */
    @Test
    void shouldExposePendingExecutionForWorker() {
        FeoFabricacaoV1StageExecution execution = FeoFabricacaoV1StageExecution.builder()
                .id(7L)
                .jobId("job-1")
                .experiment(experiment())
                .stageCode("planejamento-entregaveis")
                .status(FeoFabricacaoV1StageStatus.PENDING)
                .inputPayload("{\"requestId\":\"experiment-66\",\"experimentId\":\"66\"}")
                .build();
        when(executionRepository.findPendingOrStaleRunning(
                eq("planejamento-entregaveis"),
                any(),
                any(Pageable.class)))
                .thenReturn(List.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var pending = service.listPending("planejamento-entregaveis", 10);

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().executionId()).isEqualTo("7");
        assertThat(execution.getStatus()).isEqualTo(FeoFabricacaoV1StageStatus.RUNNING);
    }

    /** Deve enfileirar redação quando o planejamento conclui com próxima etapa contratada. */
    @Test
    void shouldEnqueueContentWritingAfterPlanningCompletion() {
        FeoFabricacaoV1StageExecution execution = FeoFabricacaoV1StageExecution.builder()
                .id(7L)
                .jobId("job-1")
                .experiment(experiment())
                .stageCode("planejamento-entregaveis")
                .status(FeoFabricacaoV1StageStatus.RUNNING)
                .inputPayload("{\"requestId\":\"experiment-66\",\"experimentId\":\"66\"}")
                .build();
        when(executionRepository.findByIdAndStageCode(7L, "planejamento-entregaveis"))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.complete(
                "planejamento-entregaveis",
                7L,
                new FeoFabricacaoV1CompleteRequest(
                        "feo-worker",
                        "job-1",
                        "COMPLETED",
                        Map.of("requestId", "experiment-66", "packageTitle", "Pacote Final"),
                        List.of(),
                        Map.of("deliverableCount", 1),
                        null,
                        "redacao-entregaveis"));

        ArgumentCaptor<FeoFabricacaoV1StageExecution> captor =
                ArgumentCaptor.forClass(FeoFabricacaoV1StageExecution.class);
        verify(executionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStageCode()).isEqualTo("redacao-entregaveis");
        assertThat(captor.getAllValues().get(1).getInputPayload()).contains("\"plan\"");
    }

    /** Deve enfileirar montagem somente após a redação gerar conteúdo final. */
    @Test
    void shouldEnqueuePackageAssemblyAfterContentWritingCompletion() {
        FeoFabricacaoV1StageExecution execution = FeoFabricacaoV1StageExecution.builder()
                .id(9L)
                .jobId("job-1")
                .experiment(experiment())
                .stageCode("redacao-entregaveis")
                .status(FeoFabricacaoV1StageStatus.RUNNING)
                .inputPayload("{\"context\":{\"requestId\":\"experiment-66\"},\"plan\":{\"packageTitle\":\"Pacote\"}}")
                .build();
        when(executionRepository.findByIdAndStageCode(9L, "redacao-entregaveis"))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.complete(
                "redacao-entregaveis",
                9L,
                new FeoFabricacaoV1CompleteRequest(
                        "feo-worker",
                        "job-1",
                        "COMPLETED",
                        Map.of(
                                "context", Map.of("requestId", "experiment-66"),
                                "plan", Map.of("packageTitle", "Pacote Final"),
                                "contentPackage", Map.of("qualityScore", 92)),
                        List.of(),
                        Map.of("qualityScore", 92),
                        null,
                        "montagem-pacote"));

        ArgumentCaptor<FeoFabricacaoV1StageExecution> captor =
                ArgumentCaptor.forClass(FeoFabricacaoV1StageExecution.class);
        verify(executionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStageCode()).isEqualTo("montagem-pacote");
        assertThat(captor.getAllValues().get(1).getInputPayload()).contains("\"contentPackage\"");
    }

    /** Deve materializar pacote e entregáveis finais quando a montagem termina. */
    @Test
    void shouldMaterializeDeliverablePackageAfterAssemblyCompletion() {
        FeoFabricacaoV1StageExecution execution = FeoFabricacaoV1StageExecution.builder()
                .id(8L)
                .jobId("job-1")
                .experiment(experiment())
                .stageCode("montagem-pacote")
                .status(FeoFabricacaoV1StageStatus.RUNNING)
                .inputPayload("{\"requestId\":\"experiment-66\",\"experimentId\":\"66\"}")
                .build();
        when(executionRepository.findByIdAndStageCode(8L, "montagem-pacote")).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliverableRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliverablePackageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.complete(
                "montagem-pacote",
                8L,
                new FeoFabricacaoV1CompleteRequest(
                        "feo-worker",
                        "job-1",
                        "COMPLETED",
                        Map.of(
                                "manifest",
                                Map.of(
                                        "packageTitle",
                                        "Pacote Final MUSA",
                                        "items",
                                        List.of(Map.of(
                                                "fileName",
                                                "guia.pdf",
                                                "role",
                                                "Guia principal",
                                                "sha256",
                                                "abc"))),
                                "report",
                                Map.of("commercialDecision", "READY_FOR_REVIEW")),
                        List.of(),
                        Map.of("generatedFileCount", 1),
                        null,
                        null));

        ArgumentCaptor<DeliverablePackage> packageCaptor = ArgumentCaptor.forClass(DeliverablePackage.class);
        verify(deliverablePackageRepository).save(packageCaptor.capture());
        assertThat(packageCaptor.getValue().getName()).isEqualTo("Pacote Final MUSA - FEO #8");
        assertThat(packageCaptor.getValue().getDeliverables()).hasSize(1);
    }

    /** Monta experimento mínimo com nicho e hipótese comercial. */
    private Experiment experiment() {
        MarketNiche niche = MarketNiche.builder().id(3L).name("Moda feminina").build();
        Hypothesis hypothesis = Hypothesis.builder()
                .promise("Presença elegante sem gastar com luxo")
                .entrega("Plano de 7 dias")
                .uniqueMechanism("Arquitetura de Presença Elegante Acessível")
                .build();
        return Experiment.builder()
                .id(66L)
                .name("MUSA-H001-E004")
                .niche(niche)
                .hypothesisRef(hypothesis)
                .funnelPromise("Parecer intencional sem parecer rica")
                .build();
    }
}
