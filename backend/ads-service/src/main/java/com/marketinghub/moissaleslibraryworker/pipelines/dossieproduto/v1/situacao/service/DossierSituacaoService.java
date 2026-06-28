package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoItem;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoResponse;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Consulta a situação auditada das etapas do pipeline de dossiê de produto MOIS v1. */
@Service
public class DossierSituacaoService {
    private static final String INTAKE_STAGE_CODE = "intake";
    private static final String STATUS_STARTED = "INICIADO";

    private final PipelineDossieProdutoRepository repository;

    /** Cria o service com acesso ao repositório canônico da auditoria do pipeline. */
    public DossierSituacaoService(PipelineDossieProdutoRepository repository) {
        this.repository = repository;
    }

    /** Pesquisa registros da auditoria do fluxo atual pelo identificador externo, etapa e lista de status. */
    public DossierSituacaoResponse consultar(String codigoEtapa, String idExterno, DossierSituacaoRequest request) {
        List<String> status = request.status().stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .toList();
        List<PipelineDossieProduto> auditorias = buscarRegistrosDoFluxoAtual(codigoEtapa, idExterno, status);
        Map<String, String> requestsPorJob = mapearRequestsPorJob(auditorias);
        List<DossierSituacaoItem> registros = auditorias.stream()
                .map(pipeline -> toItem(pipeline, requestsPorJob.get(chaveJob(pipeline))))
                .toList();
        return new DossierSituacaoResponse(idExterno, codigoEtapa, status, registros);
    }

    /** Limita a consulta à última execução iniciada pela etapa intake, preservando histórico fora da tela atual. */
    private List<PipelineDossieProduto> buscarRegistrosDoFluxoAtual(
            String codigoEtapa, String idExterno, List<String> status) {
        if (status.isEmpty()) {
            return List.of();
        }
        Instant fluxoAtualIniciadoEm = repository
                .findTopByIdExternoAndCodigoEtapaAndStatusOrderByDataHoraDescIdDesc(
                        idExterno, INTAKE_STAGE_CODE, STATUS_STARTED)
                .map(PipelineDossieProduto::getDataHora)
                .orElse(null);
        if (fluxoAtualIniciadoEm == null) {
            return repository.findByIdExternoAndCodigoEtapaAndStatusInOrderByDataHoraDescIdDesc(
                    idExterno, codigoEtapa, status);
        }
        return repository.findByIdExternoAndCodigoEtapaAndStatusInAndDataHoraGreaterThanEqualOrderByDataHoraDescIdDesc(
                idExterno, codigoEtapa, status, fluxoAtualIniciadoEm);
    }

    /** Mapeia o último request auditado por job para exibir request e response juntos na tela. */
    private Map<String, String> mapearRequestsPorJob(List<PipelineDossieProduto> auditorias) {
        Map<String, String> requestsPorJob = new HashMap<>();
        for (PipelineDossieProduto auditoria : auditorias) {
            String request = auditoria.getRequest();
            if (request != null && !request.isBlank()) {
                requestsPorJob.putIfAbsent(chaveJob(auditoria), request);
            }
        }
        return requestsPorJob;
    }

    /** Gera a chave de correlação entre registros separados de request e response do mesmo job/etapa. */
    private String chaveJob(PipelineDossieProduto pipeline) {
        return String.join("|",
                valorChave(pipeline.getIdExterno()),
                valorChave(pipeline.getCodigoEtapa()),
                valorChave(pipeline.getJobId()));
    }

    /** Normaliza valores nulos usados na chave de correlação de auditoria. */
    private String valorChave(String valor) {
        return valor == null ? "" : valor;
    }

    /** Converte a entidade de auditoria para o contrato de consulta de situação. */
    private DossierSituacaoItem toItem(PipelineDossieProduto pipeline, String requestCorrelacionado) {
        return new DossierSituacaoItem(
                pipeline.getId(),
                pipeline.getIdExterno(),
                pipeline.getCodigoEtapa(),
                pipeline.getStatus(),
                pipeline.getDataHora(),
                pipeline.getJobId(),
                requestVisivel(pipeline.getRequest(), requestCorrelacionado),
                pipeline.getResponse(),
                pipeline.getRespostaFinal(),
                pipeline.getQuantidadeTokenEntrada(),
                pipeline.getQuantidadeTokenSaida(),
                pipeline.getModelo(),
                pipeline.getCusto(),
                pipeline.getDescricaoErro(),
                pipeline.getJobIdExterno(),
                pipeline.getPlataforma(),
                pipeline.getPrompt(),
                pipeline.getSchema(),
                pipeline.getVersaoPipeline());
    }

    /** Prioriza o request do próprio registro e usa o request correlacionado quando a linha atual é de response. */
    private String requestVisivel(String requestDoRegistro, String requestCorrelacionado) {
        return requestDoRegistro == null || requestDoRegistro.isBlank() ? requestCorrelacionado : requestDoRegistro;
    }
}
