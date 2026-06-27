package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoItem;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoResponse;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.util.List;
import org.springframework.stereotype.Service;

/** Consulta a situação auditada das etapas do pipeline de dossiê de produto MOIS v1. */
@Service
public class DossierSituacaoService {
    private final PipelineDossieProdutoRepository repository;

    /** Cria o service com acesso ao repositório canônico da auditoria do pipeline. */
    public DossierSituacaoService(PipelineDossieProdutoRepository repository) {
        this.repository = repository;
    }

    /** Pesquisa registros da auditoria pelo identificador externo, etapa e lista de status. */
    public DossierSituacaoResponse consultar(String codigoEtapa, String idExterno, DossierSituacaoRequest request) {
        List<String> status = request.status().stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .toList();
        List<DossierSituacaoItem> registros = status.isEmpty()
                ? List.of()
                : repository.findByIdExternoAndCodigoEtapaAndStatusInOrderByDataHoraDescIdDesc(
                                idExterno, codigoEtapa, status)
                        .stream()
                        .map(this::toItem)
                        .toList();
        return new DossierSituacaoResponse(idExterno, codigoEtapa, status, registros);
    }

    /** Converte a entidade de auditoria para o contrato de consulta de situação. */
    private DossierSituacaoItem toItem(PipelineDossieProduto pipeline) {
        return new DossierSituacaoItem(
                pipeline.getId(),
                pipeline.getIdExterno(),
                pipeline.getCodigoEtapa(),
                pipeline.getStatus(),
                pipeline.getDataHora(),
                pipeline.getJobId(),
                pipeline.getRequest(),
                pipeline.getResponse(),
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
}
