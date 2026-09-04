package com.marketinghub.videomanagement.pdeaudiovisualv1;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.AutomaticExecutionControl;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar consumo único, PLAY/STOP e callbacks da atividade audiovisual. */
@ExtendWith(MockitoExtension.class)
class ApolloPdeAudiovisualBpmTaskConsumerTest {
    @Mock private ApolloPdeAudiovisualBackendClient backend;
    @Mock private ApolloPdeAudiovisualRequirementEvaluator evaluator;
    @Mock private ApolloPdeAudiovisualCallbackFactory callbackFactory;
    @Mock private AutomaticExecutionControl automaticExecution;

    /** Conclui uma atividade sem audiovisual e não registra bloqueio. */
    @Test
    void shouldCompleteNotRequiredTask() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getPdeAudiovisual().setEnabled(true);
        ApolloPdeAudiovisualTask task = task(false);
        ApolloPdeAudiovisualDecision decision = new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.NOT_REQUIRED, "não exige", "liberar", null);
        Map<String, Object> payload = Map.of("resultJson", "{}");
        when(automaticExecution.allowsAutomaticExecution()).thenReturn(true);
        when(backend.claim()).thenReturn(task);
        when(evaluator.evaluate(task)).thenReturn(decision);
        when(callbackFactory.complete(task, decision)).thenReturn(payload);

        consumer(properties).processOne();

        verify(backend).complete(336L, payload);
        verify(backend, never()).block(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    /** Bloqueia vídeo obrigatório antes de qualquer produção paga. */
    @Test
    void shouldBlockTaskThatRequiresAuthorizedProduction() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getPdeAudiovisual().setEnabled(true);
        ApolloPdeAudiovisualTask task = task(true);
        ApolloPdeAudiovisualDecision decision = new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.REQUIRES_AUTHORIZATION,
                "exige vídeo",
                "autorizar no Estúdio",
                "AUTHORIZATION_REQUIRED");
        Map<String, Object> payload = Map.of("error", "exige vídeo");
        when(automaticExecution.allowsAutomaticExecution()).thenReturn(true);
        when(backend.claim()).thenReturn(task);
        when(evaluator.evaluate(task)).thenReturn(decision);
        when(callbackFactory.block(task, decision)).thenReturn(payload);

        consumer(properties).processOne();

        verify(backend).block(336L, payload);
        verify(backend, never()).complete(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    /** Mantém a fila intocada quando a rotina local ou o controle global estiver parado. */
    @Test
    void shouldStayIdleWhenDisabledOrStopped() {
        VideoManagementProperties disabled = new VideoManagementProperties();
        disabled.getPdeAudiovisual().setEnabled(false);

        consumer(disabled).processOne();

        verify(automaticExecution, never()).allowsAutomaticExecution();
        verify(backend, never()).claim();

        VideoManagementProperties stopped = new VideoManagementProperties();
        stopped.getPdeAudiovisual().setEnabled(true);
        when(automaticExecution.allowsAutomaticExecution()).thenReturn(false);

        consumer(stopped).processOne();

        verify(automaticExecution).allowsAutomaticExecution();
        verify(backend, never()).claim();
    }

    /** Impede que duas ativações sobrepostas reservem duas tarefas no mesmo executor. */
    @Test
    void shouldPreventOverlappingClaims() throws Exception {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getPdeAudiovisual().setEnabled(true);
        CountDownLatch claimStarted = new CountDownLatch(1);
        CountDownLatch releaseClaim = new CountDownLatch(1);
        when(automaticExecution.allowsAutomaticExecution()).thenReturn(true);
        when(backend.claim()).thenAnswer(ignored -> {
            claimStarted.countDown();
            releaseClaim.await(5, TimeUnit.SECONDS);
            return null;
        });
        ApolloPdeAudiovisualBpmTaskConsumer consumer = consumer(properties);

        CompletableFuture<Void> first = CompletableFuture.runAsync(consumer::processOne);
        claimStarted.await(5, TimeUnit.SECONDS);
        consumer.processOne();
        releaseClaim.countDown();
        first.get(5, TimeUnit.SECONDS);

        verify(backend, times(1)).claim();
    }

    /** Bloqueia uma lease incompatível em vez de deixá-la indefinidamente em andamento. */
    @Test
    void shouldReportInvalidSpecializedClaimAsTechnicalFailure() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getPdeAudiovisual().setEnabled(true);
        ApolloPdeAudiovisualTask valid = task(false);
        ApolloPdeAudiovisualTask invalid = new ApolloPdeAudiovisualTask(
                valid.taskId(),
                valid.agentKey(),
                valid.processCode(),
                valid.processVersion(),
                valid.activityId(),
                valid.activityName(),
                valid.title(),
                valid.description(),
                valid.sourceReference(),
                valid.receivedAt(),
                new ApolloPdeAudiovisualTask.ExecutionResource(
                        "outro-recurso", "Outro", "MODULE", null, null),
                valid.taskTarget(),
                valid.processContextJson());
        Map<String, Object> technicalPayload = Map.of("error", "contrato inválido");
        when(automaticExecution.allowsAutomaticExecution()).thenReturn(true);
        when(backend.claim()).thenReturn(invalid);
        when(callbackFactory.technicalFailure(
                org.mockito.ArgumentMatchers.eq(invalid),
                org.mockito.ArgumentMatchers.any(RuntimeException.class)))
                .thenReturn(technicalPayload);

        consumer(properties).processOne();

        verify(backend).block(336L, technicalPayload);
        verify(evaluator, never()).evaluate(org.mockito.ArgumentMatchers.any());
    }

    /** Cria o consumidor com dependências simuladas e sem contexto Spring. */
    private ApolloPdeAudiovisualBpmTaskConsumer consumer(VideoManagementProperties properties) {
        return new ApolloPdeAudiovisualBpmTaskConsumer(
                properties, backend, evaluator, callbackFactory, automaticExecution);
    }

    /** Cria uma tarefa BPM especializada para os cenários do scheduler. */
    private ApolloPdeAudiovisualTask task(boolean required) {
        ObjectMapper mapper = new ObjectMapper();
        return new ApolloPdeAudiovisualTask(
                336L,
                "videomaker",
                "pde-construction-approval",
                6,
                "audiovisual",
                "Produzir audiovisual quando previsto",
                "Audiovisual",
                "Contrato opcional",
                "product:10@private-validation-v1",
                null,
                new ApolloPdeAudiovisualTask.ExecutionResource(
                        "video-management-service", "Estúdio", "MODULE", null, null),
                new ApolloPdeAudiovisualTask.TaskTarget(
                        "product:10@private-validation-v1",
                        null,
                        10L,
                        "mira",
                        "Mira",
                        "Mira",
                        "private-validation-v1",
                        null,
                        mapper.createObjectNode().set(
                                "harness",
                                mapper.createObjectNode().put("audiovisualRequired", required))),
                "{}");
    }
}
