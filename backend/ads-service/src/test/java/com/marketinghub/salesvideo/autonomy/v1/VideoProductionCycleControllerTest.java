package com.marketinghub.salesvideo.autonomy.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

/** Responsabilidade: proteger as rotas públicas dos ciclos governados de vídeo. */
class VideoProductionCycleControllerTest {

  /** Mantém o preflight isolado em rota própria e delegado ao método sem produção. */
  @Test
  void shouldExposeProviderPreflightOnlyEndpoint() throws Exception {
    VideoProductionCycleService service = mock(VideoProductionCycleService.class);
    VideoProductionCycleController controller = new VideoProductionCycleController(service);
    VideoProductionCycleContracts.CreateRequest request =
        mock(VideoProductionCycleContracts.CreateRequest.class);

    controller.createProviderPreflight(request);

    verify(service).createProviderPreflight(request);
    Method method =
        VideoProductionCycleController.class.getMethod(
            "createProviderPreflight", VideoProductionCycleContracts.CreateRequest.class);
    assertThat(method.getAnnotation(PostMapping.class).value())
        .containsExactly("/api/sales-videos/autonomy/v1/provider-preflights");
  }

  /** Expõe reconciliação explícita antes da leitura da fila sem atribuí-la ao endpoint GET. */
  @Test
  void shouldExposeFinancialReviewReconciliationEndpoint() throws Exception {
    VideoProductionCycleService service = mock(VideoProductionCycleService.class);
    VideoProductionCycleController controller = new VideoProductionCycleController(service);

    controller.reconcileFinancialReview();

    verify(service).reconcileFinancialReviewQueue();
    Method method = VideoProductionCycleController.class.getMethod("reconcileFinancialReview");
    assertThat(method.getAnnotation(PostMapping.class).value())
        .containsExactly("/api/internal/sales-videos/autonomy/v1/financial-review/reconcile");
  }
}
