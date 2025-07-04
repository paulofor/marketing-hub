package com.marketinghub.media.web;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.dto.AssetDto;
import com.marketinghub.media.dto.CreateAudioRequest;
import com.marketinghub.media.dto.CreateVideoRequest;
import com.marketinghub.media.mapper.AssetMapper;
import com.marketinghub.media.service.MediaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Media Hub.
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaService service;
    private final AssetMapper mapper;

    public MediaController(MediaService service, AssetMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/video")
    public AssetDto createVideo(@RequestBody CreateVideoRequest request) {
        Asset asset = service.createVideo(request);
        return mapper.toDto(asset);
    }

    @PostMapping("/audio")
    public AssetDto createAudio(@RequestBody CreateAudioRequest request) {
        Asset asset = service.createAudio(request);
        return mapper.toDto(asset);
    }

    @GetMapping("/{id}")
    public AssetDto getAsset(@PathVariable Long id) {
        return mapper.toDto(service.getAsset(id));
    }

    @GetMapping
    public List<AssetDto> listAssets(@RequestParam(required = false) AssetStatus status,
                                     @RequestParam(required = false) Long campaignId) {
        return service.findAssets(status, campaignId).stream().map(mapper::toDto).collect(Collectors.toList());
    }
}
