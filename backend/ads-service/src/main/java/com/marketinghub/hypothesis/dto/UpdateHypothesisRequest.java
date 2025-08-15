package com.marketinghub.hypothesis.dto;

import java.math.BigDecimal;

public class UpdateHypothesisRequest {
    private String title;
    private Long premiseAngleId;
    private String promise;
    private String problem;
    private String persona;
    private String uniqueMechanism;
    private String successRule;
    private String offerType;
    private BigDecimal price;
    private BigDecimal kpiTargetCpl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getPremiseAngleId() { return premiseAngleId; }
    public void setPremiseAngleId(Long premiseAngleId) { this.premiseAngleId = premiseAngleId; }

    public String getPromise() { return promise; }
    public void setPromise(String promise) { this.promise = promise; }

    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }

    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }

    public String getUniqueMechanism() { return uniqueMechanism; }
    public void setUniqueMechanism(String uniqueMechanism) { this.uniqueMechanism = uniqueMechanism; }

    public String getSuccessRule() { return successRule; }
    public void setSuccessRule(String successRule) { this.successRule = successRule; }

    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getKpiTargetCpl() { return kpiTargetCpl; }
    public void setKpiTargetCpl(BigDecimal kpiTargetCpl) { this.kpiTargetCpl = kpiTargetCpl; }
}
