package com.marketinghub.creative.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.*;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

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

    /**
     * Saves the uploaded image and returns a public URL.
     */
    public String uploadImage(MultipartFile file) throws IOException {
        Path dir = Path.of("uploads");
        Files.createDirectories(dir);
        Path path = Files.createTempFile(dir, "img", file.getOriginalFilename());
        file.transferTo(path);
        return "/uploads/" + path.getFileName();
    }

    /**
     * Fetches the preview HTML from Facebook Marketing API.
     */
    public String preview(Long creativeId) throws IOException, InterruptedException {
        String token = System.getenv("FB_ACCESS_TOKEN");
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
