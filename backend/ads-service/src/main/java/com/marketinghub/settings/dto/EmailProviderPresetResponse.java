package com.marketinghub.settings.dto;

import java.util.List;

public record EmailProviderPresetResponse(
        String id,
        String name,
        String headline,
        String summary,
        String docsUrl,
        String pricingUrl,
        String pricingSummary,
        String bestFor,
        String freeTier,
        String host,
        Integer port,
        List<Integer> alternativePorts,
        boolean authEnabled,
        boolean useStartTls,
        boolean useSsl,
        String usernameHint,
        List<String> highlights,
        String notes
) {
}
