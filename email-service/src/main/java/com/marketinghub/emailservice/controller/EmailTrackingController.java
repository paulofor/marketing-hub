package com.marketinghub.emailservice.controller;

import com.marketinghub.emailservice.service.EmailLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Base64;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking")
@Tag(name = "Email Tracking", description = "Endpoints para rastreamento de abertura de e-mails")
public class EmailTrackingController {

    private static final byte[] TRANSPARENT_PIXEL = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAOb+hKsAAAAASUVORK5CYII=");

    private final EmailLogService emailLogService;

    public EmailTrackingController(EmailLogService emailLogService) {
        this.emailLogService = emailLogService;
    }

    @Operation(summary = "Registrar abertura de e-mail", description = "Retorna um pixel transparente e marca a abertura do requestId informado")
    @GetMapping("/pixel/{requestId}.png")
    public ResponseEntity<byte[]> trackOpen(@PathVariable String requestId) {
        emailLogService.markOpened(requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore().getHeaderValue());
        headers.setContentType(MediaType.IMAGE_PNG);

        return ResponseEntity.ok()
                .headers(headers)
                .body(TRANSPARENT_PIXEL);
    }
}
