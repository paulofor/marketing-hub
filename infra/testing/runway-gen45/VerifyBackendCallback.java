package com.marketinghub.salesvideo.service.providerpreflight;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialagent.service.StudioProviderTaskConsumptionQueryService;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightData;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar o callback produzido pela imagem usando o service real do backend. */
class VerifyBackendCallback {
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

  /** Consome o retorno exato do executor e comprova persistência, gates e rejeição do formato legado. */
  public static void main(String[] args) throws Exception {
    ObjectNode payload = (ObjectNode) JSON.readTree(Path.of(args[0]).toFile()).path("callbacks").get(0);
    verify(payload, "READY", null);
    ObjectNode legacy = payload.deepCopy();
    legacy.put("quotaSnapshotJson", "[]");
    verify(legacy, null, "snapshot de quotas deve ser um objeto JSON");
    ObjectNode blocked = payload.deepCopy();
    blocked.put("failureCode", "PROVIDER_QUOTA_UNKNOWN");
    blocked.put("failureDetail", "Quota não informada na simulação local");
    verify(blocked, "READY_WITH_BLOCKER", null);
    System.out.println("PASS backend real: READY persistido; quota desconhecida bloqueia; array legado recusado; zero reservas");
  }

  /** Executa as validações reais com repositórios locais novos em cada cenário. */
  private static void verify(ObjectNode payload, String expectedStatus, String expectedError) throws Exception {
    var result = JSON.treeToValue(payload, VideoProviderFinancialPreflightData.Result.class);
    var accounts = mock(VideoProviderAccountRepository.class);
    var preflights = mock(VideoProviderPreflightRepository.class);
    var reservations = mock(VideoCreditReservationRepository.class);
    var models = mock(SalesVideoProviderModelRepository.class);
    var consumption = mock(StudioProviderTaskConsumptionQueryService.class);
    var account = new VideoProviderAccount();
    account.setId(91001L);
    account.setAccountKey("RUNWAY_PRIMARY");
    account.setAggregatorName("Runway");
    account.setSourceUrl(result.sourceUrl());
    account.setCreditUnitUsd(new BigDecimal("0.01"));
    account.setReservedCredits(BigDecimal.ZERO);
    var preflight = new VideoProviderPreflight();
    preflight.setId(91007L);
    preflight.setVideoProductionCycleId(91014L);
    preflight.setProviderAccountId(91001L);
    preflight.setProductionProfile("FINAL_CAMPAIGN");
    preflight.setStatus("PENDING");
    var model = new SalesVideoProviderModel();
    model.setAdapterKey("RUNWAY");
    model.setExternalModelId("gen4.5");
    model.setLifecycleStatus("ACTIVE");
    model.setAdapterVerified(true);
    model.setPricingVerified(true);
    model.setCommercialLicenseVerified(true);
    model.setQualityGateVerified(true);
    when(accounts.findByAccountKeyForUpdate("RUNWAY_PRIMARY")).thenReturn(Optional.of(account));
    when(preflights.findByVideoProductionCycleIdForUpdate(91014L)).thenReturn(Optional.of(preflight));
    when(preflights.save(any(VideoProviderPreflight.class))).thenAnswer(call -> call.getArgument(0));
    when(models.findByAdapterKeyAndExternalModelId("RUNWAY", "gen4.5")).thenReturn(Optional.of(model));
    var service = new VideoProviderFinancialPreflightService(accounts, preflights, reservations,
        models, consumption, JSON, Clock.fixed(result.observedAt(), ZoneOffset.UTC));
    var cycle = new VideoProductionCycle();
    cycle.setId(91014L);
    cycle.setBudgetLimitUsd(new BigDecimal("8.00"));
    try {
      VideoProviderPreflight saved = service.complete(cycle, result);
      if (expectedError != null) throw new AssertionError("O backend aceitou o formato legado inválido");
      if (!expectedStatus.equals(saved.getStatus())) throw new AssertionError(saved.getStatus());
      if (saved.getEstimatedCostUsd().compareTo(new BigDecimal("1.8")) != 0) throw new AssertionError("Custo divergente");
      if (!JSON.readTree(saved.getQuotaSnapshotJson()).isObject()) throw new AssertionError("Quota fora do contrato");
      if (account.getReservedCredits().signum() != 0) throw new AssertionError("Preflight criou reserva");
      org.mockito.Mockito.verify(preflights).save(preflight);
      org.mockito.Mockito.verify(accounts).save(account);
    } catch (ResponseStatusException ex) {
      if (expectedError == null || !ex.getMessage().contains(expectedError)) throw ex;
      org.mockito.Mockito.verify(preflights, never()).save(any());
    }
    org.mockito.Mockito.verify(reservations, never()).save(any());
  }
}
