package com.marketinghub.researchintelligence.v1.controller;

import com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceCardManagementService;
import com.marketinghub.researchintelligence.v1.service.managecard.RegisterResearchIntelligenceCardRequest;
import com.marketinghub.researchintelligence.v1.service.managecard.ResearchIntelligenceCardListResponse;
import com.marketinghub.researchintelligence.v1.service.managecard.ResearchIntelligenceCardTransitionRequest;
import com.marketinghub.researchintelligence.v1.service.managecard.ResearchIntelligenceCardVersionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recebe exclusivamente a gestão interna assinada dos cartões persistidos. */
@RestController
public class ResearchIntelligenceCardManagementController {
  private static final Logger log =
      LoggerFactory.getLogger(ResearchIntelligenceCardManagementController.class);

  private final ResearchIntelligenceCardManagementService service;

  /** Inicializa a superfície interna com o serviço editorial dedicado. */
  public ResearchIntelligenceCardManagementController(
      ResearchIntelligenceCardManagementService service) {
    this.service = service;
  }

  /** Registra uma versão em rascunho após autenticar o gateway e o payload integral. */
  @PostMapping("/api/internal/research-intelligence/v1/cards")
  public ResponseEntity<ResearchIntelligenceCardVersionResponse> registerCard(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") String actor,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody RegisterResearchIntelligenceCardRequest request) {
    service.verifyInternalRequest(httpRequest, actor, idempotencyKey, request);
    log.info(
        "Payload bruto recebido pela Biblioteca requestId={} actor={} operation=register payload={}",
        httpRequest.getHeader("X-Harness-Request-Id"),
        actor,
        request);
    ResearchIntelligenceCardVersionResponse response =
        service.registerCard(request, actor, idempotencyKey);
    return ResponseEntity.created(
            URI.create(
                "/api/internal/research-intelligence/v1/cards/"
                    + response.cardKey()
                    + "/versions/"
                    + response.version()))
        .body(response);
  }

  /** Lista versões cadastradas para operação e auditoria do módulo externo. */
  @GetMapping("/api/internal/research-intelligence/v1/cards")
  public ResearchIntelligenceCardListResponse listCards(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") String actor,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String collection,
      @RequestParam(defaultValue = "100") int limit) {
    service.verifyInternalRequest(httpRequest, actor, null, null);
    return service.listCards(status, collection, limit);
  }

  /** Recupera uma versão específica sem consultar ou baixar novamente sua fonte. */
  @GetMapping("/api/internal/research-intelligence/v1/cards/{cardKey}/versions/{version}")
  public ResearchIntelligenceCardVersionResponse getCard(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") String actor,
      @PathVariable String cardKey,
      @PathVariable int version) {
    service.verifyInternalRequest(httpRequest, actor, null, null);
    return service.getCard(cardKey, version);
  }

  /** Encaminha um rascunho completo para revisão editorial humana. */
  @PostMapping(
      "/api/internal/research-intelligence/v1/cards/{cardKey}/versions/{version}/submit-review")
  public ResearchIntelligenceCardVersionResponse submitForReview(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") String actor,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @PathVariable String cardKey,
      @PathVariable int version,
      @Valid @RequestBody ResearchIntelligenceCardTransitionRequest request) {
    service.verifyInternalRequest(httpRequest, actor, idempotencyKey, request);
    log.info(
        "Payload bruto recebido pela Biblioteca requestId={} actor={} operation=submit-review cardKey={} version={} payload={}",
        httpRequest.getHeader("X-Harness-Request-Id"),
        actor,
        cardKey,
        version,
        request);
    return service.submitCardForReview(cardKey, version, actor, request.reason());
  }

  /** Ativa uma versão revisada e substitui atomicamente sua versão anterior. */
  @PostMapping("/api/internal/research-intelligence/v1/cards/{cardKey}/versions/{version}/activate")
  public ResearchIntelligenceCardVersionResponse activate(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") String actor,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @PathVariable String cardKey,
      @PathVariable int version,
      @Valid @RequestBody ResearchIntelligenceCardTransitionRequest request) {
    service.verifyInternalRequest(httpRequest, actor, idempotencyKey, request);
    log.info(
        "Payload bruto recebido pela Biblioteca requestId={} actor={} operation=activate cardKey={} version={} payload={}",
        httpRequest.getHeader("X-Harness-Request-Id"),
        actor,
        cardKey,
        version,
        request);
    return service.activateCard(cardKey, version, actor, request.reason());
  }

  /** Arquiva uma versão ativa mantendo conteúdo e histórico consultáveis. */
  @PostMapping("/api/internal/research-intelligence/v1/cards/{cardKey}/versions/{version}/archive")
  public ResearchIntelligenceCardVersionResponse archive(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") String actor,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @PathVariable String cardKey,
      @PathVariable int version,
      @Valid @RequestBody ResearchIntelligenceCardTransitionRequest request) {
    service.verifyInternalRequest(httpRequest, actor, idempotencyKey, request);
    log.info(
        "Payload bruto recebido pela Biblioteca requestId={} actor={} operation=archive cardKey={} version={} payload={}",
        httpRequest.getHeader("X-Harness-Request-Id"),
        actor,
        cardKey,
        version,
        request);
    return service.archiveCard(cardKey, version, actor, request.reason());
  }
}
