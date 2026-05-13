package com.marketinghub.oprm.market.dto;

import java.math.BigDecimal;

public record OprmMarketSizeUpsertDto(String cnaeCode,
                                      Long totalEstabelecimentos,
                                      Long totalEstabelecimentosAtivos,
                                      Long totalEmpresas,
                                      Long totalEmpresasMei,
                                      Long totalEmpresasSimples,
                                      BigDecimal avgSociosPorEmpresa) {}
