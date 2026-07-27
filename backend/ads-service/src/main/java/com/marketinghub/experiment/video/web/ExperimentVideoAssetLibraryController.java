package com.marketinghub.experiment.video.web;

import com.marketinghub.experiment.video.dto.ExperimentVideoAssetDto;
import com.marketinghub.experiment.video.service.ExperimentVideoAssetService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a biblioteca global de vídeos comerciais dos experimentos. */
@RestController
@RequestMapping("/api/experiments/video-assets")
public class ExperimentVideoAssetLibraryController {
  private final ExperimentVideoAssetService service;

  /** Inicializa o controller com o serviço de vídeos de experimento. */
  public ExperimentVideoAssetLibraryController(ExperimentVideoAssetService service) {
    this.service = service;
  }

  /** Lista todos os vídeos comerciais registrados nos experimentos. */
  @GetMapping
  public List<ExperimentVideoAssetDto> listAll() {
    return service.listAll();
  }
}
