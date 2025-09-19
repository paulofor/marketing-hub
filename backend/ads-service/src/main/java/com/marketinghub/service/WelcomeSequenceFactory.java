package com.marketinghub.service;

import com.marketinghub.model.SequenceTemplate;
import com.marketinghub.model.SequenceStep;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory responsible for building the default welcome sequence sent to new leads.
 */
@Component
public class WelcomeSequenceFactory {

    /**
     * Creates the default welcome template used when a lead is captured.
     */
    public SequenceTemplate createWelcomeTemplate() {
        SequenceTemplate template = SequenceTemplate.builder()
                .name("Welcome")
                .steps(List.of(
                        SequenceStep.builder().stepOrder(1).content("Welcome!").delaySeconds(0).build(),
                        SequenceStep.builder().stepOrder(2).content("How can we help?").delaySeconds(5).build()
                ))
                .build();
        template.getSteps().forEach(step -> step.setSequenceTemplate(template));
        return template;
    }
}

