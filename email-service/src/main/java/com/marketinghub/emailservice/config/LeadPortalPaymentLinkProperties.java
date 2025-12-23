package com.marketinghub.emailservice.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@ConfigurationProperties(prefix = "lead-portal.payment-link")
public class LeadPortalPaymentLinkProperties {

    private boolean validateHost = true;
    private List<String> allowedHosts = new ArrayList<>(Arrays.asList(
            "www.mercadopago.com.br",
            "mercadopago.com.br",
            "www.mercadopago.com"));
    private String buttonColor = "#00a650";
    private String buttonText = "Quero liberar as imagens originais";
    private String plainTextIntro = "Finalize o pagamento e libere as imagens originais:";

    public boolean isValidateHost() {
        return validateHost;
    }

    public void setValidateHost(boolean validateHost) {
        this.validateHost = validateHost;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        if (!CollectionUtils.isEmpty(allowedHosts)) {
            this.allowedHosts = new ArrayList<>(allowedHosts);
        }
    }

    public String getButtonColor() {
        return buttonColor;
    }

    public void setButtonColor(String buttonColor) {
        this.buttonColor = buttonColor;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getPlainTextIntro() {
        return plainTextIntro;
    }

    public void setPlainTextIntro(String plainTextIntro) {
        this.plainTextIntro = plainTextIntro;
    }
}
