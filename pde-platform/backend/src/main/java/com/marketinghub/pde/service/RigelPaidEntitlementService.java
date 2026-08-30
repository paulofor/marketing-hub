package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.MercadoPagoEntitlementRequest;
import com.marketinghub.pde.model.AccessGrant;
import java.math.BigDecimal;
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
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Controla o entitlement pago do Rigel a partir da auditoria autoritativa do Mercado Pago. */
@Service
public class RigelPaidEntitlementService {
    public static final String PRODUCT_SLUG = "kit-whatsapp-pronto";
    public static final String EXPERIENCE_VERSION = "kit-whatsapp-pronto-pde-v2";
    public static final String PAID_SOURCE = "MERCADO_PAGO";
    public static final String REFUNDED_SOURCE = "MERCADO_PAGO_REFUNDED";
    private static final String PROVIDER = "MERCADO_PAGO";
    private static final String OFFER_REFERENCE = "experiment:89";
    private static final long PRODUCT_ID = 9L;
    private static final long EXPERIMENT_ID = 89L;
    private static final int AMOUNT_CENTS = 34_900;
    private static final String CURRENCY = "BRL";
    private static final Logger log = LoggerFactory.getLogger(RigelPaidEntitlementService.class);

    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final Map<String, PaymentEntitlement> inMemoryPayments = new ConcurrentHashMap<>();

    /** Recebe o banco operacional que compartilha a trilha financeira confirmada pelo backend principal. */
    public RigelPaidEntitlementService(
            @Value("${pde.access.jdbc-url:}") String jdbcUrl,
            @Value("${pde.access.jdbc-username:}") String jdbcUsername,
            @Value("${pde.access.jdbc-password:}") String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    /** Registra um estado financeiro fictício e segregado para a homologação local autenticada. */
    public InternalPaymentResult recordInternalQaPayment(
            String email, String transactionId, String paymentStatus, String experienceVersion) {
        String normalizedEmail = normalizeEmail(email);
        if (!normalizedEmail.endsWith("@sandbox.local")) {
            throw new IllegalArgumentException("Pagamento de homologação aceita somente e-mail sandbox.local");
        }
        requireExactExperience(experienceVersion);
        String normalizedStatus = normalizePaymentStatus(paymentStatus);
        PaymentEntitlement candidate = new PaymentEntitlement(
                required(transactionId, "Identificador do pagamento de homologação não informado"),
                PRODUCT_SLUG,
                EXPERIENCE_VERSION,
                OFFER_REFERENCE,
                AMOUNT_CENTS,
                CURRENCY,
                normalizedStatus,
                sha256(normalizedEmail),
                null,
                Instant.now(),
                isRefunded(normalizedStatus) ? Instant.now() : null);
        PaymentWriteResult result = usesJdbcStorage()
                ? recordDatabasePayment(candidate)
                : recordInMemoryPayment(candidate);
        return new InternalPaymentResult(
                result.payment().transactionId(),
                result.payment().paymentStatus(),
                result.created() ? "RECORDED" : "DUPLICATE_OR_UPDATED",
                result.payment().verifiedAt().toString());
    }

    /** Valida e persiste a confirmação recém-consultada na API autoritativa do Mercado Pago. */
    public PaymentReconciliationResult recordVerifiedPayment(MercadoPagoEntitlementRequest request) {
        PaymentEntitlement candidate = verifiedCandidate(request);
        PaymentWriteResult result = usesJdbcStorage()
                ? recordDatabasePayment(candidate)
                : recordInMemoryPayment(candidate);
        log.info(
                "Entitlement financeiro do Rigel reconciliado; transactionId={}, status={}, result={}",
                candidate.transactionId(),
                candidate.paymentStatus(),
                result.created() ? "RECORDED" : "DUPLICATE_OR_UPDATED");
        return new PaymentReconciliationResult(
                candidate.transactionId(),
                candidate.paymentStatus(),
                result.created() ? "RECORDED" : "DUPLICATE_OR_UPDATED",
                result.payment().verifiedAt().toString());
    }

    /** Localiza uma compra aprovada, vincula-a ao token uma única vez e devolve sua referência. */
    public PaidClaim claimApprovedPayment(String email, String accessToken) {
        PaymentEntitlement payment = approvedPayment(email);
        String accessHash = sha256(required(accessToken, "Token de acesso não informado"));
        PaymentEntitlement claimed = payment.accessReferenceHash() == null
                ? linkAccess(payment, accessHash)
                : payment;
        if (!accessHash.equals(claimed.accessReferenceHash())) {
            throw new SecurityException("Pagamento já vinculado a outro acesso do Kit WhatsApp Pronto");
        }
        return toPaidClaim(claimed);
    }

    /** Confirma a compra antes de qualquer grant ser criado, sem reservar ou expor um token. */
    public PaidClaim requireApprovedPayment(String email) {
        PaymentEntitlement payment = approvedPayment(email);
        return toPaidClaim(payment);
    }

    /** Exige compra aprovada, versão exata e vínculo com o token em toda fronteira paga do Rigel. */
    public void requireActiveAccess(AccessGrant grant) {
        requireExactExperience(grant.getExperienceVersion());
        if ("INTERNAL_QA".equalsIgnoreCase(grant.getSource())) {
            return;
        }
        if (!PAID_SOURCE.equalsIgnoreCase(grant.getSource())
                && !REFUNDED_SOURCE.equalsIgnoreCase(grant.getSource())) {
            throw new SecurityException("Acesso do Kit não foi originado por pagamento confirmado");
        }
        PaymentEntitlement payment = latestPayment(grant.getEmail())
                .orElseThrow(() -> new SecurityException(
                        "Pagamento vigente do Kit WhatsApp Pronto não encontrado"));
        requireApproved(payment);
        if (!sha256(grant.getToken()).equals(payment.accessReferenceHash())) {
            throw new SecurityException("Pagamento não corresponde ao token desta área do Kit");
        }
    }

    /** Confirma que o reembolso mais recente pertence exatamente ao token atualmente liberado. */
    public boolean shouldRevokeAccess(String email, String transactionId, String accessToken) {
        return findConfirmedRefund(email, transactionId, accessToken).isPresent();
    }

    /** Retorna o reembolso exato e seus campos comerciais quando ele pertence ao acesso atual. */
    public Optional<RefundClaim> findConfirmedRefund(
            String email, String transactionId, String accessToken) {
        PaymentEntitlement latest = latestPayment(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pagamento do reembolso não encontrado para esta compradora"));
        if (!latest.transactionId().equals(required(
                transactionId, "Identificador do reembolso não informado"))) {
            return Optional.empty();
        }
        if (!isRefunded(latest.paymentStatus())) {
            throw new IllegalArgumentException("Transação informada ainda não possui reembolso confirmado");
        }
        boolean sameAccess = latest.accessReferenceHash() != null
                && MessageDigest.isEqual(
                        latest.accessReferenceHash().getBytes(StandardCharsets.UTF_8),
                        sha256(required(accessToken, "Token do acesso reembolsado não informado"))
                                .getBytes(StandardCharsets.UTF_8));
        if (!sameAccess) {
            return Optional.empty();
        }
        return Optional.of(new RefundClaim(
                latest.transactionId(),
                BigDecimal.valueOf(latest.amountCents(), 2),
                latest.currency(),
                EXPERIMENT_ID,
                latest.paymentStatus(),
                latest.refundedAt() == null ? latest.verifiedAt() : latest.refundedAt()));
    }

    /** Converte o registro financeiro aprovado nos correlatores exigidos pelos eventos comerciais. */
    private PaidClaim toPaidClaim(PaymentEntitlement payment) {
        return new PaidClaim(
                payment.transactionId(),
                payment.verifiedAt(),
                BigDecimal.valueOf(payment.amountCents(), 2),
                payment.currency(),
                EXPERIMENT_ID);
    }

    /** Informa se o produto informado usa a guarda comercial específica do Rigel. */
    public boolean supports(String productSlug) {
        return PRODUCT_SLUG.equals(productSlug);
    }

    /** Confere a versão comercial imutável aprovada para o Kit. */
    public void requireExactExperience(String experienceVersion) {
        if (!EXPERIENCE_VERSION.equals(experienceVersion)) {
            throw new SecurityException("A compra não corresponde à versão paga vigente do Kit WhatsApp Pronto");
        }
    }

    /** Grava ou atualiza o estado financeiro local preservando idempotência da transação. */
    private PaymentWriteResult recordInMemoryPayment(PaymentEntitlement candidate) {
        PaymentEntitlement existing = inMemoryPayments.get(candidate.transactionId());
        validateSameContract(existing, candidate);
        validateStatusTransition(existing, candidate);
        if (existing == null) {
            inMemoryPayments.put(candidate.transactionId(), candidate);
            return new PaymentWriteResult(candidate, true);
        }
        if (existing.paymentStatus().equals(candidate.paymentStatus())) {
            return new PaymentWriteResult(existing, false);
        }
        PaymentEntitlement updated = existing.withStatus(
                candidate.paymentStatus(), candidate.verifiedAt(), candidate.refundedAt());
        inMemoryPayments.put(candidate.transactionId(), updated);
        return new PaymentWriteResult(updated, false);
    }

    /** Grava ou atualiza o estado financeiro no MySQL sem criar duas linhas para a mesma transação. */
    private PaymentWriteResult recordDatabasePayment(PaymentEntitlement candidate) {
        Optional<PaymentEntitlement> existing = loadPaymentByTransaction(candidate.transactionId());
        if (existing.isPresent()) {
            return updateDatabasePayment(existing.get(), candidate);
        }
        String sql = "INSERT INTO pde_payment_audit "
                + "(provider, transaction_id, product_slug, experience_version, offer_hash, amount_cents, currency, "
                + "payment_status, buyer_reference_hash, verified_at, refunded_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPaymentInsert(statement, candidate);
            statement.executeUpdate();
            return new PaymentWriteResult(candidate, true);
        } catch (SQLException ex) {
            if (isConstraintViolation(ex)) {
                PaymentEntitlement concurrent = loadPaymentByTransaction(candidate.transactionId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Conflito financeiro sem trilha recuperável do Mercado Pago", ex));
                return updateDatabasePayment(concurrent, candidate);
            }
            log.error(
                    "Falha ao persistir pagamento do Rigel; transactionId={}",
                    candidate.transactionId(),
                    ex);
            throw new IllegalStateException("Não foi possível registrar o pagamento do Rigel", ex);
        }
    }

    /** Atualiza somente o estado mutável de uma transação já comprovada com o mesmo contrato. */
    private PaymentWriteResult updateDatabasePayment(
            PaymentEntitlement existing, PaymentEntitlement candidate) {
        validateSameContract(existing, candidate);
        validateStatusTransition(existing, candidate);
        if (existing.paymentStatus().equals(candidate.paymentStatus())) {
            return new PaymentWriteResult(existing, false);
        }
        String sql = "UPDATE pde_payment_audit SET payment_status = ?, verified_at = ?, refunded_at = ? "
                + "WHERE provider = ? AND transaction_id = ?";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidate.paymentStatus());
            statement.setTimestamp(2, Timestamp.from(candidate.verifiedAt()));
            statement.setTimestamp(3, toTimestamp(candidate.refundedAt()));
            statement.setString(4, PROVIDER);
            statement.setString(5, candidate.transactionId());
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Pagamento do Rigel desapareceu durante a atualização");
            }
            return new PaymentWriteResult(
                    existing.withStatus(
                            candidate.paymentStatus(), candidate.verifiedAt(), candidate.refundedAt()),
                    false);
        } catch (SQLException ex) {
            log.error(
                    "Falha ao atualizar pagamento do Rigel; transactionId={}",
                    candidate.transactionId(),
                    ex);
            throw new IllegalStateException("Não foi possível atualizar o pagamento do Rigel", ex);
        }
    }

    /** Localiza o estado financeiro mais recente do e-mail sem persistir a PII na auditoria. */
    private Optional<PaymentEntitlement> latestPayment(String email) {
        String buyerHash = sha256(normalizeEmail(email));
        if (!usesJdbcStorage()) {
            return inMemoryPayments.values().stream()
                    .filter(payment -> PRODUCT_SLUG.equals(payment.productSlug()))
                    .filter(payment -> EXPERIENCE_VERSION.equals(payment.experienceVersion()))
                    .filter(payment -> buyerHash.equals(payment.buyerReferenceHash()))
                    .max(Comparator.comparing(PaymentEntitlement::verifiedAt));
        }
        String sql = "SELECT transaction_id, product_slug, experience_version, offer_hash, amount_cents, currency, "
                + "payment_status, buyer_reference_hash, access_reference_hash, verified_at, refunded_at "
                + "FROM pde_payment_audit WHERE provider = ? AND product_slug = ? AND experience_version = ? "
                + "AND buyer_reference_hash = ? ORDER BY verified_at DESC, id DESC LIMIT 1";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PROVIDER);
            statement.setString(2, PRODUCT_SLUG);
            statement.setString(3, EXPERIENCE_VERSION);
            statement.setString(4, buyerHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readPayment(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            log.error("Falha ao consultar entitlement pago do Rigel; productSlug={}", PRODUCT_SLUG, ex);
            throw new IllegalStateException("Não foi possível confirmar o pagamento do Kit", ex);
        }
    }

    /** Localiza e valida o pagamento vigente associado ao e-mail informado. */
    private PaymentEntitlement approvedPayment(String email) {
        PaymentEntitlement payment = latestPayment(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compra confirmada do Kit WhatsApp Pronto não encontrada para este e-mail"));
        requireApproved(payment);
        return payment;
    }

    /** Localiza uma transação específica para deduplicar atualização e vínculo. */
    private Optional<PaymentEntitlement> loadPaymentByTransaction(String transactionId) {
        String sql = "SELECT transaction_id, product_slug, experience_version, offer_hash, amount_cents, currency, "
                + "payment_status, buyer_reference_hash, access_reference_hash, verified_at, refunded_at "
                + "FROM pde_payment_audit WHERE provider = ? AND transaction_id = ?";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PROVIDER);
            statement.setString(2, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readPayment(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            log.error(
                    "Falha ao consultar pagamento do Rigel; transactionId={}", transactionId, ex);
            throw new IllegalStateException("Não foi possível consultar o pagamento do Kit", ex);
        }
    }

    /** Vincula a transação aprovada ao hash do token sem permitir troca posterior de credencial. */
    private PaymentEntitlement linkAccess(PaymentEntitlement payment, String accessHash) {
        if (!usesJdbcStorage()) {
            PaymentEntitlement linked = payment.withAccessReference(accessHash);
            inMemoryPayments.put(payment.transactionId(), linked);
            return linked;
        }
        String sql = "UPDATE pde_payment_audit SET access_reference_hash = ?, access_released_at = ? "
                + "WHERE provider = ? AND transaction_id = ? AND access_reference_hash IS NULL";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accessHash);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setString(3, PROVIDER);
            statement.setString(4, payment.transactionId());
            statement.executeUpdate();
            PaymentEntitlement linked = loadPaymentByTransaction(payment.transactionId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Pagamento desapareceu durante o vínculo do acesso"));
            if (!accessHash.equals(linked.accessReferenceHash())) {
                throw new SecurityException("Pagamento já vinculado a outro acesso do Kit WhatsApp Pronto");
            }
            return linked;
        } catch (SQLException ex) {
            log.error(
                    "Falha ao vincular acesso ao pagamento do Rigel; transactionId={}",
                    payment.transactionId(),
                    ex);
            throw new IllegalStateException("Não foi possível vincular o acesso à compra", ex);
        }
    }

    /** Rejeita pagamento não aprovado e apresenta reembolso como causa funcional específica. */
    private void requireApproved(PaymentEntitlement payment) {
        if (isRefunded(payment.paymentStatus())) {
            throw new SecurityException("O pagamento foi reembolsado e o acesso pago foi encerrado");
        }
        if (!"approved".equals(payment.paymentStatus())) {
            throw new SecurityException("O pagamento do Kit ainda não está aprovado");
        }
        validateCanonicalContract(payment);
    }

    /** Confere que o registro corresponde ao produto, oferta, valor e moeda aprovados. */
    private void validateCanonicalContract(PaymentEntitlement payment) {
        boolean valid = PRODUCT_SLUG.equals(payment.productSlug())
                && EXPERIENCE_VERSION.equals(payment.experienceVersion())
                && OFFER_REFERENCE.equals(payment.offerHash())
                && AMOUNT_CENTS == payment.amountCents()
                && CURRENCY.equals(payment.currency());
        if (!valid) {
            throw new SecurityException("Pagamento diverge do contrato comercial do Kit WhatsApp Pronto");
        }
    }

    /** Impede reutilização da mesma transação com compradora ou contrato financeiro diferente. */
    private void validateSameContract(PaymentEntitlement existing, PaymentEntitlement candidate) {
        if (existing == null) {
            return;
        }
        boolean same = existing.productSlug().equals(candidate.productSlug())
                && existing.experienceVersion().equals(candidate.experienceVersion())
                && existing.offerHash().equals(candidate.offerHash())
                && existing.amountCents() == candidate.amountCents()
                && existing.currency().equals(candidate.currency())
                && existing.buyerReferenceHash().equals(candidate.buyerReferenceHash());
        if (!same) {
            throw new IllegalArgumentException(
                    "Transação Mercado Pago já utilizada com contrato financeiro diferente");
        }
    }

    /** Impede que retry atrasado reative uma transação já reembolsada ou contestada. */
    private void validateStatusTransition(
            PaymentEntitlement existing, PaymentEntitlement candidate) {
        if (existing != null
                && isRefunded(existing.paymentStatus())
                && "approved".equals(candidate.paymentStatus())) {
            throw new IllegalArgumentException(
                    "Transação Mercado Pago reembolsada não pode voltar ao estado aprovado");
        }
    }

    /** Constrói o registro canônico após validar identidade, preço, moeda, compradora e experimento. */
    private PaymentEntitlement verifiedCandidate(MercadoPagoEntitlementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Pagamento Mercado Pago não informado");
        }
        String productSlug = required(request.externalReference(), "Produto do pagamento não informado");
        if (!PRODUCT_SLUG.equals(productSlug)) {
            throw new IllegalArgumentException("Pagamento não pertence ao Kit WhatsApp Pronto");
        }
        long experimentId = metadataLong(request.metadata(), "experimentId");
        long productId = metadataLong(request.metadata(), "productId");
        String metadataProduct = metadataText(request.metadata(), "productKey");
        if (experimentId != EXPERIMENT_ID
                || productId != PRODUCT_ID
                || !PRODUCT_SLUG.equals(metadataProduct)) {
            throw new IllegalArgumentException("Metadados do pagamento divergem do contrato comercial do Kit");
        }
        int amountCents = cents(request.amount());
        String currency = required(request.currency(), "Moeda do pagamento não informada")
                .toUpperCase(Locale.ROOT);
        if (amountCents != AMOUNT_CENTS || !CURRENCY.equals(currency)) {
            throw new IllegalArgumentException("Valor ou moeda divergem da oferta aprovada do Kit");
        }
        String status = normalizePaymentStatus(request.paymentStatus());
        if ("approved".equals(status) && request.dateApproved() == null) {
            throw new IllegalArgumentException("Pagamento aprovado sem data de aprovação autoritativa");
        }
        Instant verifiedAt = request.dateApproved() == null ? Instant.now() : request.dateApproved();
        return new PaymentEntitlement(
                required(request.paymentId(), "Pagamento Mercado Pago sem identificador"),
                PRODUCT_SLUG,
                EXPERIENCE_VERSION,
                OFFER_REFERENCE,
                amountCents,
                currency,
                status,
                sha256(normalizeEmail(request.buyerEmail())),
                null,
                verifiedAt,
                isRefunded(status) ? Instant.now() : null);
    }

    /** Lê número inteiro dos metadados sem depender do tipo numérico usado no JSON. */
    private long metadataLong(Map<String, Object> metadata, String field) {
        if (metadata == null || !metadata.containsKey(field)) {
            throw new IllegalArgumentException("Metadado financeiro obrigatório ausente: " + field);
        }
        Object value = metadata.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Metadado financeiro vazio: " + field);
        }
        try {
            return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
        } catch (RuntimeException ex) {
            log.error("Metadado financeiro inválido no entitlement do Rigel; field={}", field, ex);
            throw new IllegalArgumentException("Metadado financeiro inválido: " + field, ex);
        }
    }

    /** Lê texto obrigatório dos metadados de atribuição do checkout. */
    private String metadataText(Map<String, Object> metadata, String field) {
        if (metadata == null || !metadata.containsKey(field)) {
            throw new IllegalArgumentException("Metadado financeiro obrigatório ausente: " + field);
        }
        Object value = metadata.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Metadado financeiro vazio: " + field);
        }
        return required(String.valueOf(value), "Metadado financeiro vazio: " + field);
    }

    /** Converte o valor decimal em centavos sem arredondar divergências comerciais. */
    private int cents(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Valor do pagamento não informado");
        }
        try {
            return amount.movePointRight(2).intValueExact();
        } catch (ArithmeticException ex) {
            log.error("Valor monetário inválido no entitlement do Rigel; amount={}", amount, ex);
            throw new IllegalArgumentException("Valor monetário do pagamento é inválido", ex);
        }
    }

    /** Preenche a inserção JDBC sem registrar e-mail ou token em texto puro. */
    private void bindPaymentInsert(PreparedStatement statement, PaymentEntitlement payment)
            throws SQLException {
        statement.setString(1, PROVIDER);
        statement.setString(2, payment.transactionId());
        statement.setString(3, payment.productSlug());
        statement.setString(4, payment.experienceVersion());
        statement.setString(5, payment.offerHash());
        statement.setInt(6, payment.amountCents());
        statement.setString(7, payment.currency());
        statement.setString(8, payment.paymentStatus());
        statement.setString(9, payment.buyerReferenceHash());
        statement.setTimestamp(10, Timestamp.from(payment.verifiedAt()));
        statement.setTimestamp(11, toTimestamp(payment.refundedAt()));
    }

    /** Reconstrói o registro financeiro retornado pelo banco operacional. */
    private PaymentEntitlement readPayment(ResultSet resultSet) throws SQLException {
        Timestamp refundedAt = resultSet.getTimestamp("refunded_at");
        return new PaymentEntitlement(
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
                refundedAt == null ? null : refundedAt.toInstant());
    }

    /** Normaliza somente os estados finais que alteram pagamento ou reembolso. */
    private String normalizePaymentStatus(String paymentStatus) {
        String normalized = required(paymentStatus, "Status do pagamento não informado")
                .toLowerCase(Locale.ROOT);
        if (!java.util.Set.of("approved", "refunded", "charged_back").contains(normalized)) {
            throw new IllegalArgumentException("Status financeiro do Mercado Pago não suportado");
        }
        return normalized;
    }

    /** Identifica estados finais que encerram o entitlement pago. */
    private boolean isRefunded(String paymentStatus) {
        return "refunded".equals(paymentStatus) || "charged_back".equals(paymentStatus);
    }

    /** Normaliza o e-mail antes de gerar sua referência irreversível. */
    private String normalizeEmail(String email) {
        return required(email, "E-mail da compradora não informado").toLowerCase(Locale.ROOT);
    }

    /** Exige texto não vazio e remove espaços residuais. */
    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /** Calcula uma referência irreversível compatível com a auditoria do backend principal. */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Algoritmo de hash indisponível no entitlement pago do Rigel", ex);
            throw new IllegalStateException("Não foi possível proteger a referência financeira", ex);
        }
    }

    /** Abre o banco operacional sem expor credenciais em logs. */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /** Informa se a trilha de entitlement deve usar persistência durável. */
    private boolean usesJdbcStorage() {
        return jdbcUrl != null && !jdbcUrl.isBlank();
    }

    /** Identifica conflito de unicidade em MySQL e bancos de teste compatíveis. */
    private boolean isConstraintViolation(SQLException ex) {
        return ex.getErrorCode() == 1062
                || (ex.getSQLState() != null && ex.getSQLState().startsWith("23"));
    }

    /** Converte instante opcional para o tipo temporal usado pelo JDBC. */
    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    /** Representa o pagamento autoritativo mínimo necessário para liberar e revogar acesso. */
    private record PaymentEntitlement(
            String transactionId,
            String productSlug,
            String experienceVersion,
            String offerHash,
            int amountCents,
            String currency,
            String paymentStatus,
            String buyerReferenceHash,
            String accessReferenceHash,
            Instant verifiedAt,
            Instant refundedAt) {

        /** Preserva o contrato e troca somente o estado financeiro confirmado. */
        private PaymentEntitlement withStatus(
                String status, Instant newVerifiedAt, Instant newRefundedAt) {
            return new PaymentEntitlement(
                    transactionId,
                    productSlug,
                    experienceVersion,
                    offerHash,
                    amountCents,
                    currency,
                    status,
                    buyerReferenceHash,
                    accessReferenceHash,
                    newVerifiedAt,
                    newRefundedAt);
        }

        /** Preserva o pagamento e registra somente o hash do token liberado. */
        private PaymentEntitlement withAccessReference(String reference) {
            return new PaymentEntitlement(
                    transactionId,
                    productSlug,
                    experienceVersion,
                    offerHash,
                    amountCents,
                    currency,
                    paymentStatus,
                    buyerReferenceHash,
                    reference,
                    verifiedAt,
                    refundedAt);
        }
    }

    /** Retorna a compra aprovada com os correlatores exigidos pela telemetria comercial. */
    public record PaidClaim(
            String transactionId,
            Instant approvedAt,
            BigDecimal amountBrl,
            String currency,
            long experimentId) {}

    /** Retorna o reembolso confirmado sem expor e-mail ou bearer da compradora. */
    public record RefundClaim(
            String transactionId,
            BigDecimal amountBrl,
            String currency,
            long experimentId,
            String providerStatus,
            Instant confirmedAt) {}

    /** Retorna o resultado sanitizado do provedor fictício usado na homologação. */
    public record InternalPaymentResult(
            String transactionId, String paymentStatus, String result, String verifiedAt) {}

    /** Retorna ao emissor apenas a identidade e o resultado idempotente da conciliação. */
    public record PaymentReconciliationResult(
            String transactionId, String paymentStatus, String result, String verifiedAt) {}

    /** Combina o registro final e se a transação foi criada nesta chamada. */
    private record PaymentWriteResult(PaymentEntitlement payment, boolean created) {}
}
