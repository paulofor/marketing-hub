package com.marketinghub.settings.email;

import com.marketinghub.settings.dto.EmailProviderPresetResponse;
import com.marketinghub.settings.dto.EmailSmtpSettingsResponse;
import com.marketinghub.settings.dto.TestEmailRequest;
import com.marketinghub.settings.dto.TestEmailResponse;
import com.marketinghub.settings.dto.UpdateEmailSmtpSettingsRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/email-service")
public class EmailSettingsController {

    private final EmailSmtpSettingsService smtpSettingsService;
    private final EmailSmtpTestService smtpTestService;
    private final EmailProviderPresetService providerPresetService;

    public EmailSettingsController(EmailSmtpSettingsService smtpSettingsService,
                                   EmailSmtpTestService smtpTestService,
                                   EmailProviderPresetService providerPresetService) {
        this.smtpSettingsService = smtpSettingsService;
        this.smtpTestService = smtpTestService;
        this.providerPresetService = providerPresetService;
    }

    @GetMapping("/smtp")
    public EmailSmtpSettingsResponse getSmtpSettings() {
        return smtpSettingsService.getSettings();
    }

    @PutMapping("/smtp")
    public EmailSmtpSettingsResponse updateSmtpSettings(@Valid @RequestBody UpdateEmailSmtpSettingsRequest request) {
        return smtpSettingsService.update(request);
    }

    @PostMapping("/smtp/test")
    public TestEmailResponse sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        return smtpTestService.sendTestEmail(request);
    }

    @GetMapping("/providers")
    public List<EmailProviderPresetResponse> listProviderPresets() {
        return providerPresetService.listPresets();
    }
}
