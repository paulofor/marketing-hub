package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalFormResponseDto;
import com.marketinghub.leadportal.service.LeadPortalFormResponseService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the most recent Lead Portal form submissions to the frontend. */
@RestController
@RequestMapping("/api/lead-portal/form-responses")
@Validated
public class LeadPortalFormResponseController {

    private final LeadPortalFormResponseService service;

    public LeadPortalFormResponseController(LeadPortalFormResponseService service) {
        this.service = service;
    }

    @GetMapping
    public List<LeadPortalFormResponseDto> listRecent(
            @RequestParam(name = "limit", defaultValue = "50") @Positive int limit) {
        return service.listRecentResponses(limit);
    }
}
