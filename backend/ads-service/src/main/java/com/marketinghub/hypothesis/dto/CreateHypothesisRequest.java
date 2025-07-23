package com.marketinghub.hypothesis.dto;

import java.math.BigDecimal;

public class CreateHypothesisRequest {
    private String title;
    private Long premiseAngleId;
    private String offerType;
    private BigDecimal price;
    private BigDecimal kpiTargetCpl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getPremiseAngleId() { return premiseAngleId; }
    public void setPremiseAngleId(Long premiseAngleId) { this.premiseAngleId = premiseAngleId; }
    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getKpiTargetCpl() { return kpiTargetCpl; }
    public void setKpiTargetCpl(BigDecimal kpiTargetCpl) { this.kpiTargetCpl = kpiTargetCpl; }
}
