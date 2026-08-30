package com.marketinghub.businessprocess.execution.service.humanactivity;

/** Responsabilidade: representar uma condição objetiva de uma decisão humana no processo. */
public record HumanProductProcessActivityRequirement(
    String code, String title, boolean satisfied, String detail, String recommendation) {}
