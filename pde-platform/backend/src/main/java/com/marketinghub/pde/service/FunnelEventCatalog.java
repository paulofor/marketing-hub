package com.marketinghub.pde.service;

import java.util.List;
import java.util.Set;

/** Responsabilidade: manter o catálogo único dos eventos aceitos pela jornada PDE. */
public final class FunnelEventCatalog {
    public static final String CONTRACT_VERSION = "PDE_COMMERCIAL_JOURNEY_EVENTS_V1";
    public static final List<String> REQUIRED_COMMERCIAL_JOURNEY_EVENTS = List.of(
            "PAGE_VIEW",
            "VALUE_MOMENT",
            "CTA_VIEWED",
            "CHECKOUT_STARTED",
            "PURCHASE_COMPLETED",
            "ACCESS_RELEASED",
            "MISSION_COMPLETED",
            "FIRST_USE",
            "REFUND_CONFIRMED");

    private static final Set<String> ALLOWED_EVENTS = Set.of(
            "PED_ENTRY",
            "PAGE_VIEW",
            "PAGE_LOAD",
            "PAGE_VISIBLE_TIME",
            "SCREEN_VIEW",
            "SCREEN_TIME",
            "SECTION_VIEW",
            "SCROLL_DEPTH",
            "CTA_VIEWED",
            "VIDEO_VIEWED",
            "VIDEO_PLAY",
            "VIDEO_PROGRESS_25",
            "VIDEO_PROGRESS_50",
            "VIDEO_PROGRESS_75",
            "VIDEO_COMPLETED",
            "VIDEO_ERROR",
            "VIDEO_CTA_CLICKED",
            "UI_CLICK",
            "LINK_CLICK",
            "FIELD_FOCUS",
            "FIELD_INPUT",
            "FIELD_FILLED",
            "FIELD_ABANDONED",
            "FUNNEL_EXPERIMENT_ASSIGNED",
            "PRESENCE_MAP_CHOICE_SELECTED",
            "DIAGNOSTIC_CHOICE_SELECTED",
            "DIAGNOSTIC_SUBMITTED",
            "DIAGNOSTIC_COMPLETED",
            "PROBLEM_RECOGNIZED",
            "CATEGORY_UNDERSTOOD",
            "MECHANISM_VIEWED",
            "TASTING_STARTED",
            "VALUE_MOMENT",
            "MICRO_EXPERIENCE_STARTED",
            "REAL_INPUT_SUBMITTED",
            "MICRO_RESULT_RECEIVED",
            "PAID_CONTINUATION_VIEWED",
            "LOGIN_STARTED",
            "LOGIN_COMPLETED",
            "PAYWALL_VIEWED",
            "SUBSCRIPTION_CLICKED",
            "CHECKOUT_STARTED",
            "PURCHASE_COMPLETED",
            "SUBSCRIPTION_APPROVED",
            "ACCESS_RELEASED",
            "FIRST_USE",
            "MISSION_OPEN",
            "MISSION_COMPLETED",
            "MISSION_FEEDBACK_SUBMITTED",
            "JOURNEY_COMPLETED",
            "DELIVERY_COMPLETED",
            "REFUND_CONFIRMED",
            "MISSION_INTERACTION_SAVED",
            "AI_GUIDANCE_REQUESTED",
            "MATERIAL_OPEN",
            "SUPPORT_REQUESTED",
            "EXPERIMENT_CONSENT_RECORDED",
            "EXPERIMENT_SESSION_STARTED",
            "TRANSITION_PAUSE_EXPERIENCE_COMPLETED",
            "TRANSITION_PAUSE_TASK_STARTED",
            "TRANSITION_PAUSE_FIRST_STEP_COMPLETED",
            "TRANSITION_PAUSE_EXPERIENCE_EXITED",
            "TRANSITION_PAUSE_SAFETY_STOPPED");

    /** Impede instanciação porque o catálogo é um contrato estático e imutável. */
    private FunnelEventCatalog() {}

    /** Normaliza e valida um tipo de evento contra o mesmo catálogo exposto publicamente. */
    public static String normalize(String eventType) {
        String normalized = eventType == null ? "" : eventType.trim().toUpperCase();
        if (!ALLOWED_EVENTS.contains(normalized)) {
            throw new IllegalArgumentException("Evento PDE não suportado: " + eventType);
        }
        return normalized;
    }

    /** Confirma que todos os eventos comerciais obrigatórios permanecem implementados. */
    public static boolean supportsRequiredCommercialJourney() {
        return ALLOWED_EVENTS.containsAll(REQUIRED_COMMERCIAL_JOURNEY_EVENTS);
    }
}
