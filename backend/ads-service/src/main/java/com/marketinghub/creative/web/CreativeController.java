package com.marketinghub.creative.web;

import com.marketinghub.creative.dto.AssetUploadResponse;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.dto.CreativeDto;
import com.marketinghub.creative.mapper.CreativeMapper;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.creative.dto.UpdateCreativeLabelsRequest;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetUploadCategory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST controller for creatives.
 */
@RestController
public class CreativeController {
    private final CreativeService service;
    private final CreativeMapper mapper;
    private final AssetRepository assetRepository;

    public CreativeController(CreativeService service, CreativeMapper mapper, AssetRepository assetRepository) {
        this.service = service;
        this.mapper = mapper;
        this.assetRepository = assetRepository;
    }

    @PostMapping("/api/experiments/{id}/creatives")
    public CreativeDto create(@PathVariable Long id, @RequestBody CreateCreativeRequest request) {
        return mapper.toDto(service.create(id, request));
    }

    @GetMapping("/api/experiments/{id}/creatives")
    public List<CreativeDto> list(@PathVariable Long id) {
        List<CreativeDto> dtos = StreamSupport.stream(service.listByExperiment(id).spliterator(), false)
                .map(mapper::toDto)
                .toList();
        List<String> creativeUrls = dtos.stream()
                .map(CreativeDto::getImageUrl)
                .filter(StringUtils::hasText)
                .toList();
        if (creativeUrls.isEmpty()) {
            return dtos;
        }
        Map<String, Asset> assetsByUrl = assetRepository.findByUrlIn(creativeUrls).stream()
                .filter(asset -> StringUtils.hasText(asset.getUrl()))
                .collect(Collectors.toMap(
                        Asset::getUrl,
                        Function.identity(),
                        (left, right) -> {
                            Long leftId = Objects.requireNonNullElse(left.getId(), 0L);
                            Long rightId = Objects.requireNonNullElse(right.getId(), 0L);
                            return Comparator.<Long>naturalOrder().compare(leftId, rightId) >= 0 ? left : right;
                        }));
        dtos.forEach(dto -> {
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

    @PutMapping("/api/creatives/{id}")
    public CreativeDto update(@PathVariable Long id, @RequestBody CreateCreativeRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @DeleteMapping("/api/creatives/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("/api/creatives/{id}/labels")
    public CreativeDto patchLabels(@PathVariable Long id,
                                   @RequestBody UpdateCreativeLabelsRequest request) {
        return mapper.toDto(service.updateLabels(id,
                request.getAngleId(), request.getVisualProofId(), request.getEmotionalTriggerId()));
    }

    @PostMapping("/api/assets")
    public ResponseEntity<AssetUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "prompt", required = false) String prompt,
                                                      @RequestParam(value = "intermediatePrompt", required = false)
                                                      String intermediatePrompt,
                                                      @RequestParam(value = "model", required = false) String model,
                                                      @RequestParam(value = "category", required = false) String category,
                                                      @RequestParam(value = "experimentId", required = false) Long experimentId,
                                                      @RequestParam(value = "flowId", required = false) Long flowId,
                                                      @RequestParam(value = "flowSlug", required = false) String flowSlug) throws IOException {
        AssetUploadCategory uploadCategory = AssetUploadCategory.fromKey(category);
        AssetUploadResponse response = service.uploadImage(file, model, prompt, intermediatePrompt,
                uploadCategory, experimentId, flowId, flowSlug);
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(response.url())) {
            headers.setLocation(URI.create(response.url()));
            headers.set(HttpHeaders.CONTENT_LOCATION, response.url());
        }
        return ResponseEntity.ok().headers(headers).body(response);
    }

    @GetMapping("/api/creatives/{id}/preview")
    public String preview(@PathVariable Long id) throws Exception {
        return service.preview(id);
    }
}
