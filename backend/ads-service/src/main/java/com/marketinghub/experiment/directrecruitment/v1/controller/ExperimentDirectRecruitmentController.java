package com.marketinghub.experiment.directrecruitment.v1.controller;

import com.marketinghub.experiment.directrecruitment.v1.service.ExperimentDirectRecruitmentService;
import com.marketinghub.experiment.directrecruitment.v1.service.activate.ActivateDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.campaign.DirectRecruitmentCampaignResponse;
import com.marketinghub.experiment.directrecruitment.v1.service.createdraft.CreateDirectRecruitmentDraftRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.pause.PauseDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.publicview.PublicDirectRecruitmentResponse;
import com.marketinghub.experiment.directrecruitment.v1.service.submit.SubmitDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.submit.SubmitDirectRecruitmentResponse;
import com.marketinghub.experiment.directrecruitment.v1.service.visit.RegisterDirectRecruitmentVisitRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.visit.RegisterDirectRecruitmentVisitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a atividade administrativa e pública de recrutamento direto. */
@RestController
@RequestMapping("/api")
@Tag(
    name = "Experimentos — recrutamento direto",
    description = "Forma uma amostra inbound consentida sem persistir identidade em claro.")
public class ExperimentDirectRecruitmentController {
  private final ExperimentDirectRecruitmentService service;

  /** Configura o serviço canônico do recrutamento direto. */
  public ExperimentDirectRecruitmentController(ExperimentDirectRecruitmentService service) {
    this.service = service;
  }

  /** Consulta conteúdo, métricas e próximo gate da atividade. */
  @GetMapping("/experiments/{experimentId}/direct-recruitment")
  @Operation(summary = "Consulta a atividade de recrutamento direto")
  public DirectRecruitmentCampaignResponse getCampaign(@PathVariable Long experimentId) {
    return service.getCampaign(experimentId);
  }

  /** Prepara o rascunho versionado do convite sem distribuí-lo. */
  @PostMapping("/experiments/{experimentId}/direct-recruitment/draft")
  @Operation(summary = "Cria o rascunho do convite consentido")
  public DirectRecruitmentCampaignResponse createDraft(
      @PathVariable Long experimentId,
      @Valid @RequestBody CreateDirectRecruitmentDraftRequest request) {
    return service.createDraft(experimentId, request);
  }

  /** Ativa o convite após aprovação humana explícita. */
  @PostMapping("/experiments/{experimentId}/direct-recruitment/activate")
  @Operation(summary = "Ativa o convite sem executar distribuição externa")
  public DirectRecruitmentCampaignResponse activate(
      @PathVariable Long experimentId,
      @Valid @RequestBody ActivateDirectRecruitmentRequest request) {
    return service.activate(experimentId, request);
  }

  /** Pausa novas adesões preservando o histórico já coletado. */
  @PostMapping("/experiments/{experimentId}/direct-recruitment/pause")
  @Operation(summary = "Pausa o convite de recrutamento")
  public DirectRecruitmentCampaignResponse pause(
      @PathVariable Long experimentId, @Valid @RequestBody PauseDirectRecruitmentRequest request) {
    return service.pause(experimentId, request);
  }

  /** Entrega o conteúdo público e a disponibilidade vigente do convite. */
  @GetMapping("/public/direct-recruitments/{token}")
  @Operation(summary = "Consulta um convite público de participação")
  public PublicDirectRecruitmentResponse getPublicCampaign(@PathVariable String token) {
    return service.getPublicCampaign(token);
  }

  /** Registra uma visita pública única e pseudonimizada. */
  @PostMapping("/public/direct-recruitments/{token}/visits")
  @Operation(summary = "Registra visita única ao convite")
  public RegisterDirectRecruitmentVisitResponse registerVisit(
      @PathVariable String token,
      @Valid @RequestBody RegisterDirectRecruitmentVisitRequest request) {
    return service.registerVisit(token, request);
  }

  /** Recebe, qualifica e registra uma adesão consentida. */
  @PostMapping("/public/direct-recruitments/{token}/submissions")
  @Operation(summary = "Registra adesão consentida e qualificada")
  public SubmitDirectRecruitmentResponse submit(
      @PathVariable String token, @Valid @RequestBody SubmitDirectRecruitmentRequest request) {
    return service.submit(token, request);
  }
}
