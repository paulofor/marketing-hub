package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.repository.jpa.financialagent.StudioProviderTaskConsumptionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: proteger a projeção financeira imutável usada pelo preflight de vídeo. */
@ExtendWith(MockitoExtension.class)
class StudioProviderTaskConsumptionQueryServiceTest {
  @Mock private StudioProviderTaskConsumptionRepository repository;
  @InjectMocks private StudioProviderTaskConsumptionQueryService service;

  /** Converte os agregados do banco sem expor o repository ou perder precisão monetária. */
  @Test
  void shouldMapProviderEfficiencyProjection() {
    when(repository.summarizeEfficiencyByProvider("RUNWAY"))
        .thenReturn(
            List.<Object[]>of(
                new Object[] {"gen4_turbo", 3L, 2L, new BigDecimal("4.125000"), 1L, 75L}));

    List<ProviderRouteEfficiencyView> result = service.summarizeEfficiencyByProvider("RUNWAY");

    assertThat(result)
        .containsExactly(
            new ProviderRouteEfficiencyView(
                "gen4_turbo", 3L, 2L, new BigDecimal("4.125000"), 1L, new BigDecimal("75")));
  }
}
