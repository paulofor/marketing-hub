package com.marketinghub.experiment.video.web;

import com.marketinghub.experiment.video.dto.CreateExperimentVideoAssetRequest;
import com.marketinghub.experiment.video.dto.ExperimentVideoAssetDto;
import com.marketinghub.experiment.video.dto.RequestExperimentVeoVideoRequest;
import com.marketinghub.experiment.video.dto.UpdateExperimentVideoAssetRequest;
import com.marketinghub.experiment.video.service.ExperimentVideoAssetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe vídeos de experimento para planejamento, revisão e liberação do funil.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/video-assets")
public class ExperimentVideoAssetController {
    private final ExperimentVideoAssetService service;

    /** Inicializa o controller com o serviço de vídeos de experimento. */
    public ExperimentVideoAssetController(ExperimentVideoAssetService service) {
        this.service = service;
    }

    /** Lista os vídeos vinculados a um experimento. */
    @GetMapping
    public List<ExperimentVideoAssetDto> list(@PathVariable Long experimentId) {
        return service.list(experimentId);
    }

    /** Cria um novo vídeo comercial para o experimento. */
    @PostMapping
    public ExperimentVideoAssetDto create(@PathVariable Long experimentId,
                                          @Valid @RequestBody CreateExperimentVideoAssetRequest request) {
        return service.create(experimentId, request);
    }

    /** Cria perfil, script, job VEO e vínculo obrigatório de vídeo para o experimento. */
    @PostMapping("/veo-render-requests")
    public ExperimentVideoAssetDto requestVeoRender(@PathVariable Long experimentId,
                                                    @Valid @RequestBody RequestExperimentVeoVideoRequest request) {
        return service.requestVeoRender(experimentId, request);
    }

    /** Atualiza status, revisão e vínculos de um vídeo comercial do experimento. */
    @PatchMapping("/{videoAssetId}")
    public ExperimentVideoAssetDto update(@PathVariable Long experimentId,
                                          @PathVariable Long videoAssetId,
                                          @RequestBody UpdateExperimentVideoAssetRequest request) {
        return service.update(experimentId, videoAssetId, request);
    }
}
