package com.marketinghub.creative.web;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.CreativeVideoReviewSourceType;
import com.marketinghub.creative.dto.AssetUploadResponse;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.dto.CreativeAgentReviewPendingDto;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.creative.dto.CreativeDto;
import com.marketinghub.creative.dto.CreativeImprovementPendingDto;
import com.marketinghub.creative.dto.CreativeImprovementResultRequest;
import com.marketinghub.creative.dto.CreativeVideoReviewDto;
import com.marketinghub.creative.dto.UpdateCreativeLabelsRequest;
import com.marketinghub.creative.dto.UpdateCreativeStatusRequest;
import com.marketinghub.creative.mapper.CreativeMapper;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetUploadCategory;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Responsabilidade: expor endpoints REST para criação, revisão e publicação de criativos. */
@RestController
public class CreativeController {
  private final CreativeService service;
  private final CreativeMapper mapper;
  private final AssetRepository assetRepository;

  public CreativeController(
      CreativeService service, CreativeMapper mapper, AssetRepository assetRepository) {
    this.service = service;
    this.mapper = mapper;
    this.assetRepository = assetRepository;
  }

  /** Cria um criativo vinculado ao experimento informado. */
  @PostMapping("/api/experiments/{id}/creatives")
  public CreativeDto create(@PathVariable Long id, @RequestBody CreateCreativeRequest request) {
    return mapper.toDto(service.create(id, request));
  }

  /** Cria uma nova versão em rascunho sem alterar o criativo original. */
  @PostMapping("/api/creatives/{id}/versions")
  public CreativeDto createVersion(
      @PathVariable Long id, @RequestBody CreateCreativeRequest request) {
    return mapper.toDto(service.createVersion(id, request));
  }

  /** Reutiliza um anúncio aprovado do mesmo produto como rascunho auditável do experimento. */
  @PostMapping("/api/experiments/{experimentId}/creatives/reuse/{sourceCreativeId}")
  public CreativeDto reuseInExperiment(
      @PathVariable Long experimentId, @PathVariable Long sourceCreativeId) {
    return mapper.toDto(service.reuseInExperiment(experimentId, sourceCreativeId));
  }

  /** Lista os criativos de um experimento. */
  @GetMapping("/api/experiments/{id}/creatives")
  public List<CreativeDto> list(@PathVariable Long id) {
    List<CreativeDto> dtos =
        StreamSupport.stream(service.listByExperiment(id).spliterator(), false)
            .map(mapper::toDto)
            .toList();
    List<String> creativeUrls =
        dtos.stream().map(CreativeDto::getImageUrl).filter(StringUtils::hasText).toList();
    if (creativeUrls.isEmpty()) {
      return dtos;
    }
    Map<String, Asset> assetsByUrl =
        assetRepository.findByUrlIn(creativeUrls).stream()
            .filter(asset -> StringUtils.hasText(asset.getUrl()))
            .collect(
                Collectors.toMap(
                    Asset::getUrl,
                    Function.identity(),
                    (left, right) -> {
                      Long leftId = Objects.requireNonNullElse(left.getId(), 0L);
                      Long rightId = Objects.requireNonNullElse(right.getId(), 0L);
                      return Comparator.<Long>naturalOrder().compare(leftId, rightId) >= 0
                          ? left
                          : right;
                    }));
    dtos.forEach(
        dto -> {
          Asset asset = assetsByUrl.get(dto.getImageUrl());
          if (asset != null && StringUtils.hasText(asset.getPrompt())) {
            dto.setImagePrompt(asset.getPrompt().trim());
          }
          if (asset != null && StringUtils.hasText(asset.getPromptIntermediate())) {
            dto.setImageIntermediatePrompt(asset.getPromptIntermediate().trim());
          }
        });
    return dtos;
  }

  /** Lista os criativos de vídeo que precisam de revisão comercial. */
  @GetMapping("/api/creatives/video-review")
  public List<CreativeVideoReviewDto> listVideoReview(
      @RequestParam(value = "status", required = false) CreativeStatus status) {
    return service.listVideoReviewQueue(status);
  }

  /** Atualiza o status de um item da fila única de revisão de vídeos. */
  @PatchMapping("/api/creatives/video-review/{sourceType}/{id}/status")
  public CreativeVideoReviewDto updateVideoReviewStatus(
      @PathVariable CreativeVideoReviewSourceType sourceType,
      @PathVariable Long id,
      @RequestBody UpdateCreativeStatusRequest request) {
    return service.updateVideoReviewStatus(
        sourceType, id, request.status(), request.rejectionReason());
  }

  /** Atualiza todos os dados de um criativo existente. */
  @PutMapping("/api/creatives/{id}")
  public CreativeDto update(@PathVariable Long id, @RequestBody CreateCreativeRequest request) {
    return mapper.toDto(service.update(id, request));
  }

  /** Atualiza o status de revisão de um criativo, incluindo motivo quando houver reprovação. */
  @PatchMapping("/api/creatives/{id}/status")
  public CreativeDto updateStatus(
      @PathVariable Long id, @RequestBody UpdateCreativeStatusRequest request) {
    return mapper.toDto(service.updateStatus(id, request.status(), request.rejectionReason()));
  }

  /** Entrega ao AI Worker anúncios pendentes e os marca como em processamento. */
  @GetMapping("/api/internal/creatives/agent-review/stage-executions/pending")
  public List<CreativeAgentReviewPendingDto> pendingAgentReviews(
      @RequestParam(value = "limit", defaultValue = "5") int limit) {
    return service.claimAgentReviewQueue(limit);
  }

  /** Solicita revisão do agente sem alterar ou liberar o conteúdo do anúncio. */
  @PostMapping("/api/creatives/{id}/agent-review/request")
  public CreativeDto requestAgentReview(@PathVariable Long id) {
    return mapper.toDto(service.requestAgentReview(id));
  }

  /** Recebe e persiste o resultado auditável da revisão multimodal. */
  @PostMapping("/api/internal/creatives/{id}/agent-review/result")
  public CreativeDto applyAgentReview(
      @PathVariable Long id, @RequestBody CreativeAgentReviewResultRequest request) {
    return mapper.toDto(service.applyAgentReview(id, request));
  }

  /** Entrega ao AI Worker correções decididas pelo agente e marca cada item como processando. */
  @GetMapping("/api/internal/creatives/agent-improvement/stage-executions/pending")
  public List<CreativeImprovementPendingDto> pendingAgentImprovements(
      @RequestParam(value = "limit", defaultValue = "3") int limit) {
    return service.claimAgentImprovementQueue(limit);
  }

  /** Recebe a nova arte e cria uma versão que volta automaticamente para revisão do agente. */
  @PostMapping("/api/internal/creatives/{id}/agent-improvement/result")
  public CreativeDto completeAgentImprovement(
      @PathVariable Long id, @RequestBody CreativeImprovementResultRequest request) {
    return mapper.toDto(service.completeAgentImprovement(id, request));
  }

  /** Remove um criativo existente. */
  @DeleteMapping("/api/creatives/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  /** Atualiza os rótulos comerciais de um criativo. */
  @PatchMapping("/api/creatives/{id}/labels")
  public CreativeDto patchLabels(
      @PathVariable Long id, @RequestBody UpdateCreativeLabelsRequest request) {
    return mapper.toDto(
        service.updateLabels(
            id, request.getAngleId(), request.getVisualProofId(), request.getEmotionalTriggerId()));
  }

  /** Salva um asset enviado manualmente para uso em criativos. */
  @PostMapping("/api/assets")
  public ResponseEntity<AssetUploadResponse> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "prompt", required = false) String prompt,
      @RequestParam(value = "intermediatePrompt", required = false) String intermediatePrompt,
      @RequestParam(value = "model", required = false) String model,
      @RequestParam(value = "category", required = false) String category,
      @RequestParam(value = "experimentId", required = false) Long experimentId,
      @RequestParam(value = "flowId", required = false) Long flowId,
      @RequestParam(value = "flowSlug", required = false) String flowSlug)
      throws IOException {
    AssetUploadCategory uploadCategory = AssetUploadCategory.fromKey(category);
    AssetUploadResponse response =
        service.uploadImage(
            file,
            model,
            prompt,
            intermediatePrompt,
            uploadCategory,
            experimentId,
            flowId,
            flowSlug);
    HttpHeaders headers = new HttpHeaders();
    if (StringUtils.hasText(response.url())) {
      headers.setLocation(URI.create(response.url()));
      headers.set(HttpHeaders.CONTENT_LOCATION, response.url());
    }
    return ResponseEntity.ok().headers(headers).body(response);
  }

  /** Retorna o HTML de prévia do criativo publicado na Meta quando disponível. */
  @GetMapping("/api/creatives/{id}/preview")
  public String preview(@PathVariable Long id) throws Exception {
    return service.preview(id);
  }
}
