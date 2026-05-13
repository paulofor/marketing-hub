package com.marketinghub.oprm.market;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Entity
@Data
@Table(name = "oprm_market_size_by_cnae")
public class OprmMarketSizeByCnae {
    @EmbeddedId
    private OprmMarketSizeByCnaeId id;
    private long totalEstabelecimentos;
    private long totalEstabelecimentosAtivos;
    private long totalEmpresas;
    private long totalEmpresasMei;
    private long totalEmpresasSimples;
    private BigDecimal avgSociosPorEmpresa;
    private Instant updatedAt;
}
