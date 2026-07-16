package com.marketinghub.imagegenerator.web;

import com.marketinghub.imagegenerator.dto.ImageGeneratorRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse;
import com.marketinghub.imagegenerator.service.ImageGeneratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a geração manual de imagens por IA para a tela do Marketing Hub. */
@RestController
@RequestMapping("/api/image-generator")
public class ImageGeneratorController {
    private final ImageGeneratorService service;

    /** Inicializa o controller com o serviço de geração de imagens. */
    public ImageGeneratorController(ImageGeneratorService service) {
        this.service = service;
    }

    /** Gera uma imagem a partir do prompt informado pelo usuário. */
    @PostMapping("/generations")
    public ImageGeneratorResponse generate(@Valid @RequestBody ImageGeneratorRequest request) {
        return service.generate(request);
    }
}
