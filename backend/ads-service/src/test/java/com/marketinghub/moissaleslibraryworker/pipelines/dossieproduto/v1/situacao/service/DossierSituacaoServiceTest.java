package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoRequest;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a consulta de situação do dossiê MOIS v1 limitada ao fluxo atual. */
@ExtendWith(MockitoExtension.class)
class DossierSituacaoServiceTest {
    @Mock
    private PipelineDossieProdutoRepository repository;

    /** Garante que auditorias antigas não voltem aos cards depois de um reprocessamento. */
    @Test
    void consultarFiltraRegistrosPeloInicioMaisRecenteDoFluxoAtual() {
        DossierSituacaoService service = new DossierSituacaoService(repository);
        Instant inicioAtual = Instant.parse("2026-06-28T03:27:06Z");
        PipelineDossieProduto intakeAtual = registro("400", "intake", "INICIADO", inicioAtual);
        PipelineDossieProduto registroAtual =
                registro("400", "product-understanding", "CONCLUIDO", Instant.parse("2026-06-28T03:28:25Z"));

        when(repository.findTopByIdExternoAndCodigoEtapaAndStatusOrderByDataHoraDescIdDesc(
                        "400", "intake", "INICIADO"))
                .thenReturn(Optional.of(intakeAtual));
        when(repository
                        .findByIdExternoAndCodigoEtapaAndStatusInAndDataHoraGreaterThanEqualOrderByDataHoraDescIdDesc(
                                "400", "product-understanding", List.of("CONCLUIDO"), inicioAtual))
                .thenReturn(List.of(registroAtual));

        var response = service.consultar(
                "product-understanding", "400", new DossierSituacaoRequest(List.of("CONCLUIDO")));

        assertThat(response.registros()).hasSize(1);
        assertThat(response.registros().getFirst().dataHora()).isEqualTo(Instant.parse("2026-06-28T03:28:25Z"));
        verify(repository).findByIdExternoAndCodigoEtapaAndStatusInAndDataHoraGreaterThanEqualOrderByDataHoraDescIdDesc(
                "400", "product-understanding", List.of("CONCLUIDO"), inicioAtual);
    }

    /** Garante que a linha de response exiba o request correlacionado do mesmo job para auditoria completa na tela. */
    @Test
    void consultarCorrelacionaRequestComResponseDoMesmoJob() {
        DossierSituacaoService service = new DossierSituacaoService(repository);
        Instant inicioAtual = Instant.parse("2026-06-28T03:27:06Z");
        PipelineDossieProduto intakeAtual = registro("286", "intake", "INICIADO", inicioAtual);
        PipelineDossieProduto response = registro("286", "product-understanding", "CONCLUIDO", Instant.parse("2026-06-28T22:44:00Z"));
        response.setResponse("{\"output\":[]}");
        PipelineDossieProduto request = registro("286", "product-understanding", "AGUARDANDO_RETORNO_MODULO", Instant.parse("2026-06-28T22:43:00Z"));
        request.setRequest("{\"model\":\"gpt-5.2-2025-12-11\"}");

        when(repository.findTopByIdExternoAndCodigoEtapaAndStatusOrderByDataHoraDescIdDesc(
                        "286", "intake", "INICIADO"))
                .thenReturn(Optional.of(intakeAtual));
        when(repository
                        .findByIdExternoAndCodigoEtapaAndStatusInAndDataHoraGreaterThanEqualOrderByDataHoraDescIdDesc(
                                "286",
                                "product-understanding",
                                List.of("AGUARDANDO_RETORNO_MODULO", "CONCLUIDO"),
                                inicioAtual))
                .thenReturn(List.of(response, request));

        var resultado = service.consultar(
                "product-understanding",
                "286",
                new DossierSituacaoRequest(List.of("AGUARDANDO_RETORNO_MODULO", "CONCLUIDO")));

        assertThat(resultado.registros()).hasSize(2);
        assertThat(resultado.registros().getFirst().response()).isEqualTo("{\"output\":[]}");
        assertThat(resultado.registros().getFirst().request()).isEqualTo("{\"model\":\"gpt-5.2-2025-12-11\"}");
    }

    /** Monta uma auditoria mínima do pipeline para validar a fronteira de reprocessamento. */
    private PipelineDossieProduto registro(String idExterno, String etapa, String status, Instant dataHora) {
        PipelineDossieProduto registro = new PipelineDossieProduto();
        registro.setId(1L);
        registro.setIdExterno(idExterno);
        registro.setCodigoEtapa(etapa);
        registro.setStatus(status);
        registro.setDataHora(dataHora);
        registro.setJobId("job-atual");
        registro.setVersaoPipeline("v1");
        return registro;
    }
}
