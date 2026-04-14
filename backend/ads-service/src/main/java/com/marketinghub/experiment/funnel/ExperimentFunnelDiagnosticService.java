package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticReasonCode;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.experiment.funnel.dto.FunnelThresholdCheckDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExperimentFunnelDiagnosticService {

    private final ExperimentFunnelService funnelService;
    private final ExperimentFunnelDiagnosticConfig config;

    public ExperimentFunnelDiagnosticService(ExperimentFunnelService funnelService,
                                             ExperimentFunnelDiagnosticConfig config) {
        this.funnelService = funnelService;
        this.config = config;
    }

    public ExperimentFunnelDiagnosticsResponseDto diagnose(Long experimentId) {
        List<ExperimentFunnelStageDto> stages = funnelService.summarize(experimentId);
        Map<ExperimentFunnelStage, ExperimentFunnelStageDto> byStage = stages.stream()
                .collect(Collectors.toMap(ExperimentFunnelStageDto::getStage, stage -> stage));

        List<ExperimentFunnelStageDiagnosticDto> diagnostics = config.prioritizedRules().stream()
                .map(rule -> diagnoseRule(byStage, rule))
                .sorted(Comparator.comparing(dto -> dto.stageKey().getOrder()))
                .toList();

        long optimizationEvents = Optional.ofNullable(byStage.get(ExperimentFunnelStage.COMPRA))
                .map(ExperimentFunnelStageDto::getTotalCount)
                .orElse(0L);
        String contextualAlert = optimizationEvents < config.minOptimizationEventVolumeForContext()
                ? "Alerta contextual: o evento principal de otimização ainda está com volume baixo para aprendizado da mídia."
                : null;

        return new ExperimentFunnelDiagnosticsResponseDto(diagnostics, contextualAlert);
    }

    private ExperimentFunnelStageDiagnosticDto diagnoseRule(Map<ExperimentFunnelStage, ExperimentFunnelStageDto> byStage,
                                                            ExperimentFunnelDiagnosticConfig.ConversionRuleSpec rule) {
        long attempts = Optional.ofNullable(byStage.get(rule.from()))
                .map(ExperimentFunnelStageDto::getTotalCount)
                .orElse(0L);
        long successes = Optional.ofNullable(byStage.get(rule.to()))
                .map(ExperimentFunnelStageDto::getTotalCount)
                .orElse(0L);
        List<FunnelThresholdCheckDto> thresholdChecks = buildThresholdChecks(rule, attempts, successes);

        if (attempts == 0 && successes == 0) {
            return new ExperimentFunnelStageDiagnosticDto(
                    rule.to(),
                    rule.to().getLabel(),
                    attempts,
                    successes,
                    null,
                    rule.minAcceptableRate(),
                    null,
                    thresholdChecks,
                    FunnelDiagnosticStatus.NO_DATA,
                    FunnelDiagnosticReasonCode.NO_ATTEMPTS,
                    "Ainda não há dados dessa etapa para concluir.",
                    false
            );
        }

        if (successes > attempts || (attempts == 0 && successes > 0)) {
            return new ExperimentFunnelStageDiagnosticDto(
                    rule.to(),
                    rule.to().getLabel(),
                    attempts,
                    successes,
                    attempts > 0 ? (double) successes / attempts : null,
                    rule.minAcceptableRate(),
                    null,
                    thresholdChecks,
                    FunnelDiagnosticStatus.TECHNICAL_ISSUE_SUSPECTED,
                    FunnelDiagnosticReasonCode.SEQUENTIAL_INCONSISTENCY,
                    "Possível problema técnico nesta etapa. Os números entre etapas sequenciais estão inconsistentes.",
                    true
            );
        }

        int attemptsFor95Confidence = (int) Math.ceil(3.0 / rule.minAcceptableRate());
        double observedRate = attempts > 0 ? (double) successes / attempts : 0.0;

        if (successes == 0) {
            double upper95 = attempts > 0 ? 3.0 / attempts : 1.0;
            if (attempts < attemptsFor95Confidence) {
                return new ExperimentFunnelStageDiagnosticDto(
                        rule.to(),
                        rule.to().getLabel(),
                        attempts,
                        successes,
                        observedRate,
                        rule.minAcceptableRate(),
                        upper95,
                        thresholdChecks,
                        FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                        FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                        "Ainda cedo para concluir nesta etapa.",
                        false
                );
            }
            if (upper95 <= rule.minAcceptableRate()) {
                return new ExperimentFunnelStageDiagnosticDto(
                        rule.to(),
                        rule.to().getLabel(),
                        attempts,
                        successes,
                        observedRate,
                        rule.minAcceptableRate(),
                        upper95,
                        thresholdChecks,
                        FunnelDiagnosticStatus.STATISTICALLY_FAILED,
                        FunnelDiagnosticReasonCode.RULE_OF_THREE_FAILED,
                        "Etapa reprovada estatisticamente no limite definido.",
                        false
                );
            }
            return new ExperimentFunnelStageDiagnosticDto(
                    rule.to(),
                    rule.to().getLabel(),
                    attempts,
                    successes,
                    observedRate,
                    rule.minAcceptableRate(),
                    upper95,
                    thresholdChecks,
                    FunnelDiagnosticStatus.WEAK_SIGNAL,
                    FunnelDiagnosticReasonCode.RULE_OF_THREE_STILL_INCONCLUSIVE,
                    "Sinal fraco nesta etapa. Continue coletando eventos.",
                    false
            );
        }

        if (observedRate < rule.minAcceptableRate()) {
            return new ExperimentFunnelStageDiagnosticDto(
                    rule.to(),
                    rule.to().getLabel(),
                    attempts,
                    successes,
                    observedRate,
                    rule.minAcceptableRate(),
                    null,
                    thresholdChecks,
                    FunnelDiagnosticStatus.WEAK_SIGNAL,
                    FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                    "Sinal fraco nesta etapa. Continue monitorando.",
                    false
            );
        }

        return new ExperimentFunnelStageDiagnosticDto(
                rule.to(),
                rule.to().getLabel(),
                attempts,
                successes,
                observedRate,
                rule.minAcceptableRate(),
                null,
                thresholdChecks,
                FunnelDiagnosticStatus.HEALTHY_OR_INCONCLUSIVE,
                FunnelDiagnosticReasonCode.HEALTHY_OR_INCONCLUSIVE,
                "Sem indício forte de reprovação estatística nesta etapa.",
                false
        );
    }

    private List<FunnelThresholdCheckDto> buildThresholdChecks(ExperimentFunnelDiagnosticConfig.ConversionRuleSpec rule,
                                                               long attempts,
                                                               long successes) {
        return rule.allThresholdRates().stream()
                .map(thresholdRate -> {
                    int attemptsFor95Confidence = (int) Math.ceil(3.0 / thresholdRate);
                    Double upper95 = attempts > 0 ? 3.0 / attempts : null;
                    boolean attemptsTargetReached = attempts >= attemptsFor95Confidence;
                    boolean statisticallyFailed = successes == 0
                            && attemptsTargetReached
                            && upper95 != null
                            && upper95 <= thresholdRate;
                    return new FunnelThresholdCheckDto(
                            thresholdRate,
                            attemptsFor95Confidence,
                            upper95,
                            statisticallyFailed,
                            attemptsTargetReached
                    );
                })
                .toList();
    }

}
