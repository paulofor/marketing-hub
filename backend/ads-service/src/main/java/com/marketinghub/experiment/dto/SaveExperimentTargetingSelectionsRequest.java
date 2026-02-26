package com.marketinghub.experiment.dto;

import com.marketinghub.targeting.TargetingCandidateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SaveExperimentTargetingSelectionsRequest {
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        @NotNull
        private TargetingCandidateType candidateType;

        @NotBlank
        private String term;

        private Long targetingElementId;
    }
}
