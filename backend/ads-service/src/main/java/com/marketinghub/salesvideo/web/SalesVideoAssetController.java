package com.marketinghub.salesvideo.web;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.dto.AssetDto;
import com.marketinghub.media.mapper.AssetMapper;
import com.marketinghub.salesvideo.service.SalesVideoAssetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Endpoints internos para upload de assets do módulo de vídeo.
 */
@RestController
@RequestMapping("/internal/video/assets")
public class SalesVideoAssetController {
    private final SalesVideoAssetService assetService;
    private final AssetMapper assetMapper;

    public SalesVideoAssetController(SalesVideoAssetService assetService,
                                     AssetMapper assetMapper) {
        this.assetService = assetService;
        this.assetMapper = assetMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AssetDto upload(@RequestParam("file") MultipartFile file,
                           @RequestParam(name = "assetType", required = false) AssetType assetType,
                           @RequestParam(name = "provider", required = false) MediaProvider provider,
                           @RequestParam(name = "metadata", required = false) String metadata) throws IOException {
        Asset asset = assetService.store(file, assetType, provider, metadata);
        return assetMapper.toDto(asset);
    }
}
