package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.CapturedWebSource;
import com.marketinghub.oprm.domain.OccupationPersonaRoutineCardPayload;
import com.marketinghub.oprm.domain.OccupationProfileSnapshotPayload;
import com.marketinghub.oprm.domain.OccupationWebSourceSnapshotPayload;
import com.marketinghub.oprm.domain.RoutineConstraintSignal;
import com.marketinghub.oprm.domain.RoutinePainSignal;
import com.marketinghub.oprm.domain.RoutineTaskPattern;
import com.marketinghub.oprm.domain.RoutineWorkaroundSignal;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RoutineInferenceService {

    private static final int MAX_TOP_TASKS = 4;

    private final OccupationResolverService occupationResolverService;
    private final WebEnrichmentService webEnrichmentService;

    public RoutineInferenceService(OccupationResolverService occupationResolverService,
                                   WebEnrichmentService webEnrichmentService) {
        this.occupationResolverService = occupationResolverService;
        this.webEnrichmentService = webEnrichmentService;
    }

    public ArtifactEnvelope inferRoutine(String rawOccupationLabel, String nicheName, String locale, String correlationId) {
        ArtifactEnvelope profileSnapshot = occupationResolverService.resolveToProfileSnapshot(
                rawOccupationLabel,
                nicheName,
                locale,
                correlationId
        );

        ArtifactEnvelope webSnapshot = webEnrichmentService.enrichOccupation(
                rawOccupationLabel,
                nicheName,
                locale,
                correlationId
        );

        OccupationProfileSnapshotPayload profilePayload = (OccupationProfileSnapshotPayload) profileSnapshot.payload();
        OccupationWebSourceSnapshotPayload webPayload = (OccupationWebSourceSnapshotPayload) webSnapshot.payload();

        List<RoutineTaskPattern> taskPatterns = buildTaskPatterns(profilePayload, webPayload);
        List<RoutineConstraintSignal> constraintSignals = buildConstraintSignals(profilePayload, taskPatterns);
        List<RoutinePainSignal> painSignals = buildPainSignals(constraintSignals, taskPatterns);
        List<RoutineWorkaroundSignal> workaroundSignals = buildWorkaroundSignals(taskPatterns, painSignals);

        Map<String, Integer> sourceMix = Map.of(
                "captured_web_sources", webPayload.capturedSources().size(),
                "structured_records", profilePayload.sourceRecordIds().size()
        );

        OccupationPersonaRoutineCardPayload cardPayload = new OccupationPersonaRoutineCardPayload(
                profilePayload.aliasResolution().rawLabel(),
                profilePayload.occupationName(),
                List.copyOf(profilePayload.aliasResolution().matchType().equals("ALIAS")
                        ? List.of(profilePayload.aliasResolution().rawLabel())
                        : List.of()),
                profilePayload.nicheName(),
                buildRoutineSummary(profilePayload, taskPatterns, painSignals),
                taskPatterns,
                profilePayload.toolsList().stream().limit(3).toList(),
                constraintSignals,
                profilePayload.workContextList().stream().limit(3).toList(),
                "frequent direct interaction with end customers during service delivery",
                "revenue depends on maintaining task consistency and client retention",
                "high admin burden from fragmented communication, scheduling and follow-up",
                workaroundSignals,
                painSignals,
                List.of(
                        "reduzir retrabalho operacional",
                        "ganhar previsibilidade diária",
                        "diminuir tempo em tarefas administrativas"
                ),
                List.of(
                        "padronização de rotina com checklist acionável",
                        "automação de lembretes e acompanhamento",
                        "centralização de tarefas e comunicação"
                ),
                webPayload.capturedSources().stream().map(CapturedWebSource::url).toList(),
                sourceMix,
                Instant.now()
        );

        double confidenceScore = calculateConfidence(taskPatterns, painSignals, webPayload.capturedSources());
        String effectiveCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;

        return new ArtifactEnvelope(
                "occupationPersonaRoutineCard",
                "1.0",
                UUID.randomUUID().toString(),
                "oprm",
                "oprm.routine-inference",
                Instant.now(),
                effectiveCorrelationId,
                UUID.randomUUID().toString(),
                webPayload.capturedSources().stream().map(CapturedWebSource::url).toList(),
                List.of(
                        profileSnapshot.artifactId(),
                        webSnapshot.artifactId(),
                        "routineTaskPattern",
                        "routineConstraintSignal",
                        "routinePainSignal",
                        "routineWorkaroundSignal"
                ),
                cardPayload,
                "GENERATED",
                confidenceScore,
                Map.of(
                        "phase", "phase-3",
                        "task_patterns", taskPatterns.size(),
                        "constraint_signals", constraintSignals.size(),
                        "pain_signals", painSignals.size(),
                        "workaround_signals", workaroundSignals.size()
                )
        );
    }

    private List<RoutineTaskPattern> buildTaskPatterns(OccupationProfileSnapshotPayload profilePayload,
                                                       OccupationWebSourceSnapshotPayload webPayload) {
        List<String> signals = webPayload.semanticRoutineSignals().stream()
                .map(signal -> signal.toLowerCase(Locale.ROOT))
                .toList();

        return profilePayload.taskList().stream()
                .limit(MAX_TOP_TASKS)
                .map(task -> {
                    String normalizedTask = task.toLowerCase(Locale.ROOT);
                    boolean hasSignal = signals.stream().anyMatch(signal -> signal.contains(normalizedTask));
                    String frequency = hasSignal ? "HIGH_RECURRENT" : "MEDIUM_RECURRENT";
                    String trigger = hasSignal
                            ? "triggered by recurring customer demand and daily execution"
                            : "triggered by periodic operational needs";
                    String timeCost = hasSignal ? "MEDIUM_HIGH" : "MEDIUM";
                    return new RoutineTaskPattern(
                            task,
                            "core routine activity for " + profilePayload.occupationName(),
                            trigger,
                            frequency,
                            timeCost,
                            "uses tools such as " + String.join(", ", profilePayload.toolsList().stream().limit(2).toList()),
                            webPayload.capturedSources().stream().map(CapturedWebSource::url).toList()
                    );
                })
                .toList();
    }

    private List<RoutineConstraintSignal> buildConstraintSignals(OccupationProfileSnapshotPayload profilePayload,
                                                                 List<RoutineTaskPattern> taskPatterns) {
        List<String> evidenceRefs = taskPatterns.stream()
                .flatMap(pattern -> pattern.evidenceRefs().stream())
                .distinct()
                .toList();

        return List.of(
                new RoutineConstraintSignal(
                        "TIME_FRAGMENTATION",
                        "high context switching between execution tasks and admin follow-up",
                        0.82,
                        String.join("; ", profilePayload.workContextList().stream().limit(2).toList()),
                        evidenceRefs
                ),
                new RoutineConstraintSignal(
                        "TOOL_FRAGMENTATION",
                        "routine distributed across multiple communication and planning tools",
                        0.75,
                        "tool usage spread across " + String.join(", ", profilePayload.toolsList().stream().limit(3).toList()),
                        evidenceRefs
                )
        );
    }

    private List<RoutinePainSignal> buildPainSignals(List<RoutineConstraintSignal> constraints,
                                                     List<RoutineTaskPattern> taskPatterns) {
        List<String> evidenceRefs = taskPatterns.stream()
                .flatMap(pattern -> pattern.evidenceRefs().stream())
                .distinct()
                .toList();

        return constraints.stream()
                .sorted(Comparator.comparing(RoutineConstraintSignal::severityScore).reversed())
                .map(constraint -> new RoutinePainSignal(
                        "operational overload: " + constraint.constraintType().toLowerCase(Locale.ROOT),
                        "constraint leads to recurrent friction and reduces daily predictability",
                        "OPERATIONAL",
                        Math.min(0.95, constraint.severityScore() + 0.08),
                        0.84,
                        "uses ad-hoc checklist and manual reminders to keep activities moving",
                        evidenceRefs
                ))
                .toList();
    }

    private List<RoutineWorkaroundSignal> buildWorkaroundSignals(List<RoutineTaskPattern> taskPatterns,
                                                                 List<RoutinePainSignal> painSignals) {
        String relatedTask = taskPatterns.isEmpty() ? "general routine" : taskPatterns.getFirst().taskLabel();
        String relatedPain = painSignals.isEmpty() ? "operational overload" : painSignals.getFirst().painLabel();
        List<String> evidenceRefs = taskPatterns.stream()
                .flatMap(pattern -> pattern.evidenceRefs().stream())
                .distinct()
                .toList();

        return List.of(
                new RoutineWorkaroundSignal(
                        "manual-priority-list",
                        "maintains temporary task ordering manually between customer demands",
                        relatedTask,
                        relatedPain,
                        0.71,
                        evidenceRefs
                ),
                new RoutineWorkaroundSignal(
                        "chat-based-follow-up",
                        "uses message threads as informal task tracker",
                        relatedTask,
                        relatedPain,
                        0.76,
                        evidenceRefs
                )
        );
    }

    private String buildRoutineSummary(OccupationProfileSnapshotPayload profilePayload,
                                       List<RoutineTaskPattern> taskPatterns,
                                       List<RoutinePainSignal> painSignals) {
        return profilePayload.occupationName()
                + " routine inferred with "
                + taskPatterns.size()
                + " recurring task patterns and "
                + painSignals.size()
                + " primary pain signals from structured and web evidence";
    }

    private double calculateConfidence(List<RoutineTaskPattern> taskPatterns,
                                       List<RoutinePainSignal> painSignals,
                                       List<CapturedWebSource> sources) {
        long capturedSources = sources.stream().filter(source -> "CAPTURED".equals(source.captureStatus())).count();
        double base = 0.62;
        double taskContribution = Math.min(0.18, taskPatterns.size() * 0.04);
        double painContribution = Math.min(0.12, painSignals.size() * 0.05);
        double sourceContribution = Math.min(0.10, capturedSources * 0.03);
        return Math.min(0.95, base + taskContribution + painContribution + sourceContribution);
    }
}
