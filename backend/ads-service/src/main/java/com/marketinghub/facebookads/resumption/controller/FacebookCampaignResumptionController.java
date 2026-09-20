package com.marketinghub.facebookads.resumption.controller;

import com.marketinghub.facebookads.resumption.service.FacebookCampaignResumptionService;
import com.marketinghub.facebookads.resumption.service.request.ResumeCampaignRequest;
import com.marketinghub.facebookads.resumption.service.result.ResumeCampaignResult;
import com.marketinghub.facebookads.resumption.service.summary.ResumeCampaignSummary;
import com.marketinghub.facebookads.resumption.service.view.ResumeCampaignView;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Expõe os comandos administrativos e a fila canônica de retomada ao executor Facebook. */
@RestController
@RequestMapping("/api/facebook-campaign-resumptions")
public class FacebookCampaignResumptionController {
  private final FacebookCampaignResumptionService service;

  /** Recebe o serviço canônico que governa a retomada. */
  public FacebookCampaignResumptionController(FacebookCampaignResumptionService service) {
    this.service = service;
  }

  /** Consulta disponibilidade e progresso persistidos para a tela. */
  @GetMapping("/experiments/{id}")
  public ResumeCampaignSummary summary(@PathVariable Long id) {
    return service.summary(id);
  }

  /** Registra autorização explícita sem ativar mídia durante a requisição da tela. */
  @PostMapping("/experiments/{id}")
  public ResumeCampaignView request(
      @PathVariable Long id, @RequestBody ResumeCampaignRequest input) {
    return service.request(id, input);
  }

  /** Entrega o ponto inicial de consumo de trabalho pelo executor. */
  @GetMapping("/pending")
  public List<ResumeCampaignView> pending() {
    return service.pending();
  }

  /** Reserva uma única execução antes de qualquer chamada de escrita na Meta. */
  @PostMapping("/{id}/claim")
  public ResumeCampaignView claim(@PathVariable Long id) {
    return service.claim(id);
  }

  /** Recebe evidência de sucesso ou falha correlacionada à reserva. */
  @PostMapping("/{id}/result")
  public ResumeCampaignView result(@PathVariable Long id, @RequestBody ResumeCampaignResult input) {
    return service.result(id, input);
  }
}
