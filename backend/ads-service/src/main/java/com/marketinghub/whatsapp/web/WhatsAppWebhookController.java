package com.marketinghub.whatsapp.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.whatsapp.service.WhatsAppAccountService;
import com.marketinghub.whatsapp.service.WhatsAppMessagingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook endpoint for the Meta WhatsApp Cloud API.
 */
@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {
    private final WhatsAppAccountService accountService;
    private final WhatsAppMessagingService messagingService;

    public WhatsAppWebhookController(WhatsAppAccountService accountService, WhatsAppMessagingService messagingService) {
        this.accountService = accountService;
        this.messagingService = messagingService;
    }

    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && StringUtils.hasText(verifyToken)) {
            if (accountService.findByVerifyToken(verifyToken).isPresent()) {
                return ResponseEntity.ok(StringUtils.hasText(challenge) ? challenge : "");
            }
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody JsonNode payload) {
        messagingService.handleWebhook(payload);
        return ResponseEntity.accepted().build();
    }
}
