package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Representa o contexto validado recebido do backend para fabricar uma oferta.
 */
public record FabricationContext(
        String requestId,
        String experimentId,
        String offerName,
        String niche,
        String centralPromise,
        String promisedResult,
        String coreMechanism,
        String proofSummary,
        List<String> deliverables,
        List<String> validationSignals) {

    /**
     * Retorna um contexto minimo para testes locais e smoke tests.
     */
    public static FabricationContext sample() {
        return new FabricationContext(
                "local-request",
                "experiment-local",
                "Kit Radar de Giro e Caixa do Estoque",
                "Lojas de vestuario",
                "Transformar estoque confuso em prioridades claras de acao.",
                "Saber o que expor, promover, observar ou evitar comprar novamente.",
                "Radar de Giro e Caixa cruzando giro, margem, tempo parado e caracteristicas das pecas.",
                "Mini-diagnostico com amostra de produtos reais e plano de 7 dias.",
                List.of(
                        "Planilha Radar de Giro e Caixa",
                        "Checklist de dados minimos",
                        "Roteiro semanal de leitura do estoque",
                        "Plano de acao de 7 dias"),
                List.of("Dor validada: dinheiro parado no cabide", "Promessa testada em experimento comercial"));
    }
}
