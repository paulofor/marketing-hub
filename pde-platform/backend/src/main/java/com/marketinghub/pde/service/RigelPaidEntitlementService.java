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

/** Controla entitlements pagos de PDEs a partir da auditoria autoritativa do Mercado Pago. */
@Service
public class RigelPaidEntitlementService {
    public static final String PRODUCT_SLUG = "kit-whatsapp-pronto";
    public static final String EXPERIENCE_VERSION = "kit-whatsapp-pronto-pde-v2";
    public static final String PAID_SOURCE = "MERCADO_PAGO";
    public static final String REFUNDED_SOURCE = "MERCADO_PAGO_REFUNDED";
    public static final String MIRA_PRODUCT_SLUG = "pde-planejado-36";
    public static final String MIRA_EXPERIENCE_VERSION = "mira-commercial-v1";
    private static final String PROVIDER = "MERCADO_PAGO";
    private static final String OFFER_REFERENCE = "experiment:89";
    private static final long PRODUCT_ID = 9L;
    private static final long EXPERIMENT_ID = 89L;
    private static final int AMOUNT_CENTS = 34_900;
    private static final String CURRENCY = "BRL";
    private static final Map<String, PaidProductPolicy> POLICIES = Map.of(
            PRODUCT_SLUG,
            new PaidProductPolicy(
                    PRODUCT_SLUG,
                    EXPERIENCE_VERSION,
                    OFFER_REFERENCE,
                    PRODUCT_ID,
                    EXPERIMENT_ID,
                    AMOUNT_CENTS,
                    CURRENCY,
                    "Kit WhatsApp Pronto"),
            MIRA_PRODUCT_SLUG,
            new PaidProductPolicy(
                    MIRA_PRODUCT_SLUG,
                    MIRA_EXPERIENCE_VERSION,
                    "experiment:93",
                    10L,
                    93L,
                    4_900,
                    CURRENCY,
                    "Mira"));
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
                "Entitlement financeiro PDE reconciliado; productSlug={}, transactionId={}, status={}, result={}",
                candidate.productSlug(),
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
        return claimApprovedPayment(PRODUCT_SLUG, email, accessToken);
    }

    /** Vincula a compra aprovada do produto informado a um único token de acesso. */
    public PaidClaim claimApprovedPayment(String productSlug, String email, String accessToken) {
        PaidProductPolicy policy = policy(productSlug);
        PaymentEntitlement payment = approvedPayment(policy, email);
        String accessHash = sha256(required(accessToken, "Token de acesso não informado"));
        PaymentEntitlement claimed = payment.accessReferenceHash() == null
                ? linkAccess(payment, accessHash)
                : payment;
        if (!accessHash.equals(claimed.accessReferenceHash())) {
            throw new SecurityException("Pagamento já vinculado a outro acesso de " + policy.displayName());
        }
        return toPaidClaim(policy, claimed);
    }

    /** Confirma a compra antes de qualquer grant ser criado, sem reservar ou expor um token. */
    public PaidClaim requireApprovedPayment(String email) {
        return requireApprovedPayment(PRODUCT_SLUG, email);
    }

    /** Confirma a compra aprovada do produto antes de criar ou reutilizar um grant. */
    public PaidClaim requireApprovedPayment(String productSlug, String email) {
        PaidProductPolicy policy = policy(productSlug);
        PaymentEntitlement payment = approvedPayment(policy, email);
        return toPaidClaim(policy, payment);
    }

    /** Exige compra aprovada, versão exata e vínculo com o token em toda fronteira paga. */
    public void requireActiveAccess(AccessGrant grant) {
        PaidProductPolicy policy = policy(grant.getProductSlug());
        requireExactExperience(policy.productSlug(), grant.getExperienceVersion());
        if ("INTERNAL_QA".equalsIgnoreCase(grant.getSource())) {
            return;
        }
        if (!PAID_SOURCE.equalsIgnoreCase(grant.getSource())
                && !REFUNDED_SOURCE.equalsIgnoreCase(grant.getSource())) {
            throw new SecurityException(
                    "Acesso de " + policy.displayName() + " não foi originado por pagamento confirmado");
        }
        PaymentEntitlement payment = latestPayment(policy, grant.getEmail())
                .orElseThrow(() -> new SecurityException(
                        "Pagamento vigente de " + policy.displayName() + " não encontrado"));
        requireApproved(policy, payment);
        if (!sha256(grant.getToken()).equals(payment.accessReferenceHash())) {
            throw new SecurityException(
                    "Pagamento não corresponde ao token desta área de " + policy.displayName());
        }
    }

    /** Confirma que o reembolso mais recente pertence exatamente ao token atualmente liberado. */
    public boolean shouldRevokeAccess(String email, String transactionId, String accessToken) {
        return findConfirmedRefund(email, transactionId, accessToken).isPresent();
    }

    /** Retorna o reembolso exato e seus campos comerciais quando ele pertence ao acesso atual. */
    public Optional<RefundClaim> findConfirmedRefund(
            String email, String transactionId, String accessToken) {
        return findConfirmedRefund(PRODUCT_SLUG, email, transactionId, accessToken);
    }

    /** Retorna o reembolso confirmado do produto e token exatos. */
    public Optional<RefundClaim> findConfirmedRefund(
            String productSlug, String email, String transactionId, String accessToken) {
        PaidProductPolicy policy = policy(productSlug);
        PaymentEntitlement latest = latestPayment(policy, email)
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
                policy.experimentId(),
                latest.paymentStatus(),
                latest.refundedAt() == null ? latest.verifiedAt() : latest.refundedAt()));
    }

    /** Converte o registro financeiro aprovado nos correlatores exigidos pelos eventos comerciais. */
    private PaidClaim toPaidClaim(PaidProductPolicy policy, PaymentEntitlement payment) {
        return new PaidClaim(
                payment.transactionId(),
                payment.verifiedAt(),
                BigDecimal.valueOf(payment.amountCents(), 2),
                payment.currency(),
                policy.experimentId());
    }

    /** Informa se o produto informado usa a guarda comercial do Mercado Pago. */
    public boolean supports(String productSlug) {
        return POLICIES.containsKey(productSlug);
    }

    /** Confere a versão comercial imutável aprovada para o Kit. */
    public void requireExactExperience(String experienceVersion) {
        requireExactExperience(PRODUCT_SLUG, experienceVersion);
    }

    /** Confere a versão comercial imutável do produto pago informado. */
    public void requireExactExperience(String productSlug, String experienceVersion) {
        PaidProductPolicy policy = policy(productSlug);
        if (!policy.experienceVersion().equals(experienceVersion)) {
            throw new SecurityException(
                    "A compra não corresponde à versão paga vigente de " + policy.displayName());
        }
    }

    /** Retorna a versão comercial congelada do produto pago. */
    public String experienceVersion(String productSlug) {
        return policy(productSlug).experienceVersion();
    }

    /** Acrescenta correlação financeira segura aos eventos do acesso pago. */
    public Map<String, Object> enrichAccessMetadata(
            AccessGrant grant, Map<String, Object> source) {
        if (!supports(grant.getProductSlug())) {
            return source == null ? Map.of() : Map.copyOf(source);
        }
        PaidProductPolicy policy = policy(grant.getProductSlug());
        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        if (source != null) metadata.putAll(source);
        metadata.remove("accessToken");
        metadata.put("productSlug", policy.productSlug());
        metadata.put("experimentId", policy.experimentId());
        metadata.put("experienceVersion", policy.experienceVersion());
        metadata.put("accessReferenceHash", sha256(grant.getToken()));
        return Map.copyOf(metadata);
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
                    "Falha ao persistir pagamento PDE; productSlug={}, transactionId={}",
                    candidate.productSlug(),
                    candidate.transactionId(),
                    ex);
            throw new IllegalStateException("Não foi possível registrar o pagamento PDE", ex);
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
                throw new IllegalStateException("Pagamento PDE desapareceu durante a atualização");
            }
            return new PaymentWriteResult(
                    existing.withStatus(
                            candidate.paymentStatus(), candidate.verifiedAt(), candidate.refundedAt()),
                    false);
        } catch (SQLException ex) {
            log.error(
                    "Falha ao atualizar pagamento PDE; productSlug={}, transactionId={}",
                    candidate.productSlug(),
                    candidate.transactionId(),
                    ex);
            throw new IllegalStateException("Não foi possível atualizar o pagamento PDE", ex);
        }
    }

    /** Localiza o estado financeiro mais recente do e-mail sem persistir a PII na auditoria. */
    private Optional<PaymentEntitlement> latestPayment(PaidProductPolicy policy, String email) {
        String buyerHash = sha256(normalizeEmail(email));
        if (!usesJdbcStorage()) {
            return inMemoryPayments.values().stream()
                    .filter(payment -> policy.productSlug().equals(payment.productSlug()))
                    .filter(payment -> policy.experienceVersion().equals(payment.experienceVersion()))
                    .filter(payment -> buyerHash.equals(payment.buyerReferenceHash()))
                    .max(Comparator.comparing(PaymentEntitlement::verifiedAt));
        }
        String sql = "SELECT transaction_id, product_slug, experience_version, offer_hash, amount_cents, currency, "
                + "payment_status, buyer_reference_hash, access_reference_hash, verified_at, refunded_at "
                + "FROM pde_payment_audit WHERE provider = ? AND product_slug = ? AND experience_version = ? "
                + "AND buyer_reference_hash = ? ORDER BY verified_at DESC, id DESC LIMIT 1";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PROVIDER);
            statement.setString(2, policy.productSlug());
            statement.setString(3, policy.experienceVersion());
            statement.setString(4, buyerHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readPayment(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            log.error(
                    "Falha ao consultar entitlement pago; productSlug={}", policy.productSlug(), ex);
            throw new IllegalStateException(
                    "Não foi possível confirmar o pagamento de " + policy.displayName(), ex);
        }
    }

    /** Localiza e valida o pagamento vigente associado ao e-mail informado. */
    private PaymentEntitlement approvedPayment(PaidProductPolicy policy, String email) {
        PaymentEntitlement payment = latestPayment(policy, email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compra confirmada de " + policy.displayName()
                                + " não encontrada para este e-mail"));
        requireApproved(policy, payment);
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
                    "Falha ao consultar pagamento PDE; transactionId={}", transactionId, ex);
            throw new IllegalStateException("Não foi possível consultar o pagamento PDE", ex);
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
                throw new SecurityException("Pagamento já vinculado a outro acesso deste produto");
            }
            return linked;
        } catch (SQLException ex) {
            log.error(
                    "Falha ao vincular acesso ao pagamento PDE; productSlug={}, transactionId={}",
                    payment.productSlug(),
                    payment.transactionId(),
                    ex);
            throw new IllegalStateException("Não foi possível vincular o acesso à compra", ex);
        }
    }

    /** Rejeita pagamento não aprovado e apresenta reembolso como causa funcional específica. */
    private void requireApproved(PaidProductPolicy policy, PaymentEntitlement payment) {
        if (isRefunded(payment.paymentStatus())) {
            throw new SecurityException("O pagamento foi reembolsado e o acesso pago foi encerrado");
        }
        if (!"approved".equals(payment.paymentStatus())) {
            throw new SecurityException(
                    "O pagamento de " + policy.displayName() + " ainda não está aprovado");
        }
        validateCanonicalContract(policy, payment);
    }

    /** Confere que o registro corresponde ao produto, oferta, valor e moeda aprovados. */
    private void validateCanonicalContract(
            PaidProductPolicy policy, PaymentEntitlement payment) {
        boolean valid = policy.productSlug().equals(payment.productSlug())
                && policy.experienceVersion().equals(payment.experienceVersion())
                && policy.offerReference().equals(payment.offerHash())
                && policy.amountCents() == payment.amountCents()
                && policy.currency().equals(payment.currency());
        if (!valid) {
            throw new SecurityException(
                    "Pagamento diverge do contrato comercial de " + policy.displayName());
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
        PaidProductPolicy policy = policy(productSlug);
        long experimentId = metadataLong(request.metadata(), "experimentId");
        long productId = metadataLong(request.metadata(), "productId");
        String metadataProduct = metadataText(request.metadata(), "productKey");
        if (experimentId != policy.experimentId()
                || productId != policy.productId()
                || !policy.productSlug().equals(metadataProduct)) {
            throw new IllegalArgumentException(
                    "Metadados do pagamento divergem do contrato comercial de "
                            + policy.displayName());
        }
        int amountCents = cents(request.amount());
        String currency = required(request.currency(), "Moeda do pagamento não informada")
                .toUpperCase(Locale.ROOT);
        if (amountCents != policy.amountCents() || !policy.currency().equals(currency)) {
            throw new IllegalArgumentException(
                    "Valor ou moeda divergem da oferta aprovada de " + policy.displayName());
        }
        String status = normalizePaymentStatus(request.paymentStatus());
        if ("approved".equals(status) && request.dateApproved() == null) {
            throw new IllegalArgumentException("Pagamento aprovado sem data de aprovação autoritativa");
        }
        Instant verifiedAt = request.dateApproved() == null ? Instant.now() : request.dateApproved();
        return new PaymentEntitlement(
                required(request.paymentId(), "Pagamento Mercado Pago sem identificador"),
                policy.productSlug(),
                policy.experienceVersion(),
                policy.offerReference(),
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
            log.error("Metadado financeiro inválido no entitlement PDE; field={}", field, ex);
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
            log.error("Valor monetário inválido no entitlement PDE; amount={}", amount, ex);
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

    /** Resolve o contrato financeiro imutável do produto sem aceitar fallback entre ofertas. */
    private PaidProductPolicy policy(String productSlug) {
        PaidProductPolicy policy = POLICIES.get(required(productSlug, "Produto pago não informado"));
        if (policy == null) {
            throw new IllegalArgumentException("Produto não usa entitlement Mercado Pago: " + productSlug);
        }
        return policy;
    }

    /** Calcula uma referência irreversível compatível com a auditoria do backend principal. */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Algoritmo de hash indisponível no entitlement pago PDE", ex);
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

    /** Congela produto, versão, oferta e preço aceitos para uma liberação paga. */
    private record PaidProductPolicy(
            String productSlug,
            String experienceVersion,
            String offerReference,
            long productId,
            long experimentId,
            int amountCents,
            String currency,
            String displayName) {}

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
