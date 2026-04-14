package com.marketinghub.experiment.funnel;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuração inicial dos diagnósticos estatísticos do funil.
 * Mantém thresholds em um ponto único para facilitar evolução sem espalhar regra na UI.
 */
@Component
public class ExperimentFunnelDiagnosticConfig {

    private static final int MIN_OPTIMIZATION_EVENT_VOLUME_FOR_CONTEXT = 50;

    private static final List<ConversionRuleSpec> PRIORITIZED_RULES = List.of(
            new ConversionRuleSpec(ExperimentFunnelStage.VISUALIZACAO_FORM, ExperimentFunnelStage.ENVIO_FORM, 0.10),
            new ConversionRuleSpec(ExperimentFunnelStage.ENVIO_FORM, ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA, 0.05),
            new ConversionRuleSpec(ExperimentFunnelStage.ACESSO_CHECKOUT, ExperimentFunnelStage.COMPRA, 0.03)
    );

    public List<ConversionRuleSpec> prioritizedRules() {
        return PRIORITIZED_RULES;
    }

    public int minOptimizationEventVolumeForContext() {
        return MIN_OPTIMIZATION_EVENT_VOLUME_FOR_CONTEXT;
    }

    public record ConversionRuleSpec(
            ExperimentFunnelStage from,
            ExperimentFunnelStage to,
            double minAcceptableRate
    ) {
    }
}
