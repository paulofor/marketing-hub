package com.marketinghub.businessprocesscomposition.service.getcomposition;

import java.util.List;

/** Contrato de leitura da relação entre um processo de valor e seus subprocessos vigentes. */
public record BusinessProcessCompositionResponse(
    BusinessProcessReferenceResponse process,
    BusinessProcessReferenceResponse parentProcess,
    Integer subprocessCount,
    List<BusinessProcessReferenceResponse> subprocesses) {}
