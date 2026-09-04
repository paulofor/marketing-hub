package com.marketinghub.harnesslibraryapi.controller;

import com.marketinghub.harnesslibraryapi.api.CardListResponse;
import com.marketinghub.harnesslibraryapi.api.CardTransitionRequest;
import com.marketinghub.harnesslibraryapi.api.CardVersionResponse;
import com.marketinghub.harnesslibraryapi.api.RegisterCardRequest;
import com.marketinghub.harnesslibraryapi.config.ApiKeyAuthenticationFilter;
import com.marketinghub.harnesslibraryapi.service.HarnessLibraryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Oferece uma única superfície JSON para cadastro e curadoria externa dos cartões. */
@RestController
@Validated
public class HarnessLibraryController {
  private static final String CARD_KEY_PATTERN = "^[a-z0-9][a-z0-9-]{2,119}$";
  private static final String ACTOR_PATTERN = "^[A-Za-z0-9._@-]{3,120}$";
  private static final String IDEMPOTENCY_PATTERN = "^[A-Za-z0-9._:-]{8,128}$";

  private final HarnessLibraryService service;

  /** Recebe a orquestração sem conhecer cliente HTTP ou persistência. */
  public HarnessLibraryController(HarnessLibraryService service) {
    this.service = service;
  }

  /** Cadastra uma versão em rascunho a partir do JSON recebido por curl. */
  @PostMapping("/v1/cards")
  public ResponseEntity<CardVersionResponse> register(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") @Pattern(regexp = ACTOR_PATTERN) String actor,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = IDEMPOTENCY_PATTERN)
          String idempotencyKey,
      @Valid @RequestBody RegisterCardRequest request) {
    String requestId = requestId(httpRequest);
    CardVersionResponse response = service.register(request, actor, idempotencyKey, requestId);
    return ResponseEntity.created(
            URI.create("/v1/cards/" + response.cardKey() + "/versions/" + response.version()))
        .body(response);
  }

  /** Lista versões com filtros executados pela fonte de verdade. */
  @GetMapping("/v1/cards")
  public CardListResponse list(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") @Pattern(regexp = ACTOR_PATTERN) String actor,
      @RequestParam(required = false) @Pattern(regexp = "^(DRAFT|IN_REVIEW|ACTIVE|ARCHIVED)$")
          String status,
      @RequestParam(required = false) @Size(max = 80) String collection,
      @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit) {
    return service.list(status, collection, limit, actor, requestId(httpRequest));
  }

  /** Consulta conteúdo e auditoria de uma versão específica. */
  @GetMapping("/v1/cards/{cardKey}/versions/{version}")
  public CardVersionResponse get(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") @Pattern(regexp = ACTOR_PATTERN) String actor,
      @PathVariable @Pattern(regexp = CARD_KEY_PATTERN) String cardKey,
      @PathVariable @Min(1) int version) {
    return service.get(cardKey, version, actor, requestId(httpRequest));
  }

  /** Submete o rascunho à revisão antes que qualquer agente possa consumi-lo. */
  @PostMapping("/v1/cards/{cardKey}/versions/{version}/submit-review")
  public CardVersionResponse submitForReview(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") @Pattern(regexp = ACTOR_PATTERN) String actor,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = IDEMPOTENCY_PATTERN)
          String idempotencyKey,
      @PathVariable @Pattern(regexp = CARD_KEY_PATTERN) String cardKey,
      @PathVariable @Min(1) int version,
      @Valid @RequestBody CardTransitionRequest request) {
    String requestId = requestId(httpRequest);
    return service.submitForReview(cardKey, version, request, actor, idempotencyKey, requestId);
  }

  /** Ativa a versão revisada e deixa sua seleção disponível globalmente. */
  @PostMapping("/v1/cards/{cardKey}/versions/{version}/activate")
  public CardVersionResponse activate(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") @Pattern(regexp = ACTOR_PATTERN) String actor,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = IDEMPOTENCY_PATTERN)
          String idempotencyKey,
      @PathVariable @Pattern(regexp = CARD_KEY_PATTERN) String cardKey,
      @PathVariable @Min(1) int version,
      @Valid @RequestBody CardTransitionRequest request) {
    String requestId = requestId(httpRequest);
    return service.activate(cardKey, version, request, actor, idempotencyKey, requestId);
  }

  /** Arquiva uma versão ativa sem removê-la da auditoria. */
  @PostMapping("/v1/cards/{cardKey}/versions/{version}/archive")
  public CardVersionResponse archive(
      HttpServletRequest httpRequest,
      @RequestHeader("X-Actor") @Pattern(regexp = ACTOR_PATTERN) String actor,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = IDEMPOTENCY_PATTERN)
          String idempotencyKey,
      @PathVariable @Pattern(regexp = CARD_KEY_PATTERN) String cardKey,
      @PathVariable @Min(1) int version,
      @Valid @RequestBody CardTransitionRequest request) {
    String requestId = requestId(httpRequest);
    return service.archive(cardKey, version, request, actor, idempotencyKey, requestId);
  }

  /** Recupera a correlação criada pelo filtro antes da autenticação. */
  private String requestId(HttpServletRequest request) {
    return String.valueOf(request.getAttribute(ApiKeyAuthenticationFilter.REQUEST_ID_ATTRIBUTE));
  }
}
