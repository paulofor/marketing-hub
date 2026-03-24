package com.marketinghub.salesvideo.web;

import com.marketinghub.salesvideo.dto.CreateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.dto.LandingVideoSlotDto;
import com.marketinghub.salesvideo.dto.UpdateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.service.LandingVideoSlotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints administrativos responsáveis por vincular vídeos às landing pages.
 */
@RestController
@RequestMapping("/api/landing-pages/{landingId}/video-slots")
public class LandingVideoSlotController {
    private final LandingVideoSlotService slotService;

    public LandingVideoSlotController(LandingVideoSlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping
    public LandingVideoSlotDto create(@PathVariable Long landingId,
                                      @Valid @RequestBody CreateLandingVideoSlotRequest request) {
        return slotService.create(landingId, request);
    }

    @PatchMapping("/{slotId}")
    public LandingVideoSlotDto update(@PathVariable Long landingId,
                                      @PathVariable Long slotId,
                                      @RequestBody UpdateLandingVideoSlotRequest request) {
        return slotService.update(landingId, slotId, request);
    }
}
