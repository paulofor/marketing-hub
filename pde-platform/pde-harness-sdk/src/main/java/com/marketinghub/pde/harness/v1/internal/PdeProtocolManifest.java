package com.marketinghub.pde.harness.v1.internal;

/** Descreve a versão e a integridade do contrato oficial fixado no SDK. */
public record PdeProtocolManifest(
    String codexVersion,
    String protocolVersion,
    String schemaResource,
    String schemaSha256,
    String generatedAt,
    String source,
    String transport) {}
