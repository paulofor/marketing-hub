package com.marketinghub.pde.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Consulta a API publica da Pepper para reconciliar transacoes pagas sem postback. */
@Component
public class PepperTransactionClient implements PepperTransactionGateway {
    private static final Logger log = LoggerFactory.getLogger(PepperTransactionClient.class);
    private static final String EXPERIENCE_VERSION_MARKER = "__pde_version__";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final String apiToken;
    private final Set<String> acceptedOfferHashes;
    private final int syncLookbackDays;
    private final int expectedPaidAmountCents;
    private final String expectedCurrency;
    private final String fallbackExperienceVersion;

    /** Recebe configuracoes da API Pepper e prepara o cliente HTTP. */
    public PepperTransactionClient(
            ObjectMapper objectMapper,
            @Value("${pde.access.pepper.api-base-url:https://api.cloud.pepperpay.com.br/public/v1}") String apiBaseUrl,
            @Value("${pde.access.pepper.api-token:}") String apiToken,
            @Value("${pde.access.pepper.offer-hashes:owm6x,c8mnn}") String offerHashes,
            @Value("${pde.access.pepper.sync-lookback-days:14}") int syncLookbackDays,
            @Value("${pde.access.pepper.expected-paid-amount-cents:6700}") int expectedPaidAmountCents,
            @Value("${pde.access.pepper.expected-currency:BRL}") String expectedCurrency,
            @Value("${pde.access.pepper.fallback-experience-version:musa-pde-entry-v7-espelho-antes-de-sair}")
                    String fallbackExperienceVersion) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiBaseUrl = trimTrailingSlash(apiBaseUrl);
        this.apiToken = apiToken;
        this.acceptedOfferHashes = parseOfferHashes(offerHashes);
        this.syncLookbackDays = Math.max(1, syncLookbackDays);
        this.expectedPaidAmountCents = Math.max(1, expectedPaidAmountCents);
        this.expectedCurrency = expectedCurrency == null ? "" : expectedCurrency.trim().toUpperCase();
        this.fallbackExperienceVersion = fallbackExperienceVersion == null ? "" : fallbackExperienceVersion.trim();
    }

    /** Busca transacoes pagas e filtra somente ofertas comerciais aceitas pelo Clube MUSA. */
    @Override
    public PepperTransactionSearchResult findPaidTransactions(String search) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("Token da API Pepper nao configurado para reconciliacao PDE");
        }
        String url = buildTransactionsUrl(search);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .GET()
                .build();
        try {
            log.info("Request Pepper enviado; method=GET, url={}, searchPresent={}", url, search != null && !search.isBlank());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Response bruto Pepper recebido; status={}, url={}, payload={}", response.statusCode(), url, response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Falha ao consultar transacoes Pepper; status={}, url={}", response.statusCode(), url);
                throw new IllegalStateException("API Pepper recusou a consulta de transacoes");
            }
            return parsePaidTransactions(response.body());
        } catch (IOException ex) {
            log.error("Falha de IO ao consultar transacoes Pepper; url={}", url, ex);
            throw new IllegalStateException("Nao foi possivel consultar transacoes Pepper", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Consulta de transacoes Pepper interrompida; url={}", url, ex);
            throw new IllegalStateException("Consulta de transacoes Pepper interrompida", ex);
        }
    }

    /** Busca uma transacao especifica por hash e aplica os mesmos filtros comerciais. */
    @Override
    public PepperTransactionSearchResult findPaidTransactionByHash(String transactionHash) {
        try {
            return parseSinglePaidTransaction(requestTransactionByHash(transactionHash));
        } catch (IOException ex) {
            log.error("Resposta inválida ao consultar transacao Pepper; transactionHash={}", transactionHash, ex);
            throw new IllegalStateException("A Pepper respondeu uma transação inválida", ex);
        }
    }

    /** Consulta uma transação aceita sem descartar o estado de reembolso ou chargeback. */
    @Override
    public PepperTransactionSnapshot findTransactionByHash(String transactionHash) {
        try {
            PepperTransactionSnapshot snapshot = toTransactionSnapshot(
                    objectMapper.readTree(requestTransactionByHash(transactionHash)));
            if (snapshot == null) {
                throw new IllegalArgumentException("Transação Pepper fora da oferta, valor ou moeda aprovados");
            }
            return snapshot;
        } catch (IOException ex) {
            log.error("Resposta inválida ao consultar estado Pepper; transactionHash={}", transactionHash, ex);
            throw new IllegalStateException("A Pepper respondeu um estado de transação inválido", ex);
        }
    }

    /** Executa a consulta autenticada por hash preservando URL e stack trace no diagnóstico. */
    private String requestTransactionByHash(String transactionHash) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("Token da API Pepper nao configurado para reconciliacao PDE");
        }
        if (transactionHash == null || transactionHash.isBlank()) {
            throw new IllegalArgumentException("Hash da transacao Pepper nao informado");
        }
        String url = apiBaseUrl + "/transactions/" + URLEncoder.encode(transactionHash.trim(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .GET()
                .build();
        try {
            log.info("Request Pepper enviado; method=GET, url={}, transactionHash={}", url, transactionHash.trim());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Response bruto Pepper recebido; status={}, url={}, payload={}", response.statusCode(), url, response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Falha ao consultar transacao Pepper; status={}, url={}", response.statusCode(), url);
                throw new IllegalStateException("API Pepper recusou a consulta da transacao");
            }
            return response.body();
        } catch (IOException ex) {
            log.error("Falha de IO ao consultar transacao Pepper; url={}", url, ex);
            throw new IllegalStateException("Nao foi possivel consultar transacao Pepper", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Consulta de transacao Pepper interrompida; url={}", url, ex);
            throw new IllegalStateException("Consulta de transacao Pepper interrompida", ex);
        }
    }

    /** Monta a URL de listagem com busca opcional por e-mail, nome ou documento. */
    private String buildTransactionsUrl(String search) {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(syncLookbackDays);
        StringBuilder builder = new StringBuilder(apiBaseUrl)
                .append("/transactions?perPage=50")
                .append("&dateInterval%5Bstart%5D=")
                .append(startDate)
                .append("&dateInterval%5Bend%5D=")
                .append(endDate);
        if (search != null && !search.isBlank()) {
            builder.append("&search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    /** Interpreta o JSON da Pepper e preserva apenas transacoes pagas da oferta configurada. */
    private PepperTransactionSearchResult parsePaidTransactions(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            return new PepperTransactionSearchResult(0, List.of());
        }
        List<PepperPaidTransaction> paidTransactions = new ArrayList<>();
        int scannedTransactions = 0;
        for (JsonNode transaction : data) {
            scannedTransactions++;
            PepperPaidTransaction paidTransaction = toPaidTransaction(transaction);
            if (paidTransaction != null) {
                paidTransactions.add(paidTransaction);
            }
        }
        return new PepperTransactionSearchResult(scannedTransactions, paidTransactions);
    }

    /** Interpreta uma transacao unica retornada pela Pepper. */
    private PepperTransactionSearchResult parseSinglePaidTransaction(String responseBody) throws IOException {
        JsonNode transaction = objectMapper.readTree(responseBody);
        PepperPaidTransaction paidTransaction = toPaidTransaction(transaction);
        return new PepperTransactionSearchResult(1, paidTransaction == null ? List.of() : List.of(paidTransaction));
    }

    /** Converte uma transacao Pepper paga e aceita em contrato interno. */
    private PepperPaidTransaction toPaidTransaction(JsonNode transaction) {
        PepperTransactionSnapshot snapshot = toTransactionSnapshot(transaction);
        if (snapshot == null || !"paid".equalsIgnoreCase(snapshot.paymentStatus())) {
            return null;
        }
        return new PepperPaidTransaction(
                snapshot.transactionId(),
                snapshot.buyerEmail(),
                snapshot.paymentStatus(),
                snapshot.offerHash(),
                snapshot.offerTitle(),
                snapshot.amount(),
                snapshot.currency(),
                snapshot.experienceVersion());
    }

    /** Converte uma transação aceita preservando seu estado financeiro atual. */
    private PepperTransactionSnapshot toTransactionSnapshot(JsonNode transaction) {
        String status = text(transaction, "payment_status");
        String offerHash = text(transaction.path("offer"), "hash");
        Integer amount = integer(transaction, "amount");
        String currency = resolvedCurrency(transaction);
        String buyerEmail = text(transaction.path("customer"), "email");
        String transactionId = resolvedTransactionId(transaction);
        if (status == null
                || transactionId == null
                || transactionId.isBlank()
                || buyerEmail == null
                || buyerEmail.isBlank()
                || !isAcceptedOfferHash(offerHash)
                || !isAcceptedAmountAndCurrency(amount, currency)) {
            log.warn(
                    "Transacao Pepper ignorada por contrato comercial; transactionPresent={}, status={}, offerHash={}, amount={}, currency={}, buyerPresent={}",
                    transactionId != null && !transactionId.isBlank(),
                    status,
                    offerHash,
                    amount,
                    currency,
                    buyerEmail != null && !buyerEmail.isBlank());
            return null;
        }
        return new PepperTransactionSnapshot(
                transactionId,
                buyerEmail,
                status,
                offerHash,
                text(transaction.path("offer"), "title"),
                amount,
                currency,
                resolveExperienceVersion(transaction));
    }

    /** Extrai a versão imutável anexada ao UTM sem confundir o criativo original. */
    private String resolveExperienceVersion(JsonNode transaction) {
        String utmContent = text(transaction, "utm_content");
        if (utmContent == null) {
            log.warn(
                    "Transação Pepper sem UTM de versão; fallbackExperienceVersion={}",
                    fallbackExperienceVersion);
            return fallbackExperienceVersion;
        }
        int markerIndex = utmContent.lastIndexOf(EXPERIENCE_VERSION_MARKER);
        if (markerIndex < 0) {
            log.warn(
                    "Transação Pepper sem versão atribuída no UTM; fallbackExperienceVersion={}",
                    fallbackExperienceVersion);
            return fallbackExperienceVersion;
        }
        String version = utmContent.substring(markerIndex + EXPERIENCE_VERSION_MARKER.length()).trim();
        return version.isBlank() || version.length() > 80 ? fallbackExperienceVersion : version;
    }

    /** Resolve o identificador mais estavel disponivel no payload da Pepper. */
    private String resolvedTransactionId(JsonNode transaction) {
        String hash = text(transaction, "hash");
        return hash == null || hash.isBlank() ? text(transaction, "transaction") : hash;
    }

    /** Confere se a oferta pertence ao produto PDE comercial atual. */
    private boolean isAcceptedOfferHash(String offerHash) {
        return offerHash != null && acceptedOfferHashes.contains(offerHash.trim());
    }

    /** Confere exatamente o preço e a moeda aprovados no plano comercial vigente. */
    private boolean isAcceptedAmountAndCurrency(Integer amount, String currency) {
        return amount != null
                && amount == expectedPaidAmountCents
                && currency != null
                && expectedCurrency.equalsIgnoreCase(currency);
    }

    /** Resolve a moeda explicitamente informada pelo provedor sem assumir BRL por omissão. */
    private String resolvedCurrency(JsonNode transaction) {
        String currency = text(transaction, "currency");
        if (currency == null || currency.isBlank()) {
            currency = text(transaction, "currency_code");
        }
        return currency == null ? null : currency.trim().toUpperCase();
    }

    /** Le campo textual opcional do JSON da Pepper. */
    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.path(fieldName);
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    /** Le campo inteiro opcional do JSON da Pepper. */
    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.path(fieldName);
        return value == null || !value.isNumber() ? null : value.asInt();
    }

    /** Remove barras finais para formar endpoints sem duplicidade. */
    private String trimTrailingSlash(String value) {
        return value == null || value.isBlank() ? "" : value.replaceAll("/+$", "");
    }

    /** Normaliza a lista de ofertas aceitas configurada no ambiente. */
    private Set<String> parseOfferHashes(String offerHashes) {
        Set<String> hashes = new LinkedHashSet<>();
        if (offerHashes != null) {
            for (String hash : offerHashes.split(",")) {
                if (!hash.isBlank()) {
                    hashes.add(hash.trim());
                }
            }
        }
        return hashes;
    }
}
