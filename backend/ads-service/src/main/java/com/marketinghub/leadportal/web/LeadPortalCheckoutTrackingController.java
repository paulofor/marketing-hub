package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.service.LeadPortalCheckoutTrackingService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público usado nos e-mails para registrar o acesso ao checkout.
 */
@RestController
@RequestMapping("/api/public/lead-portal/purchases")
public class LeadPortalCheckoutTrackingController {

    private final LeadPortalCheckoutTrackingService trackingService;

    public LeadPortalCheckoutTrackingController(LeadPortalCheckoutTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{purchaseId}/checkout")
    public ResponseEntity<Void> redirectToCheckout(@PathVariable long purchaseId,
                                                   @RequestParam(name = "sid", required = false) String submissionId) {
        var redirect = trackingService.registerCheckoutAccess(purchaseId, submissionId);
        if (redirect.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect.get().url()))
                .build();
    }
}
