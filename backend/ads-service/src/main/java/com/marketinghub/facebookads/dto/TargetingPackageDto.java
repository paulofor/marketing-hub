package com.marketinghub.facebookads.dto;

import com.marketinghub.targeting.dto.TargetingElementDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pacote com os elementos aprovados de segmentação para um experimento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetingPackageDto {
    private List<TargetingElementDto> interests;
    private List<TargetingElementDto> jobTitles;
    private List<TargetingElementDto> behaviors;
}
