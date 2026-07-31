package com.marketinghub.repository.jpa.whatsapp;

import com.marketinghub.whatsapp.WhatsAppMessage;
import com.marketinghub.whatsapp.WhatsAppMessageDirection;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório JPA para consultar mensagens e conversas do WhatsApp. */
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
  /** Busca uma mensagem pelo identificador retornado pela Meta. */
  Optional<WhatsAppMessage> findByMessageId(String messageId);

  /** Lista mensagens por direção em ordem cronológica reversa de criação. */
  Page<WhatsAppMessage> findByDirectionOrderByCreatedAtDesc(
      WhatsAppMessageDirection direction, Pageable pageable);

  /** Lista todas as mensagens em ordem cronológica reversa de criação. */
  Page<WhatsAppMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

  /** Lista mensagens de um contato específico em ordem cronológica reversa de criação. */
  @Query(
      """
      select m from WhatsAppMessage m
      where (:direction is null or m.direction = :direction)
        and (
          (m.direction = com.marketinghub.whatsapp.WhatsAppMessageDirection.INBOUND and m.fromNumber = :contactNumber)
          or (m.direction = com.marketinghub.whatsapp.WhatsAppMessageDirection.OUTBOUND and m.toNumber = :contactNumber)
        )
      order by m.createdAt desc
      """)
  Page<WhatsAppMessage> findByContactNumberAndDirection(
      @Param("contactNumber") String contactNumber,
      @Param("direction") WhatsAppMessageDirection direction,
      Pageable pageable);

  /** Resume conversas agrupando mensagens por conta e telefone do contato. */
  @Query(
      value =
          """
          SELECT
              MIN(m.id) AS id,
              m.account_id AS accountId,
              a.display_name AS accountDisplayName,
              CASE WHEN m.direction = 'INBOUND' THEN m.from_number ELSE m.to_number END AS contactNumber,
              MAX(COALESCE(m.message_timestamp, m.sent_at, m.received_at, m.created_at)) AS lastMessageAt,
              SUM(CASE WHEN m.direction = 'INBOUND' THEN 1 ELSE 0 END) AS inboundCount,
              SUM(CASE WHEN m.direction = 'OUTBOUND' THEN 1 ELSE 0 END) AS outboundCount,
              SUM(
                  CASE
                    WHEN m.direction = 'INBOUND'
                     AND COALESCE(m.message_timestamp, m.sent_at, m.received_at, m.created_at) >
                         COALESCE((
                             SELECT MAX(COALESCE(o.message_timestamp, o.sent_at, o.received_at, o.created_at))
                             FROM whatsapp_message o
                             WHERE o.account_id = m.account_id
                               AND o.direction = 'OUTBOUND'
                               AND o.to_number = CASE WHEN m.direction = 'INBOUND' THEN m.from_number ELSE m.to_number END
                         ), TIMESTAMP('1970-01-01 00:00:00'))
                    THEN 1 ELSE 0
                  END
              ) AS pendingInboundCount
          FROM whatsapp_message m
          JOIN whatsapp_account a ON a.id = m.account_id
          WHERE CASE WHEN m.direction = 'INBOUND' THEN m.from_number ELSE m.to_number END IS NOT NULL
          GROUP BY m.account_id, a.display_name, CASE WHEN m.direction = 'INBOUND' THEN m.from_number ELSE m.to_number END
          ORDER BY lastMessageAt DESC
          """,
      countQuery =
          """
          SELECT COUNT(*)
          FROM (
              SELECT m.account_id, CASE WHEN m.direction = 'INBOUND' THEN m.from_number ELSE m.to_number END AS contactNumber
              FROM whatsapp_message m
              WHERE CASE WHEN m.direction = 'INBOUND' THEN m.from_number ELSE m.to_number END IS NOT NULL
              GROUP BY m.account_id, CASE WHEN m.direction = 'INBOUND' THEN m.from_number ELSE m.to_number END
          ) grouped_conversations
          """,
      nativeQuery = true)
  Page<WhatsAppConversationProjection> findConversationSummaries(Pageable pageable);

  /** Projeção agregada de uma conversa do WhatsApp. */
  interface WhatsAppConversationProjection {
    /** Retorna um identificador estável derivado da conversa. */
    Long getId();

    /** Retorna a conta WhatsApp associada à conversa. */
    Long getAccountId();

    /** Retorna o nome interno da conta WhatsApp. */
    String getAccountDisplayName();

    /** Retorna o telefone do contato da conversa. */
    String getContactNumber();

    /** Retorna a data da última mensagem trocada na conversa. */
    Instant getLastMessageAt();

    /** Retorna a quantidade de mensagens recebidas do contato. */
    Long getInboundCount();

    /** Retorna a quantidade de mensagens enviadas ao contato. */
    Long getOutboundCount();

    /** Retorna mensagens recebidas depois da última resposta enviada. */
    Long getPendingInboundCount();
  }
}
