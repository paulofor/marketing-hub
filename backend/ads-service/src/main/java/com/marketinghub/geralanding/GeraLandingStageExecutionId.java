package com.marketinghub.geralanding;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GeraLandingStageExecutionId implements Serializable {
    private Long experimentId;
    private String stageCode;
    private Instant executionRequestedAt;
}
