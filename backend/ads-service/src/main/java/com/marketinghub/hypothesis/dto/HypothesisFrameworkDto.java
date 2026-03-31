package com.marketinghub.hypothesis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HypothesisFrameworkDto {
    private String version;
    @Builder.Default
    private Pain pain = new Pain();
    @Builder.Default
    private Result result = new Result();
    @Builder.Default
    private Mechanism mechanism = new Mechanism();
    @Builder.Default
    private Proof proof = new Proof();
    @Builder.Default
    private Offer offer = new Offer();
    @Builder.Default
    private Checklist checklist = new Checklist();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pain {
        private String surface;
        private String root;
        private String emotional;
        private String social;
        private String cost;
        @JsonAlias({"summary", "resumo", "item resumido", "itemResumido"})
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Result {
        private String desiredResult;
        private String desiredIdentity;
        private String businessOutcome;
        private String successSignal;
        @JsonAlias({"summary", "resumo", "item resumido", "itemResumido"})
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Mechanism {
        private String core;
        private String unique;
        private String visible;
        private String believability;
        @JsonAlias({"summary", "resumo", "item resumido", "itemResumido"})
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Proof {
        private String type;
        private String asset;
        private String message;
        private String deliveryStage;
        @JsonAlias({"summary", "resumo", "item resumido", "itemResumido"})
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Offer {
        private String name;
        @JsonAlias("promise")
        private String corePromise;
        private String deliverables;
        private String riskReversal;
        @JsonAlias("priceNarrative")
        private String priceLogic;
        private String cta;
        private BigDecimal priceAmount;
        private String offerType;
        @JsonAlias({"summary", "resumo", "item resumido", "itemResumido"})
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Checklist {
        @Builder.Default
        private Boolean painReady = Boolean.FALSE;
        @Builder.Default
        private Boolean resultReady = Boolean.FALSE;
        @Builder.Default
        private Boolean mechanismReady = Boolean.FALSE;
        @Builder.Default
        private Boolean proofReady = Boolean.FALSE;
        @Builder.Default
        private Boolean offerReady = Boolean.FALSE;
        @Builder.Default
        private Boolean approvedForExperiment = Boolean.FALSE;
        private String notes;
    }
}
