package com.marketinghub.salesvideo.service.clipplan;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir atalhos de render, duplicação e vazamento entre produto/perfil/ciclo.
 */
class RunwayRenderPreflightTest {
  /** Exige ciclo antes de criar job e não considera TEST como autorização de gasto. */
  @Test
  void missingCycleCannotRender() {
    var cycles = mock(VideoProductionCycleRepository.class);
    var gate =
        new RunwayRenderPreflight(
            mock(SalesVideoProviderModelRepository.class),
            cycles,
            mock(VideoProviderFinancialPreflightService.class),
            new ObjectMapper(),
            mock(VideoProjectRepository.class));
    var request = new RequestVideoRenderRequest();
    request.setProviderName("RUNWAY");
    request.setMetadataJson("{}");
    assertThatThrownBy(() -> gate.validate(70L, 12L, request))
        .hasMessageContaining("ciclo governado");
  }

  /**
   * Aceita contexto governado, mas bloqueia produto, perfil diferente e duplicação no mesmo ciclo.
   */
  @Test
  void isolatesIdentitiesAndPreventsDuplicate() {
    var models = mock(SalesVideoProviderModelRepository.class);
    var cycles = mock(VideoProductionCycleRepository.class);
    var projects = mock(VideoProjectRepository.class);
    var financial = mock(VideoProviderFinancialPreflightService.class);
    var cycle = new VideoProductionCycle();
    cycle.setId(23L);
    cycle.setVideoProjectId(34L);
    cycle.setProductId(70L);
    var project = new VideoProject();
    project.setId(34L);
    project.setSalesVideoProfileId(12L);
    when(cycles.findById(23L)).thenReturn(Optional.of(cycle));
    when(projects.findById(34L)).thenReturn(Optional.of(project));
    var model = StudioClipPlannerTest.active();
    model.setPricingObservedAt(Instant.now());
    when(models.findByProviderName("RUNWAY")).thenReturn(Optional.of(model));
    var reservation = new VideoCreditReservation();
    reservation.setId(91L);
    reservation.setFullProductionReservedUsd(new BigDecimal("8"));
    when(financial.requireActiveReservation(23L)).thenReturn(reservation);
    var gate = new RunwayRenderPreflight(models, cycles, financial, new ObjectMapper(), projects);
    var request = new RequestVideoRenderRequest();
    request.setProviderName("RUNWAY");
    request.setMetadataJson(
        "{\"videoProductionCycleId\":23,\"videoProjectId\":34,\"productId\":70,\"providerCreditReservationId\":91,\"financialApprovedBy\":\"Plutus\"}");
    assertThatCode(() -> gate.validate(70L, 12L, request)).doesNotThrowAnyException();
    assertThatThrownBy(() -> gate.validate(71L, 12L, request))
        .hasMessageContaining("outro produto");
    assertThatThrownBy(() -> gate.validate(70L, 13L, request)).hasMessageContaining("Perfil");
    model.setLifecycleStatus("HOMOLOGATION");
    assertThatThrownBy(() -> gate.validate(70L, 12L, request)).hasMessageContaining("homologação");
    model.setLifecycleStatus("ACTIVE");
    cycle.setSalesVideoJobId(55L);
    assertThatThrownBy(() -> gate.validate(70L, 12L, request)).hasMessageContaining("conciliar");
  }
}
