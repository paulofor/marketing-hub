package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.leadportal.service.executionprofile.ProfilePackageSource;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.product.executionprofile.v1.service.*;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract;
import com.marketinghub.repository.jdbc.leadportal.ExecutionProfileImagePackageRepository;
import com.marketinghub.repository.jpa.productexecution.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: comprovar reserva do pior caso e coerência com os retries reais do executor.
 */
class LeadPortalExecutionProfileBudgetTest {
  /** Impede alteração dos retries do executor sem rever o contrato financeiro do pacote. */
  @Test
  void workerRetryContractMatchesReservation() throws Exception {
    Path worker =
        Path.of(
            "../../ai-worker/src/main/java/com/marketinghub/worker/leadportal/image/LeadPortalImageProcessingService.java");
    assertThat(Files.readString(worker))
        .contains(
            "MAX_IMAGE_ATTEMPTS = "
                + LeadPortalExecutionProfileBudget.MAX_BASE_IMAGE_ATTEMPTS
                + ";");
  }

  /** Reserva todas as chamadas possíveis e rejeita entrega com quantidade inferior à prometida. */
  @Test
  void reservesFullPackageWithRetries() throws Exception {
    var packages = mock(ExecutionProfileImagePackageRepository.class);
    var bindings = mock(ExecutionProfileBindingRepository.class);
    var consumption = mock(ExecutionProfileConsumptionRepository.class);
    var context = mock(ExecutionProfileContext.class);
    var budget = mock(ExecutionProfileBudget.class);
    var profile = new ExecutionProfile();
    profile.setId(1L);
    var binding = new ExecutionProfileBinding();
    binding.setId(2L);
    var contract = mock(ProfileContract.class);
    when(contract.capability()).thenReturn(ProfileContract.Capability.PERSONALIZED_IMAGES);
    when(contract.includedUnits()).thenReturn(2);
    when(context.contract(profile)).thenReturn(contract);
    when(context.bound(7L, "experiment:9")).thenReturn(Optional.of(profile));
    when(bindings.existsByProductId(7L)).thenReturn(true);
    when(bindings.findByProductIdAndSourceReference(7L, "experiment:9"))
        .thenReturn(Optional.of(binding));
    when(packages.findSources(88L))
        .thenReturn(
            List.of(new ProfilePackageSource(7L, "experiment:9", 2, true, "Teste privado")));
    var adapter =
        new LeadPortalExecutionProfileBudget(packages, bindings, consumption, context, budget);
    adapter.reserve(88L);
    verify(budget)
        .reserve(
            eq(7L),
            eq("experiment:9"),
            eq("lead-portal-package:88"),
            eq("lead-portal-package:88:attempt:1"),
            anyString(),
            eq("gpt-image-2.5-sunburst"),
            eq(6),
            eq(false));
    assertThatThrownBy(() -> adapter.requireComplete(88L, 1))
        .hasMessageContaining("quantidade contratada");
    adapter.requireComplete(88L, 2);
  }
}
