package com.marketinghub.creative.service;

import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.service.VideoProductProofService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Entrega pixels das provas editoriais exclusivamente no escopo auditado do anúncio reservado. */
@Service
public class CreativeEditorialEvidenceService {
  private static final Logger log = LoggerFactory.getLogger(CreativeEditorialEvidenceService.class);
  private final CreativeRepository creatives;
  private final CreativeMediaGovernanceEvidenceService governance;
  private final VideoProductProofService proofs;
  private final VideoProjectRepository projects;

  /**
   * Conecta apenas fontes de leitura; o leitor de prova respeita a fronteira tardia de contexto.
   */
  public CreativeEditorialEvidenceService(
      CreativeRepository creatives,
      CreativeMediaGovernanceEvidenceService governance,
      @Lazy VideoProductProofService proofs,
      VideoProjectRepository projects) {
    this.creatives = creatives;
    this.governance = governance;
    this.proofs = proofs;
    this.projects = projects;
  }

  /** Revalida anúncio, experimento, receita e hash antes de disponibilizar a captura declarada. */
  @Transactional(readOnly = true)
  public byte[] readProof(Long creativeId, Long experimentId, Long taskId, Long evidenceId) {
    var creative =
        creatives
            .findByIdWithExperiment(creativeId)
            .filter(value -> value.getExperiment().getId().equals(experimentId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var evidence = governance.resolve(creative);
    if (evidence == null
        || !EditorialMediaGovernanceEvidenceResolver.CONTRACT.equals(evidence.contractVersion())
        || !"VERIFIED".equals(evidence.verificationStatus())
        || evidence.editorialMediaEvidence() == null)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Governança editorial não comprovada.");
    var details = evidence.editorialMediaEvidence();
    if (details.projectId() == null
        || !experimentId.equals(details.experimentId())
        || creative.getExperiment().getProduct() == null
        || !creative.getExperiment().getProduct().getId().equals(details.productId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Prova editorial de outro contexto.");
    var source =
        details.productProofSources().stream()
            .filter(
                value ->
                    sameId(value.get("taskId"), taskId)
                        && sameId(value.get("evidenceId"), evidenceId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var project =
        projects
            .findById(details.projectId())
            .filter(
                value ->
                    experimentId.equals(value.getExperimentId())
                        && details.productId().equals(value.getProductId())
                        && details.productVersion().equals(value.getCampaignKey())
                        && value.getTenantId().equals(source.get("tenantId")))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "Projeto de outra prova editorial."));
    try {
      byte[] bytes = proofs.readVerified(project, taskId, evidenceId);
      byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
      if (bytes.length > 12 * 1024 * 1024
          || bytes.length < signature.length
          || !java.util.Arrays.equals(signature, java.util.Arrays.copyOf(bytes, signature.length)))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Captura PNG inválida ou excede o limite.");
      String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      if (!sha.equals(source.get("sha256")))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Pixels divergem da prova auditada.");
      return bytes;
    } catch (RuntimeException | NoSuchAlgorithmException ex) {
      log.error(
          "Falha na leitura editorial. creativeId={} experimentId={} taskId={} evidenceId={}",
          creativeId,
          experimentId,
          taskId,
          evidenceId,
          ex);
      if (ex instanceof RuntimeException runtime) throw runtime;
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Compara identificadores numéricos íntegros mesmo após a desserialização do contrato. */
  private boolean sameId(Object value, Long expected) {
    return value instanceof Number number
        && expected != null
        && number.longValue() == expected
        && number.doubleValue() == expected.doubleValue();
  }
}
