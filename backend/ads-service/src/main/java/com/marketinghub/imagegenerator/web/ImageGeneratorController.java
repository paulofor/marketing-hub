package com.marketinghub.imagegenerator.web;

import com.marketinghub.imagegenerator.dto.ImageGenerationHistoryItem;
import com.marketinghub.imagegenerator.dto.ImageGeneratorRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse;
import com.marketinghub.imagegenerator.dto.LandingImagePromotionRequest;
import com.marketinghub.imagegenerator.dto.LandingImagePromotionResponse;
import com.marketinghub.imagegenerator.service.ImageGeneratorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  /** Gera as imagens comparativas a partir do prompt informado pelo usuário. */
  @PostMapping("/generations")
  public ImageGeneratorResponse generate(@Valid @RequestBody ImageGeneratorRequest request) {
    return service.generate(request);
  }

  /** Lista gerações recentes do contexto sem carregar os assets base64. */
  @GetMapping("/generations/recent")
  public List<ImageGenerationHistoryItem> recent(
      @RequestParam Long productId,
      @RequestParam Long commercialPlanId,
      @RequestParam(required = false) Long experimentId) {
    return service.listRecent(productId, commercialPlanId, experimentId);
  }

  /** Recupera o asset persistido escolhido dentro do mesmo contexto comercial. */
  @GetMapping("/generations/{jobId}")
  public ImageGeneratorResponse.ImageGeneratorResult get(
      @PathVariable String jobId,
      @RequestParam Long productId,
      @RequestParam Long commercialPlanId,
      @RequestParam(required = false) Long experimentId) {
    return service.getGeneratedImage(productId, commercialPlanId, experimentId, jobId);
  }

  /** Aplica uma geração concluída a um slot da landing sem publicar a página. */
  @PostMapping("/generations/{jobId}/landing-assets")
  public LandingImagePromotionResponse promoteToLanding(
      @PathVariable String jobId, @Valid @RequestBody LandingImagePromotionRequest request) {
    return service.promoteToLanding(jobId, request);
  }
}
