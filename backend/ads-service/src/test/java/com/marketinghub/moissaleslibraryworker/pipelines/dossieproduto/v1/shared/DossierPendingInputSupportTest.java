package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a montagem de input rico para etapas pendentes do dossiê MOIS v1. */
@ExtendWith(MockitoExtension.class)
class DossierPendingInputSupportTest {
    @Mock
    private PipelineDossieProdutoRepository repository;

    /** Garante que a etapa pendente receba histórico funcional apenas do fluxo atual. */
    @Test
    void inputForIncluiRespostasAnterioresDoFluxoAtual() {
        Instant inicioAtual = Instant.parse("2026-06-29T07:26:23Z");
        PipelineDossieProduto intake = registro("262", "intake", "INICIADO", inicioAtual, null);
        PipelineDossieProduto entendimento = registro(
                "262",
                "product-understanding",
                "CONCLUIDO",
                Instant.parse("2026-06-29T07:26:57Z"),
                "{\"produto\":\"A REVOLUÇÃO DO IODO\",\"promessa\":\"restaurar saúde\"}");

        when(repository.findTopByIdExternoAndCodigoEtapaAndStatusOrderByDataHoraDescIdDesc(
                        "262", "intake", "INICIADO"))
                .thenReturn(Optional.of(intake));
        when(repository.findByIdExternoAndVersaoPipelineAndDataHoraGreaterThanEqualOrderByDataHoraAscIdAsc(
                        "262", "v1", inicioAtual))
                .thenReturn(List.of(intake, entendimento));

        Map<String, Object> input = DossierPendingInputSupport.inputFor(
                262L, "job-ancoras", "investigation-anchor-builder", "warmup-resource-discovery", repository);

        assertThat(input).containsEntry("productKey", "262");
        assertThat(input).containsEntry("stageCode", "investigation-anchor-builder");
        assertThat(input.get("previousStageResponses").toString()).contains("A REVOLUÇÃO DO IODO");
        assertThat(input.get("previousStages").toString()).contains("product-understanding", "restaurar saúde");
        verify(repository).findByIdExternoAndVersaoPipelineAndDataHoraGreaterThanEqualOrderByDataHoraAscIdAsc(
                "262", "v1", inicioAtual);
    }

    /** Monta uma auditoria mínima para simular histórico persistido do pipeline. */
    private PipelineDossieProduto registro(
            String idExterno,
            String etapa,
            String status,
            Instant dataHora,
            String response) {
        PipelineDossieProduto registro = new PipelineDossieProduto();
        registro.setIdExterno(idExterno);
        registro.setCodigoEtapa(etapa);
        registro.setStatus(status);
        registro.setDataHora(dataHora);
        registro.setResponse(response);
        registro.setVersaoPipeline("v1");
        return registro;
    }
}
