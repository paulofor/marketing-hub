package com.marketinghub.experiment.dto;

import com.marketinghub.targeting.TargetingCandidateType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Requisição para criar uma variação de público a ser testada em um experimento.
 */
@Data
public class CreateExperimentAudienceTestRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String hypothesis;

    @NotBlank
    private String successMetric;

    private BigDecimal dailyBudget;

    @Valid
    @NotEmpty
    private List<Item> items = new ArrayList<>();

    /**
     * Item de targeting oficial da Meta escolhido para a variação.
     */
    @Data
    public static class Item {
        @NotNull
        private TargetingCandidateType candidateType;

        @NotNull
        private Long targetingElementId;
    }
}
