package com.marketinghub.experiment;

/**
 * Stage of the experiment along the Dor → Resultado → Mecanismo → Prova → Oferta pipeline.
 */
public enum ExperimentStage {
    /**
     * Ads running on Meta properties (Instagram/Facebook).
     */
    AD,
    /**
     * Landing page or lead capture step.
     */
    LANDING,
    /**
     * Delivery of the personalized sample/proof.
     */
    SAMPLE,
    /**
     * Offer/sales conversation.
     */
    SALES
}
