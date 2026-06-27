package com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service.BackendNichoCnaeV3PipelineSituacaoService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service.searchPipelineSituacao.NichoCnaeV3PipelineSituacaoRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service.searchPipelineSituacao.NichoCnaeV3PipelineSituacaoResponse;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller interno para consultar a situação auditada do pipeline NichoCNAE v3. */
@RestController
public class BackendNichoCnaeV3PipelineSituacaoController {
    private final BackendNichoCnaeV3PipelineSituacaoService service;

    /** Inicializa o controller com o service canônico de consulta de situação. */
    public BackendNichoCnaeV3PipelineSituacaoController(BackendNichoCnaeV3PipelineSituacaoService service) {
        this.service = service;
    }

    /** Consulta registros de auditoria por etapa, id externo e lista de status. */
    @PostMapping({
        "/api/internal/oprmcoletormei/nichocnae/v1/{etapa}/stage-executions/{idExterno}/situacao",
        "/api/internal/oprmcoletormei/nichocnae/v3/{etapa}/stage-executions/{idExterno}/situacao"
    })
    public List<NichoCnaeV3PipelineSituacaoResponse> search(
            @PathVariable String etapa,
            @PathVariable String idExterno,
            @RequestBody NichoCnaeV3PipelineSituacaoRequest request) {
        return service.search(etapa, idExterno, request == null ? null : request.status());
    }
}
