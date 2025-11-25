package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalSubmissionDto;
import com.marketinghub.leadportal.service.LeadPortalSubmissionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exibe os envios de imagem realizados no Lead Portal.
 */
@RestController
@RequestMapping("/api/lead-portal/submissions")
public class LeadPortalSubmissionController {
    private final LeadPortalSubmissionService submissionService;

    public LeadPortalSubmissionController(LeadPortalSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping
    public List<LeadPortalSubmissionDto> list() {
        return submissionService.listWithImages();
    }
}
