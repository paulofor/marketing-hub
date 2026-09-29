package com.marketinghub.experiment.run.service.homologation;

/** Responsabilidade: sinalizar evidência funcional incompatível com o escopo atual do run. */
public class ExperimentRunHomologationEvidenceException extends RuntimeException {

  /** Cria a falha com a orientação auditável que deve ser devolvida ao operador. */
  public ExperimentRunHomologationEvidenceException(String message) {
    super(message);
  }
}
