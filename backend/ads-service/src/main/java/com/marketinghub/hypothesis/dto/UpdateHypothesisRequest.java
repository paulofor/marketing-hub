package com.marketinghub.hypothesis.dto;

import java.math.BigDecimal;

public class UpdateHypothesisRequest {
    private String title;
    private Long premiseAngleId;
    private String promise;
    private String problem;
    private String persona;
    private String mechanism;
    private String uniqueMechanism;
    private String entrega;
    private String successRule;
    private String prompt;
    private String model;
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

    public String getMechanism() { return mechanism; }
    public void setMechanism(String mechanism) { this.mechanism = mechanism; }

    public String getUniqueMechanism() { return uniqueMechanism; }
    public void setUniqueMechanism(String uniqueMechanism) { this.uniqueMechanism = uniqueMechanism; }

    public String getEntrega() { return entrega; }
    public void setEntrega(String entrega) { this.entrega = entrega; }

    public String getSuccessRule() { return successRule; }
    public void setSuccessRule(String successRule) { this.successRule = successRule; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getKpiTargetCpl() { return kpiTargetCpl; }
    public void setKpiTargetCpl(BigDecimal kpiTargetCpl) { this.kpiTargetCpl = kpiTargetCpl; }
}
