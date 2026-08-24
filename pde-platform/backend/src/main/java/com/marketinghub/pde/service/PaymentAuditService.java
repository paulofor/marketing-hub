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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
    public boolean recordVerifiedPayment(String productSlug, PepperPaidTransaction transaction) {
        PaymentAuditRecord candidate = new PaymentAuditRecord(
                PROVIDER,
                required(transaction.transactionId(), "Transação Pepper sem identificador"),
                required(productSlug, "Produto PDE não informado para auditoria financeira"),
                required(transaction.experienceVersion(), "Transação Pepper sem versão comercial"),
                required(transaction.offerHash(), "Transação Pepper sem oferta"),
                transaction.amount(),
                required(transaction.currency(), "Transação Pepper sem moeda"),
                required(transaction.paymentStatus(), "Transação Pepper sem status"),
                sha256(required(transaction.buyerEmail(), "Transação Pepper sem compradora")),
                null,
                Instant.now(),
                null,
                null);
        if (candidate.amountCents() == null || candidate.amountCents() <= 0) {
            throw new IllegalArgumentException("Transação Pepper sem valor auditável");
        }
        if (usesJdbcStorage()) {
            return insertOrValidateDatabaseRecord(candidate);
        }
        AtomicBoolean inserted = new AtomicBoolean(false);
        inMemoryRecords.compute(candidate.transactionId(), (transactionId, existing) -> {
            validateIdempotentReuse(existing, candidate);
            if (existing == null) {
                inserted.set(true);
                return candidate;
            }
            return existing;
        });
        return inserted.get();
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

    /** Confirma reembolso no provedor, atualiza a trilha uma vez e devolve o acesso correlacionado. */
    public RefundAuditResult recordVerifiedRefund(
            String productSlug, PepperTransactionSnapshot transaction) {
        String status = required(transaction.paymentStatus(), "Reembolso Pepper sem status").toLowerCase();
        if (!Set.of("refunded", "chargeback").contains(status)) {
            throw new IllegalArgumentException("Transação Pepper ainda não possui reembolso confirmado");
        }
        PaymentAuditRecord refund = new PaymentAuditRecord(
                PROVIDER,
                required(transaction.transactionId(), "Reembolso Pepper sem identificador"),
                required(productSlug, "Produto PDE não informado no reembolso"),
                required(transaction.experienceVersion(), "Reembolso Pepper sem versão comercial"),
                required(transaction.offerHash(), "Reembolso Pepper sem oferta"),
                transaction.amount(),
                required(transaction.currency(), "Reembolso Pepper sem moeda"),
                status,
                sha256(required(transaction.buyerEmail(), "Reembolso Pepper sem compradora")),
                null,
                Instant.now(),
                null,
                Instant.now());
        if (refund.amountCents() == null || refund.amountCents() <= 0) {
            throw new IllegalArgumentException("Reembolso Pepper sem valor auditável");
        }
        if (usesJdbcStorage()) {
            return recordDatabaseRefund(refund);
        }
        AtomicBoolean changed = new AtomicBoolean(false);
        PaymentAuditRecord updated = inMemoryRecords.compute(refund.transactionId(), (ignored, existing) -> {
            validateRefundReuse(existing, refund);
            if (existing.paymentStatus().equalsIgnoreCase(status)) {
                return existing;
            }
            changed.set(true);
            return existing.withRefund(status, refund.refundedAt());
        });
        return new RefundAuditResult(updated.accessReferenceHash(), changed.get(), updated.refundedAt());
    }

    /** Retorna um registro somente para testes do contrato idempotente sem expor uma API pública. */
    Optional<PaymentAuditRecord> findForTesting(String transactionId) {
        if (usesJdbcStorage()) {
            return loadDatabaseRecord(transactionId);
        }
        return Optional.ofNullable(inMemoryRecords.get(transactionId));
    }

    /** Insere a trilha financeira ou confere que o retry representa exatamente a mesma compra. */
    private boolean insertOrValidateDatabaseRecord(PaymentAuditRecord candidate) {
        String sql = "INSERT INTO pde_payment_audit "
                + "(provider, transaction_id, product_slug, experience_version, offer_hash, amount_cents, currency, payment_status, "
                + "buyer_reference_hash, verified_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidate.provider());
            statement.setString(2, candidate.transactionId());
            statement.setString(3, candidate.productSlug());
            statement.setString(4, candidate.experienceVersion());
            statement.setString(5, candidate.offerHash());
            statement.setInt(6, candidate.amountCents());
            statement.setString(7, candidate.currency());
            statement.setString(8, candidate.paymentStatus());
            statement.setString(9, candidate.buyerReferenceHash());
            statement.setTimestamp(10, Timestamp.from(candidate.verifiedAt()));
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            if (isConstraintViolation(ex)) {
                PaymentAuditRecord existing = loadDatabaseRecord(candidate.transactionId())
                        .orElseThrow(() -> new IllegalStateException("Conflito financeiro sem trilha recuperável", ex));
                validateIdempotentReuse(existing, candidate);
                return false;
            }
            log.error("Falha ao persistir auditoria financeira; provider={}", PROVIDER, ex);
            throw new IllegalStateException("Não foi possível persistir a auditoria financeira", ex);
        }
    }

    /** Lê uma compra pelo identificador idempotente do provedor. */
    private Optional<PaymentAuditRecord> loadDatabaseRecord(String transactionId) {
        String sql = "SELECT provider, transaction_id, product_slug, experience_version, offer_hash, amount_cents, currency, "
                + "payment_status, buyer_reference_hash, access_reference_hash, verified_at, access_released_at, refunded_at "
                + "FROM pde_payment_audit WHERE provider = ? AND transaction_id = ?";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PROVIDER);
            statement.setString(2, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Timestamp releasedAt = resultSet.getTimestamp("access_released_at");
                Timestamp refundedAt = resultSet.getTimestamp("refunded_at");
                return Optional.of(new PaymentAuditRecord(
                        resultSet.getString("provider"),
                        resultSet.getString("transaction_id"),
                        resultSet.getString("product_slug"),
                        resultSet.getString("experience_version"),
                        resultSet.getString("offer_hash"),
                        resultSet.getInt("amount_cents"),
                        resultSet.getString("currency"),
                        resultSet.getString("payment_status"),
                        resultSet.getString("buyer_reference_hash"),
                        resultSet.getString("access_reference_hash"),
                        resultSet.getTimestamp("verified_at").toInstant(),
                        releasedAt == null ? null : releasedAt.toInstant(),
                        refundedAt == null ? null : refundedAt.toInstant()));
            }
        } catch (SQLException ex) {
            log.error("Falha ao ler auditoria financeira; provider={}", PROVIDER, ex);
            throw new IllegalStateException("Não foi possível ler a auditoria financeira", ex);
        }
    }

    /** Atualiza o pagamento confirmado com trava de concorrência e retry idempotente. */
    private RefundAuditResult recordDatabaseRefund(PaymentAuditRecord refund) {
        PaymentAuditRecord existing = loadDatabaseRecord(refund.transactionId())
                .orElseThrow(() -> new IllegalArgumentException("Pagamento original não encontrado para reembolso"));
        validateRefundReuse(existing, refund);
        if (existing.paymentStatus().equalsIgnoreCase(refund.paymentStatus())) {
            return new RefundAuditResult(existing.accessReferenceHash(), false, existing.refundedAt());
        }
        String sql = "UPDATE pde_payment_audit SET payment_status = ?, refunded_at = ? "
                + "WHERE provider = ? AND transaction_id = ? AND payment_status = ?";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, refund.paymentStatus());
            statement.setTimestamp(2, Timestamp.from(refund.refundedAt()));
            statement.setString(3, PROVIDER);
            statement.setString(4, refund.transactionId());
            statement.setString(5, existing.paymentStatus());
            if (statement.executeUpdate() == 1) {
                return new RefundAuditResult(existing.accessReferenceHash(), true, refund.refundedAt());
            }
            PaymentAuditRecord concurrent = loadDatabaseRecord(refund.transactionId())
                    .orElseThrow(() -> new IllegalStateException("Reembolso perdeu a trilha financeira original"));
            validateRefundReuse(concurrent, refund);
            if (!concurrent.paymentStatus().equalsIgnoreCase(refund.paymentStatus())) {
                throw new IllegalStateException("Estado financeiro mudou durante a conciliação do reembolso");
            }
            return new RefundAuditResult(concurrent.accessReferenceHash(), false, concurrent.refundedAt());
        } catch (SQLException ex) {
            log.error(
                    "Falha ao persistir reembolso na auditoria financeira; provider={}, transactionId={}",
                    PROVIDER,
                    refund.transactionId(),
                    ex);
            throw new IllegalStateException("Não foi possível persistir a auditoria do reembolso", ex);
        }
    }

    /** Confere que o reembolso pertence exatamente à compra original já auditada. */
    private void validateRefundReuse(PaymentAuditRecord existing, PaymentAuditRecord refund) {
        if (existing == null) {
            throw new IllegalArgumentException("Pagamento original não encontrado para reembolso");
        }
        boolean samePayment = existing.productSlug().equals(refund.productSlug())
                && java.util.Objects.equals(existing.experienceVersion(), refund.experienceVersion())
                && existing.offerHash().equals(refund.offerHash())
                && existing.amountCents().equals(refund.amountCents())
                && existing.currency().equalsIgnoreCase(refund.currency())
                && existing.buyerReferenceHash().equals(refund.buyerReferenceHash());
        if (!samePayment) {
            throw new IllegalArgumentException("Reembolso Pepper diverge da compra original auditada");
        }
    }

    /** Compara todos os atributos que tornam um retry seguro e comercialmente equivalente. */
    private void validateIdempotentReuse(PaymentAuditRecord existing, PaymentAuditRecord candidate) {
        if (existing == null) {
            return;
        }
        boolean samePayment = existing.productSlug().equals(candidate.productSlug())
                && java.util.Objects.equals(existing.experienceVersion(), candidate.experienceVersion())
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
            String experienceVersion,
            String offerHash,
            Integer amountCents,
            String currency,
            String paymentStatus,
            String buyerReferenceHash,
            String accessReferenceHash,
            Instant verifiedAt,
            Instant accessReleasedAt,
            Instant refundedAt) {

        /** Vincula o hash do acesso sem alterar a identidade financeira original. */
        PaymentAuditRecord withReleasedAccess(String accessReferenceHash, Instant accessReleasedAt) {
            return new PaymentAuditRecord(
                    provider,
                    transactionId,
                    productSlug,
                    experienceVersion,
                    offerHash,
                    amountCents,
                    currency,
                    paymentStatus,
                    buyerReferenceHash,
                    accessReferenceHash,
                    verifiedAt,
                    accessReleasedAt,
                    refundedAt);
        }

        /** Atualiza somente o estado e o horário final do reembolso confirmado. */
        PaymentAuditRecord withRefund(String paymentStatus, Instant refundedAt) {
            return new PaymentAuditRecord(
                    provider,
                    transactionId,
                    productSlug,
                    experienceVersion,
                    offerHash,
                    amountCents,
                    currency,
                    paymentStatus,
                    buyerReferenceHash,
                    accessReferenceHash,
                    verifiedAt,
                    accessReleasedAt,
                    refundedAt);
        }
    }

    /** Expõe ao reconciliador somente a referência mínima para revogar o acesso uma vez. */
    public record RefundAuditResult(String accessReferenceHash, boolean newlyRecorded, Instant refundedAt) {}
}
