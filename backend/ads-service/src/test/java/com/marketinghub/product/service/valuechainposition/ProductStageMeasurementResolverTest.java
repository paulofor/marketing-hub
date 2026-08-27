package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductProcessPeriod;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductProcessPeriodRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a consolidação temporal e financeira exibida nos cards. */
class ProductStageMeasurementResolverTest {
  private final ProductProcessPeriodRepository periods = mock(ProductProcessPeriodRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final StudioCostLedgerEntryRepository ledger =
      mock(StudioCostLedgerEntryRepository.class);
  private final ProductStageMeasurementResolver resolver =
      new ProductStageMeasurementResolver(
          periods,
          plans,
          tasks,
          ledger,
          new ObjectMapper(),
          Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC));

  /** Soma custos conhecidos, preserva lacunas e corrige o backfill pela primeira execução. */
  @Test
  void measuresCurrentProcessWithExplicitCostCoverage() {
    Product product = Product.builder().id(9L).build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).build();
    BusinessProcessDefinition process = process(43L, "communication", null);
    AgentTask known = task(1L, process, "2026-08-20T12:00:00Z", "1.50000000", "COMPLETED");
    AgentTask unknown = task(2L, process, "2026-08-21T12:00:00Z", null, "BLOCKED");
    ProductProcessPeriod period = new ProductProcessPeriod();
    period.setProduct(product);
    period.setProcessDefinition(process);
    period.setProcessCodeSnapshot("communication");
    period.setProcessNameSnapshot("Comunicação");
    period.setSequenceNumber(4);
    period.setEnteredAt(Instant.parse("2026-08-23T12:00:00Z"));
    period.setEntryEvidence("BACKFILLED_PRODUCT_UPDATE");
    StudioCostLedgerEntry mediaCost = new StudioCostLedgerEntry();
    mediaCost.setId(7L);
    mediaCost.setProductId(9L);
    mediaCost.setEstimatedCostUsd(new BigDecimal("2.250000"));
    mediaCost.setCostEvidence("PROVIDER_RATE_CARD_ESTIMATE");
    mediaCost.setStartedAt(Instant.parse("2026-08-22T12:00:00Z"));
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(List.of(unknown, known));
    when(periods.findByProductIdOrderByEnteredAtAscIdAsc(9L)).thenReturn(List.of(period));
    when(ledger.findByProductIdOrderByCreatedAtAsc(9L)).thenReturn(List.of(mediaCost));
    when(ledger.findByCommercialPlanIdInOrderByCreatedAtAsc(List.of(4L)))
        .thenReturn(List.of(mediaCost));

    ProductStageMeasurementResponse result =
        resolver.resolveProcessMeasurements(product, List.of(item(process, 4)), process).getFirst();

    assertThat(result.sequenceLabel()).isEqualTo("4");
    assertThat(result.enteredAt()).isEqualTo(Instant.parse("2026-08-20T12:00:00Z"));
    assertThat(result.entryEvidence()).isEqualTo("BACKFILLED_EXECUTION_HISTORY");
    assertThat(result.elapsedDays()).isEqualTo(5L);
    assertThat(result.knownEstimatedCostUsd()).isEqualByComparingTo("3.75000000");
    assertThat(result.costCoverage()).isEqualTo("PARTIAL");
    assertThat(result.costedExecutionCount()).isEqualTo(2);
    assertThat(result.uncostedExecutionCount()).isEqualTo(1);
  }

  /** Considera a entrada no subprocesso seguinte como saída que comprova o avanço. */
  @Test
  void measuresSubprocessExitFromNextSubprocessEntry() {
    Product product = Product.builder().id(9L).build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).build();
    BusinessProcessDefinition creative = process(17L, "creative", "communication");
    BusinessProcessDefinition landing = process(18L, "landing", "communication");
    AgentTask creativeTask = task(1L, creative, "2026-08-20T12:00:00Z", "1.00000000", "BLOCKED");
    AgentTask landingTask = task(2L, landing, "2026-08-22T12:00:00Z", "0.50000000", "IN_PROGRESS");
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(List.of(landingTask, creativeTask));
    when(ledger.findByProductIdOrderByCreatedAtAsc(9L)).thenReturn(List.of());
    when(ledger.findByCommercialPlanIdInOrderByCreatedAtAsc(List.of(4L))).thenReturn(List.of());

    List<ProductStageMeasurementResponse> result =
        resolver.resolveSubprocessMeasurements(product, List.of(creative, landing), landing, 4);

    assertThat(result).hasSize(2);
    assertThat(result.getFirst().sequenceLabel()).isEqualTo("4.1");
    assertThat(result.getFirst().trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.getFirst().exitedAt()).isEqualTo(landingTask.getCreatedAt());
    assertThat(result.getFirst().exitEvidence()).isEqualTo("NEXT_SUBPROCESS_STARTED");
    assertThat(result.getFirst().objectiveAchieved()).isTrue();
    assertThat(result.getFirst().elapsedDays()).isEqualTo(2L);
    assertThat(result.get(1).trackingStatus()).isEqualTo("CURRENT");
    assertThat(result.get(1).sequenceLabel()).isEqualTo("4.2");
    assertThat(result.get(1).elapsedDays()).isEqualTo(3L);
  }

  /** Encerra o subprocesso no objetivo criativo aprovado sem fabricar a entrada no seguinte. */
  @Test
  void recognizesApprovedCreativePackageBeforeNextSubprocessStarts() {
    Product product = Product.builder().id(9L).build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).build();
    BusinessProcessDefinition creative =
        process(48L, "creative-production-approval", "communication");
    BusinessProcessDefinition landing = process(18L, "landing-page-generation", "communication");
    List<AgentTask> approvedTasks =
        List.of(
            approvedCreativeTask(1L, creative, "route"),
            approvedCreativeTask(2L, creative, "produce"),
            approvedCreativeTask(3L, creative, "customer"),
            approvedCreativeTask(4L, creative, "commercial"));
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(approvedTasks);
    when(ledger.findByProductIdOrderByCreatedAtAsc(9L)).thenReturn(List.of());
    when(ledger.findByCommercialPlanIdInOrderByCreatedAtAsc(List.of(4L))).thenReturn(List.of());

    ProductStageMeasurementResponse result =
        resolver
            .resolveSubprocessMeasurements(product, List.of(creative, landing), null)
            .getFirst();

    assertThat(result.trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(result.exitedAt()).isEqualTo(Instant.parse("2026-08-25T10:00:00Z"));
    assertThat(result.exitEvidence()).isEqualTo("SUBPROCESS_OBJECTIVE_ACHIEVED");
  }

  /** Mostra o subprocesso pronto sem inventar sua entrada, duração ou custo. */
  @Test
  void showsReadySubprocessWithoutExecutionEvidence() {
    Product product = Product.builder().id(9L).build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).build();
    BusinessProcessDefinition creative =
        process(48L, "creative-production-approval", "communication");
    BusinessProcessDefinition landing = process(18L, "landing-page-generation", "communication");
    List<AgentTask> approvedTasks =
        List.of(
            approvedCreativeTask(1L, creative, "route"),
            approvedCreativeTask(2L, creative, "produce"),
            approvedCreativeTask(3L, creative, "customer"),
            approvedCreativeTask(4L, creative, "commercial"));
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(approvedTasks);
    when(ledger.findByProductIdOrderByCreatedAtAsc(9L)).thenReturn(List.of());
    when(ledger.findByCommercialPlanIdInOrderByCreatedAtAsc(List.of(4L))).thenReturn(List.of());

    List<ProductStageMeasurementResponse> result =
        resolver.resolveSubprocessMeasurements(
            product, List.of(creative, landing), landing, 4, true);

    assertThat(result).hasSize(2);
    ProductStageMeasurementResponse readyLanding = result.get(1);
    assertThat(readyLanding.sequenceLabel()).isEqualTo("4.2");
    assertThat(readyLanding.trackingStatus()).isEqualTo("PLANNED");
    assertThat(readyLanding.enteredAt()).isNull();
    assertThat(readyLanding.elapsedDays()).isNull();
    assertThat(readyLanding.knownEstimatedCostUsd()).isZero();
    assertThat(readyLanding.costCoverage()).isEqualTo("NO_EXECUTIONS");
  }

  /** Encerra 4.2 somente com Quality Review, Psique e Têmis aprovando a mesma landing. */
  @Test
  void recognizesApprovedLandingBeforeCommercialIntegrationStarts() {
    Product product = Product.builder().id(9L).build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).build();
    BusinessProcessDefinition creative =
        process(17L, "creative-production-approval", "communication");
    BusinessProcessDefinition landing = process(18L, "landing-page-generation", "communication");
    List<AgentTask> approvedTasks =
        List.of(
            approvedLandingTask(1L, landing, "html", "2026-08-26T10:00:00Z"),
            approvedLandingTask(2L, landing, "customer", "2026-08-26T10:01:00Z"),
            approvedLandingTask(3L, landing, "commercial", "2026-08-26T10:02:00Z"));
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(approvedTasks.reversed());
    when(ledger.findByProductIdOrderByCreatedAtAsc(9L)).thenReturn(List.of());
    when(ledger.findByCommercialPlanIdInOrderByCreatedAtAsc(List.of(4L))).thenReturn(List.of());

    ProductStageMeasurementResponse result =
        resolver
            .resolveSubprocessMeasurements(product, List.of(creative, landing), null, 4)
            .getFirst();

    assertThat(result.sequenceLabel()).isEqualTo("4.2");
    assertThat(result.trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(result.exitedAt()).isEqualTo(Instant.parse("2026-08-26T10:02:00Z"));
    assertThat(result.exitEvidence()).isEqualTo("SUBPROCESS_OBJECTIVE_ACHIEVED");
    assertThat(resolver.objectiveAchieved(product, landing)).isTrue();
  }

  /** Ignora a tentativa bloqueada quando uma repetição posterior aprova integralmente a landing. */
  @Test
  void recognizesApprovedLandingRetryAfterBlockedAttempt() {
    Product product = Product.builder().id(9L).build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).build();
    BusinessProcessDefinition landing = process(18L, "landing-page-generation", "communication");
    AgentTask blockedHtml =
        approvedLandingTask(1L, landing, "html", "2026-08-26T10:00:00Z");
    AgentTask blockedCustomer =
        task(2L, landing, "2026-08-26T10:01:00Z", "0.10000000", "BLOCKED");
    blockedCustomer.setProcessActivityId("customer");
    blockedCustomer.setSourceReference("commercial-plan:4@v3:journey");
    blockedCustomer.setResultJson("{\"decision\":\"ADJUST\"}");
    List<AgentTask> approvedRetry =
        List.of(
            approvedLandingTask(3L, landing, "html", "2026-08-27T10:00:00Z"),
            approvedLandingTask(4L, landing, "customer", "2026-08-27T10:01:00Z"),
            approvedLandingTask(5L, landing, "commercial", "2026-08-27T10:02:00Z"));
    approvedRetry.forEach(
        task -> task.setSourceReference("commercial-plan:4@v3:journey:attempt:2"));
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(
            List.of(
                approvedRetry.get(2),
                approvedRetry.get(1),
                approvedRetry.get(0),
                blockedCustomer,
                blockedHtml));
    when(ledger.findByProductIdOrderByCreatedAtAsc(9L)).thenReturn(List.of());
    when(ledger.findByCommercialPlanIdInOrderByCreatedAtAsc(List.of(4L))).thenReturn(List.of());

    ProductStageMeasurementResponse result =
        resolver.resolveSubprocessMeasurements(product, List.of(landing), null, 4).getFirst();

    assertThat(result.trackingStatus()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(result.exitedAt()).isEqualTo(Instant.parse("2026-08-27T10:02:00Z"));
    assertThat(resolver.objectiveAchieved(product, landing)).isTrue();
  }

  /** Não confunde atualização de tarefa bloqueada com objetivo do macroprocesso atingido. */
  @Test
  void keepsHistoricalProcessRecordedWithoutDownstreamEntryEvidence() {
    Product product = Product.builder().id(9L).build();
    CommercialPlan plan = CommercialPlan.builder().id(4L).build();
    BusinessProcessDefinition discovery = process(10L, "discovery", null);
    BusinessProcessDefinition communication = process(43L, "communication", null);
    AgentTask blocked = task(1L, discovery, "2026-08-20T12:00:00Z", null, "BLOCKED");
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(List.of(blocked));
    when(periods.findByProductIdOrderByEnteredAtAscIdAsc(9L)).thenReturn(List.of());
    when(ledger.findByProductIdOrderByCreatedAtAsc(9L)).thenReturn(List.of());
    when(ledger.findByCommercialPlanIdInOrderByCreatedAtAsc(List.of(4L))).thenReturn(List.of());

    ProductStageMeasurementResponse result =
        resolver
            .resolveProcessMeasurements(
                product, List.of(item(discovery, 1), item(communication, 4)), communication)
            .getFirst();

    assertThat(result.trackingStatus()).isEqualTo("RECORDED");
    assertThat(result.exitedAt()).isNull();
    assertThat(result.objectiveAchieved()).isFalse();
  }

  /** Monta uma tarefa operacional com datas e custo previsíveis. */
  private AgentTask task(
      Long id, BusinessProcessDefinition process, String createdAt, String costUsd, String status) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(process);
    task.setStatus(status);
    task.setCreatedAt(Instant.parse(createdAt));
    task.setUpdatedAt(task.getCreatedAt());
    task.setEstimatedCostUsd(costUsd == null ? null : new BigDecimal(costUsd));
    return task;
  }

  /** Monta uma atividade criativa concluída com decisão humana e pacote íntegro. */
  private AgentTask approvedCreativeTask(
      Long id, BusinessProcessDefinition process, String activityId) {
    AgentTask task = task(id, process, "2026-08-25T10:00:00Z", "0.10000000", "COMPLETED");
    task.setProcessActivityId(activityId);
    task.setEvidenceJson(
        """
        {"creativePackageId":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","assets":[{"url":"https://cdn.example/asset.png"}],"importedByHuman":true,"published":false,"externalMediaSpendUsd":0}
        """);
    return task;
  }

  /** Monta uma aprovação funcional da mesma execução oficial de landing. */
  private AgentTask approvedLandingTask(
      Long id, BusinessProcessDefinition process, String activityId, String completedAt) {
    AgentTask task = task(id, process, completedAt, "0.10000000", "COMPLETED");
    task.setProcessActivityId(activityId);
    task.setSourceReference("commercial-plan:4@v3:journey");
    task.setDeliveredAt(Instant.parse(completedAt));
    if ("html".equals(activityId)) {
      task.setEvidenceJson(
          """
          {"approvalRecommendation":"APPROVE_FOR_PUBLICATION","landingHtml":"<html>Rigel</html>","checkoutUrl":"https://www.mercadopago.com.br/checkout"}
          """);
    } else {
      task.setResultJson("{\"decision\":\"APPROVED\"}");
    }
    return task;
  }

  /** Monta uma definição enxuta de processo ou subprocesso. */
  private BusinessProcessDefinition process(Long id, String code, String parentCode) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setName(code);
    process.setParentProcessCode(parentCode);
    return process;
  }

  /** Monta a posição de uma definição na cadeia principal. */
  private BusinessProcessChainItem item(BusinessProcessDefinition process, int sequence) {
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    item.setSequenceNumber(sequence);
    return item;
  }
}
