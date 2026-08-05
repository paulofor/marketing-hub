package com.marketinghub.pde.transitionpause.v1;

import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.FunnelEventResponse;
import com.marketinghub.pde.service.AccessService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Controla consentimento, atribuição A/B e eventos do experimento Pausa de Transição v1. */
@Service
public class TransitionPauseExperimentService {
    static final String PRODUCT_SLUG = "pausa-de-transicao";
    static final String EXPERIENCE_VERSION = "pausa-de-transicao-v1";
    private static final Set<String> ALLOWED_EVENTS = Set.of(
            "EXPERIENCE_COMPLETED", "TASK_STARTED", "FIRST_STEP_COMPLETED", "EXPERIENCE_EXITED", "SAFETY_STOPPED");
    private static final List<String> VARIANT_A_STEPS = List.of(
            "Acomode-se por até três minutos, mantendo liberdade para parar a qualquer momento.",
            "Perceba a respiração e o apoio do corpo, sem tentar mudar o que sente.",
            "Imagine apenas o primeiro passo concreto da tarefa ficando mais simples.",
            "Ao terminar, levante-se e experimente esse primeiro passo por cinco minutos.");
    private static final List<String> VARIANT_B_STEPS = List.of(
            "Escolha agora o menor primeiro passo concreto da tarefa.",
            "Prepare somente o material necessário para esse passo.",
            "Execute o primeiro passo por cinco minutos e depois decida livremente se deseja continuar.");

    private final AccessService accessService;

    /** Recebe a fonte canônica de persistência dos eventos PDE. */
    public TransitionPauseExperimentService(AccessService accessService) {
        this.accessService = accessService;
    }

    /** Retorna hipótese, limites, métricas e variantes do experimento. */
    public TransitionPauseContractResponse getContract() {
        return new TransitionPauseContractResponse(
                PRODUCT_SLUG,
                EXPERIENCE_VERSION,
                "Pausa de Transição v1",
                "Relaxamento breve com visualização do primeiro passo aumenta o início da tarefa em até dez minutos.",
                "TASK_STARTED_WITHIN_10_MINUTES",
                List.of("FIRST_STEP_COMPLETED", "EFFORT_CHANGE", "SAFETY_STOP_RATE"),
                List.of("tarefas cotidianas não clínicas", "participação voluntária", "adultos capazes de consentir"),
                List.of("tratamento de saúde", "uso durante direção ou operação de máquinas", "promessa garantida", "indução sem consentimento"),
                List.of("ansiedade", "dissociação", "tontura", "desconforto", "perda de autonomia"),
                List.of(
                        new TransitionPauseContractResponse.VariantResponse("A", "Pausa e visualização", 180, VARIANT_A_STEPS),
                        new TransitionPauseContractResponse.VariantResponse("B", "Instrução objetiva", 60, VARIANT_B_STEPS)));
    }

    /** Inicia uma sessão consentida e atribui uma variante estável pelo identificador da sessão. */
    public TransitionPauseSessionResponse startSession(TransitionPauseSessionRequest request) {
        String variant = Math.floorMod(request.sessionId().hashCode(), 2) == 0 ? "A" : "B";
        Map<String, Object> metadata = baseMetadata(request.participantId(), request.sessionId(), variant);
        metadata.put("consentAccepted", true);
        metadata.put("safetyAcknowledged", true);
        metadata.put("voluntaryParticipation", true);
        metadata.put("taskDescription", request.taskDescription().trim());
        record("EXPERIMENT_CONSENT_RECORDED", metadata);
        record("EXPERIMENT_SESSION_STARTED", metadata);
        List<String> steps = "A".equals(variant) ? VARIANT_A_STEPS : VARIANT_B_STEPS;
        return new TransitionPauseSessionResponse(
                request.sessionId(), variant, "A".equals(variant) ? 180 : 60, steps,
                "Pare imediatamente se sentir desconforto. Você pode sair a qualquer momento, sem prejuízo.");
    }

    /** Registra desfecho mensurável e bloqueia eventos fora do protocolo v1. */
    public FunnelEventResponse recordOutcome(TransitionPauseEventRequest request) {
        String eventType = request.eventType().trim().toUpperCase();
        if (!ALLOWED_EVENTS.contains(eventType)) {
            throw new IllegalArgumentException("Evento não permitido no protocolo Pausa de Transição v1");
        }
        Map<String, Object> metadata = baseMetadata(
                request.participantId(), request.sessionId(), assignedVariant(request.sessionId()));
        putIfPresent(metadata, "effortBefore", request.effortBefore());
        putIfPresent(metadata, "effortAfter", request.effortAfter());
        putIfPresent(metadata, "secondsUntilTaskStarted", request.secondsUntilTaskStarted());
        putIfPresent(metadata, "firstStepCompleted", request.firstStepCompleted());
        putIfPresent(metadata, "discomfortNote", request.discomfortNote());
        metadata.put("humanReported", true);
        return record("TRANSITION_PAUSE_" + eventType, metadata);
    }

    /** Deriva a variante de forma determinística para impedir troca durante a mesma sessão. */
    private String assignedVariant(String sessionId) {
        return Math.floorMod(sessionId.hashCode(), 2) == 0 ? "A" : "B";
    }

    /** Cria os metadados mínimos que permitem comparar variantes sem identificar a pessoa. */
    private Map<String, Object> baseMetadata(String participantId, String sessionId, String variant) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("experienceVersion", EXPERIENCE_VERSION);
        metadata.put("participantId", participantId.trim());
        metadata.put("sessionId", sessionId.trim());
        metadata.put("variant", variant);
        metadata.put("paidTraffic", false);
        return metadata;
    }

    /** Adiciona somente métricas efetivamente informadas pela pessoa. */
    private void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null && (!(value instanceof String text) || !text.isBlank())) {
            metadata.put(key, value);
        }
    }

    /** Persiste o evento na trilha auditável já usada pelo motor PDE. */
    private FunnelEventResponse record(String eventType, Map<String, Object> metadata) {
        return accessService.recordFunnelEvent(new FunnelEventRequest(
                PRODUCT_SLUG, eventType, null, null, null, "transition-pause-experiment", null, metadata));
    }
}
