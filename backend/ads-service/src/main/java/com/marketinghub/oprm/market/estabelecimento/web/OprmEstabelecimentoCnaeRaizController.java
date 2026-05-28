package com.marketinghub.oprm.market.estabelecimento.web;

import com.marketinghub.oprm.market.estabelecimento.dto.OprmEstabelecimentoCnaeRaizBatchRequestDto;
import com.marketinghub.oprm.market.estabelecimento.dto.OprmEstabelecimentoCnaeRaizBatchResponseDto;
import com.marketinghub.oprm.market.estabelecimento.service.OprmEstabelecimentoCnaeRaizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller OPRM dedicado à ingestão operacional de vínculos CNPJ raiz/CNAE dos estabelecimentos.
 */
@RestController
@RequestMapping("/api/oprm/market/estabelecimentos-cnae-raiz")
@RequiredArgsConstructor
public class OprmEstabelecimentoCnaeRaizController {

    private final OprmEstabelecimentoCnaeRaizService service;

    /**
     * Recebe um lote de estabelecimentos normalizados pelo coletor e delega a persistência ao serviço.
     */
    @PostMapping("/batch")
    public OprmEstabelecimentoCnaeRaizBatchResponseDto upsertBatch(
            @RequestBody OprmEstabelecimentoCnaeRaizBatchRequestDto request) {
        return service.upsertBatch(request);
    }
}
