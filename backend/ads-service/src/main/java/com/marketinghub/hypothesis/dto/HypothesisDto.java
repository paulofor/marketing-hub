package com.marketinghub.hypothesis.dto;

import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.OfferType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HypothesisDto {
    private Long id;
    private Long experimentId;
    private String title;
    private Long premiseAngleId;
    private OfferType offerType;
    private BigDecimal kpiTargetCpl;
    private HypothesisStatus status;
}
