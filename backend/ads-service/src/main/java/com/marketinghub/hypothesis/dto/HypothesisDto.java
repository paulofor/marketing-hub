package com.marketinghub.hypothesis.dto;

import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.OfferType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class HypothesisDto {
    private UUID id;
    private Long marketNicheId;
    private String title;
    private Long premiseAngleId;
    private OfferType offerType;
    private BigDecimal price;
    private BigDecimal kpiTargetCpl;
    private HypothesisStatus status;
}
