package com.marketinghub.experiment.run.service.homologation;

import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import java.util.List;

/** Responsabilidade: receber as evidencias funcionais que concluem a homologacao de um run. */
public record ExperimentRunHomologationRequest(List<GateEvidence> gates) {
  /** Responsabilidade: representar o resultado auditavel de um gate funcional homologado. */
  public record GateEvidence(
      String gateCode, ExperimentRunGateStatus status, String summary, String evidenceReference) {}
}
