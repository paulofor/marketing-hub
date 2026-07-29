package com.marketinghub.socialdistribution;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: armazenar uma conta social conectável para distribuição orgânica. */
@Entity
@Table(name = "social_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Rede social da conta. */
  @Enumerated(EnumType.STRING)
  @Column(name = "platform", length = 32, nullable = false)
  private SocialPlatform platform;

  /** Nome operacional exibido nas telas. */
  @Column(name = "display_name", length = 191, nullable = false)
  private String displayName;

  /** Identificador público da conta ou canal. */
  @Column(name = "handle", length = 191)
  private String handle;

  /** Identificador externo retornado pela plataforma, como channelId ou business account id. */
  @Column(name = "external_account_id", length = 191)
  private String externalAccountId;

  /** Modo de conexão exigido para publicar nessa conta. */
  @Enumerated(EnumType.STRING)
  @Column(name = "connection_mode", length = 32, nullable = false)
  private SocialConnectionMode connectionMode;

  /** Estado operacional da conexão. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 32, nullable = false)
  private SocialAccountStatus status;

  /** Escopos OAuth exigidos pela rede para publicar e ler métricas. */
  @Lob
  @Column(name = "required_scopes", columnDefinition = "LONGTEXT")
  private String requiredScopes;

  /** Observação operacional sobre credenciais, auditoria ou pendências de conexão. */
  @Lob
  @Column(name = "setup_notes", columnDefinition = "LONGTEXT")
  private String setupNotes;

  /** Data em que a conexão foi considerada pronta. */
  @Column(name = "connected_at")
  private Instant connectedAt;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
