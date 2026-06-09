package com.marketinghub.facebookads.service.targetingPackage;

import com.marketinghub.facebookads.dto.TargetingPackageDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contrato enxuto com o pacote de segmentação necessário para publicar um conjunto de anúncios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacebookAdSetTargetingPackageDto {
    private Long experimentId;
    private TargetingPackageDto targeting;
}
