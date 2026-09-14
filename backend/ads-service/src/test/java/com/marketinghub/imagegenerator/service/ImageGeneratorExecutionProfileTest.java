package com.marketinghub.imagegenerator.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.imagegenerator.dto.ImageGeneratorRequest;
import com.marketinghub.openai.OpenAiProperties;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.executionprofile.v1.service.ExecutionProfileBudget;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Responsabilidade: provar que a geração real reserva antes do HTTP e preserva a falha cobrada. */
class ImageGeneratorExecutionProfileTest {
  private final ExecutionProfileBudget budget = mock(ExecutionProfileBudget.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final AtomicInteger calls = new AtomicInteger();
  private ImageGeneratorService service;
  private boolean fail;

  /**
   * Monta geração completa com cliente HTTP substituto, auditoria simulada e nenhuma credencial
   * real.
   */
  @BeforeEach
  void setup() {
    var client =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  verify(budget)
                      .reserve(
                          eq(7L),
                          eq("experiment:9"),
                          eq("administrative-prototype"),
                          eq("operation-1"),
                          eq("Entrada sintética"),
                          eq("gpt-image-2.5-sunburst"),
                          eq(2),
                          eq(true));
                  calls.incrementAndGet();
                  return Mono.just(
                      ClientResponse.create(fail ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK)
                          .header("Content-Type", "application/json")
                          .body(
                              fail
                                  ? "{\"error\":\"fixture\"}"
                                  : "{\"id\":\"fixture\",\"output\":[{\"type\":\"image_generation_call\",\"result\":\"dGVzdA==\"}]}")
                          .build());
                })
            .build();
    var settings = mock(OpenAiProperties.class);
    when(settings.isEnabled()).thenReturn(true);
    var products = mock(ProductRepository.class);
    when(products.existsById(7L)).thenReturn(true);
    var product = new Product();
    product.setId(7L);
    var plan = new CommercialPlan();
    plan.setId(8L);
    when(plans.findById(8L)).thenReturn(Optional.of(plan));
    when(plans.findIdsByProductId(7L)).thenReturn(List.of(8L));
    var experiment = new Experiment();
    experiment.setId(9L);
    experiment.setProduct(product);
    var experiments = mock(ExperimentRepository.class);
    when(experiments.existsById(9L)).thenReturn(true);
    when(experiments.findById(9L)).thenReturn(Optional.of(experiment));
    var audit = mock(ImageGenerationRequestRepository.class);
    when(audit.save(any())).thenAnswer(i -> i.getArgument(0));
    when(budget.reserve(any(), any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
        .thenReturn(10L);
    service =
        new ImageGeneratorService(
            client,
            settings,
            audit,
            mock(ImageDerivativeService.class),
            products,
            plans,
            experiments,
            mock(StudioCostLedgerService.class),
            new ObjectMapper().findAndRegisterModules(),
            "gpt-5.6",
            "gpt-image-2.5-sunburst",
            null);
    ReflectionTestUtils.setField(service, "executionProfileBudget", budget);
  }

  /** Gera somente após a reserva e conserva custo não informado como pendência. */
  @Test
  void generatesReservedBatchAndAuditsUnknownCost() {
    assertThat(service.generate(request()).images()).hasSize(2);
    assertThat(calls).hasValue(2);
    verify(budget)
        .settle(eq(7L), eq(10L), isNull(), startsWith("image_generation_batch:"), eq(false));
  }

  /** Um gate negado impede qualquer chamada HTTP ao provedor. */
  @Test
  void blockedProfileDoesNotCallProvider() {
    when(budget.reserve(any(), any(), any(), any(), any(), any(), anyInt(), anyBoolean()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Plutus pendente"));
    assertThatThrownBy(() -> service.generate(request())).hasMessageContaining("Plutus");
    assertThat(calls).hasValue(0);
  }

  /** Falha de todas as imagens não apaga a reserva nem transforma a cobrança em zero. */
  @Test
  void providerFailureKeepsUnknownCost() {
    fail = true;
    assertThatThrownBy(() -> service.generate(request()))
        .isInstanceOf(ResponseStatusException.class);
    verify(budget)
        .settle(eq(7L), eq(10L), isNull(), startsWith("image_generation_batch:"), eq(true));
    assertThat(calls).hasValue(2);
  }

  /** Plano de outro produto não pode receber custo atribuído ao contexto vinculado. */
  @Test
  void foreignPlanDoesNotConsume() {
    when(plans.findIdsByProductId(7L)).thenReturn(List.of(88L));
    assertThatThrownBy(() -> service.generate(request()))
        .hasMessageContaining("não pertence ao produto");
    assertThat(calls).hasValue(0);
    verifyNoInteractions(budget);
  }

  /** Fornece somente identidades e entrada privadas do teste local. */
  private ImageGeneratorRequest request() {
    return new ImageGeneratorRequest(7L, 8L, 9L, "Entrada sintética", "operation-1");
  }
}
