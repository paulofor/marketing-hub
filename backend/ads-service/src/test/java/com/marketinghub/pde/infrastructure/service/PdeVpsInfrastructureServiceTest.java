package com.marketinghub.pde.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.marketinghub.pde.infrastructure.PdeVpsStatus;
import com.marketinghub.pde.infrastructure.service.saveVps.SavePdeVpsServerRequest;
import com.marketinghub.repository.jpa.pde.PdeVpsServerRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar regras de negócio da infraestrutura VPS dos PDEs. */
class PdeVpsInfrastructureServiceTest {

  /** Deve bloquear custo mensal negativo para preservar margem correta do produto. */
  @Test
  void createServerRejectsNegativeMonthlyCost() {
    PdeVpsInfrastructureService service =
        new PdeVpsInfrastructureService(mock(PdeVpsServerRepository.class));
    SavePdeVpsServerRequest request =
        new SavePdeVpsServerRequest(
            "DokeHost PDE principal",
            "DokeHost",
            "163.245.200.7",
            "VPS Linux",
            "Brasil",
            2,
            4,
            80,
            new BigDecimal("-1.00"),
            "metodo-musa-7-dias",
            "production",
            "v6.clubemusa.com.br",
            PdeVpsStatus.ACTIVE,
            "Produção inicial");

    assertThatThrownBy(() -> service.createServer(request))
        .isInstanceOf(ResponseStatusException.class);
  }
}
