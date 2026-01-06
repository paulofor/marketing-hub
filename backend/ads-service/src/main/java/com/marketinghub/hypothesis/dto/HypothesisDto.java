package com.marketinghub.hypothesis.dto;

import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.OfferType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Data
public class HypothesisDto {
    private UUID id;
    private Long marketNicheId;
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
    private OfferType offerType;
    private BigDecimal price;
    private BigDecimal kpiTargetCpl;
    private BigDecimal costUsd;
    private HypothesisStatus status;
    private Instant generatedAt;
    private Instant createdAt;
    private List<Long> promptAttributeDescriptionIds;
}
