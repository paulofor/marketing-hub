package com.marketinghub.imagegenerator.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorRequest;
import com.marketinghub.openai.OpenAiProperties;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

/**
 * Responsabilidade: homologar geração, auditoria, ledger e recuperação em uma única integração
 * local.
 */
class ImageGeneratorCostFlowTest {
  /**
   * Usa contextos distintos e comprova replay sem modelo, cobrança duplicada ou perda de auditoria.
   */
  @ParameterizedTest
  @ValueSource(longs = {201, 502})
  void generationAndReconciliationKeepOneLedgerEntryPerAttempt(long productId) throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var calls = new AtomicInteger();
    var response =
        """
      {"model":"gpt-5.6-sol","service_tier":"flex",
       "tools":[{"type":"image_generation","model":"gpt-image-2.5-sunburst"}],
       "output":[{"type":"image_generation_call","result":"dGVzdA=="}],
       "usage":{"input_tokens":3476,"output_tokens":715,"input_tokens_details":{"cached_tokens":0}},
       "tool_usage":{"image_gen":{"input_tokens":656,"output_tokens":2058,
         "input_tokens_details":{"text_tokens":656,"image_tokens":0},
         "output_tokens_details":{"image_tokens":2058,"text_tokens":0}}}}
      """;
    var client =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  calls.incrementAndGet();
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .header("Content-Type", "application/json")
                          .body(response)
                          .build());
                })
            .build();
    var products = mock(ProductRepository.class);
    when(products.existsById(productId)).thenReturn(true);
    var product = new Product();
    product.setId(productId);
    var plan = new CommercialPlan();
    plan.setId(301L);
    var plans = mock(CommercialPlanRepository.class);
    when(plans.findById(301L)).thenReturn(Optional.of(plan));
    when(plans.findIdsByProductId(productId)).thenReturn(List.of(301L));
    var experiment = new Experiment();
    experiment.setProduct(product);
    var experiments = mock(ExperimentRepository.class);
    when(experiments.findById(401L)).thenReturn(Optional.of(experiment));
    when(experiments.existsById(401L)).thenReturn(true);
    var settings = mock(OpenAiProperties.class);
    when(settings.isEnabled()).thenReturn(true);
    var audit = mock(ImageGenerationRequestRepository.class);
    Map<String, ImageGenerationRequest> saved = new ConcurrentHashMap<>();
    when(audit.save(any()))
        .thenAnswer(
            i -> {
              ImageGenerationRequest row = i.getArgument(0);
              saved.put(row.getJobId(), row);
              return row;
            });
    when(audit.findCompletedByContextAndJobId(eq(productId), eq(301L), eq(401L), anyString()))
        .thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(3))));
    var ledger = mock(StudioCostLedgerEntryRepository.class);
    Map<String, StudioCostLedgerEntry> entries = new ConcurrentHashMap<>();
    when(ledger.findBySourceTypeAndSourceId(eq("IMAGE_GENERATION_REQUEST"), anyString()))
        .thenAnswer(i -> Optional.ofNullable(entries.get(i.getArgument(1))));
    when(ledger.save(any()))
        .thenAnswer(
            i -> {
              StudioCostLedgerEntry row = i.getArgument(0);
              entries.put(row.getSourceId(), row);
              return row;
            });
    var pricing = mock(OpenAiPricingService.class);
    when(pricing.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.of(new BigDecimal("0.014102")));
    var derivatives = mock(ImageDerivativeService.class);
    var service =
        new ImageGeneratorService(
            client,
            settings,
            audit,
            derivatives,
            products,
            plans,
            experiments,
            new StudioCostLedgerService(ledger),
            json,
            "gpt-5.6-sol",
            "gpt-image-2.5-sunburst",
            null);
    ReflectionTestUtils.setField(service, "usageCost", new ImageGenerationUsageCost(pricing, json));
    var result =
        service.generate(new ImageGeneratorRequest(productId, 301L, 401L, "Entrada sintética"));
    assertThat(result.images()).hasSize(2);
    assertThat(calls).hasValue(2);
    assertThat(entries).hasSize(2);
    for (var image : result.images()) {
      var cost = service.reconcileCost(productId, 301L, 401L, image.jobId());
      assertThat(cost.status()).isEqualTo("ESTIMATED");
      assertThat(cost.estimatedCostUsd()).isEqualByComparingTo("0.079122");
      service.reconcileCost(productId, 301L, 401L, image.jobId());
      assertThat(service.getGeneratedImage(productId, 301L, 401L, image.jobId()).imageBase64())
          .isEqualTo("dGVzdA==");
      assertThat(entries.get(image.jobId()).getEstimatedCostUsd()).isEqualByComparingTo("0.079122");
      assertThat(saved.get(image.jobId()).getOpenAiResponseBody()).contains("tool_usage");
    }
    assertThat(calls).hasValue(2);
    assertThat(entries).hasSize(2);
    assertThatThrownBy(
            () -> service.reconcileCost(999L, 301L, 401L, result.images().getFirst().jobId()))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    // Uma falha de derivação não pode apagar o consumo já produzido pelo provedor.
    when(derivatives.createVariants(anyString(), anyString()))
        .thenThrow(new IllegalStateException("falha sintética"));
    assertThatThrownBy(
            () ->
                service.generate(new ImageGeneratorRequest(productId, 301L, 401L, "Outra entrada")))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(entries).hasSize(4);
    assertThat(entries.values())
        .allSatisfy(e -> assertThat(e.getEstimatedCostUsd()).isEqualByComparingTo("0.079122"));
    assertThat(saved.values())
        .allSatisfy(a -> assertThat(a.getOpenAiResponseBody()).contains("tool_usage"));
  }
}
