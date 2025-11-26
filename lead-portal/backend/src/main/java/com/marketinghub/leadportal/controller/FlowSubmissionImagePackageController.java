package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.FlowSubmissionImagePackageResponse;
import com.marketinghub.leadportal.service.FlowSubmissionImagePackageService;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image-packages")
@CrossOrigin
@Validated
public class FlowSubmissionImagePackageController {

    private final FlowSubmissionImagePackageService imagePackageService;

    public FlowSubmissionImagePackageController(FlowSubmissionImagePackageService imagePackageService) {
        this.imagePackageService = imagePackageService;
    }

    @GetMapping("/pending")
    public List<FlowSubmissionImagePackageResponse> listPendingPackages() {
        return imagePackageService.listPendingPackages();
    }
}
