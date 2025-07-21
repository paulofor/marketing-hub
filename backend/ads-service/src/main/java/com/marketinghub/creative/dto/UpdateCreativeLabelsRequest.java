package com.marketinghub.creative.dto;

import java.util.Set;
import lombok.Data;

@Data
public class UpdateCreativeLabelsRequest {
    private Set<Long> angles;
    private Set<Long> visualProofs;
    private Set<Long> emotionalTriggers;
}
