package com.marketinghub.salesvideo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.media.mapper.AssetMapper;
import com.marketinghub.salesvideo.dto.DeliveryExampleDto;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Confere o contrato HTTP usado pelo editor, sem executar workers ou alterar dados comerciais. */
class DeliveryExamplesHttpContractTest {
  /**
   * Lista a origem canônica e vincula títulos e IDs, sem aceitar proveniência fornecida pelo
   * cliente.
   */
  @Test
  void exposesCatalogAndBindsSelection() throws Exception {
    var service = mock(SalesVideoService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new SalesVideoController(service, mock(AssetMapper.class)))
            .build();
    when(service.deliveryExamples(9001L))
        .thenReturn(
            List.of(
                new DeliveryExampleDto(
                    9002L,
                    9003L,
                    "Amostra",
                    "https://example.invalid/a.png",
                    "a".repeat(64),
                    "Fixture",
                    "Uso local")));
    mvc.perform(get("/api/sales-videos/projects/9001/delivery-examples"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].visualAssetId").value(9002))
        .andExpect(jsonPath("$[0].rightsStatement").value("Uso local"));
    mvc.perform(
            post("/api/sales-videos/jobs/9004/request-post-production")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"requestedBy\":\"fixture@sandbox.local\",\"captionText\":\"Veja uma amostra\",\"deliveryExamples\":[{\"visualAssetId\":9002,\"captionCueIndex\":0,\"headline\":\"Seu kit\"}]}"))
        .andExpect(status().isOk());
    verify(service)
        .requestPostProduction(
            eq(9004L),
            argThat(
                request ->
                    request.getDeliveryExamples().getFirst().visualAssetId() == 9002L
                        && "Seu kit".equals(request.getDeliveryExamples().getFirst().headline())
                        && !request.isNarrationRevisionConfirmed()));
  }
}
