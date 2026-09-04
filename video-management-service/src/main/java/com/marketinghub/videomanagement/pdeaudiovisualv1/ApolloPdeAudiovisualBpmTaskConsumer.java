package com.marketinghub.videomanagement.pdeaudiovisualv1;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.AutomaticExecutionControl;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir uma atividade audiovisual PDE por vez sem orquestrar a cadeia. */
@Component
public class ApolloPdeAudiovisualBpmTaskConsumer {
    private static final Logger log = LoggerFactory.getLogger(ApolloPdeAudiovisualBpmTaskConsumer.class);
    private final VideoManagementProperties properties;
    private final ApolloPdeAudiovisualBackendClient backend;
    private final ApolloPdeAudiovisualRequirementEvaluator evaluator;
    private final ApolloPdeAudiovisualCallbackFactory callbackFactory;
    private final AutomaticExecutionControl automaticExecution;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Configura fila, regra determinística, auditoria e controle operacional de Apolo. */
    public ApolloPdeAudiovisualBpmTaskConsumer(
            VideoManagementProperties properties,
            ApolloPdeAudiovisualBackendClient backend,
            ApolloPdeAudiovisualRequirementEvaluator evaluator,
            ApolloPdeAudiovisualCallbackFactory callbackFactory,
            AutomaticExecutionControl automaticExecution) {
        this.properties = properties;
        this.backend = backend;
        this.evaluator = evaluator;
        this.callbackFactory = callbackFactory;
        this.automaticExecution = automaticExecution;
    }

    /** Reserva e responde no máximo uma atividade já liberada pelo backend. */
    @Scheduled(cron = "25 */1 * * * *")
    public void processOne() {
        if (!properties.getPdeAudiovisual().isEnabled()
                || !automaticExecution.allowsAutomaticExecution()
                || !running.compareAndSet(false, true)) {
            return;
        }
        ApolloPdeAudiovisualTask task = null;
        boolean callbackStarted = false;
        try {
            task = backend.claim();
            if (task == null) {
                return;
            }
            validateClaim(task);
            ApolloPdeAudiovisualDecision decision = evaluator.evaluate(task);
            if (decision.canComplete()) {
                Map<String, Object> payload = callbackFactory.complete(task, decision);
                callbackStarted = true;
                backend.complete(task.taskId(), payload);
            } else {
                Map<String, Object> payload = callbackFactory.block(task, decision);
                callbackStarted = true;
                backend.block(task.taskId(), payload);
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Falha no consumo audiovisual BPM de Apolo. taskId={} sourceReference={}",
                    task == null ? null : task.taskId(),
                    task == null ? null : task.sourceReference(),
                    ex);
            if (task != null && !callbackStarted) {
                reportTechnicalFailure(task, ex);
            }
        } finally {
            running.set(false);
        }
    }

    /** Persiste contrato inválido para que a falha não deixe a lease invisível em andamento. */
    private void reportTechnicalFailure(ApolloPdeAudiovisualTask task, RuntimeException cause) {
        try {
            backend.block(task.taskId(), callbackFactory.technicalFailure(task, cause));
        } catch (RuntimeException callbackException) {
            log.error(
                    "Falha ao registrar bloqueio técnico audiovisual. taskId={} sourceReference={}",
                    task.taskId(),
                    task.sourceReference(),
                    callbackException);
        }
    }

    /** Confirma que o backend devolveu exatamente a especialização solicitada pelo executor. */
    private void validateClaim(ApolloPdeAudiovisualTask task) {
        String resourceCode = task.executionResource() == null
                ? null
                : task.executionResource().resourceCode();
        if (!ApolloPdeAudiovisualBackendClient.AGENT_KEY.equals(task.agentKey())
                || !ApolloPdeAudiovisualBackendClient.PROCESS_CODE.equals(task.processCode())
                || !ApolloPdeAudiovisualBackendClient.ACTIVITY_ID.equals(task.activityId())
                || !ApolloPdeAudiovisualBackendClient.EXECUTION_RESOURCE_CODE.equals(resourceCode)) {
            throw new IllegalArgumentException(
                    "A fila audiovisual retornou tarefa fora do contrato especializado de Apolo.");
        }
    }
}
