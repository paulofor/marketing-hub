package com.marketinghub.leadportal.service.executionprofile;

/** Responsabilidade: transportar a origem persistida do pacote para a reserva financeira. */
public record ProfilePackageSource(
    Long productId, String reference, int outputs, boolean withBase, String input) {}
