package com.marketinghub.oprm.market.repository;

import com.marketinghub.oprm.market.OprmMarketSizeByCnae;
import com.marketinghub.oprm.market.OprmMarketSizeByCnaeId;
import com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositório responsável por consultar e paginar os agregados de tamanho de mercado por CNAE.
 */
public interface OprmMarketSizeByCnaeRepository extends JpaRepository<OprmMarketSizeByCnae, OprmMarketSizeByCnaeId> {
    /**
     * Retorna o ranking paginado dos CNAEs no snapshot mais recente, ordenado por score OPRM decrescente.
     */
    @Query("""
            select new com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto(
                ms.id.snapshotDate,
                ms.id.cnaeCode,
                c.description,
                ms.totalEstabelecimentos,
                ms.totalEstabelecimentosAtivos,
                ms.totalEmpresas,
                ms.totalEmpresasMei,
                ms.totalEmpresasSimples,
                sc.opportunityScore,
                sc.scoreStatus
            )
            from OprmMarketSizeByCnae ms
            left join OprmCnpjCnaeDim c on c.cnaeCode = ms.id.cnaeCode
            left join OprmCnaeOpportunityScore sc on sc.cnaeCode = ms.id.cnaeCode
            where ms.id.snapshotDate = (select max(m2.id.snapshotDate) from OprmMarketSizeByCnae m2)
            order by sc.opportunityScore desc, ms.id.cnaeCode asc
            """)
    List<OprmTopCnaeMarketVolumeDto> findTopByLatestSnapshot(Pageable pageable);
}
