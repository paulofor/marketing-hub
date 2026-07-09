package com.marketinghub.experiment.manual;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Transporta os campos do wizard manual para criar nicho, hipótese e experimento sem acionar IA.
 */
@Data
public class ManualExperimentCreationRequest {
    private String nicheName;
    private String nicheAudience;
    private String nicheDescription;
    private String marketReference;
    private String pains;
    private String desires;
    private String likelyChannels;
    private String hypothesisStatement;
    private String persona;
    private String problem;
    private String promise;
    private String mechanism;
    private String proof;
    private String successSignal;
    private String offerName;
    private String leadMagnet;
    private String productName;
    private String primaryCta;
    private BigDecimal testPrice;
    private String promiseLimit;
    private String validationType;
    private String experimentChannel;
    private BigDecimal dailyBudget;
    private BigDecimal kpiTargetCpl;
    private Integer sampleSize;
    private String creativeAngles;
    private String successCriteria;
    private String discardCriteria;
}
