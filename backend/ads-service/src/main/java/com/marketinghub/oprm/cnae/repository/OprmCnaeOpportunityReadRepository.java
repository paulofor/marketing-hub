package com.marketinghub.oprm.cnae.repository;

import com.marketinghub.oprm.cnae.dto.OprmCnaeOpportunityCandidateDto;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositório JDBC responsável por leituras operacionais de CNAEs para o OPRM sem aplicar regra de negócio.
 */
@Repository
public class OprmCnaeOpportunityReadRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Inicializa o repositório com o template JDBC usado para consultas SQL filtradas.
     */
    public OprmCnaeOpportunityReadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retorna CNAEs sem score persistido no snapshot mais recente, usando apenas filtros e ordenação de leitura.
     */
    public List<OprmCnaeOpportunityCandidateDto> findMissingScores(int limit) {
        return jdbcTemplate.query("""
                select ms.snapshot_date,
                       ms.cnae_code,
                       coalesce(dim.description, ms.cnae_code) as cnae_description,
                       ms.total_estabelecimentos,
                       ms.total_estabelecimentos_ativos,
                       ms.total_empresas,
                       ms.total_empresas_mei,
                       ms.total_empresas_simples
                  from oprm_market_size_by_cnae ms
                  left join oprm_cnpj_cnae_dim dim on dim.cnae_code = ms.cnae_code
                  left join oprm_cnae_opportunity_score score on score.cnae_code = ms.cnae_code
                 where ms.snapshot_date = (select max(ms2.snapshot_date) from oprm_market_size_by_cnae ms2)
                   and score.cnae_code is null
                 order by ms.total_empresas_mei desc, ms.cnae_code asc
                 limit ?
                """, (rs, rowNum) -> new OprmCnaeOpportunityCandidateDto(
                        rs.getDate("snapshot_date").toLocalDate(),
                        rs.getString("cnae_code"),
                        rs.getString("cnae_description"),
                        rs.getLong("total_estabelecimentos"),
                        rs.getLong("total_estabelecimentos_ativos"),
                        rs.getLong("total_empresas"),
                        rs.getLong("total_empresas_mei"),
                        rs.getLong("total_empresas_simples")), limit);
    }
}
