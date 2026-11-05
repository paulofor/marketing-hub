package com.marketinghub.salesvideo.service.clipplan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightService;
import com.marketinghub.videocontract.v1.VideoCapabilities;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: impedir que render administrativo contorne catálogo, ciclo e reserva governada.
 */
@Component
public class RunwayRenderPreflight {
  private static final Logger log = LoggerFactory.getLogger(RunwayRenderPreflight.class);
  private final SalesVideoProviderModelRepository models;
  private final VideoProductionCycleRepository cycles;
  private final VideoProviderFinancialPreflightService financial;
  private final ObjectMapper mapper;
  private final com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository projects;

  /** Conecta cadastro homologado, ciclo real e reserva antes de criar qualquer job Runway. */
  public RunwayRenderPreflight(
      SalesVideoProviderModelRepository models,
      VideoProductionCycleRepository cycles,
      VideoProviderFinancialPreflightService financial,
      ObjectMapper mapper,
      com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository projects) {
    this.models = models;
    this.cycles = cycles;
    this.financial = financial;
    this.mapper = mapper;
    this.projects = projects;
  }

  /**
   * Exige identidade, decisão persistida de Plutus e reserva inclusive quando o render se chama
   * TEST.
   */
  public void validate(Long productId, Long profileId, RequestVideoRenderRequest request) {
    String name = request.getProviderName();
    if (name == null || !name.toUpperCase(java.util.Locale.ROOT).startsWith("RUNWAY")) return;
    try {
      var metadata =
          mapper.readTree(request.getMetadataJson() == null ? "{}" : request.getMetadataJson());
      long cycleId = metadata.path("videoProductionCycleId").asLong(0);
      var cycle =
          cycles
              .findById(cycleId)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Use o ciclo governado do Estúdio: preflight e reserva integral são obrigatórios antes de produzir"));
      if (!Objects.equals(cycle.getProductId(), productId)
          || !Objects.equals(metadata.path("videoProjectId").asLong(), cycle.getVideoProjectId())
          || metadata.path("productId").asLong() != productId
          || !"Plutus".equals(metadata.path("financialApprovedBy").asText()))
        throw new IllegalArgumentException(
            "Render pertence a outro produto, projeto ou decisão financeira");
      var project =
          projects
              .findById(cycle.getVideoProjectId())
              .orElseThrow(() -> new IllegalArgumentException("Projeto do ciclo não encontrado"));
      if (!Objects.equals(project.getSalesVideoProfileId(), profileId))
        throw new IllegalArgumentException("Perfil de vídeo não pertence ao projeto do ciclo");
      if (cycle.getSalesVideoJobId() != null)
        throw new IllegalArgumentException(
            "O ciclo já possui um job; conciliar resultado e reutilizar arquivo/callback antes de repetir");
      if (!"APPROVED".equals(cycle.getFinancialDecision()))
        throw new IllegalArgumentException(
            "Este ciclo não possui parecer financeiro aprovado de Plutus. Consulte a revisão e sua pendência antes de gerar.");
      financial.assertFullCostCoverageBeforeRender(cycle);
      var reservation = financial.requireActiveReservation(cycleId);
      if (reservation.getFullProductionReservedUsd() == null
          || reservation.getFullProductionReservedUsd().signum() < 0
          || reservation.getId() != metadata.path("providerCreditReservationId").asLong())
        throw new IllegalArgumentException(
            "Reserva integral ausente ou divergente; não gerar outra tomada");
      if (!"RUNWAY_ROUTER".equals(name)) {
        var model =
            models
                .findByProviderName(name)
                .orElseThrow(() -> new IllegalArgumentException("Modelo não cadastrado"));
        if (!"ACTIVE".equals(model.getLifecycleStatus())
            || !model.isAdapterVerified()
            || !model.isPricingVerified()
            || !model.isQualityGateVerified()
            || !model.isCommercialLicenseVerified()
            || model.getPricingObservedAt() == null
            || model.getPricingObservedAt().isBefore(Instant.now().minus(30, ChronoUnit.DAYS)))
          throw new IllegalArgumentException(
              "Modelo sem homologação ou preço vigente; revisar catálogo sem geração");
        VideoCapabilities.provider(name)
            .ifPresent(
                capability -> {
                  if (capability.adapterModes().isEmpty())
                    throw new IllegalArgumentException("Adapter não implementa esse modelo");
                });
      }
    } catch (JsonProcessingException ex) {
      log.error("Contrato de render inválido; profileId={} provider={}", profileId, name, ex);
      throw new IllegalArgumentException("Metadados do preflight inválidos", ex);
    }
  }
}
