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
            new ConversionRuleSpec(
                    ExperimentFunnelStage.VISUALIZACAO_ANUNCIO,
                    ExperimentFunnelStage.ACESSO_FORM_LEAD,
                    0.015
            ),
            new ConversionRuleSpec(
                    ExperimentFunnelStage.VISUALIZACAO_FORM,
                    ExperimentFunnelStage.ENVIO_FORM,
                    0.10,
                    List.of(0.05, 0.03, 0.02)
            ),
            new ConversionRuleSpec(ExperimentFunnelStage.ENVIO_FORM, ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA, 0.05),
            new ConversionRuleSpec(ExperimentFunnelStage.ACESSO_CHECKOUT, ExperimentFunnelStage.COMPRA, 0.03)
    );

    private static final List<ConversionRuleSpec> LOW_TICKET_PRIORITIZED_RULES = List.of(
            new ConversionRuleSpec(
                    ExperimentFunnelStage.VISUALIZACAO_ANUNCIO,
                    ExperimentFunnelStage.ACESSO_FORM_LEAD,
                    0.015
            ),
            new ConversionRuleSpec(
                    ExperimentFunnelStage.VISUALIZACAO_FORM,
                    ExperimentFunnelStage.ACESSO_CHECKOUT,
                    0.03,
                    List.of(0.02, 0.01)
            ),
            new ConversionRuleSpec(ExperimentFunnelStage.ACESSO_CHECKOUT, ExperimentFunnelStage.COMPRA, 0.03)
    );

    /**
     * Retorna as transições do funil que devem ser diagnosticadas pelo backend.
     */
    public List<ConversionRuleSpec> prioritizedRules() {
        return PRIORITIZED_RULES;
    }

    /**
     * Retorna as transições de venda direta low-ticket, sem etapas de formulário.
     */
    public List<ConversionRuleSpec> lowTicketPrioritizedRules() {
        return LOW_TICKET_PRIORITIZED_RULES;
    }

    /**
     * Retorna o volume mínimo de evento principal para considerar o aprendizado operacional suficiente.
     */
    public int minOptimizationEventVolumeForContext() {
        return MIN_OPTIMIZATION_EVENT_VOLUME_FOR_CONTEXT;
    }

    public record ConversionRuleSpec(
            ExperimentFunnelStage from,
            ExperimentFunnelStage to,
            double minAcceptableRate,
            List<Double> additionalThresholdRates
    ) {
        /**
         * Cria uma regra simples com apenas o limite principal de conversão.
         */
        public ConversionRuleSpec(ExperimentFunnelStage from,
                                  ExperimentFunnelStage to,
                                  double minAcceptableRate) {
            this(from, to, minAcceptableRate, List.of());
        }

        /**
         * Retorna todos os limites de conversão ordenados do mais exigente ao menos exigente.
         */
        public List<Double> allThresholdRates() {
            return java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(minAcceptableRate),
                            additionalThresholdRates.stream()
                    )
                    .distinct()
                    .sorted(java.util.Comparator.reverseOrder())
                    .toList();
        }
    }
}
