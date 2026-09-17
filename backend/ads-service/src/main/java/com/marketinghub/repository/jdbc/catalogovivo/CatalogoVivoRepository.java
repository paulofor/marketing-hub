package com.marketinghub.repository.jdbc.catalogovivo;

import com.marketinghub.catalogovivo.v1.service.catalog.*;
import com.marketinghub.catalogovivo.v1.service.pending.CatalogPromptResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Responsabilidade: persistir versões, vínculos e auditoria do Catálogo Vivo no banco principal.
 */
@Repository
@RequiredArgsConstructor
public class CatalogoVivoRepository {
  private final JdbcTemplate jdbc;
  private static final String BINDINGS =
      """
      SELECT b.*, p.id process_id,p.version_number process_version,a.activity_id,a.name activity_name,
             g.agent_key,g.nickname agent_name,t.code product_type_code
      FROM catalogo_vivo_binding_v1 b
      JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
      JOIN business_process_definition p ON p.id=a.process_definition_id
      JOIN agent g ON g.id=b.agent_id JOIN product_type_definition t ON t.id=b.product_type_id
      """;

  /** Serializa alterações e fixação de versões pelo processo, sem bloquear outros catálogos. */
  public void lockProcess(long processId) {
    jdbc.queryForObject(
        "SELECT id FROM business_process_definition WHERE id=? FOR UPDATE", Long.class, processId);
  }

  /** Lista vínculos do processo exato na ordem de suas atividades persistidas. */
  public List<CatalogBinding> bindings(long processId) {
    return jdbc.query(BINDINGS + " WHERE p.id=? ORDER BY a.id", (r, n) -> binding(r), processId);
  }

  /** Lê o estado confirmado após o lock, inclusive dentro de transações BPM REPEATABLE READ. */
  public List<CatalogBinding> currentBindings(long processId) {
    return jdbc.query(
        BINDINGS + " WHERE p.id=? ORDER BY a.id FOR UPDATE", (r, n) -> binding(r), processId);
  }

  /** Lê a versão confirmada sem reutilizar snapshot anterior ao lock do processo. */
  public Optional<CatalogVersion> currentVersion(long id) {
    return jdbc
        .query(
            "SELECT * FROM catalogo_vivo_prompt_version_v1 WHERE id=? LOCK IN SHARE MODE",
            (r, n) -> version(r),
            id)
        .stream()
        .findFirst();
  }

  /**
   * Filtra os textos do agente no SQL sem carregar históricos ou métricas dos outros executores.
   */
  public List<CatalogBinding> bindingsForAgent(String agentKey) {
    return jdbc.query(
        BINDINGS
            + " WHERE p.process_code='opala-commercial-preparation-v1' AND p.version_number=1 AND g.agent_key=? ORDER BY a.id",
        (r, n) -> binding(r),
        agentKey);
  }

  /** Consulta a identidade de um vínculo sem inferir processo pela versão mais recente. */
  public Optional<CatalogBinding> binding(long id) {
    return jdbc.query(BINDINGS + " WHERE b.id=?", (r, n) -> binding(r), id).stream().findFirst();
  }

  /** Converte o vínculo relacional para contrato interno tipado. */
  private CatalogBinding binding(ResultSet r) throws SQLException {
    return new CatalogBinding(
        r.getLong("id"),
        r.getLong("process_id"),
        r.getInt("process_version"),
        r.getLong("activity_definition_id"),
        r.getString("activity_id"),
        r.getString("activity_name"),
        r.getLong("agent_id"),
        r.getString("agent_key"),
        r.getString("agent_name"),
        r.getLong("product_type_id"),
        r.getString("product_type_code"),
        r.getString("executor_module"),
        r.getString("schema_id"),
        r.getString("schema_sha256"),
        (Long) r.getObject("active_version_id"));
  }

  /** Retorna todas as versões da atividade, incluindo rascunhos e versões já usadas. */
  public List<CatalogVersion> versions(long bindingId) {
    return jdbc.query(
        "SELECT * FROM catalogo_vivo_prompt_version_v1 WHERE binding_id=? ORDER BY version_number DESC",
        (r, n) -> version(r),
        bindingId);
  }

  /** Carrega uma versão pelo identificador imutável. */
  public Optional<CatalogVersion> version(long id) {
    return jdbc
        .query("SELECT * FROM catalogo_vivo_prompt_version_v1 WHERE id=?", (r, n) -> version(r), id)
        .stream()
        .findFirst();
  }

  /** Converte texto e datas da versão, preservando a ausência de revisão do rascunho. */
  private CatalogVersion version(ResultSet r) throws SQLException {
    return new CatalogVersion(
        r.getLong("id"),
        r.getLong("binding_id"),
        r.getInt("version_number"),
        r.getString("text_content"),
        r.getString("sha256"),
        r.getString("status"),
        r.getString("created_by"),
        r.getTimestamp("created_at").toInstant(),
        r.getString("reviewed_by"),
        r.getTimestamp("reviewed_at") == null ? null : r.getTimestamp("reviewed_at").toInstant(),
        r.getString("review_note"));
  }

  /** Cria um rascunho numerado sob lock do processo; não atualiza texto de versão anterior. */
  public long insertDraft(long bindingId, String text, String sha, String actor) {
    int number =
        jdbc
                .query(
                    "SELECT version_number FROM catalogo_vivo_prompt_version_v1 WHERE binding_id=? ORDER BY version_number DESC LIMIT 1 FOR UPDATE",
                    (r, n) -> r.getInt(1),
                    bindingId)
                .stream()
                .findFirst()
                .orElse(0)
            + 1;
    var key = new GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var s =
              c.prepareStatement(
                  "INSERT INTO catalogo_vivo_prompt_version_v1(binding_id,version_number,text_content,sha256,status,created_by,created_at) VALUES(?,?,?,?,'DRAFT',?,?)",
                  java.sql.Statement.RETURN_GENERATED_KEYS);
          s.setLong(1, bindingId);
          s.setInt(2, number);
          s.setString(3, text);
          s.setString(4, sha);
          s.setString(5, actor);
          s.setTimestamp(6, Timestamp.from(Instant.now()));
          return s;
        },
        key);
    return Objects.requireNonNull(key.getKey()).longValue();
  }

  /** Registra revisão somente uma vez, sem alterar o conteúdo ou seu hash. */
  public void review(long versionId, String actor, String reason) {
    jdbc.update(
        "UPDATE catalogo_vivo_prompt_version_v1 SET status='REVIEWED',reviewed_by=?,reviewed_at=?,review_note=? WHERE id=? AND status='DRAFT'",
        actor,
        Timestamp.from(Instant.now()),
        reason,
        versionId);
  }

  /** Seleciona versão da própria atividade; chave composta impede referência cruzada. */
  public void activate(long bindingId, long versionId) {
    jdbc.update(
        "UPDATE catalogo_vivo_binding_v1 SET active_version_id=? WHERE id=?", versionId, bindingId);
  }

  /** Acrescenta evento imutável de criação, revisão, ativação ou recuperação. */
  public void audit(long bindingId, long versionId, String action, String actor, String note) {
    jdbc.update(
        "INSERT INTO catalogo_vivo_audit_v1(binding_id,version_id,action,operator_name,note,created_at) VALUES(?,?,?,?,?,?)",
        bindingId,
        versionId,
        action,
        actor,
        note,
        Timestamp.from(Instant.now()));
  }

  /** Lista decisões da atividade em ordem inversa, limitadas à consulta administrativa. */
  public List<CatalogResponse.Event> events(long bindingId) {
    return jdbc.query(
        "SELECT * FROM catalogo_vivo_audit_v1 WHERE binding_id=? ORDER BY id DESC LIMIT 100",
        (r, n) ->
            new CatalogResponse.Event(
                r.getLong("id"),
                r.getLong("version_id"),
                r.getString("action"),
                r.getString("operator_name"),
                r.getString("note"),
                r.getTimestamp("created_at").toInstant()),
        bindingId);
  }

  /** Fixa a versão e a origem na criação da tarefa, sem permitir sobrescrita posterior. */
  public void pin(long taskId, long bindingId, long versionId, String source) {
    jdbc.update(
        "INSERT INTO catalogo_vivo_task_prompt_v1(task_id,binding_id,version_id,source_reference,fixed_at) VALUES(?,?,?,?,?)",
        taskId,
        bindingId,
        versionId,
        source,
        Timestamp.from(Instant.now()));
  }

  /** Recupera o texto fixado, inclusive quando outra versão já está ativa. */
  public Optional<CatalogPromptResponse> pinned(long taskId) {
    return jdbc
        .query(
            """
        SELECT v.*,b.schema_id,b.schema_sha256,b.executor_module,g.agent_key,a.activity_id,p.version_number process_version
        FROM catalogo_vivo_task_prompt_v1 s JOIN catalogo_vivo_prompt_version_v1 v ON v.id=s.version_id
        JOIN catalogo_vivo_binding_v1 b ON b.id=s.binding_id JOIN agent g ON g.id=b.agent_id
        JOIN business_process_activity_definition a ON a.id=b.activity_definition_id
        JOIN business_process_definition p ON p.id=a.process_definition_id WHERE s.task_id=?
        """,
            (r, n) ->
                new CatalogPromptResponse(
                    "DATABASE",
                    r.getLong("binding_id"),
                    r.getLong("id"),
                    r.getInt("version_number"),
                    r.getString("text_content"),
                    r.getString("sha256"),
                    r.getString("schema_id"),
                    r.getString("schema_sha256"),
                    r.getString("agent_key"),
                    r.getString("activity_id"),
                    r.getInt("process_version"),
                    r.getString("executor_module")),
            taskId)
        .stream()
        .findFirst();
  }

  /** Confere a origem congelada para impedir reaproveitamento da tarefa em outro experimento. */
  public String pinnedSource(long taskId) {
    return jdbc.queryForObject(
        "SELECT source_reference FROM catalogo_vivo_task_prompt_v1 WHERE task_id=?",
        String.class,
        taskId);
  }

  /**
   * Lista utilizações recentes da atividade com versão fixada e referência rastreável da tarefa.
   */
  public List<CatalogResponse.Usage> usages(long bindingId) {
    return jdbc.query(
        "SELECT s.task_id,s.version_id,v.version_number,s.source_reference,t.status,s.fixed_at FROM catalogo_vivo_task_prompt_v1 s JOIN agent_task t ON t.id=s.task_id JOIN catalogo_vivo_prompt_version_v1 v ON v.id=s.version_id WHERE s.binding_id=? ORDER BY s.fixed_at DESC LIMIT 30",
        (r, n) ->
            new CatalogResponse.Usage(
                r.getLong("task_id"),
                r.getLong("version_id"),
                r.getInt("version_number"),
                r.getString("source_reference"),
                r.getString("status"),
                r.getTimestamp("fixed_at").toInstant()),
        bindingId);
  }

  /** Conta usos operacionais do catálogo; não representa vendas ou aprovação comercial. */
  public long pinnedCount(long processId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM catalogo_vivo_task_prompt_v1 s JOIN agent_task t ON t.id=s.task_id WHERE t.process_definition_id=?",
        Long.class,
        processId);
  }

  /** Conta somente bloqueios de resolução persistidos, sem analisar logs ou prompts extensos. */
  public long failureCount(long processId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM agent_task WHERE process_definition_id=? AND blocker_action LIKE 'CATALOGO_VIVO:%'",
        Long.class, processId);
  }
}
