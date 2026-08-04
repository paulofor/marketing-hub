package com.marketinghub.mois.metaads.v1.controller;

import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Expõe o contrato administrativo e interno da investigação Meta v1. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MoisMetaAdInvestigationController {

  private final MoisMetaAdInvestigationService service;

  /** Cria uma investigação pela tela do Marketing Hub. */
  @PostMapping("/v1/mois/meta-ad-investigations")
  @ResponseStatus(HttpStatus.CREATED)
  public MoisMetaAdDtos.InvestigationResponse create(
      @Valid @RequestBody MoisMetaAdDtos.CreateInvestigationRequest request) {
    return service.create(request);
  }

  /** Lista investigações do workspace para acompanhamento comercial. */
  @GetMapping("/v1/mois/meta-ad-investigations")
  public MoisMetaAdDtos.InvestigationListResponse list(@RequestParam String workspaceId) {
    return service.list(workspaceId);
  }

  /** Exibe evidências, lacunas, gate e ficha ética de uma investigação. */
  @GetMapping("/v1/mois/meta-ad-investigations/{id}")
  public MoisMetaAdDtos.InvestigationResponse get(@PathVariable long id) {
    return service
        .get(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "investigação não encontrada"));
  }

  /** Reserva uma pendência para o coletor oficial da Meta. */
  @GetMapping("/internal/mois/meta-ad-library/v1/investigations/pending")
  public MoisMetaAdDtos.PendingInvestigationResponse pending() {
    return service
        .claimPending()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT));
  }

  /** Recebe e persiste observações brutas vindas do coletor. */
  @PostMapping("/internal/mois/meta-ad-library/v1/investigations/{id}/observations")
  public MoisMetaAdDtos.ObservationBatchResponse ingest(
      @PathVariable long id, @Valid @RequestBody MoisMetaAdDtos.ObservationBatchRequest request) {
    return service.ingest(id, request);
  }

  /** Registra a conclusão técnica da coleta e preserva o diagnóstico comercial. */
  @PostMapping("/internal/mois/meta-ad-library/v1/investigations/{id}/complete")
  public MoisMetaAdDtos.InvestigationResponse complete(
      @PathVariable long id, @RequestBody MoisMetaAdDtos.CompleteInvestigationRequest request) {
    return service.complete(id, request);
  }
}
