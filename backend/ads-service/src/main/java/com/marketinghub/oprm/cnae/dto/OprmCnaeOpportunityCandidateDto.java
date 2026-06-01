package com.marketinghub.oprm.cnae.dto;

import java.time.LocalDate;

/**
 * DTO de leitura de CNAE elegível para cálculo de score pelo módulo OPRM.
 */
public record OprmCnaeOpportunityCandidateDto(
        LocalDate snapshotDate,
        String cnaeCode,
        String cnaeDescription,
        long totalEstabelecimentos,
        long totalEstabelecimentosAtivos,
        long totalEmpresas,
        long totalEmpresasMei,
        long totalEmpresasSimples) {}
