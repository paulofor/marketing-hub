package com.marketinghub.creative.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.*;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.creative.label.repository.VisualProofRepository;
import com.marketinghub.creative.label.repository.EmotionalTriggerRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Service layer for creatives.
 */
@Service
@RequiredArgsConstructor
public class CreativeService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CreativeRepository repository;
    private final ExperimentRepository experimentRepository;
    private final AngleRepository angleRepository;
    private final VisualProofRepository visualProofRepository;
    private final EmotionalTriggerRepository emotionalTriggerRepository;
    private final AssetRepository assetRepository;
    private final HttpClient httpClient;

    /**
     * Creates and stores a creative.
     */
    @Transactional
    public Creative create(Long experimentId, CreateCreativeRequest request) {
        Experiment exp = experimentRepository.findById(experimentId).orElseThrow();
        Creative creative = Creative.builder()
                .experiment(exp)
                .headline(request.getHeadline())
                .primaryText(request.getPrimaryText())
                .imageUrl(request.getImageUrl())
                .status(request.getStatus())
                .build();
        return repository.save(creative);
    }

    /**
     * Updates an existing creative.
     */
    @Transactional
    public Creative update(Long id, CreateCreativeRequest request) {
        Creative creative = repository.findById(id).orElseThrow();
        creative.setHeadline(request.getHeadline());
        creative.setPrimaryText(request.getPrimaryText());
        creative.setImageUrl(request.getImageUrl());
        creative.setStatus(request.getStatus());
        return creative;
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Iterable<Creative> listByExperiment(Long experimentId) {
        return repository.findByExperimentId(experimentId);
    }

    @Transactional
    public Creative updateLabels(Long id, Long angleId,
                                 Long proofId,
                                 Long triggerId) {
        Creative creative = repository.findById(id).orElseThrow();
        if (angleId != null) {
            creative.setAngles(java.util.Set.of(angleRepository.findById(angleId).orElseThrow()));
        }
        if (proofId != null) {
            creative.setVisualProofs(java.util.Set.of(visualProofRepository.findById(proofId).orElseThrow()));
        }
        if (triggerId != null) {
            creative.setEmotionalTriggers(java.util.Set.of(emotionalTriggerRepository.findById(triggerId).orElseThrow()));
        }
        return creative;
    }

    /**
     * Saves the uploaded image and returns a public URL.
     */
    public String uploadImage(MultipartFile file, String model, String prompt) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }
        Path dir = Path.of("uploads");
        Files.createDirectories(dir);
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String suffix = extension != null && !extension.isBlank() ? "." + extension : ".bin";
        Path path = Files.createTempFile(dir, "img-", suffix);
        file.transferTo(path);
        String relativeUrl = "/uploads/" + path.getFileName();
        Asset asset = Asset.builder()
                .type(AssetType.IMAGE)
                .provider(MediaProvider.OPENAI)
                .status(AssetStatus.READY)
                .url(relativeUrl)
                .model(StringUtils.hasText(model) ? model : null)
                .prompt(prompt)
                .build();
        assetRepository.save(asset);
        return relativeUrl;
    }

    /**
     * Fetches the preview HTML from Facebook Marketing API.
     */
    public String preview(Long creativeId) throws IOException, InterruptedException {
        String token = System.getProperty("FB_ACCESS_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getenv("FB_ACCESS_TOKEN");
        }
        if (token == null || token.isBlank()) {
            return "";
        }
        String url = "https://graph.facebook.com/v19.0/adcreatives/" + creativeId
                + "/previews?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = MAPPER.readTree(resp.body());
        if (node.has("data") && node.get("data").isArray() && node.get("data").size() > 0) {
            return node.get("data").get(0).get("body").asText();
        }
        return "";
    }
}
