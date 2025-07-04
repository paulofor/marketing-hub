package com.marketinghub.media.service;

import com.marketinghub.media.*;
import com.marketinghub.media.client.*;
import com.marketinghub.media.dto.CreateAudioRequest;
import com.marketinghub.media.dto.CreateVideoRequest;
import com.marketinghub.media.repository.AssetRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service layer for Media Hub.
 */
@Service
public class MediaService {
    private final AssetRepository repository;
    private final SynthesiaClient synthesia;
    private final HeyGenClient heyGen;
    private final ElevenLabsClient elevenLabs;
    private final RunwayClient runway;
    private final SimpMessagingTemplate messagingTemplate;

    public MediaService(AssetRepository repository, SynthesiaClient synthesia, HeyGenClient heyGen,
                        ElevenLabsClient elevenLabs, RunwayClient runway, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.synthesia = synthesia;
        this.heyGen = heyGen;
        this.elevenLabs = elevenLabs;
        this.runway = runway;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates a video asset asynchronously.
     */
    @Transactional
    public Asset createVideo(CreateVideoRequest request) {
        Asset asset = Asset.builder()
                .type(AssetType.VIDEO)
                .provider(request.getProvider())
                .status(AssetStatus.PENDING)
                .payload(request.getScript())
                .campaignId(request.getCampaignId())
                .build();
        repository.save(asset);
        createVideoAsync(asset, request);
        return asset;
    }

    @Async
    void createVideoAsync(Asset asset, CreateVideoRequest request) {
        asset.setStatus(AssetStatus.PROCESSING);
        repository.save(asset);
        Mono<Map<String, Object>> mono;
        if (request.getProvider() == MediaProvider.SYNTHESIA) {
            mono = synthesia.createVideo(Map.of("script", request.getScript()));
        } else {
            mono = heyGen.createVideo(Map.of("script", request.getScript()));
        }
        mono.map(resp -> {
            asset.setExternalId(resp.get("id").toString());
            repository.save(asset);
            return asset;
        }).subscribe();
    }

    /**
     * Creates an audio asset asynchronously.
     */
    @Transactional
    public Asset createAudio(CreateAudioRequest request) {
        Asset asset = Asset.builder()
                .type(AssetType.AUDIO)
                .provider(request.getProvider())
                .status(AssetStatus.PENDING)
                .payload(request.getScript())
                .campaignId(request.getCampaignId())
                .build();
        repository.save(asset);
        createAudioAsync(asset, request);
        return asset;
    }

    @Async
    void createAudioAsync(Asset asset, CreateAudioRequest request) {
        asset.setStatus(AssetStatus.PROCESSING);
        repository.save(asset);
        Mono<Map<String, Object>> mono = elevenLabs.createSpeech(request.getVoice(), Map.of("text", request.getScript()));
        mono.map(resp -> {
            asset.setExternalId(resp.get("id").toString());
            repository.save(asset);
            return asset;
        }).subscribe();
    }

    public Asset getAsset(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public List<Asset> findAssets(AssetStatus status, Long campaignId) {
        if (status != null && campaignId != null) {
            return repository.findByStatusAndCampaignId(status, campaignId);
        } else if (status != null) {
            return repository.findByStatus(status);
        } else {
            return repository.findAll();
        }
    }

    /**
     * Periodically refresh status of processing assets.
     */
    @Scheduled(fixedDelay = 300000)
    public void refreshStatus() {
        List<Asset> processing = repository.findByStatus(AssetStatus.PROCESSING);
        for (Asset asset : processing) {
            Mono<Map<String, Object>> mono;
            switch (asset.getProvider()) {
                case SYNTHESIA -> mono = synthesia.getVideo(asset.getExternalId());
                case HEYGEN -> mono = heyGen.getVideo(asset.getExternalId());
                case RUNWAY -> mono = runway.getJob(asset.getExternalId());
                default -> mono = Mono.empty();
            }
            mono.map(resp -> {
                // TODO: map provider response to status/url
                asset.setStatus(AssetStatus.READY);
                asset.setUrl("TODO");
                repository.save(asset);
                messagingTemplate.convertAndSend("/topic/assets", asset.getId());
                return asset;
            }).subscribe();
        }
    }
}
