package com.marketinghub.imagegeneration.web;

import com.marketinghub.imagegeneration.dto.ImageGenerationModelDto;
import com.marketinghub.imagegeneration.service.ImageGenerationCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image-generation/models")
public class ImageGenerationModelController {

    private final ImageGenerationCatalogService catalogService;

    public ImageGenerationModelController(ImageGenerationCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ImageGenerationModelDto> listModels() {
        return catalogService.listModels();
    }
}
