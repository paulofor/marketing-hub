package com.marketinghub.hypothesis.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.util.List;

public class CreateHypothesisRequest {
    private Long marketNicheId;
    private String title;
    private Long premiseAngleId;
    private String promise;
    private String problem;
    private String persona;
    private String mechanism;

    @JsonAlias("unique_mechanism")
    private String uniqueMechanism;
    private String entrega;
    private String successRule;
    private String prompt;
    private String model;
    private BigDecimal costUsd;
    private BigDecimal cost;
    private BigDecimal expense;
    private String offerType;
    private BigDecimal price;
    private BigDecimal kpiTargetCpl;
    private Long offerPackageId;
    private List<Long> promptAttributeDescriptionIds;
    private HypothesisFrameworkDto framework;

    public Long getMarketNicheId() { return marketNicheId; }
    public void setMarketNicheId(Long marketNicheId) { this.marketNicheId = marketNicheId; }

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
    public BigDecimal getCostUsd() { return costUsd; }
    public void setCostUsd(BigDecimal costUsd) { this.costUsd = costUsd; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getExpense() { return expense; }
    public void setExpense(BigDecimal expense) { this.expense = expense; }
    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getKpiTargetCpl() { return kpiTargetCpl; }
    public void setKpiTargetCpl(BigDecimal kpiTargetCpl) { this.kpiTargetCpl = kpiTargetCpl; }

    public Long getOfferPackageId() { return offerPackageId; }
    public void setOfferPackageId(Long offerPackageId) { this.offerPackageId = offerPackageId; }

    public List<Long> getPromptAttributeDescriptionIds() { return promptAttributeDescriptionIds; }
    public void setPromptAttributeDescriptionIds(List<Long> promptAttributeDescriptionIds) { this.promptAttributeDescriptionIds = promptAttributeDescriptionIds; }

    public HypothesisFrameworkDto getFramework() { return framework; }
    public void setFramework(HypothesisFrameworkDto framework) { this.framework = framework; }
}
