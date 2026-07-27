package com.marketinghub.pde.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar versões produtivas PDE vinculadas ao produto. */
@Service
@Slf4j
public class PdeProductionSlotService {

    private static final String DEFAULT_PDE_PRODUCT_SLUG = "metodo-musa-7-dias";
    private static final Duration VALIDATION_TIMEOUT = Duration.ofSeconds(12);
    private static final long MINIMUM_VIDEO_ASSET_BYTES = 100_000L;
    private static final String VALIDATION_OK = "OK";
    private static final String VALIDATION_FAILED = "FAILED";

    private final PdeProductionSlotRepository repository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com o repositório canônico de slots PDE. */
    public PdeProductionSlotService(
            PdeProductionSlotRepository repository,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /** Resolve o produto PDE padrão quando a tela não informa um slug específico. */
    public String resolveProductSlug(String productSlug) {
        return StringUtils.hasText(productSlug) ? productSlug.trim() : DEFAULT_PDE_PRODUCT_SLUG;
    }

    /** Lista os slots produtivos persistidos para o produto PDE informado. */
    public List<PostDeployPdeProductionSlotDto> listProductionSlotsForProduct(String productSlug) {
        return repository.findByProductSlugOrderBySlotCodeAsc(resolveProductSlug(productSlug)).stream()
                .map(this::toProductionSlotDto)
                .toList();
    }

    /** Cria ou atualiza um slot produtivo versionado para manter hipóteses PDE em URLs paralelas. */
    public PostDeployPdeProductionSlotDto saveProductionSlot(
            String productSlug,
            Long defaultSourceExperimentId,
            PostDeployPdeProductionSlotRequestDto request) {
        String resolvedProductSlug = resolveProductSlug(productSlug);
        String slotCode = normalizeRequired(request.slotCode(), "Código do slot PDE obrigatório").toLowerCase(Locale.ROOT);
        String domain = normalizeDomain(request.domain());
        String publicUrl = StringUtils.hasText(request.publicUrl())
                ? request.publicUrl().trim()
                : "https://" + domain;
        PdeProductionSlot slot = repository.findByProductSlugAndSlotCode(resolvedProductSlug, slotCode)
                .orElseGet(PdeProductionSlot::new);
        slot.setSlotCode(slotCode);
        slot.setProductSlug(resolvedProductSlug);
        slot.setDomain(domain);
        slot.setPublicUrl(publicUrl);
        slot.setBackendUrl(StringUtils.hasText(request.backendUrl()) ? request.backendUrl().trim() : null);
        slot.setExperienceVersion(normalizeRequired(request.experienceVersion(), "Versão PDE obrigatória"));
        slot.setTargetEnvironment(StringUtils.hasText(request.targetEnvironment())
                ? request.targetEnvironment().trim()
                : "production-" + slotCode);
        slot.setStatus(request.status() != null ? request.status() : PdeProductionSlotStatus.PLANNED);
        slot.setSourceExperimentId(request.sourceExperimentId() != null
                ? request.sourceExperimentId()
                : defaultSourceExperimentId);
        slot.setNotes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null);
        return toProductionSlotDto(repository.save(slot));
    }

    /** Valida por HTTP se a URL produtiva entrega o contrato público declarado para o PDE. */
    public PostDeployPdeProductionSlotDto validateProductionSlot(String productSlug, String slotCode) {
        String resolvedProductSlug = resolveProductSlug(productSlug);
        String normalizedSlotCode = normalizeRequired(slotCode, "Código do slot PDE obrigatório")
                .toLowerCase(Locale.ROOT);
        PdeProductionSlot slot = repository.findByProductSlugAndSlotCode(resolvedProductSlug, normalizedSlotCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot PDE não encontrado"));
        try {
            ValidationResult result = validateSlotDelivery(slot);
            applyValidationResult(slot, result);
            return toProductionSlotDto(repository.save(slot));
        } catch (IOException ex) {
            log.warn(
                    "Falha de IO ao validar URL produtiva PDE; productSlug={}, slotCode={}, publicUrl={}",
                    resolvedProductSlug,
                    normalizedSlotCode,
                    slot.getPublicUrl(),
                    ex);
            applyValidationResult(slot, ValidationResult.failed(null, "Falha de acesso à URL pública", ex.getMessage()));
            return toProductionSlotDto(repository.save(slot));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn(
                    "Validação de URL produtiva PDE interrompida; productSlug={}, slotCode={}, publicUrl={}",
                    resolvedProductSlug,
                    normalizedSlotCode,
                    slot.getPublicUrl(),
                    ex);
            applyValidationResult(slot, ValidationResult.failed(null, "Validação interrompida", ex.getMessage()));
            return toProductionSlotDto(repository.save(slot));
        } catch (RuntimeException ex) {
            log.warn(
                    "Falha ao validar contrato público PDE; productSlug={}, slotCode={}, publicUrl={}",
                    resolvedProductSlug,
                    normalizedSlotCode,
                    slot.getPublicUrl(),
                    ex);
            applyValidationResult(slot, ValidationResult.failed(null, "Contrato público inválido", ex.getMessage()));
            return toProductionSlotDto(repository.save(slot));
        }
    }

    /** Converte o slot persistido em contrato administrativo do painel. */
    private PostDeployPdeProductionSlotDto toProductionSlotDto(PdeProductionSlot slot) {
        return new PostDeployPdeProductionSlotDto(
                slot.getId(),
                slot.getSlotCode(),
                slot.getProductSlug(),
                slot.getDomain(),
                slot.getPublicUrl(),
                slot.getBackendUrl(),
                slot.getExperienceVersion(),
                slot.getTargetEnvironment(),
                slot.getStatus(),
                slot.getSourceExperimentId(),
                slot.getNotes(),
                slot.getValidationStatus(),
                slot.getValidationCheckedAt(),
                slot.getValidationHttpStatus(),
                slot.getValidationSummary(),
                slot.getValidationDetail(),
                slot.getValidationContractSlug(),
                slot.getValidationContractHealthPath(),
                slot.getValidationResolvedUrl(),
                slot.getCreatedAt(),
                slot.getUpdatedAt());
    }

    /** Executa as chamadas HTTP mínimas que provam a entrega pública do slot. */
    private ValidationResult validateSlotDelivery(PdeProductionSlot slot) throws IOException, InterruptedException {
        HttpResponse<String> health = get(slot.getPublicUrl() + "/healthz");
        if (!isSuccess(health) || !health.body().contains("UP")) {
            return ValidationResult.failed(
                    health.statusCode(),
                    "Health público não respondeu como UP",
                    "Resposta /healthz: HTTP " + health.statusCode());
        }

        HttpResponse<String> contractResponse = get(slot.getPublicUrl() + "/pde-health-contract.json");
        if (!isSuccess(contractResponse)) {
            return ValidationResult.failed(
                    contractResponse.statusCode(),
                    "Contrato público do PDE não foi entregue",
                    "Resposta /pde-health-contract.json: HTTP " + contractResponse.statusCode());
        }
        JsonNode contract = objectMapper.readTree(contractResponse.body());
        String contractSlug = text(contract, "slug");
        String healthPath = StringUtils.hasText(text(contract, "healthPath")) ? text(contract, "healthPath") : "/";
        if (!slot.getProductSlug().equals(contractSlug)) {
            return ValidationResult.failed(
                    contractResponse.statusCode(),
                    "Contrato público aponta para outro produto",
                    "Esperado " + slot.getProductSlug() + ", recebido " + contractSlug,
                    contractSlug,
                    healthPath,
                    null);
        }
        if (!hasNonEmptyArray(contract, "requiredTexts")) {
            return ValidationResult.failed(
                    contractResponse.statusCode(),
                    "Contrato público não declara textos obrigatórios",
                    "requiredTexts ausente ou vazio",
                    contractSlug,
                    healthPath,
                    null);
        }

        String resolvedUrl = resolveUrl(slot.getPublicUrl(), healthPath);
        HttpResponse<String> page = get(resolvedUrl);
        if (!isSuccess(page)) {
            return ValidationResult.failed(
                    page.statusCode(),
                    "Entrada pública do funil não respondeu com sucesso",
                    "Resposta " + resolvedUrl + ": HTTP " + page.statusCode(),
                    contractSlug,
                    healthPath,
                    resolvedUrl);
        }
        if (!page.body().contains("<script") || !page.body().contains("type=\"module\"")) {
            return ValidationResult.failed(
                    page.statusCode(),
                    "Entrada pública não parece carregar a aplicação PDE",
                    "HTML sem script module do frontend",
                    contractSlug,
                    healthPath,
                    resolvedUrl);
        }
        String expectedAsset = expectedAsset(slot.getExperienceVersion());
        if (StringUtils.hasText(expectedAsset)) {
            AssetValidationResult asset = validateAsset(slot.getPublicUrl() + expectedAsset);
            if (!asset.valid()) {
                return ValidationResult.failed(
                        asset.httpStatus(),
                        "Ativo obrigatório da versão PDE não foi entregue",
                        "Ativo esperado: " + expectedAsset + ". " + asset.detail(),
                        contractSlug,
                        healthPath,
                        resolvedUrl);
            }
        }
        return ValidationResult.ok(
                page.statusCode(),
                "URL produtiva validada",
                "Health, contrato público, entrada do funil e ativo versionado responderam.",
                contractSlug,
                healthPath,
                resolvedUrl);
    }

    /** Aplica o resultado auditável da validação no slot persistido. */
    private void applyValidationResult(PdeProductionSlot slot, ValidationResult result) {
        slot.setValidationStatus(result.status());
        slot.setValidationCheckedAt(Instant.now());
        slot.setValidationHttpStatus(result.httpStatus());
        slot.setValidationSummary(result.summary());
        slot.setValidationDetail(result.detail());
        slot.setValidationContractSlug(result.contractSlug());
        slot.setValidationContractHealthPath(result.contractHealthPath());
        slot.setValidationResolvedUrl(result.resolvedUrl());
    }

    /** Executa uma chamada GET com timeout curto para validação operacional do PDE. */
    private HttpResponse<String> get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(VALIDATION_TIMEOUT)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Executa uma chamada GET descartando o corpo para validar ativos grandes. */
    private HttpResponse<Void> getWithoutBody(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(VALIDATION_TIMEOUT)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    /** Informa se a resposta HTTP está na faixa de sucesso. */
    private boolean isSuccess(HttpResponse<String> response) {
        return isSuccess(response.statusCode());
    }

    /** Informa se o status HTTP está na faixa de sucesso. */
    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /** Valida cabeçalhos mínimos para impedir falso 200 servindo HTML no lugar de MP4. */
    private AssetValidationResult validateAsset(String url) throws IOException, InterruptedException {
        HttpResponse<Void> response = getWithoutBody(url);
        int statusCode = response.statusCode();
        String contentType = response.headers().firstValue("content-type").orElse("");
        long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1L);
        if (!isSuccess(statusCode)) {
            return AssetValidationResult.failed(statusCode, "HTTP " + statusCode);
        }
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("video/")) {
            return AssetValidationResult.failed(
                    statusCode,
                    "Content-Type recebido: " + (StringUtils.hasText(contentType) ? contentType : "ausente"));
        }
        if (contentLength >= 0 && contentLength < MINIMUM_VIDEO_ASSET_BYTES) {
            return AssetValidationResult.failed(
                    statusCode,
                    "Content-Length baixo para vídeo comercial: " + contentLength + " bytes");
        }
        return AssetValidationResult.ok(statusCode);
    }

    /** Lê um campo textual do contrato público do PDE. */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    /** Verifica se o contrato publicou uma lista não vazia. */
    private boolean hasNonEmptyArray(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isArray() && value.size() > 0;
    }

    /** Resolve a URL de entrada do funil declarada pelo contrato público. */
    private String resolveUrl(String publicUrl, String healthPath) {
        String normalizedPublicUrl = publicUrl.replaceAll("/+$", "");
        String normalizedPath = healthPath.startsWith("/") ? healthPath : "/" + healthPath;
        return normalizedPublicUrl + normalizedPath;
    }

    /** Mapeia versões PDE conhecidas para ativos que precisam existir no domínio público. */
    private String expectedAsset(String experienceVersion) {
        if ("musa-pde-entry-v5-video-explicativo".equals(experienceVersion)) {
            return "/assets/musa-v5-video-explicativo.mp4";
        }
        if ("musa-pde-entry-v6-video-motivacional".equals(experienceVersion)) {
            return "/assets/musa-v6-video-motivacional.mp4";
        }
        return null;
    }

    /** Normaliza um campo obrigatório textual antes de persistir contrato de publicação. */
    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    /** Normaliza domínio removendo protocolo e barra final para evitar duplicidade operacional. */
    private String normalizeDomain(String value) {
        String domain = normalizeRequired(value, "Domínio do slot PDE obrigatório")
                .replaceFirst("^https?://", "")
                .replaceAll("/+$", "")
                .toLowerCase(Locale.ROOT);
        if (!domain.endsWith("clubemusa.com.br")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Slot PDE MUSA deve usar subdomínio de clubemusa.com.br");
        }
        return domain;
    }

    /** Resultado auditável da validação real de uma URL produtiva PDE. */
    private record ValidationResult(
            String status,
            Integer httpStatus,
            String summary,
            String detail,
            String contractSlug,
            String contractHealthPath,
            String resolvedUrl) {

        /** Cria resultado de validação aprovada. */
        private static ValidationResult ok(
                Integer httpStatus,
                String summary,
                String detail,
                String contractSlug,
                String contractHealthPath,
                String resolvedUrl) {
            return new ValidationResult(VALIDATION_OK, httpStatus, summary, detail, contractSlug, contractHealthPath, resolvedUrl);
        }

        /** Cria resultado de validação reprovada sem dados de contrato. */
        private static ValidationResult failed(Integer httpStatus, String summary, String detail) {
            return failed(httpStatus, summary, detail, null, null, null);
        }

        /** Cria resultado de validação reprovada com evidências parciais. */
        private static ValidationResult failed(
                Integer httpStatus,
                String summary,
                String detail,
                String contractSlug,
                String contractHealthPath,
                String resolvedUrl) {
            return new ValidationResult(VALIDATION_FAILED, httpStatus, summary, detail, contractSlug, contractHealthPath, resolvedUrl);
        }
    }

    /** Resultado da validação de um ativo público do slot PDE. */
    private record AssetValidationResult(boolean valid, Integer httpStatus, String detail) {

        /** Cria resultado de ativo válido. */
        private static AssetValidationResult ok(Integer httpStatus) {
            return new AssetValidationResult(true, httpStatus, "Ativo público confirmado.");
        }

        /** Cria resultado de ativo inválido. */
        private static AssetValidationResult failed(Integer httpStatus, String detail) {
            return new AssetValidationResult(false, httpStatus, detail);
        }
    }
}
