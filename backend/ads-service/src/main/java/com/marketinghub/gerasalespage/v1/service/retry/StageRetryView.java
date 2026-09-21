package com.marketinghub.gerasalespage.v1.service.retry;

/** Responsabilidade: informar a retomada permitida pelo backend sem inferência da tela. */
public record StageRetryView(
    String idJob, String stageCode, String status, boolean available, String reason) {}
