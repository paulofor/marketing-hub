package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service;

import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending.CandidateGeneratorPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Expõe contratos de leitura/escrita do backend para a etapa candidate-generator do pipeline NichoCNAE v2. */
@Service
public class BackendCandidateGeneratorService {
    /** Lista pendências disponíveis para consumo canônico pelo executor OPRM NichoCNAE v2. */
    public List<CandidateGeneratorPendingResponse> pending() {
        return List.of();
    }
}
