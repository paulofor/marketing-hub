package com.marketinghub.pde.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Mantém trilha financeira idempotente sem persistir e-mail ou token reutilizável da cliente. */
@Service
public class PaymentAuditService {
    private static final Logger log = LoggerFactory.getLogger(PaymentAuditService.class);
    private static final String PROVIDER = "PEPPER";

    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final Map<String, PaymentAuditRecord> inMemoryRecords = new ConcurrentHashMap<>();

    /** Recebe as credenciais do mesmo banco operacional usado pela plataforma PDE. */
    public PaymentAuditService(
            @Value("${pde.access.jdbc-url:}") String jdbcUrl,
            @Value("${pde.access.jdbc-username:}") String jdbcUsername,
            @Value("${pde.access.jdbc-password:}") String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    /** Registra a comprovação do provedor e bloqueia reutilização incompatível da mesma transação. */
    public void recordVerifiedPayment(String productSlug, PepperPaidTransaction transaction) {
        PaymentAuditRecord candidate = new PaymentAuditRecord(
                PROVIDER,
                required(transaction.transactionId(), "Transação Pepper sem identificador"),
                required(productSlug, "Produto PDE não informado para auditoria financeira"),
                required(transaction.offerHash(), "Transação Pepper sem oferta"),
                transaction.amount(),
                required(transaction.currency(), "Transação Pepper sem moeda"),
                required(transaction.paymentStatus(), "Transação Pepper sem status"),
                sha256(required(transaction.buyerEmail(), "Transação Pepper sem compradora")),
                null,
                Instant.now(),
                null);
        if (candidate.amountCents() == null || candidate.amountCents() <= 0) {
            throw new IllegalArgumentException("Transação Pepper sem valor auditável");
        }
        if (usesJdbcStorage()) {
            insertOrValidateDatabaseRecord(candidate);
            return;
        }
        inMemoryRecords.compute(candidate.transactionId(), (transactionId, existing) -> {
            validateIdempotentReuse(existing, candidate);
            return existing == null ? candidate : existing;
        });
    }

    /** Vincula a liberação ao pagamento usando somente hash não reutilizável do token. */
    public void linkReleasedAccess(String transactionId, String accessToken) {
        String tokenHash = sha256(required(accessToken, "Acesso PDE não informado para auditoria financeira"));
        Instant releasedAt = Instant.now();
        if (usesJdbcStorage()) {
            String sql = "UPDATE pde_payment_audit SET access_reference_hash = ?, access_released_at = ? "
                    + "WHERE provider = ? AND transaction_id = ?";
            try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, tokenHash);
                statement.setTimestamp(2, Timestamp.from(releasedAt));
                statement.setString(3, PROVIDER);
                statement.setString(4, transactionId);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Pagamento auditado não encontrado para vincular o acesso");
                }
                return;
            } catch (SQLException ex) {
                log.error("Falha ao vincular acesso à auditoria financeira; provider={}", PROVIDER, ex);
                throw new IllegalStateException("Não foi possível concluir a auditoria financeira do acesso", ex);
            }
        }
        inMemoryRecords.compute(transactionId, (ignored, existing) -> {
            if (existing == null) {
                throw new IllegalStateException("Pagamento auditado não encontrado para vincular o acesso");
            }
            if (existing.accessReferenceHash() != null && !existing.accessReferenceHash().equals(tokenHash)) {
                throw new IllegalArgumentException("Transação Pepper já vinculada a outro acesso");
            }
            return existing.withReleasedAccess(tokenHash, releasedAt);
        });
    }

    /** Retorna um registro somente para testes do contrato idempotente sem expor uma API pública. */
    Optional<PaymentAuditRecord> findForTesting(String transactionId) {
        if (usesJdbcStorage()) {
            return loadDatabaseRecord(transactionId);
        }
        return Optional.ofNullable(inMemoryRecords.get(transactionId));
    }

    /** Insere a trilha financeira ou confere que o retry representa exatamente a mesma compra. */
    private void insertOrValidateDatabaseRecord(PaymentAuditRecord candidate) {
        String sql = "INSERT INTO pde_payment_audit "
                + "(provider, transaction_id, product_slug, offer_hash, amount_cents, currency, payment_status, "
                + "buyer_reference_hash, verified_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidate.provider());
            statement.setString(2, candidate.transactionId());
            statement.setString(3, candidate.productSlug());
            statement.setString(4, candidate.offerHash());
            statement.setInt(5, candidate.amountCents());
            statement.setString(6, candidate.currency());
            statement.setString(7, candidate.paymentStatus());
            statement.setString(8, candidate.buyerReferenceHash());
            statement.setTimestamp(9, Timestamp.from(candidate.verifiedAt()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (isConstraintViolation(ex)) {
                PaymentAuditRecord existing = loadDatabaseRecord(candidate.transactionId())
                        .orElseThrow(() -> new IllegalStateException("Conflito financeiro sem trilha recuperável", ex));
                validateIdempotentReuse(existing, candidate);
                return;
            }
            log.error("Falha ao persistir auditoria financeira; provider={}", PROVIDER, ex);
            throw new IllegalStateException("Não foi possível persistir a auditoria financeira", ex);
        }
    }

    /** Lê uma compra pelo identificador idempotente do provedor. */
    private Optional<PaymentAuditRecord> loadDatabaseRecord(String transactionId) {
        String sql = "SELECT provider, transaction_id, product_slug, offer_hash, amount_cents, currency, "
                + "payment_status, buyer_reference_hash, access_reference_hash, verified_at, access_released_at "
                + "FROM pde_payment_audit WHERE provider = ? AND transaction_id = ?";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PROVIDER);
            statement.setString(2, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Timestamp releasedAt = resultSet.getTimestamp("access_released_at");
                return Optional.of(new PaymentAuditRecord(
                        resultSet.getString("provider"),
                        resultSet.getString("transaction_id"),
                        resultSet.getString("product_slug"),
                        resultSet.getString("offer_hash"),
                        resultSet.getInt("amount_cents"),
                        resultSet.getString("currency"),
                        resultSet.getString("payment_status"),
                        resultSet.getString("buyer_reference_hash"),
                        resultSet.getString("access_reference_hash"),
                        resultSet.getTimestamp("verified_at").toInstant(),
                        releasedAt == null ? null : releasedAt.toInstant()));
            }
        } catch (SQLException ex) {
            log.error("Falha ao ler auditoria financeira; provider={}", PROVIDER, ex);
            throw new IllegalStateException("Não foi possível ler a auditoria financeira", ex);
        }
    }

    /** Compara todos os atributos que tornam um retry seguro e comercialmente equivalente. */
    private void validateIdempotentReuse(PaymentAuditRecord existing, PaymentAuditRecord candidate) {
        if (existing == null) {
            return;
        }
        boolean samePayment = existing.productSlug().equals(candidate.productSlug())
                && existing.offerHash().equals(candidate.offerHash())
                && existing.amountCents().equals(candidate.amountCents())
                && existing.currency().equalsIgnoreCase(candidate.currency())
                && existing.paymentStatus().equalsIgnoreCase(candidate.paymentStatus())
                && existing.buyerReferenceHash().equals(candidate.buyerReferenceHash());
        if (!samePayment) {
            throw new IllegalArgumentException("Transação Pepper já utilizada com contrato financeiro diferente");
        }
    }

    /** Identifica violação de unicidade de modo compatível com MySQL e H2 de teste. */
    private boolean isConstraintViolation(SQLException ex) {
        return ex.getErrorCode() == 1062 || (ex.getSQLState() != null && ex.getSQLState().startsWith("23"));
    }

    /** Abre a conexão JDBC sem registrar credenciais nos logs. */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /** Informa se a auditoria deve ser durável no banco operacional. */
    private boolean usesJdbcStorage() {
        return jdbcUrl != null && !jdbcUrl.isBlank();
    }

    /** Exige valor textual para impedir trilhas financeiras ambíguas. */
    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /** Produz referência não reutilizável para conciliar auditoria sem guardar PII ou credencial. */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Algoritmo de hash indisponível para auditoria financeira", ex);
            throw new IllegalStateException("Não foi possível proteger a referência financeira", ex);
        }
    }

    /** Representa a trilha mínima necessária para conciliação e prevenção de liberação duplicada. */
    record PaymentAuditRecord(
            String provider,
            String transactionId,
            String productSlug,
            String offerHash,
            Integer amountCents,
            String currency,
            String paymentStatus,
            String buyerReferenceHash,
            String accessReferenceHash,
            Instant verifiedAt,
            Instant accessReleasedAt) {

        /** Vincula o hash do acesso sem alterar a identidade financeira original. */
        PaymentAuditRecord withReleasedAccess(String accessReferenceHash, Instant accessReleasedAt) {
            return new PaymentAuditRecord(
                    provider,
                    transactionId,
                    productSlug,
                    offerHash,
                    amountCents,
                    currency,
                    paymentStatus,
                    buyerReferenceHash,
                    accessReferenceHash,
                    verifiedAt,
                    accessReleasedAt);
        }
    }
}
