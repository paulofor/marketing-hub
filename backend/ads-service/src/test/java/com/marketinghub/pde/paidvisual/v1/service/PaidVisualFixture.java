package com.marketinghub.pde.paidvisual.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.imagegenerator.service.ImageGenerationUsageCost;
import com.marketinghub.pde.paidvisual.v1.PaidVisualOrder;
import com.marketinghub.pde.visualpersonalization.v1.service.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.pde.PaidVisualOrderRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Responsabilidade: fornecer dependências locais com serviços reais e nenhum acesso pago externo.
 */
public class PaidVisualFixture {
  public final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  public final Map<String, PaidVisualOrder> ordersMap = new LinkedHashMap<>();
  public final Map<String, ImageGenerationRequest> imagesMap = new LinkedHashMap<>();
  public final PaidVisualOrderRepository orders = mock(PaidVisualOrderRepository.class);
  public final ImageGenerationRequestRepository images =
      mock(ImageGenerationRequestRepository.class);
  public final ProductRepository products = mock(ProductRepository.class);
  public final PaidVisualPolicy policy = mock(PaidVisualPolicy.class);
  public final PaidVisualMail mail = mock(PaidVisualMail.class);
  public final StudioCostLedgerService ledger = mock(StudioCostLedgerService.class);
  public final VisualPreparationService visual;
  public final PaidVisualService service;
  public final VisualPreparationExecution execution;
  public String deliveredToken;

  /** Configura persistência simulada e contratos de dois produtos sem nomes ou IDs especiais. */
  public PaidVisualFixture() {
    when(products.findLockedById(anyLong()))
        .thenAnswer(
            i -> {
              var p = new Product();
              p.setId(i.getArgument(0));
              p.setAutomaticExecutionEnabled(true);
              return Optional.of(p);
            });
    when(orders.findById(anyString()))
        .thenAnswer(i -> Optional.ofNullable(ordersMap.get(i.getArgument(0))));
    when(orders.lockById(anyString()))
        .thenAnswer(i -> Optional.ofNullable(ordersMap.get(i.getArgument(0))));
    when(orders.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var o = (PaidVisualOrder) i.getArgument(0);
              ordersMap.put(o.getPaymentId(), o);
              return o;
            });
    when(orders.findByAccessHash(anyString()))
        .thenAnswer(
            i ->
                ordersMap.values().stream()
                    .filter(o -> Objects.equals(o.getAccessHash(), i.getArgument(0)))
                    .findFirst());
    when(orders.findFirstByExperimentIdAndBuyerEmailOrderByPaidAtDesc(anyLong(), anyString()))
        .thenAnswer(
            i ->
                ordersMap.values().stream()
                    .filter(
                        o ->
                            o.getExperimentId().equals(i.getArgument(0))
                                && o.getBuyerEmail().equals(i.getArgument(1)))
                    .max(Comparator.comparing(PaidVisualOrder::getPaidAt)));
    when(images.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var r = (ImageGenerationRequest) i.getArgument(0);
              imagesMap.put(r.getJobId(), r);
              return r;
            });
    when(images.findFirstByJobId(anyString()))
        .thenAnswer(i -> Optional.ofNullable(imagesMap.get(i.getArgument(0))));
    when(images.findByJobId(anyString()))
        .thenAnswer(i -> Optional.ofNullable(imagesMap.get(i.getArgument(0))));
    when(images.findByStatusAndJobIdStartingWithOrderByCreatedAtAsc(
            anyString(), anyString(), any()))
        .thenAnswer(
            i ->
                imagesMap.values().stream()
                    .filter(r -> r.getStatus().equals(i.getArgument(0)))
                    .toList());
    when(images.findByStatusAndOpenAiRequestBodyIsNullAndJobIdStartingWithOrderByCreatedAtAsc(
            anyString(), anyString(), any()))
        .thenReturn(List.of());
    when(policy.resolve(anyLong(), anyBoolean())).thenAnswer(i -> contract(i.getArgument(0)));
    when(policy.resolveSettlement(anyLong(), anyString()))
        .thenAnswer(i -> contract(i.getArgument(0)));
    doAnswer(
            i -> {
              deliveredToken = ((String) i.getArgument(2)).split("#access=")[1];
              return null;
            })
        .when(mail)
        .send(anyString(), anyString(), anyString());
    visual =
        new VisualPreparationService(
            products,
            mock(CommercialPlanRepository.class),
            mock(ExperimentRepository.class),
            images,
            mock(VisualPreparationBudget.class),
            json);
    service = new PaidVisualService(orders, products, images, policy, mail, visual);
    var cost = mock(ImageGenerationUsageCost.class);
    when(cost.estimate(any(), anyString()))
        .thenReturn(
            Optional.of(
                new ImageGenerationUsageCost.Estimate(
                    new BigDecimal("0.20"), "LOCAL_ESTIMATE", "local-double", "2026-10-21")));
    var tx = mock(PlatformTransactionManager.class);
    when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    execution =
        new VisualPreparationExecution(
            images,
            products,
            visual,
            mock(VisualPreparationBudget.class),
            ledger,
            cost,
            tx,
            service);
  }

  /** Cria contrato local da mesma forma persistida por uma versão revisada. */
  public ObjectNode contract(Long experimentId) {
    var n = json.createObjectNode();
    n.put("contractVersion", PaidVisualPolicy.VERSION);
    n.put("productId", experimentId + 100);
    n.put("experimentId", experimentId);
    n.put("productName", "Produto local " + experimentId);
    n.put("productSlug", "produto-" + experimentId);
    n.put("productVersion", "commercial-v1");
    n.put("commercialPlanId", experimentId + 200);
    n.put("priceBrl", 79);
    n.put("accessDays", 7);
    n.put("maxRegenerations", 1);
    n.put("model", "gpt-5.6");
    n.put("maxGenerationUsd", new BigDecimal("0.50"));
    n.put("deliveryUrl", "https://example.test/paid-visual.html?experiment=" + experimentId);
    return n;
  }
}
