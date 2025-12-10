package com.marketinghub.emailservice.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "email-service")
public record EmailServiceProperties(
        @NotBlank @Email String defaultFromAddress,
        boolean dryRun
) {
}
