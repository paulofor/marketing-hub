package com.marketinghub.gerasalespage.v1.service;

/** Resposta do comando que inicia o GeraSalesPage v1 para um experimento. */
public record GeraSalesPageStartResponse(Long experimentId, String stageCode, String jobid, String status) {}
