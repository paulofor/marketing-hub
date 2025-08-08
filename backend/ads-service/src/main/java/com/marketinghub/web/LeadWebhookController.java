package com.marketinghub.web;

import com.marketinghub.dto.LeadDTO;
import com.marketinghub.service.LeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook endpoint receiving leadgen payloads.
 */
@RestController
@RequestMapping("/webhook")
public class LeadWebhookController {
    private final LeadService leadService;

    public LeadWebhookController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping("/leadgen")
    public ResponseEntity<Void> receiveLead(@RequestBody LeadDTO dto) {
        leadService.saveFromWebhook(dto);
        return ResponseEntity.accepted().build();
    }
}
