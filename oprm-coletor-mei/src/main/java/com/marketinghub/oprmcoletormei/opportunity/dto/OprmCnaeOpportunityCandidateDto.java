package com.marketinghub.oprmcoletormei.opportunity.dto;

import java.time.LocalDate;

/** DTO que representa CNAE sem score recebido do backend para processamento no OPRM. */
public record OprmCnaeOpportunityCandidateDto(
        LocalDate snapshotDate,
        String cnaeCode,
        String cnaeDescription,
        long totalEstabelecimentos,
        long totalEstabelecimentosAtivos,
        long totalEmpresas,
        long totalEmpresasMei,
        long totalEmpresasSimples) {}
