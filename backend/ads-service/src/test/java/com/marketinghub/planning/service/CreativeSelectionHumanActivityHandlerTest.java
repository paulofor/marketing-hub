package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.dto.CommercialPlanVisualAssetDto;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.product.Product;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: proteger o efeito especializado da seleção humana de criativos. */
@ExtendWith(MockitoExtension.class)
class CreativeSelectionHumanActivityHandlerTest {
  @Mock private CommercialPlanApprovedProcessAssetService approvedAssets;

  /** Exige os dois pareceres do mesmo arquivo antes de liberar a confirmação humana. */
  @Test
  void exposesApprovedPixelsAsHumanDecisionRequirement() {
    CreativeSelectionHumanActivityHandler handler =
        new CreativeSelectionHumanActivityHandler(approvedAssets);
    Product product = product();
    when(approvedAssets.readiness(product, "experiment:93"))
        .thenReturn(
            new CommercialPlanApprovedProcessAssetService.ImportReadiness(
                true, "Pixels aprovados.", 8L, List.of(407L)));

    var readiness = handler.readiness(process(), activity(), product, "experiment:93");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.actionLabel()).isEqualTo("Aprovar e vincular peças");
    assertThat(readiness.workspaceReferenceId()).isEqualTo(8L);
    assertThat(readiness.requirements())
        .singleElement()
        .satisfies(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("APPROVED_CREATIVE_PIXELS");
              assertThat(requirement.satisfied()).isTrue();
            });
  }

  /** Vincula o ativo e inclui IDs e hash na evidência da própria decisão humana. */
  @Test
  void importsAssetsAndReturnsAuditableCompletion() {
    CreativeSelectionHumanActivityHandler handler =
        new CreativeSelectionHumanActivityHandler(approvedAssets);
    Product product = product();
    CommercialPlanVisualAssetDto asset = asset();
    when(approvedAssets.importForHumanDecision(product, "experiment:93"))
        .thenReturn(
            new CommercialPlanApprovedProcessAssetService.ImportResult(
                8L, "experiment:93", "package-id", List.of(asset)));
    ProductProcessActivityExecutionRequest request =
        new ProductProcessActivityExecutionRequest(
            "APPROVE",
            "Operador",
            "Peças revisadas e aprovadas.",
            "agent-task:527; agent-task:528",
            "CONFIRM:creative-production-approval:human",
            Map.of());

    var completion =
        handler.completeApproval(process(), activity(), product, "experiment:93", request);

    assertThat(completion.objectiveAchieved()).isTrue();
    assertThat(completion.structuredEvidence())
        .containsEntry("commercialPlanId", 8L)
        .containsEntry("creativePackageId", "package-id")
        .containsEntry("published", false)
        .containsEntry("externalMediaSpendAuthorized", false);
    verify(approvedAssets).importForHumanDecision(product, "experiment:93");
  }

  /** Monta o processo canônico governado pelo handler. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    return process;
  }

  /** Monta a atividade humana terminal do processo criativo. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("human");
    return activity;
  }

  /** Monta o produto usado para correlacionar plano e experimento. */
  private Product product() {
    Product product = new Product();
    product.setId(10L);
    return product;
  }

  /** Monta o ativo já aprovado devolvido ao gate humano. */
  private CommercialPlanVisualAssetDto asset() {
    return new CommercialPlanVisualAssetDto(
        701L,
        "https://cdn.example/mira.png",
        "IMAGE",
        "Mira aprovada",
        "ADS",
        List.of("ADS", "LANDING"),
        "Processo aprovado",
        "Uso autorizado",
        "a".repeat(64),
        "package-id",
        1,
        CommercialPlanVisualAssetStatus.APPROVED,
        null,
        CommercialPlanVisualAssetReviewStatus.APPROVED,
        "Aprovado por Têmis",
        CommercialPlanVisualAssetReviewStatus.APPROVED,
        "Aprovado por Psique",
        Instant.parse("2026-09-26T15:30:00Z"),
        Instant.parse("2026-09-26T15:30:00Z"));
  }
}
