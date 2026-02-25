package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.ImageMaterialCaseResponse;
import com.marketinghub.leadportal.dto.ImageMaterialDashboardResponse;
import com.marketinghub.leadportal.service.ImageMaterialService;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image-material")
@CrossOrigin
@Validated
public class ImageMaterialController {

    private final ImageMaterialService imageMaterialService;

    public ImageMaterialController(ImageMaterialService imageMaterialService) {
        this.imageMaterialService = imageMaterialService;
    }

    @GetMapping("/dashboard")
    public ImageMaterialDashboardResponse getDashboard(
            @RequestParam(value = "flowSlug", defaultValue = "formulario-simples-personal-trainer") String flowSlug,
            @RequestParam(value = "limit", defaultValue = "8") @Positive int limit) {
        return imageMaterialService.getDashboard(flowSlug, limit);
    }

    @GetMapping("/submissions/{id}")
    public ImageMaterialCaseResponse getSubmission(@PathVariable("id") UUID id) {
        return imageMaterialService.getCase(id);
    }
}
