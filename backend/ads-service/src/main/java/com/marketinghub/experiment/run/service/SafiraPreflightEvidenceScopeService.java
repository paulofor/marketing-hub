package com.marketinghub.experiment.run.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest.GateEvidence;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.safira.commercial.v1.service.SafiraCommercialContext;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: vincular o preflight Safira ao slot, aos pixels e ao contrato comercial atual.
 */
@Service
@RequiredArgsConstructor
public class SafiraPreflightEvidenceScopeService {
  private static final String LANDING_GATE = ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED;
  private final SafiraCommercialContext context;
  private final ExperimentRunGateResultRepository gateResults;

  /** Reconhece somente runs cujo produto possui o tipo cadastrado Safira. */
  public boolean applies(ExperimentRun run) {
    return run != null
        && run.getExperiment() != null
        && context.applies(run.getExperiment().getProduct());
  }

  /** Confere se o gate aprovado pertence ao slot, ao HTML e ao contrato ainda vigentes. */
  public boolean hasCurrentEvidence(ExperimentRun run) {
    if (!applies(run)) return true;
    Scope scope = currentScope(run);
    return gateResults.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId()).stream()
        .filter(gate -> LANDING_GATE.equals(gate.getGateCode()))
        .filter(gate -> gate.getStatus() == ExperimentRunGateStatus.PASS)
        .map(ExperimentRunGateResult::getEvidenceReference)
        .anyMatch(reference -> matches(scope, reference));
  }

  /** Recusa evidência de outra publicação e grava no run a versão material conferida. */
  public void validateAndBind(ExperimentRun run, GateEvidence evidence) {
    if (!applies(run)) return;
    Scope scope = currentScope(run);
    if (evidence == null || !matches(scope, evidence.evidenceReference()))
      throw new IllegalArgumentException(
          "A homologação Safira deve referenciar o slot, o SHA-256 da experiência e o contrato"
              + " comercial vigentes: "
              + scope.requiredReference());
    run.setAssetBundleVersion(Math.toIntExact(scope.slotId()));
  }

  /** Expõe a referência mínima que deve acompanhar a homologação funcional atual. */
  public String requiredReference(ExperimentRun run) {
    return applies(run) ? currentScope(run).requiredReference() : null;
  }

  /** Resolve uma fotografia sem relógio ou outro dado volátil. */
  private Scope currentScope(ExperimentRun run) {
    JsonNode snapshot = context.snapshot("experiment:" + run.getExperiment().getId());
    long slotId = snapshot.path("slotId").asLong(0L);
    String experienceHash = snapshot.path("experienceHash").asText();
    String fingerprint = snapshot.path("fingerprint").asText();
    SafiraCommercialContext.require(
        slotId > 0 && !experienceHash.isBlank() && !fingerprint.isBlank(),
        "A homologação Safira exige slot e experiência publicados com identidade verificável.");
    return new Scope(slotId, experienceHash, fingerprint);
  }

  /** Compara tokens completos e não aceita coincidência parcial entre versões. */
  private boolean matches(Scope scope, String reference) {
    if (reference == null || reference.isBlank()) return false;
    List<String> tokens =
        Arrays.stream(reference.split(";"))
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .toList();
    return tokens.contains("slot:" + scope.slotId())
        && tokens.contains("experience-sha256:" + scope.experienceHash())
        && tokens.contains("safira-fingerprint:" + scope.fingerprint());
  }

  /** Mantém juntas as identidades imutáveis exigidas pelo preflight Safira. */
  private record Scope(Long slotId, String experienceHash, String fingerprint) {
    /** Monta a referência auditável exibida ao operador e persistida no gate. */
    private String requiredReference() {
      return "slot:"
          + slotId
          + ";experience-sha256:"
          + experienceHash
          + ";safira-fingerprint:"
          + fingerprint;
    }
  }
}
