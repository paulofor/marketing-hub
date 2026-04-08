package com.marketinghub.experiment.frameworkimage.web.internal;

import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageWebReadyRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageWebnizationPendingAssetDto;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/framework-image/assets")
public class FrameworkImageWebnizationInternalController {
    private final FrameworkImageGenerationService service;

    public FrameworkImageWebnizationInternalController(FrameworkImageGenerationService service) {
        this.service = service;
    }

    @GetMapping("/pending-webnization")
    public List<FrameworkImageWebnizationPendingAssetDto> listPendingWebnization(
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return service.listPendingWebnizationAssets(limit != null ? limit : 20);
    }

    @PostMapping("/{assetId}/web-ready")
    public void markWebReady(@PathVariable Long assetId,
                             @Valid @RequestBody FrameworkImageWebReadyRequest request) {
        service.markAssetAsWebReady(assetId, request.webUrl());
    }
}
