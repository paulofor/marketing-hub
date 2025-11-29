package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.dto.LeadPortalImagePackageDetailDto;
import com.marketinghub.leadportal.dto.LeadPortalImagePackageSummaryDto;
import com.marketinghub.leadportal.service.LeadPortalImagePackageService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes read-only endpoints that surface Lead Portal image packages inside Marketing Hub.
 */
@RestController
@RequestMapping("/api/lead-portal/image-packages")
public class LeadPortalImagePackageController {

    private final LeadPortalImagePackageService imagePackageService;

    public LeadPortalImagePackageController(LeadPortalImagePackageService imagePackageService) {
        this.imagePackageService = imagePackageService;
    }

    @GetMapping
    public List<LeadPortalImagePackageSummaryDto> list(
            @RequestParam(name = "status", required = false) List<FlowSubmissionImagePackageStatus> statuses) {
        Collection<FlowSubmissionImagePackageStatus> filter = statuses == null ? List.of() : new LinkedHashSet<>(statuses);
        return imagePackageService.listImagePackages(filter);
    }

    @GetMapping("/{id}")
    public LeadPortalImagePackageDetailDto get(@PathVariable("id") long id) {
        return imagePackageService.getImagePackage(id);
    }
}
