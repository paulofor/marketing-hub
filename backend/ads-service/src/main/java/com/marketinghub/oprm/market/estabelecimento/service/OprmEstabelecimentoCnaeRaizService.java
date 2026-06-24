package com.marketinghub.oprm.market.estabelecimento.service;

import com.marketinghub.oprm.market.estabelecimento.dto.OprmEstabelecimentoCnaeRaizBatchRequestDto;
import com.marketinghub.oprm.market.estabelecimento.dto.OprmEstabelecimentoCnaeRaizBatchResponseDto;
import com.marketinghub.oprm.market.estabelecimento.dto.OprmEstabelecimentoCnaeRaizUpsertDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço responsável por persistir emails associados a CNAEs dos estabelecimentos da Receita no OPRM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OprmEstabelecimentoCnaeRaizService {

    private static final String UPSERT_ESTABELECIMENTO_CNAE_RAIZ_SQL = """
            INSERT INTO oprm_estabelecimento_cnae_raiz (
              cnpj_raiz,
              cnae_code,
              email,
              updated_at
            ) VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              email = VALUES(email),
              updated_at = VALUES(updated_at)
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Valida e grava em lote somente vínculos que tenham email associado ao CNAE.
     */
    @Transactional
    public OprmEstabelecimentoCnaeRaizBatchResponseDto upsertBatch(OprmEstabelecimentoCnaeRaizBatchRequestDto request) {
        if (request == null || request.estabelecimentos() == null || request.estabelecimentos().isEmpty()) {
            return new OprmEstabelecimentoCnaeRaizBatchResponseDto(0, 0);
        }
        List<OprmEstabelecimentoCnaeRaizUpsertDto> rows = normalizeAndValidate(request.estabelecimentos());
        if (rows.isEmpty()) {
            return new OprmEstabelecimentoCnaeRaizBatchResponseDto(request.estabelecimentos().size(), 0);
        }
        Timestamp updatedAt = Timestamp.from(Instant.now());
        try {
            jdbcTemplate.batchUpdate(
                    UPSERT_ESTABELECIMENTO_CNAE_RAIZ_SQL,
                    rows,
                    rows.size(),
                    (ps, row) -> {
                        ps.setString(1, row.cnpjRaiz());
                        ps.setString(2, row.cnaeCode());
                        ps.setString(3, row.email());
                        ps.setTimestamp(4, updatedAt);
                    }
            );
        } catch (RuntimeException ex) {
            log.error("Falha ao persistir lote de estabelecimentos OPRM. received={} valid={} sqlTentada='{}' exceptionClass={} exceptionMessage={} errorLine={}",
                    request.estabelecimentos().size(),
                    rows.size(),
                    UPSERT_ESTABELECIMENTO_CNAE_RAIZ_SQL,
                    ex.getClass().getName(),
                    ex.getMessage(),
                    firstStackLine(ex),
                    ex);
            throw ex;
        }
        log.info("Lote de estabelecimentos OPRM persistido com sucesso. received={} persisted={}",
                request.estabelecimentos().size(), rows.size());
        return new OprmEstabelecimentoCnaeRaizBatchResponseDto(request.estabelecimentos().size(), rows.size());
    }

    /**
     * Normaliza o lote e mantém somente linhas com chave mínima e email para proteger o ciclo 3.
     */
    private List<OprmEstabelecimentoCnaeRaizUpsertDto> normalizeAndValidate(List<OprmEstabelecimentoCnaeRaizUpsertDto> inputRows) {
        List<OprmEstabelecimentoCnaeRaizUpsertDto> rows = new ArrayList<>();
        for (OprmEstabelecimentoCnaeRaizUpsertDto row : inputRows) {
            if (row == null) {
                continue;
            }
            String cnpjRaiz = normalizeDigits(row.cnpjRaiz());
            String cnaeCode = normalizeDigits(row.cnaeCode());
            if (cnpjRaiz.length() != 8 || cnaeCode.length() != 7) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada estabelecimento precisa informar cnpjRaiz com 8 dígitos e cnaeCode com 7 dígitos.");
            }
            String email = normalizeEmail(row.email());
            if (email == null) {
                continue;
            }
            rows.add(new OprmEstabelecimentoCnaeRaizUpsertDto(cnpjRaiz, cnaeCode, email));
        }
        return rows;
    }

    /**
     * Identifica a primeira linha de stack trace disponível para diagnóstico operacional.
     */
    private String firstStackLine(RuntimeException ex) {
        return ex.getStackTrace().length == 0 ? "sem-stacktrace" : ex.getStackTrace()[0].toString();
    }

    /**
     * Remove caracteres não numéricos de campos de chave oriundos dos arquivos da Receita.
     */
    private String normalizeDigits(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    /**
     * Normaliza emails vazios para null e limita o valor ao tamanho suportado pelo schema.
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        return normalized.length() > 254 ? normalized.substring(0, 254) : normalized;
    }
}
