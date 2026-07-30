package com.marketinghub.pde.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.publishslotcontract.PublishPdeProductionSlotContractRequest;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoAssetDto;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoPanelDto;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final String VALIDATION_OK = "OK";
  private static final String VALIDATION_FAILED = "FAILED";
  private static final String DEFAULT_LAYOUT_KEY = "video-explicativo";
  private static final Pattern SCRIPT_SRC_PATTERN =
      Pattern.compile("<script[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

  private final PdeProductionSlotRepository repository;
  private final ExperimentVideoAssetRepository videoAssetRepository;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com o repositório canônico de slots PDE. */
  public PdeProductionSlotService(
      PdeProductionSlotRepository repository,
      ExperimentVideoAssetRepository videoAssetRepository,
      HttpClient httpClient,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.videoAssetRepository = videoAssetRepository;
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

  /** Lista cada versão produtiva PDE com os vídeos HLS resolvidos pelo backend. */
  public List<PdeProductionSlotVideoPanelDto> listProductionSlotVideosForProduct(
      String productSlug) {
    List<PdeProductionSlot> slots =
        repository.findByProductSlugOrderBySlotCodeAsc(resolveProductSlug(productSlug));
    List<ExperimentVideoAsset> videos =
        videoAssetRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(this::isPdeHeroHlsVideo)
            .toList();
    return slots.stream().map(slot -> toVideoPanelDto(slot, slots, videos)).toList();
  }

  /** Cria ou atualiza um slot produtivo versionado para manter hipóteses PDE em URLs paralelas. */
  public PostDeployPdeProductionSlotDto saveProductionSlot(
      String productSlug,
      Long defaultSourceExperimentId,
      PostDeployPdeProductionSlotRequestDto request) {
    String resolvedProductSlug = resolveProductSlug(productSlug);
    String slotCode =
        normalizeRequired(request.slotCode(), "Código do slot PDE obrigatório")
            .toLowerCase(Locale.ROOT);
    String domain = normalizeDomain(request.domain());
    String publicUrl =
        StringUtils.hasText(request.publicUrl()) ? request.publicUrl().trim() : "https://" + domain;
    PdeProductionSlot slot =
        repository
            .findByProductSlugAndSlotCode(resolvedProductSlug, slotCode)
            .orElseGet(PdeProductionSlot::new);
    slot.setSlotCode(slotCode);
    slot.setProductSlug(resolvedProductSlug);
    slot.setDomain(domain);
    slot.setPublicUrl(publicUrl);
    slot.setBackendUrl(
        StringUtils.hasText(request.backendUrl()) ? request.backendUrl().trim() : null);
    slot.setExperienceVersion(
        normalizeRequired(request.experienceVersion(), "Versão PDE obrigatória"));
    slot.setLayoutKey(
        normalizeLayoutKey(request.layoutKey(), slotCode, slot.getExperienceVersion()));
    slot.setTargetEnvironment(
        StringUtils.hasText(request.targetEnvironment())
            ? request.targetEnvironment().trim()
            : "production-" + slotCode);
    slot.setStatus(request.status() != null ? request.status() : PdeProductionSlotStatus.PLANNED);
    slot.setSourceExperimentId(
        request.sourceExperimentId() != null
            ? request.sourceExperimentId()
            : defaultSourceExperimentId);
    slot.setNotes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null);
    slot.setDraftExperienceJson(normalizeOptionalJson(request.draftExperienceJson()));
    return toProductionSlotDto(repository.save(slot));
  }

  /** Publica o contrato comercial do slot para consumo da URL versionada do PDE. */
  public PostDeployPdeProductionSlotDto publishProductionSlotContract(
      String productSlug, String slotCode, PublishPdeProductionSlotContractRequest request) {
    String resolvedProductSlug = resolveProductSlug(productSlug);
    String normalizedSlotCode =
        normalizeRequired(slotCode, "Código do slot PDE obrigatório").toLowerCase(Locale.ROOT);
    PdeProductionSlot slot =
        repository
            .findByProductSlugAndSlotCode(resolvedProductSlug, normalizedSlotCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot PDE não encontrado"));
    String contractJson =
        StringUtils.hasText(request.experienceJson())
            ? request.experienceJson()
            : slot.getDraftExperienceJson();
    String normalizedContract =
        normalizeRequiredJson(
            contractJson,
            "Informe o contrato JSON do PDE para publicar",
            slot.getExperienceVersion(),
            slot.getLayoutKey());
    slot.setDraftExperienceJson(normalizedContract);
    slot.setPublishedExperienceJson(normalizedContract);
    slot.setPublishedBy(
        StringUtils.hasText(request.publishedBy()) ? request.publishedBy().trim() : null);
    slot.setPublishedAt(Instant.now());
    return toProductionSlotDto(repository.save(slot));
  }

  /** Retorna o contrato PDE publicado por slot ou versão, quando existir. */
  public Optional<String> findPublishedExperienceJson(
      String productSlug, String slotCode, String experienceVersion) {
    String resolvedProductSlug = resolveProductSlug(productSlug);
    Optional<PdeProductionSlot> slot = Optional.empty();
    if (StringUtils.hasText(slotCode)) {
      slot =
          repository.findByProductSlugAndSlotCode(
              resolvedProductSlug, slotCode.trim().toLowerCase(Locale.ROOT));
    }
    if (slot.isEmpty() && StringUtils.hasText(experienceVersion)) {
      slot =
          repository.findFirstByProductSlugAndExperienceVersionOrderByPublishedAtDesc(
              resolvedProductSlug, experienceVersion.trim());
    }
    return slot.map(PdeProductionSlot::getPublishedExperienceJson)
        .filter(StringUtils::hasText)
        .map(String::trim);
  }

  /** Valida por HTTP se a URL produtiva entrega o contrato público declarado para o PDE. */
  public PostDeployPdeProductionSlotDto validateProductionSlot(
      String productSlug, String slotCode) {
    String resolvedProductSlug = resolveProductSlug(productSlug);
    String normalizedSlotCode =
        normalizeRequired(slotCode, "Código do slot PDE obrigatório").toLowerCase(Locale.ROOT);
    PdeProductionSlot slot =
        repository
            .findByProductSlugAndSlotCode(resolvedProductSlug, normalizedSlotCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot PDE não encontrado"));
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
      applyValidationResult(
          slot, ValidationResult.failed(null, "Falha de acesso à URL pública", ex.getMessage()));
      return toProductionSlotDto(repository.save(slot));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn(
          "Validação de URL produtiva PDE interrompida; productSlug={}, slotCode={}, publicUrl={}",
          resolvedProductSlug,
          normalizedSlotCode,
          slot.getPublicUrl(),
          ex);
      applyValidationResult(
          slot, ValidationResult.failed(null, "Validação interrompida", ex.getMessage()));
      return toProductionSlotDto(repository.save(slot));
    } catch (RuntimeException ex) {
      log.warn(
          "Falha ao validar contrato público PDE; productSlug={}, slotCode={}, publicUrl={}",
          resolvedProductSlug,
          normalizedSlotCode,
          slot.getPublicUrl(),
          ex);
      applyValidationResult(
          slot, ValidationResult.failed(null, "Contrato público inválido", ex.getMessage()));
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
        resolveSlotLayoutKey(slot),
        slot.getTargetEnvironment(),
        slot.getStatus(),
        slot.getSourceExperimentId(),
        slot.getNotes(),
        slot.getDraftExperienceJson(),
        slot.getPublishedExperienceJson(),
        slot.getPublishedBy(),
        slot.getPublishedAt(),
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

  /** Monta o contrato de vídeos de uma versão PDE sem delegar regra comercial ao frontend. */
  private PdeProductionSlotVideoPanelDto toVideoPanelDto(
      PdeProductionSlot slot, List<PdeProductionSlot> allSlots, List<ExperimentVideoAsset> videos) {
    List<PdeProductionSlotVideoAssetDto> resolvedVideos =
        videos.stream()
            .filter(video -> belongsToSlot(slot, allSlots, video))
            .sorted(this::compareVideoPriority)
            .map(video -> toVideoAssetDto(slot, video))
            .toList();
    List<String> alerts =
        resolvedVideos.stream()
            .filter(video -> "VERSION_TOKEN".equals(video.assignmentSource()))
            .filter(
                video ->
                    slot.getSourceExperimentId() != null
                        && !slot.getSourceExperimentId().equals(video.experimentId()))
            .map(
                video ->
                    "Vídeo #"
                        + video.id()
                        + " pertence ao experimento "
                        + video.experimentId()
                        + ", mas foi exibido nesta versão porque o HLS aponta para "
                        + slot.getSlotCode()
                        + ".")
            .toList();
    return new PdeProductionSlotVideoPanelDto(toProductionSlotDto(slot), resolvedVideos, alerts);
  }

  /** Verifica se o ativo é um vídeo HLS de hero que pode aparecer no painel PDE. */
  private boolean isPdeHeroHlsVideo(ExperimentVideoAsset video) {
    return video.getSlot() == ExperimentVideoSlot.LANDING_HERO
        && StringUtils.hasText(video.getHlsPlaybackUrl())
        && video.getHlsPlaybackUrl().contains(".m3u8");
  }

  /**
   * Resolve se um vídeo pertence ao slot por versão comercial ou, como fallback, por experimento.
   */
  private boolean belongsToSlot(
      PdeProductionSlot slot, List<PdeProductionSlot> allSlots, ExperimentVideoAsset video) {
    if (matchesVersionToken(slot, video)) {
      return true;
    }
    if (slot.getSourceExperimentId() == null
        || video.getExperiment() == null
        || !slot.getSourceExperimentId().equals(video.getExperiment().getId())) {
      return false;
    }
    return allSlots.stream()
        .filter(candidate -> !Objects.equals(candidate.getId(), slot.getId()))
        .noneMatch(candidate -> matchesVersionToken(candidate, video));
  }

  /** Informa se a URL HLS declara o token versionado do slot ou da experiência. */
  private boolean matchesVersionToken(PdeProductionSlot slot, ExperimentVideoAsset video) {
    String versionToken = versionToken(slot);
    if (!StringUtils.hasText(versionToken)) {
      return false;
    }
    String hls = video.getHlsPlaybackUrl().toLowerCase(Locale.ROOT);
    Pattern tokenPattern =
        Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(versionToken) + "([^a-z0-9]|$)");
    return tokenPattern.matcher(hls).find();
  }

  /** Extrai o token curto de versão usado nos ativos HLS do PDE. */
  private String versionToken(PdeProductionSlot slot) {
    String slotCode = slot.getSlotCode();
    if (StringUtils.hasText(slotCode) && slotCode.matches("(?i)v\\d+")) {
      return slotCode.toLowerCase(Locale.ROOT);
    }
    Matcher matcher =
        Pattern.compile("(^|[^a-z0-9])(v\\d+)([^a-z0-9]|$)", Pattern.CASE_INSENSITIVE)
            .matcher(slot.getExperienceVersion());
    return matcher.find() ? matcher.group(2).toLowerCase(Locale.ROOT) : null;
  }

  /** Ordena vídeos aprovados e prontos primeiro, depois mantém os mais recentes. */
  private int compareVideoPriority(ExperimentVideoAsset current, ExperimentVideoAsset next) {
    Comparator<ExperimentVideoAsset> comparator =
        Comparator.comparing(
                (ExperimentVideoAsset video) ->
                    video.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED)
            .reversed()
            .thenComparing(
                video -> video.getStatus() == ExperimentVideoStatus.READY,
                Comparator.reverseOrder())
            .thenComparing(ExperimentVideoAsset::getId, Comparator.reverseOrder());
    return comparator.compare(current, next);
  }

  /** Converte o ativo persistido em item de painel com a origem do vínculo. */
  private PdeProductionSlotVideoAssetDto toVideoAssetDto(
      PdeProductionSlot slot, ExperimentVideoAsset video) {
    Long experimentId = video.getExperiment() != null ? video.getExperiment().getId() : null;
    String assignmentSource =
        matchesVersionToken(slot, video) ? "VERSION_TOKEN" : "SOURCE_EXPERIMENT";
    return new PdeProductionSlotVideoAssetDto(
        video.getId(),
        experimentId,
        assignmentSource,
        video.getObjective(),
        video.getPrimaryMetric(),
        video.getProvider(),
        video.getModel(),
        video.getStatus(),
        video.getReviewStatus(),
        video.getAssetUrl(),
        video.getHlsPlaybackUrl(),
        video.getThumbnailUrl(),
        video.getDurationSeconds(),
        video.getSalesVideoProfile() != null ? video.getSalesVideoProfile().getId() : null,
        video.getSalesVideoJob() != null ? video.getSalesVideoJob().getId() : null,
        video.getAsset() != null ? video.getAsset().getId() : null,
        video.getLandingVideoSlot() != null ? video.getLandingVideoSlot().getId() : null);
  }

  /** Executa as chamadas HTTP mínimas que provam a entrega pública do slot. */
  private ValidationResult validateSlotDelivery(PdeProductionSlot slot)
      throws IOException, InterruptedException {
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
    String healthPath =
        StringUtils.hasText(text(contract, "healthPath")) ? text(contract, "healthPath") : "/";
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
    String scriptAssets = loadScriptAssets(resolvedUrl, page.body());
    Optional<String> textValidationError = validatePageTexts(contract, page.body(), scriptAssets);
    if (textValidationError.isPresent()) {
      return ValidationResult.failed(
          page.statusCode(),
          "Entrada pública contém copy inválida para cliente final",
          textValidationError.get(),
          contractSlug,
          healthPath,
          resolvedUrl);
    }
    String expectedStream = expectedHlsStream(slot.getExperienceVersion());
    if (StringUtils.hasText(expectedStream)) {
      AssetValidationResult stream = validateHlsStream(slot.getPublicUrl(), expectedStream);
      if (!stream.valid()) {
        return ValidationResult.failed(
            stream.httpStatus(),
            "HLS obrigatório da versão PDE não foi entregue",
            "Stream esperado: " + expectedStream + ". " + stream.detail(),
            contractSlug,
            healthPath,
            resolvedUrl);
      }
    }
    return ValidationResult.ok(
        page.statusCode(),
        "URL produtiva validada",
        "Health, contrato público, entrada do funil, copy pública e HLS versionado responderam.",
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
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url)).timeout(VALIDATION_TIMEOUT).GET().build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  /** Executa uma chamada GET descartando o corpo para validar ativos grandes. */
  private HttpResponse<Void> getWithoutBody(String url) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url)).timeout(VALIDATION_TIMEOUT).GET().build();
    return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
  }

  /** Carrega os bundles JavaScript referenciados pela entrada SPA para validar copy renderizada. */
  private String loadScriptAssets(String pageUrl, String pageHtml)
      throws IOException, InterruptedException {
    StringBuilder assets = new StringBuilder();
    for (String scriptUrl : scriptAssetUrls(pageUrl, pageHtml)) {
      HttpResponse<String> response = get(scriptUrl);
      if (isSuccess(response) && response.body() != null) {
        assets.append('\n').append(response.body());
      }
    }
    return assets.toString();
  }

  /** Extrai URLs absolutas dos scripts da entrada pública do PDE. */
  private Set<String> scriptAssetUrls(String pageUrl, String pageHtml) {
    Set<String> urls = new LinkedHashSet<>();
    Matcher matcher = SCRIPT_SRC_PATTERN.matcher(pageHtml);
    URI base = URI.create(pageUrl);
    while (matcher.find()) {
      String src = matcher.group(1);
      if (StringUtils.hasText(src)) {
        urls.add(base.resolve(src.trim()).toString());
      }
    }
    return urls;
  }

  /** Informa se a resposta HTTP está na faixa de sucesso. */
  private boolean isSuccess(HttpResponse<String> response) {
    return isSuccess(response.statusCode());
  }

  /** Informa se o status HTTP está na faixa de sucesso. */
  private boolean isSuccess(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  /** Valida o manifesto HLS e seu primeiro segmento para impedir falso 200 ou fallback HTML. */
  private AssetValidationResult validateHlsStream(String publicUrl, String streamPath)
      throws IOException, InterruptedException {
    String streamUrl = resolveUrl(publicUrl, streamPath);
    HttpResponse<Void> response = getWithoutBody(streamUrl);
    int statusCode = response.statusCode();
    String contentType = response.headers().firstValue("content-type").orElse("");
    if (!isSuccess(statusCode)) {
      return AssetValidationResult.failed(statusCode, "Manifesto HTTP " + statusCode);
    }
    if (!isHlsContentType(contentType)) {
      return AssetValidationResult.failed(
          statusCode,
          "Content-Type do manifesto recebido: "
              + (StringUtils.hasText(contentType) ? contentType : "ausente"));
    }
    HttpResponse<String> manifest = get(streamUrl);
    String segmentPath = firstHlsSegmentPath(manifest.body());
    if (!StringUtils.hasText(segmentPath)) {
      return AssetValidationResult.failed(statusCode, "Manifesto HLS sem segmento de vídeo");
    }
    return validateHlsSegment(resolveSiblingUrl(streamUrl, segmentPath));
  }

  /** Valida cabeçalhos mínimos do primeiro segmento HLS versionado. */
  private AssetValidationResult validateHlsSegment(String url)
      throws IOException, InterruptedException {
    HttpResponse<Void> response = getWithoutBody(url);
    int statusCode = response.statusCode();
    String contentType = response.headers().firstValue("content-type").orElse("");
    if (!isSuccess(statusCode)) {
      return AssetValidationResult.failed(statusCode, "HTTP " + statusCode);
    }
    String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
    if (!normalizedContentType.startsWith("video/")
        && !normalizedContentType.contains("mp2t")
        && !normalizedContentType.contains("octet-stream")) {
      return AssetValidationResult.failed(
          statusCode,
          "Content-Type do segmento recebido: "
              + (StringUtils.hasText(contentType) ? contentType : "ausente"));
    }
    return AssetValidationResult.ok(statusCode);
  }

  /** Informa se o content-type é compatível com playlist HLS. */
  private boolean isHlsContentType(String contentType) {
    String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    return normalized.contains("mpegurl")
        || normalized.contains("application/vnd.apple")
        || normalized.contains("application/x-mpegurl");
  }

  /** Extrai o primeiro segmento de vídeo do manifesto HLS. */
  private String firstHlsSegmentPath(String manifest) {
    if (!StringUtils.hasText(manifest)) {
      return null;
    }
    for (String line : manifest.split("\\R")) {
      String trimmed = line.trim();
      if (StringUtils.hasText(trimmed) && !trimmed.startsWith("#")) {
        return trimmed;
      }
    }
    return null;
  }

  /** Resolve um segmento relativo ao diretório do manifesto HLS. */
  private String resolveSiblingUrl(String manifestUrl, String segmentPath) {
    if (segmentPath.startsWith("http://") || segmentPath.startsWith("https://")) {
      return segmentPath;
    }
    int directoryEnd = manifestUrl.lastIndexOf('/');
    String directory = directoryEnd >= 0 ? manifestUrl.substring(0, directoryEnd + 1) : manifestUrl;
    return directory + segmentPath;
  }

  /** Confere textos obrigatórios e proibidos declarados no contrato público do PDE. */
  private Optional<String> validatePageTexts(
      JsonNode contract, String pageHtml, String scriptAssets) {
    String requiredSearchDocument = pageHtml + "\n" + scriptAssets;
    for (JsonNode requiredText : contract.withArray("requiredTexts")) {
      String text = requiredText.asText("");
      if (StringUtils.hasText(text) && !requiredSearchDocument.contains(text)) {
        return Optional.of("Texto obrigatório ausente: " + text);
      }
    }
    for (JsonNode forbiddenText : contract.withArray("forbiddenTexts")) {
      String text = forbiddenText.asText("");
      if (StringUtils.hasText(text)
          && pageHtml.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))) {
        return Optional.of("Texto técnico proibido encontrado: " + text.trim());
      }
    }
    return Optional.empty();
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

  /**
   * Mapeia versões PDE conhecidas para streams HLS aprovados que precisam existir no domínio
   * público.
   */
  private String expectedHlsStream(String experienceVersion) {
    return null;
  }

  /** Normaliza um campo obrigatório textual antes de persistir contrato de publicação. */
  private String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return value.trim();
  }

  /** Normaliza JSON opcional de contrato PDE antes de persistir como rascunho. */
  private String normalizeOptionalJson(String rawJson) {
    if (!StringUtils.hasText(rawJson)) {
      return null;
    }
    return normalizeRequiredJson(rawJson, "Contrato JSON da experiência PDE inválido");
  }

  /** Valida e formata o JSON obrigatório do contrato PDE. */
  private String normalizeRequiredJson(String rawJson, String message) {
    return normalizeRequiredJson(rawJson, message, null, null);
  }

  /** Valida, completa e formata o JSON obrigatório do contrato PDE. */
  private String normalizeRequiredJson(
      String rawJson, String message, String experienceVersion, String layoutKey) {
    if (!StringUtils.hasText(rawJson)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    try {
      JsonNode parsed = objectMapper.readTree(rawJson);
      if (!parsed.isObject()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Contrato JSON da experiência PDE deve ser um objeto");
      }
      ObjectNode contract = (ObjectNode) parsed;
      if (StringUtils.hasText(experienceVersion)) {
        contract.put("experienceVersion", experienceVersion.trim());
      }
      if (StringUtils.hasText(layoutKey)) {
        contract.put("layoutKey", layoutKey.trim());
      }
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, ex);
    }
  }

  /** Normaliza a chave de layout do slot ou deriva uma opção canônica da versão atual. */
  private String normalizeLayoutKey(String layoutKey, String slotCode, String experienceVersion) {
    if (StringUtils.hasText(layoutKey)) {
      return layoutKey.trim().toLowerCase(Locale.ROOT);
    }
    if ("v6".equals(slotCode)) {
      return "video-motivacional";
    }
    if ("v7".equals(slotCode)) {
      return "espelho-antes-de-sair";
    }
    if (StringUtils.hasText(experienceVersion)
        && experienceVersion.toLowerCase(Locale.ROOT).contains("estrada-desejo")) {
      return "estrada-desejo";
    }
    return DEFAULT_LAYOUT_KEY;
  }

  /** Retorna a chave de layout persistida ou uma chave compatível para registros legados. */
  private String resolveSlotLayoutKey(PdeProductionSlot slot) {
    return normalizeLayoutKey(slot.getLayoutKey(), slot.getSlotCode(), slot.getExperienceVersion());
  }

  /** Normaliza domínio removendo protocolo e barra final para evitar duplicidade operacional. */
  private String normalizeDomain(String value) {
    String domain =
        normalizeRequired(value, "Domínio do slot PDE obrigatório")
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
      return new ValidationResult(
          VALIDATION_OK,
          httpStatus,
          summary,
          detail,
          contractSlug,
          contractHealthPath,
          resolvedUrl);
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
      return new ValidationResult(
          VALIDATION_FAILED,
          httpStatus,
          summary,
          detail,
          contractSlug,
          contractHealthPath,
          resolvedUrl);
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
