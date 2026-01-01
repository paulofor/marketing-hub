package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalPaymentDto;
import com.marketinghub.leadportal.service.LeadPortalPaymentQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lead-portal/payments")
public class LeadPortalPaymentController {

    private final LeadPortalPaymentQueryService paymentQueryService;

    public LeadPortalPaymentController(LeadPortalPaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping
    public List<LeadPortalPaymentDto> list(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return paymentQueryService.listRecentPayments(limit);
    }
}
