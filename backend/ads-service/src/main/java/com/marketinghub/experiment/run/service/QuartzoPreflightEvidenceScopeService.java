package com.marketinghub.experiment.run.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest.GateEvidence;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: vincular a homologação técnica Quartzo à publicação comercial imutável atual.
 */
@Service
public class QuartzoPreflightEvidenceScopeService {
  private static final String LANDING_GATE = ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED;
  private final QuartzoCommercialContext context;
  private final ExperimentRunGateResultRepository gateResults;

  /** Configura o contexto comercial e as evidências persistidas de cada tentativa. */
  public QuartzoPreflightEvidenceScopeService(
      QuartzoCommercialContext context, ExperimentRunGateResultRepository gateResults) {
    this.context = context;
    this.gateResults = gateResults;
  }

  /** Confirma se o run pertence ao percurso Quartzo que exige publicação auditada. */
  public boolean applies(ExperimentRun run) {
    return run != null
        && run.getExperiment() != null
        && context.applies(run.getExperiment().getProduct());
  }

  /**
   * Confere se o gate aprovado aponta para a publicação, o HTML e o contrato comercial vigentes.
   */
  public boolean hasCurrentEvidence(ExperimentRun run) {
    if (!applies(run)) return true;
    Scope scope = currentScope(run);
    return gateResults.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId()).stream()
        .filter(gate -> LANDING_GATE.equals(gate.getGateCode()))
        .filter(gate -> gate.getStatus() == ExperimentRunGateStatus.PASS)
        .map(ExperimentRunGateResult::getEvidenceReference)
        .anyMatch(reference -> matches(scope, reference));
  }

  /** Valida a referência exata antes de aceitar a homologação e registra a versão da publicação. */
  public void validateAndBind(ExperimentRun run, GateEvidence evidence) {
    if (!applies(run)) return;
    Scope scope = currentScope(run);
    if (evidence == null || !matches(scope, evidence.evidenceReference())) {
      throw new IllegalArgumentException(
          "A homologação Quartzo deve referenciar a publicação auditada atual, seu hash e o"
              + " contrato comercial vigente: "
              + scope.requiredReference());
    }
    run.setAssetBundleVersion(Math.toIntExact(scope.publicationId()));
  }

  /** Explica qual identidade deve acompanhar capturas e testes do run atual. */
  public String requiredReference(ExperimentRun run) {
    return applies(run) ? currentScope(run).requiredReference() : null;
  }

  /** Resolve a fotografia comercial atual sem usar data de consulta ou texto declarado. */
  private Scope currentScope(ExperimentRun run) {
    JsonNode snapshot = context.snapshot("experiment:" + run.getExperiment().getId());
    long publicationId = snapshot.path("publicationId").asLong(0L);
    String pageHash = snapshot.path("pageHash").asText();
    String fingerprint = snapshot.path("fingerprint").asText();
    QuartzoCommercialContext.require(
        publicationId > 0 && !pageHash.isBlank() && !fingerprint.isBlank(),
        "A homologação Quartzo exige uma publicação auditada identificável.");
    return new Scope(publicationId, pageHash, fingerprint);
  }

  /** Compara tokens completos para impedir coincidência parcial entre versões. */
  private boolean matches(Scope scope, String reference) {
    if (reference == null || reference.isBlank()) return false;
    List<String> tokens =
        Arrays.stream(reference.split(";"))
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .toList();
    return tokens.contains("publication:" + scope.publicationId())
        && tokens.contains("page-sha256:" + scope.pageHash())
        && tokens.contains("quartzo-fingerprint:" + scope.fingerprint());
  }

  /** Mantém juntas as três identidades imutáveis exigidas pelo preflight Quartzo. */
  private record Scope(Long publicationId, String pageHash, String fingerprint) {
    /** Monta a referência mínima exibida ao operador e persistida no gate. */
    private String requiredReference() {
      return "publication:"
          + publicationId
          + ";page-sha256:"
          + pageHash
          + ";quartzo-fingerprint:"
          + fingerprint;
    }
  }
}
