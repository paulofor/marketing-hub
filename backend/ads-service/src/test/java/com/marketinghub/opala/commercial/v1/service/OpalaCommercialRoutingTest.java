package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.catalogovivo.v1.service.adoption.OpalaAdoption;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: provar a vigência semântica da conclusão comercial Opala. */
class OpalaCommercialRoutingTest {
  private final ObjectMapper json = new ObjectMapper();
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository activities =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final OpalaCommercialContext context = mock(OpalaCommercialContext.class);
  private final OpalaAdoptionRepository adoptions = mock(OpalaAdoptionRepository.class);
  private final OpalaCommercialRouting routing =
      new OpalaCommercialRouting(processes, chains, activities, instances, products, context);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final ObjectNode reviewed;
  private final ObjectNode current;

  /** Monta a rota adotada e uma conclusão persistida equivalente ao snapshot atual. */
  OpalaCommercialRoutingTest() throws Exception {
    reviewed =
        (ObjectNode)
            json.readTree(
                """
                {"contractVersion":"OPALA_COMMERCIAL_PREPARATION_V1","productId":4,
                 "experimentId":92,"cycleId":2,"productVersion":"fixture-v12",
                 "destinationUrl":"https://v8.example","destinationSource":"EXPERIMENT",
                 "priceBrl":67.00,"checkoutUrl":"https://checkout.example/original",
                 "budgetLimitBrl":100.00,"windowEnd":"2099-10-31T23:59:59Z",
                 "productContract":{"name":"MUSA"},
                 "financialPlan":{"id":1,"revision":1,"status":"READY"},
                 "creatives":[{"id":529,"score":67.0},{"id":530,"score":100.00}],
                 "approvedAudienceElements":[{"id":214},{"id":235}],
                 "publicationAuthorized":false,"mediaSpendAuthorized":false,
                 "evidenceType":"OPALA_COMMERCIAL_READY_V1","salesProven":false}
                """);
    current =
        (ObjectNode)
            json.readTree(
                """
                {"mediaSpendAuthorized":false,"publicationAuthorized":false,
                 "approvedAudienceElements":[{"id":235},{"id":214}],
                 "creatives":[{"score":100,"id":530},{"score":67,"id":529}],
                 "financialPlan":{"status":"READY","revision":1,"id":1},
                 "productContract":{"name":"MUSA"},"windowEnd":"2099-10-31T23:59:59Z",
                 "budgetLimitBrl":100,"checkoutUrl":"https://checkout.example/original",
                 "priceBrl":67,"destinationSource":"EXPERIMENT",
                 "destinationUrl":"https://v8.example","productVersion":"fixture-v12",
                 "cycleId":2,"experimentId":92,"productId":4,
                 "contractVersion":"OPALA_COMMERCIAL_PREPARATION_V1"}
                """);
  }

  /** Configura identidade, adoção e última ocorrência finalizada da versão corrente. */
  @BeforeEach
  void setup() throws Exception {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(14L);
    cycle.setExperimentId(92L);
    cycle.setProductVersion("fixture-v12");
    cycle.setStatus("OPEN");
    cycle.setStage("AUTHORIZATION");
    var type = ProductTypeDefinition.builder().code("PDE").build();
    var product = Product.builder().id(4L).productTypeDefinition(type).build();
    var process = new BusinessProcessDefinition();
    process.setId(77L);
    process.setProcessCode(OpalaCommercialContext.CODE);
    var activity = new BusinessProcessActivityDefinition();
    activity.setId(801L);
    activity.setActivityId("ready");
    var instance = new BusinessProcessActivityInstance();
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(reviewed.toString());
    ReflectionTestUtils.setField(routing, "catalogAdoptions", adoptions);
    when(products.findById(4L)).thenReturn(Optional.of(product));
    when(adoptions.find(2L))
        .thenReturn(
            Optional.of(
                new OpalaAdoption(
                    2L, 77L, 4L, 92L, "fixture-v12", 1L, "Teste", "Fixture", Instant.now())));
    when(processes.findById(77L)).thenReturn(Optional.of(process));
    when(activities.findByProcessDefinitionIdAndActivityId(77L, "ready"))
        .thenReturn(Optional.of(activity));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            801L, "experiment:92"))
        .thenReturn(Optional.of(instance));
    when(context.read(anyString()))
        .thenAnswer(call -> json.readTree(call.getArgument(0, String.class)));
    when(context.snapshot("experiment:92")).thenReturn(current);
  }

  /** Mantém vigente a conclusão quando somente ordem técnica e escala decimal mudam. */
  @Test
  void acceptsSemanticallyEquivalentCompletedProof() {
    assertThat(routing.completed(cycle)).isTrue();
  }

  /** Invalida a conclusão quando o checkout comercial realmente muda. */
  @Test
  void rejectsCompletedProofAfterRealCheckoutChange() {
    var changed = current.deepCopy();
    changed.put("checkoutUrl", "https://checkout.example/new");
    when(context.snapshot("experiment:92")).thenReturn(changed);

    assertThat(routing.completed(cycle)).isFalse();
  }
}
