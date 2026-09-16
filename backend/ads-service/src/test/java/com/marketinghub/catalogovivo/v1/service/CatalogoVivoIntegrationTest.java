package com.marketinghub.catalogovivo.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.automation.v1.service.ProcessRunService;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.catalogovivo.v1.service.adoption.*;
import com.marketinghub.catalogovivo.v1.service.catalog.*;
import com.marketinghub.catalogovivo.v1.service.commands.*;
import com.marketinghub.repository.jdbc.catalogovivo.*;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: comprovar integridade física, transações, API e preservação histórica do
 * piloto.
 */
@EnabledIfEnvironmentVariable(named = "CATALOGO_VIVO_MYSQL_URL", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CatalogoVivoIntegrationTest {
  static ConfigurableApplicationContext app;
  static CatalogoVivoService service;
  static CatalogoVivoRepository repository;
  static JdbcTemplate jdbc;
  static RestClient http;

  /** Inicializa migração MySQL real e API em porta local exclusiva. */
  @BeforeAll
  static void start() {
    app = CatalogoVivoLocalApplication.start();
    service = app.getBean(CatalogoVivoService.class);
    repository = app.getBean(CatalogoVivoRepository.class);
    jdbc = app.getBean(JdbcTemplate.class);
    http =
        RestClient.builder().baseUrl("http://127.0.0.1:18091/api/catalogo-vivo/v1/opala").build();
  }

  /** Fecha o servidor de teste e libera conexões; o runner remove os volumes ao terminar. */
  @AfterAll
  static void stop() {
    if (app != null) app.close();
  }

  /** Comprova o conjunto completo do SQL e recusa texto vazio antes de persistir. */
  @Test
  @Order(1)
  void loadsCompleteCatalogAndRejectsInvalidTextThroughHttp() {
    var result = http.get().retrieve().body(CatalogResponse.class);
    assertThat(result.ready()).isTrue();
    assertThat(result.items()).hasSize(7);
    assertThat(result.items())
        .extracting(i -> i.binding().agentKey())
        .contains("landing-generator", "financial-agent", "customer-agent", "meta-ad-approver");
    assertThatThrownBy(
            () ->
                http.post()
                    .uri("/bindings/" + result.items().getFirst().binding().id() + "/drafts")
                    .body(new CatalogDraftRequest("", "Fixture", "Validação"))
                    .retrieve()
                    .toBodilessEntity())
        .hasMessageContaining("400");
    assertThat(result.items())
        .allSatisfy(
            i -> {
              assertThat(i.versions()).hasSize(1);
              assertThat(i.versions().getFirst().status()).isEqualTo("REVIEWED");
            });
  }

  /** Confere FKs e unicidade com comandos reais, sem depender de CHECK no MySQL 5.7. */
  @Test
  @Order(2)
  void rejectsForeignKeysAndDuplicateVersions() {
    var catalog = service.catalog();
    var first = catalog.items().getFirst();
    var second = catalog.items().get(1);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE catalogo_vivo_binding_v1 SET active_version_id=? WHERE id=?",
                    second.binding().activeVersionId(),
                    first.binding().id()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE catalogo_vivo_binding_v1 SET agent_id=999999 WHERE id=?",
                    first.binding().id()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO catalogo_vivo_prompt_version_v1(binding_id,version_number,text_content,sha256,status,created_by,created_at) SELECT binding_id,version_number,text_content,sha256,status,created_by,created_at FROM catalogo_vivo_prompt_version_v1 LIMIT 1"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(service.catalog().ready()).isTrue();
  }

  /**
   * Cria e revisa outra versão, prova fixação por tarefa e rollback operacional sem apagar
   * histórico.
   */
  @Test
  @Order(3)
  void pinsVersionsAtCreationAndPreservesRetriesAfterActivation() {
    var original = service.catalog();
    var binding = original.items().getFirst().binding();
    var task = task(binding, 900101L, 900201L, "experiment:900092");
    pin(task);
    var before = service.prompt(task);
    var draft =
        service.draft(
            binding.id(),
            new CatalogDraftRequest(
                "Nova instrução segura: {{TASK_CONTEXT}}", "Fixture", "Teste de versão"));
    assertThatThrownBy(
            () -> service.activate(activation(original, Map.of(binding.id(), draft.id()))))
        .hasMessageContaining("não revisada");
    service.review(
        draft.id(),
        new CatalogReviewRequest("Revisor local", "Contrato conferido", draft.sha256()));
    service.activate(activation(original, Map.of(binding.id(), draft.id())));
    assertThat(service.prompt(task)).isEqualTo(before);
    var retry = task(binding, 900102L, 900201L, "experiment:900092");
    pin(retry);
    assertThat(service.prompt(retry).versionId()).isEqualTo(before.versionId());
    var next = task(binding, 900103L, 900202L, "experiment:900092");
    pin(next);
    assertThat(service.prompt(next).versionId()).isEqualTo(draft.id());
    assertThatThrownBy(() -> service.validateExecution(task, "outra instrução"))
        .hasMessageContaining("diverge");
    var context =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(
                Map.of(
                    "taskId",
                    task.getId(),
                    "agentKey",
                    binding.agentKey(),
                    "activityId",
                    binding.activityId(),
                    "sourceReference",
                    task.getSourceReference(),
                    "processCode",
                    CatalogoVivoService.PROCESS,
                    "processVersion",
                    1,
                    "catalogPromptReference",
                    Map.of("versionId", before.versionId(), "sha256", before.sha256())));
    service.validateExecution(task, before.text().replace("{{TASK_CONTEXT}}", context.toString()));
    assertThatThrownBy(
            () -> service.validateExecution(task, before.text().replace("{{TASK_CONTEXT}}", "{}")))
        .hasMessageContaining("contexto auditado");
    assertThatThrownBy(() -> service.requireModelAudit(task, "DETERMINISTIC", true))
        .hasMessageContaining("auditoria do modelo");
    task.setSourceReference("experiment:900093");
    assertThatThrownBy(() -> service.prompt(task)).hasMessageContaining("incompatível");
    task.setSourceReference("experiment:900092");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM catalogo_vivo_prompt_version_v1 WHERE id=?", before.versionId()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    service.activate(activation(service.catalog(), Map.of(binding.id(), before.versionId())));
    assertThat(service.prompt(next).versionId()).isEqualTo(draft.id());
  }

  /** Produz duas ativações concorrentes e exige conflito de revisão na segunda decisão. */
  @Test
  @Order(4)
  void serializesConcurrentActivationsAndRejectsStaleSnapshots() throws Exception {
    var original = service.catalog();
    long binding = original.items().getFirst().binding().id();
    var drafts = new ArrayList<CatalogVersion>();
    for (int n = 0; n < 2; n++) {
      var v =
          service.draft(
              binding,
              new CatalogDraftRequest(
                  "Alternativa " + n + ": {{TASK_CONTEXT}}", "Fixture", "Concorrência"));
      service.review(v.id(), new CatalogReviewRequest("Revisor", "Compatível", v.sha256()));
      drafts.add(v);
    }
    var barrier = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> work = new ArrayList<>();
      for (var v : drafts)
        work.add(
            executor.submit(
                () -> {
                  barrier.await();
                  try {
                    service.activate(activation(original, Map.of(binding, v.id())));
                    return true;
                  } catch (org.springframework.web.server.ResponseStatusException ex) {
                    assertThat(ex.getReason()).contains("Outra ativação");
                    return false;
                  }
                }));
      assertThat(List.of(work.get(0).get(), work.get(1).get()))
          .containsExactlyInAnyOrder(true, false);
    }
    service.activate(
        activation(
            service.catalog(),
            Map.of(binding, original.items().getFirst().binding().activeVersionId())));
  }

  /** Bloqueia pacote incompleto, tarefa de outro produto e ausência de versão sem fallback. */
  @Test
  @Order(5)
  void persistsResolutionBlockerAndRejectsCrossProduct() {
    var binding = service.catalog().items().getFirst().binding();
    var wrong = task(binding, 900104L, 900203L, "experiment:900093");
    pin(wrong);
    assertThat(wrong.getStatus()).isEqualTo("BLOCKED");
    assertThat(wrong.getBlockerAction()).contains("CATALOGO_VIVO:");
    assertThatThrownBy(() -> service.prompt(wrong)).hasMessageContaining("sem prompt fixado");
    var missing = task(binding, 900106L, 900205L, "experiment:900099");
    pin(missing);
    assertThat(missing.getStatus()).isEqualTo("BLOCKED");
    assertThat(missing.getExecutionError()).contains("experimento da tarefa");
    jdbc.update(
        "UPDATE catalogo_vivo_binding_v1 SET active_version_id=NULL WHERE id=?", binding.id());
    assertThat(service.catalog().issues()).anyMatch(i -> i.contains("sem versão ativa"));
    var blocked = task(binding, 900105L, 900204L, "experiment:900092");
    pin(blocked);
    assertThat(blocked.getStatus()).isEqualTo("BLOCKED");
    jdbc.update(
        "UPDATE catalogo_vivo_binding_v1 SET active_version_id=? WHERE id=?",
        binding.activeVersionId(),
        binding.id());
    assertThat(service.catalog().ready()).isTrue();
  }

  /**
   * Integra a cadeia anterior pela API, preserva seus valores e inicia uma única execução
   * correlacionada.
   */
  @Test
  @Order(6)
  void adoptsHistoricalCycleIdempotentlyWithoutChangingBudgetOrHistory() {
    var before = jdbc.queryForMap("SELECT * FROM learning_sales_cycle_v1 WHERE id=900002");
    var request = new OpalaAdoptionRequest(14L, "Operador local", "Preparação do ciclo preservado");
    String path = "/products/900004/cycles/900002/adoption";
    assertThat(http.get().uri(path).retrieve().body(OpalaAdoptionResponse.class).canAdopt())
        .isTrue();
    assertThatThrownBy(
            () ->
                http.post()
                    .uri(path)
                    .body(new OpalaAdoptionRequest(13L, "Operador", "Revisão antiga"))
                    .retrieve()
                    .toBodilessEntity())
        .hasMessageContaining("409");
    jdbc.update("UPDATE learning_sales_cycle_v1 SET status='CLOSED' WHERE id=900002");
    assertThat(http.get().uri(path).retrieve().body(OpalaAdoptionResponse.class).canAdopt())
        .isFalse();
    assertThatThrownBy(() -> http.post().uri(path).body(request).retrieve().toBodilessEntity())
        .hasMessageContaining("409");
    jdbc.update("UPDATE learning_sales_cycle_v1 SET status='OPEN' WHERE id=900002");
    jdbc.update("UPDATE experiment SET status='RUNNING' WHERE id=900092");
    assertThat(http.get().uri(path).retrieve().body(OpalaAdoptionResponse.class).canAdopt())
        .isFalse();
    jdbc.update("UPDATE experiment SET status='PLANNED' WHERE id=900092");
    var adopted = http.post().uri(path).body(request).retrieve().body(OpalaAdoptionResponse.class);
    assertThat(adopted.adopted()).isTrue();
    assertThat(adopted.preparationUrl()).contains("learningCycleId=900002", "chainId=");
    assertThat(http.post().uri(path).body(request).retrieve().body(OpalaAdoptionResponse.class))
        .isEqualTo(adopted);
    assertThat(jdbc.queryForMap("SELECT * FROM learning_sales_cycle_v1 WHERE id=900002"))
        .isEqualTo(before);
    verify(app.getBean(ProcessRunService.class), times(1))
        .start(
            900004L,
            adopted.processDefinitionId(),
            new ProcessRunCommand(
                ((Number) before.get("chain_definition_id")).longValue(),
                900002L,
                "experiment:900092"));
    var repository = app.getBean(OpalaAdoptionRepository.class);
    assertThat(
            repository.permits(
                900002,
                900004,
                ((Number) before.get("chain_definition_id")).longValue(),
                adopted.processDefinitionId(),
                "experiment:900092"))
        .isTrue();
    assertThat(
            repository.permits(
                900002,
                900005,
                ((Number) before.get("chain_definition_id")).longValue(),
                adopted.processDefinitionId(),
                "experiment:900092"))
        .isFalse();
    assertThatThrownBy(
            () ->
                http.post()
                    .uri("/products/900005/cycles/900002/adoption")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity())
        .hasMessageContaining("404");
  }

  /** Monta a seleção completa como a tela, alterando apenas as versões solicitadas. */
  private CatalogActivationRequest activation(CatalogResponse catalog, Map<Long, Long> changes) {
    var selected = new HashMap<Long, Long>();
    var expected = new HashMap<Long, Long>();
    for (var item : catalog.items()) {
      expected.put(item.binding().id(), item.binding().activeVersionId());
      selected.put(
          item.binding().id(),
          changes.getOrDefault(item.binding().id(), item.binding().activeVersionId()));
    }
    return new CatalogActivationRequest(
        selected, expected, "Fixture", "Ativação ou recuperação local");
  }

  /** Cria tarefa sintética persistida, com agente, processo e ocorrência reais do vínculo. */
  private AgentTask task(CatalogBinding binding, long id, long instance, String source) {
    jdbc.update(
        "INSERT INTO agent_task(id,assigned_agent_id,process_definition_id,process_activity_id,source_reference,activity_instance_id,status) VALUES(?,?,?,?,?,?,'PENDING')",
        id,
        binding.agentId(),
        binding.processId(),
        binding.activityId(),
        source,
        instance);
    var t = new AgentTask();
    t.setId(id);
    var agent = new Agent();
    agent.setId(binding.agentId());
    agent.setAgentKey(binding.agentKey());
    t.setAssignedAgent(agent);
    t.setProcessDefinition(
        app.getBean(BusinessProcessDefinitionRepository.class)
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                CatalogoVivoService.PROCESS, "PUBLISHED")
            .orElseThrow());
    t.setProcessActivityId(binding.activityId());
    t.setSourceReference(source);
    t.setStatus("PENDING");
    var i = new BusinessProcessActivityInstance();
    i.setId(instance);
    t.setActivityInstance(i);
    return t;
  }

  /** Executa a fixação em transação real e conserva o bloqueio do objeto no banco da fixture. */
  private void pin(AgentTask task) {
    var transaction = new TransactionTemplate(app.getBean(PlatformTransactionManager.class));
    transaction.executeWithoutResult(
        s -> {
          service.pin(task);
          jdbc.update(
              "UPDATE agent_task SET status=?,blocker_action=? WHERE id=?",
              task.getStatus(),
              task.getBlockerAction(),
              task.getId());
        });
  }
}
