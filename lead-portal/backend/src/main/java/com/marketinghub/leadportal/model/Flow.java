package com.marketinghub.leadportal.model;

import java.util.List;

/**
 * Flow shown to visitors on the public lead portal.
 */
public record Flow(
        String slug,
        String name,
        String description,
        String model,
        String prompt,
        List<FlowQuestion> questions,
        SimpleFormStyle simpleFormStyle) {
}
