package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.dto.CreateCommercialPlanVisualAssetRequest;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a governança da biblioteca audiovisual do plano comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanVisualAssetServiceTest {
  @Mock private CommercialPlanService planService;
  @Mock private CommercialPlanVisualAssetRepository repository;

  private CommercialPlanVisualAssetService service;

  /** Prepara o serviço com persistência simulada antes de cada teste. */
  @BeforeEach
  void setUp() {
    service = new CommercialPlanVisualAssetService(planService, repository);
  }

  /** Deve cadastrar vídeo normalizado como rascunho auditável. */
  @Test
  void createsVideoReference() {
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.save(any(CommercialPlanVisualAsset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.create(
            2L,
            new CreateCommercialPlanVisualAssetRequest(
                "https://cdn.example/product.mp4",
                "video",
                "Demonstração do kit",
                "ADS",
                "Produto",
                "Uso autorizado"));

    assertThat(result.mediaType()).isEqualTo("VIDEO");
    assertThat(result.status().name()).isEqualTo("DRAFT");
  }

  /** Deve bloquear tipos de mídia que os executores não conseguem consumir. */
  @Test
  void rejectsUnsupportedMediaType() {
    assertThatThrownBy(
            () ->
                service.create(
                    2L,
                    new CreateCommercialPlanVisualAssetRequest(
                        "https://cdn.example/product.pdf",
                        "DOCUMENT",
                        "Manual",
                        "DELIVERY",
                        "Produto",
                        "Uso autorizado")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("mediaType must be IMAGE or VIDEO");
  }
}
