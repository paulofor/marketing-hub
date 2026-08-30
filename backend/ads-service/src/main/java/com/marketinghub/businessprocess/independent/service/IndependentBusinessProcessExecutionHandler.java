package com.marketinghub.businessprocess.independent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputFieldResponse;
import java.util.List;

/** Responsabilidade: adaptar um processo independente ao contrato técnico que já o executa. */
public interface IndependentBusinessProcessExecutionHandler {

  /** Identifica sem ambiguidade o processo atendido pelo adaptador. */
  String processCode();

  /** Declara os campos aceitos para validação e construção dinâmica da tela. */
  List<IndependentBusinessProcessInputFieldResponse> inputFields();

  /** Cria a entidade técnica canônica e devolve sua referência BPM auditável. */
  IndependentBusinessProcessStartedExecution start(JsonNode input);
}
