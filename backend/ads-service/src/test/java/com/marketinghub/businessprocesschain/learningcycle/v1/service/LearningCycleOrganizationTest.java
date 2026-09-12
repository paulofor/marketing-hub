package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir ciclo solto, retorno fora da cadeia e perda de contexto histórico. */
class LearningCycleOrganizationTest {
  private final LearningCycleOrganization organization =
      new LearningCycleOrganization(new LearningCycleJson(new ObjectMapper()));

  /**
   * Exige um pai real na cadeia em vez de supor que qualquer processo seis pode receber o ciclo.
   */
  @Test
  void ignoresUnrelatedChains() {
    var chain = chain();
    chain.getItems().getFirst().getProcessDefinition().setProcessCode("other");
    assertThat(organization.describe(chain, cycleProcess(), null, null)).isNull();
  }

  /** Uma referência de pai sem chamada no BPM não pode autorizar a criação de ciclos. */
  @Test
  void rejectsDisconnectedPlacement() {
    var chain = chain();
    chain
        .getItems()
        .getFirst()
        .getProcessDefinition()
        .setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    var entry = organization.describe(chain, cycleProcess(), 4L, null);
    assertThat(entry.integrated()).isFalse();
    assertThat(entry.canStartCycle()).isFalse();
    assertThat(entry.guidance()).contains("histórica");
  }

  /** Um nó isolado é insuficiente; a chamada precisa receber e devolver o fluxo. */
  @Test
  void requiresBothIncomingAndOutgoingFlow() {
    var chain = chain();
    var parent = chain.getItems().getFirst().getProcessDefinition();
    parent.setDiagramJson(
        parent.getDiagramJson().replace(", {\"from\":\"learningCycle\",\"to\":\"decision\"}", ""));
    assertThat(organization.describe(chain, cycleProcess(), null, null).integrated()).isFalse();
  }

  /** Resolve a posição pelo vínculo e oferece apenas os retornos realmente presentes na cadeia. */
  @Test
  void resolvesPlacementAndFiltersUnknownReturnTargets() {
    var entry = organization.describe(chain(), cycleProcess(), 4L, null);
    assertThat(entry.canStartCycle()).isTrue();
    assertThat(entry.sequenceNumber()).isEqualTo(6);
    assertThat(entry.activityId()).isEqualTo("learningCycle");
    assertThat(entry.activitySequenceNumber()).isEqualTo(1);
    assertThat(entry.parentUrl()).endsWith("#activity-learningCycle");
    assertThat(entry.returnRoutes()).hasSize(1);
    assertThat(entry.returnRoutes().getFirst().processDefinitionId()).isEqualTo(60L);
    assertThat(entry.parentUrl()).contains("/products/4/", "processes/60", "chainId=13");
    assertThat(entry.workspaceUrl()).endsWith("chainId=13&productId=4");
  }

  /** O acesso operacional conserva o ciclo já aberto e sua versão histórica da cadeia. */
  @Test
  void resumesExactOpenCycleWithoutMigratingHistory() {
    var cycle = new LearningSalesCycle();
    cycle.setId(91L);
    cycle.setChainDefinitionId(12L);
    var entry = organization.describe(chain(), cycleProcess(), 4L, cycle);
    assertThat(entry.workspaceUrl()).endsWith("chainId=12&productId=4&cycleId=91");
    assertThat(entry.actionLabel()).isEqualTo("Retomar ciclo #91");
  }

  /** O retorno inclui a ocorrência histórica selecionada, mesmo depois do fechamento. */
  @Test
  void parentReturnPreservesSelectedClosedCycle() {
    var cycle = new LearningSalesCycle();
    cycle.setId(91L);
    cycle.setChainDefinitionId(13L);
    cycle.setStatus("ADJUSTED");
    var entry = organization.describe(chain(), cycleProcess(), 4L, cycle);
    assertThat(entry.parentUrl())
        .isEqualTo(
            "/products/4/value-chain-history/processes/60/activities?chainId=13&learningCycleId=91#activity-learningCycle");
  }

  /** Cadeias aposentadas continuam legíveis sem permitir novas iterações nesse contrato. */
  @Test
  void preservesRetiredReadOnlyPlacement() {
    var chain = chain();
    chain.setStatus("RETIRED");
    assertThat(organization.describe(chain, cycleProcess(), null, null).canStartCycle()).isFalse();
  }

  /** Cria a definição especializada cujo pai será resolvido pela composição persistida. */
  private BusinessProcessDefinition cycleProcess() {
    var value = new BusinessProcessDefinition();
    value.setId(72L);
    value.setName("Ciclos de aprendizado e vendas");
    value.setProcessCode("value-chain-learning-sales-cycle");
    value.setParentProcessCode("pde-sales-delivery-learning");
    value.setStatus("PUBLISHED");
    return value;
  }

  /** Monta uma cadeia mínima com chamada conectada, destino real e destino inexistente. */
  private BusinessProcessChainDefinition chain() {
    var process = new BusinessProcessDefinition();
    process.setId(60L);
    process.setProcessCode("pde-sales-delivery-learning");
    process.setName("Venda e aprendizado");
    process.setStatus("PUBLISHED");
    process.setDiagramJson(
        """
        {"nodes":[{"id":"learningCycle","type":"TASK","subprocessCode":"value-chain-learning-sales-cycle"}],
         "flows":[{"from":"consolidate","to":"learningCycle"}, {"from":"learningCycle","to":"decision"}],
         "learningCycleReturns":[{"label":"Coleta","condition":"Dentro dos limites","processCode":"pde-sales-delivery-learning"},
          {"label":"Inválido","condition":"Ausente","processCode":"other"}]}
        """);
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    item.setSequenceNumber(6);
    var chain = new BusinessProcessChainDefinition();
    chain.setId(13L);
    chain.setName("Cadeia PDE");
    chain.setStatus("PUBLISHED");
    chain.setItems(List.of(item));
    return chain;
  }
}
