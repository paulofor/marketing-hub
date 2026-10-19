package com.marketinghub.aiworkerconnection;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Persiste comandos e provas de conexão do Worker IA sem armazenar credenciais. */
@Entity
@Table(name = "ai_worker_codex_connection")
@Getter
@NoArgsConstructor
public class AiWorkerConnection {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "verification_url", length = 100)
  private String verificationUrl;

  @Column(name = "user_code", length = 30)
  private String userCode;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "session_authenticated", nullable = false)
  private boolean sessionAuthenticated;

  @Column(name = "checked_at")
  private Instant checkedAt;

  /** Cria um comando com prazo absoluto, sem substituir comandos anteriores. */
  public AiWorkerConnection(Instant now, Instant expiresAt) {
    this.status = "REQUESTED";
    this.requestedAt = now;
    this.expiresAt = expiresAt;
  }

  /** Reserva o comando uma única vez para o executor. */
  public void start() {
    status = "STARTING";
  }

  /** Disponibiliza exclusivamente o código transitório emitido pelo provedor. */
  public void awaitConfirmation(String url, String code) {
    verificationUrl = url;
    userCode = code;
    status = "AWAITING_CONFIRMATION";
  }

  /** Encerra o comando e elimina o código que já não pode ser utilizado. */
  public void finish(String status, Instant now) {
    this.status = status;
    this.completedAt = now;
    this.userCode = null;
    this.verificationUrl = null;
  }

  /** Atualiza a prova independente de sessão na linha de controle do executor. */
  public void reportSession(boolean authenticated, Instant now) {
    sessionAuthenticated = authenticated;
    checkedAt = now;
  }
}
