package com.marketinghub.planning.imagestudio.v1.service;

import java.util.List;

/** Responsabilidade: transportar o playbook visual governado e seus exemplos positivos. */
public record TemisVisualPlaybookDto(
    String version,
    String contextKey,
    String status,
    List<String> promotedRules,
    List<String> avoid,
    List<TemisVisualPlaybookExampleDto> approvedExamples) {}
