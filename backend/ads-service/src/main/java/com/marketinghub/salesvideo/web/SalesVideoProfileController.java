package com.marketinghub.salesvideo.web;

import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.service.SalesVideoProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints administrativos para perfis de Avatar Sales Video.
 */
@RestController
@RequestMapping("/api")
public class SalesVideoProfileController {
    private final SalesVideoProfileService profileService;

    public SalesVideoProfileController(SalesVideoProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/products/{productId}/sales-videos/profiles")
    public SalesVideoProfileDto createProfile(@PathVariable Long productId,
                                              @Valid @RequestBody CreateSalesVideoProfileRequest request) {
        return profileService.createProfile(productId, request);
    }

    @GetMapping("/products/{productId}/sales-videos/profiles")
    public List<SalesVideoProfileDto> listProfiles(@PathVariable Long productId) {
        return profileService.listProfiles(productId);
    }

    @GetMapping("/sales-videos/profiles/{profileId}")
    public SalesVideoProfileDto getProfile(@PathVariable Long profileId) {
        return profileService.getProfile(profileId);
    }

    @PostMapping("/sales-videos/profiles/{profileId}/generate-script")
    public SalesVideoJobDto requestScript(@PathVariable Long profileId,
                                          @Valid @RequestBody GenerateSalesVideoScriptRequest request) {
        return profileService.requestScriptGeneration(profileId, request);
    }

    @PostMapping("/sales-videos/profiles/{profileId}/approve-script")
    public SalesVideoScriptDto approveScript(@PathVariable Long profileId,
                                             @Valid @RequestBody ApproveSalesVideoScriptRequest request) {
        return profileService.approveScript(profileId, request);
    }

    @PostMapping("/sales-videos/profiles/{profileId}/request-render")
    public SalesVideoJobDto requestRender(@PathVariable Long profileId,
                                          @Valid @RequestBody RequestVideoRenderRequest request) {
        return profileService.requestRender(profileId, request);
    }
}
