package com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service;

import com.marketinghub.oprm.nichocnae.PipelineNichoCnae;
import com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service.searchPipelineSituacao.NichoCnaeV3PipelineSituacaoResponse;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Consulta auditorias do pipeline NichoCNAE v3 por etapa, identificador externo e situação. */
@Service
public class BackendNichoCnaeV3PipelineSituacaoService {
    private final PipelineNichoCnaeRepository repository;

    /** Inicializa o service com o repository canônico da auditoria NichoCNAE. */
    public BackendNichoCnaeV3PipelineSituacaoService(PipelineNichoCnaeRepository repository) {
        this.repository = repository;
    }

    /** Retorna os registros de auditoria que coincidem com etapa, id externo e qualquer status informado. */
    public List<NichoCnaeV3PipelineSituacaoResponse> search(String etapa, String idExterno, List<String> status) {
        if (!StringUtils.hasText(etapa)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "etapa é obrigatória.");
        }
        if (!StringUtils.hasText(idExterno)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idExterno é obrigatório.");
        }
        List<String> normalizedStatus = normalizeStatus(status);
        if (normalizedStatus.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status deve conter ao menos uma situação.");
        }
        return repository.findByCodigoEtapaAndIdExternoAndStatusInOrderByDataHoraDesc(etapa, idExterno, normalizedStatus)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Normaliza a lista de status removendo valores vazios e duplicados. */
    private List<String> normalizeStatus(List<String> status) {
        if (status == null) {
            return List.of();
        }
        return status.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /** Converte a entidade de auditoria para o contrato público do endpoint. */
    private NichoCnaeV3PipelineSituacaoResponse toResponse(PipelineNichoCnae pipeline) {
        return new NichoCnaeV3PipelineSituacaoResponse(
                pipeline.getIdExterno(),
                pipeline.getCodigoEtapa(),
                pipeline.getStatus(),
                pipeline.getDataHora(),
                pipeline.getJobId(),
                pipeline.getRequest(),
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
}
