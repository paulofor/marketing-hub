package com.marketinghub.repository.jdbc.chatgptads;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Centraliza persistência e reservas transacionais da integração ChatGPT Ads. */
@Repository
public class ChatgptAdsRepository {
  private final JdbcTemplate jdbc;

  /** Recebe a única conexão canônica do backend. */
  public ChatgptAdsRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Representa uma campanha sem interpretar seu estado de negócio. */
  public record Campaign(
      long id,
      long experimentId,
      String draft,
      String fingerprint,
      String authorization,
      String status,
      String remote,
      String metrics,
      Instant updatedAt) {}

  /** Representa o recibo e a reserva de uma operação externa. */
  public record Operation(
      long id,
      Long campaignId,
      String kind,
      String request,
      String response,
      String status,
      String lease,
      int attempts) {}

  /** Impede converter o canal de experimento que já foi encaminhado para publicação. */
  public boolean hasPublication(long experimentId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM chatgpt_ads_campaign WHERE experiment_id=? AND status<>'DRAFT'",
            Integer.class,
            experimentId)
        > 0;
  }

  /** Lista somente campanhas do filtro solicitado, em ordem estável. */
  public List<Campaign> list(Long experimentId) {
    return jdbc.query(
        "SELECT * FROM chatgpt_ads_campaign WHERE (? IS NULL OR experiment_id=?) ORDER BY id DESC LIMIT 100",
        this::campaign,
        experimentId,
        experimentId);
  }

  /** Serializa comandos concorrentes da campanha. */
  public Campaign lock(long id) {
    return jdbc.queryForObject(
        "SELECT * FROM chatgpt_ads_campaign WHERE id=? FOR UPDATE", this::campaign, id);
  }

  /** Carrega a campanha para projeção somente leitura. */
  public Campaign get(long id) {
    return jdbc.queryForObject("SELECT * FROM chatgpt_ads_campaign WHERE id=?", this::campaign, id);
  }

  /** Cria uma preparação imutável, sem herdar autorização de outro canal. */
  public long insert(long experimentId, String draft, String fingerprint, Instant now) {
    var keys = new GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var s =
              c.prepareStatement(
                  "INSERT INTO chatgpt_ads_campaign(experiment_id,draft_json,fingerprint,status,remote_json,created_at,updated_at) VALUES(?,?,?,'DRAFT','{}',?,?)",
                  java.sql.Statement.RETURN_GENERATED_KEYS);
          s.setLong(1, experimentId);
          s.setString(2, draft);
          s.setString(3, fingerprint);
          s.setTimestamp(4, Timestamp.from(now));
          s.setTimestamp(5, Timestamp.from(now));
          return s;
        },
        keys);
    return keys.getKey().longValue();
  }

  /** Substitui somente rascunho local e exige nova decisão financeira. */
  public void edit(long id, String draft, String fingerprint, Instant now) {
    jdbc.update(
        "UPDATE chatgpt_ads_campaign SET draft_json=?,fingerprint=?,authorization_json=NULL,updated_at=? WHERE id=?",
        draft,
        fingerprint,
        Timestamp.from(now),
        id);
  }

  /** Identifica a falha mais recente para permitir pausa mesmo com autorização vencida. */
  public String lastFailedKind(long campaignId) {
    var kinds =
        jdbc.queryForList(
            "SELECT kind FROM chatgpt_ads_operation WHERE campaign_id=? AND status='FAILED' ORDER BY id DESC LIMIT 1",
            String.class,
            campaignId);
    return kinds.isEmpty() ? null : kinds.getFirst();
  }

  /** Reabre a mesma operação para preservar sua chave idempotente após a recuperação. */
  public void retry(long campaignId) {
    jdbc.update(
        "UPDATE chatgpt_ads_operation SET status='PENDING',attempts=0,lease_token=NULL,lease_until=NULL WHERE campaign_id=? AND status='FAILED' ORDER BY id DESC LIMIT 1",
        campaignId);
  }

  /** Persiste uma decisão com o fingerprint exato e sem alterar o orçamento. */
  public void authorize(long id, String authorization, Instant now) {
    jdbc.update(
        "UPDATE chatgpt_ads_campaign SET authorization_json=?,updated_at=? WHERE id=?",
        authorization,
        Timestamp.from(now),
        id);
  }

  /** Persiste estado, identidade externa e erro sem apagar histórico de operações. */
  public void state(long id, String status, String remote, Instant now) {
    jdbc.update(
        "UPDATE chatgpt_ads_campaign SET status=?,remote_json=?,updated_at=? WHERE id=?",
        status,
        remote,
        Timestamp.from(now),
        id);
  }

  /** Persiste métricas como fotografia do provedor, distinta de compras conciliadas. */
  public void metrics(long id, String metrics, Instant now) {
    jdbc.update(
        "UPDATE chatgpt_ads_campaign SET metrics_json=?,updated_at=? WHERE id=?",
        metrics,
        Timestamp.from(now),
        id);
  }

  /** Lê somente fatos públicos da conexão, nunca suas chaves. */
  public String account() {
    return jdbc.queryForObject(
        "SELECT details_json FROM chatgpt_ads_connection WHERE id=1", String.class);
  }

  /** Serializa a fila global e torna repetição de comandos idempotente. */
  public void lockQueue() {
    jdbc.queryForObject("SELECT id FROM chatgpt_ads_connection WHERE id=1 FOR UPDATE", Long.class);
  }

  /** Salva a resposta segura de identificação da conta. */
  public void account(String json) {
    jdbc.update("UPDATE chatgpt_ads_connection SET details_json=? WHERE id=1", json);
  }

  /** Enfileira no máximo uma operação vigente do mesmo tipo e campanha. */
  public void enqueue(Long campaignId, String kind, String request, Instant now) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM chatgpt_ads_operation WHERE (campaign_id=? OR (campaign_id IS NULL AND ? IS NULL)) AND kind=? AND status IN ('PENDING','RUNNING')",
            Integer.class,
            campaignId,
            campaignId,
            kind);
    if (count != null && count > 0) return;
    jdbc.update(
        "INSERT INTO chatgpt_ads_operation(campaign_id,kind,request_json,status,attempts,created_at) VALUES(?,?,?,'PENDING',0,?)",
        campaignId,
        kind,
        request,
        Timestamp.from(now));
  }

  /** Reserva uma operação ou recupera a mesma chamada após expiração do lease. */
  public Operation pending(Instant now) {
    var rows =
        jdbc.query(
            "SELECT * FROM chatgpt_ads_operation WHERE status='PENDING' OR (status='RUNNING' AND lease_until<?) ORDER BY CASE WHEN kind='PAUSE' THEN 0 ELSE 1 END,id LIMIT 1 FOR UPDATE",
            this::operation,
            Timestamp.from(now));
    return rows.isEmpty() ? null : rows.getFirst();
  }

  /** Grava lease exclusivo antes de expor qualquer chamada externa. */
  public void lease(long id, String lease, Instant until) {
    jdbc.update(
        "UPDATE chatgpt_ads_operation SET status='RUNNING',lease_token=?,lease_until=?,attempts=attempts+1 WHERE id=?",
        lease,
        Timestamp.from(until),
        id);
  }

  /** Serializa o callback e impede aplicar a resposta duas vezes. */
  public Operation lockOperation(long id) {
    return jdbc.queryForObject(
        "SELECT * FROM chatgpt_ads_operation WHERE id=? FOR UPDATE", this::operation, id);
  }

  /** Preserva resposta e classificação terminal da operação. */
  public void finish(long id, String status, String response, Instant now) {
    jdbc.update(
        "UPDATE chatgpt_ads_operation SET status=?,response_json=?,finished_at=? WHERE id=?",
        status,
        response,
        Timestamp.from(now),
        id);
  }

  /** Inclui campanhas pausadas até a fotografia final, sem consultar JSON específico do banco. */
  public List<Campaign> monitored() {
    return jdbc.query(
        "SELECT * FROM chatgpt_ads_campaign WHERE status IN ('ACTIVE','AWAITING_REVIEW','READY','ACTIVATING','PAUSING') OR (status='PAUSED' AND (metrics_json IS NULL OR metrics_json NOT LIKE '%\"finalSyncedAt\":%')) ORDER BY id LIMIT 100",
        this::campaign);
  }

  /** Verifica se a campanha já possui operação em voo. */
  public boolean busy(long id) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM chatgpt_ads_operation WHERE campaign_id=? AND status IN ('PENDING','RUNNING')",
            Integer.class,
            id)
        > 0;
  }

  /** Converte colunas canônicas da campanha para o registro interno. */
  private Campaign campaign(ResultSet r, int row) throws SQLException {
    return new Campaign(
        r.getLong("id"),
        r.getLong("experiment_id"),
        r.getString("draft_json"),
        r.getString("fingerprint"),
        r.getString("authorization_json"),
        r.getString("status"),
        r.getString("remote_json"),
        r.getString("metrics_json"),
        r.getTimestamp("updated_at").toInstant());
  }

  /** Converte a operação sem interpretar seus efeitos de negócio. */
  private Operation operation(ResultSet r, int row) throws SQLException {
    long cid = r.getLong("campaign_id");
    Long campaignId = r.wasNull() ? null : cid;
    return new Operation(
        r.getLong("id"),
        campaignId,
        r.getString("kind"),
        r.getString("request_json"),
        r.getString("response_json"),
        r.getString("status"),
        r.getString("lease_token"),
        r.getInt("attempts"));
  }
}
