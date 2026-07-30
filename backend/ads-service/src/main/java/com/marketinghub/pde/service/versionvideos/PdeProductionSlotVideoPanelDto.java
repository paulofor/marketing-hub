package com.marketinghub.pde.service.versionvideos;

import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import java.util.List;

/** Consolida uma versão produtiva PDE com seus vídeos HLS comerciais. */
public record PdeProductionSlotVideoPanelDto(
    PostDeployPdeProductionSlotDto slot,
    List<PdeProductionSlotVideoAssetDto> videos,
    List<String> alerts) {}
