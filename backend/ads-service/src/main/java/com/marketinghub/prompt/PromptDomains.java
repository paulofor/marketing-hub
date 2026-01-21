package com.marketinghub.prompt;

import java.util.List;

public final class PromptDomains {
    public static final String NICHE_DETAILED_DESCRIPTION = "NICHE_DETAILED_DESCRIPTION";
    public static final String NICHE_HYPOTHESIS = "NICHE_HYPOTHESIS";
    public static final String LEAD_PORTAL_FLOW = "LEAD_PORTAL_FLOW";

    public static final List<String> ALL = List.of(
            NICHE_DETAILED_DESCRIPTION,
            NICHE_HYPOTHESIS,
            LEAD_PORTAL_FLOW);

    private PromptDomains() {
    }
}
