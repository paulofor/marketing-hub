package com.marketinghub.oprm.market.repository;

import com.marketinghub.oprm.market.OprmMarketSizeByCnae;
import com.marketinghub.oprm.market.OprmMarketSizeByCnaeId;
import com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OprmMarketSizeByCnaeRepository extends JpaRepository<OprmMarketSizeByCnae, OprmMarketSizeByCnaeId> {
    @Query("""
            select new com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto(
                ms.id.snapshotDate,
                ms.id.cnaeCode,
                c.description,
                ms.totalEstabelecimentos,
                ms.totalEstabelecimentosAtivos,
                ms.totalEmpresas,
                ms.totalEmpresasMei,
                ms.totalEmpresasSimples
            )
            from OprmMarketSizeByCnae ms
            left join OprmCnpjCnaeDim c on c.cnaeCode = ms.id.cnaeCode
            where ms.id.snapshotDate = (select max(m2.id.snapshotDate) from OprmMarketSizeByCnae m2)
            order by ms.totalEmpresas desc
            """)
    List<OprmTopCnaeMarketVolumeDto> findTopByLatestSnapshot(Pageable pageable);
}
