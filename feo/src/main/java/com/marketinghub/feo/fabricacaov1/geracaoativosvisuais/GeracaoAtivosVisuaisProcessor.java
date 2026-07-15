package com.marketinghub.feo.fabricacaov1.geracaoativosvisuais;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyInput;
import com.marketinghub.feo.fabricacaov1.contract.VisualAsset;
import com.marketinghub.feo.fabricacaov1.contract.VisualAssetSpec;
import com.marketinghub.feo.fabricacaov1.pipeline.StageArtifact;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageProcessor;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Gera capa, infográficos e figuras internas antes da montagem final do pacote.
 */
@Component
public class GeracaoAtivosVisuaisProcessor implements StageProcessor<PackageAssemblyInput, PackageAssemblyInput> {

    private final VisualAssetGenerator generator;
    private final ObjectMapper objectMapper;

    /**
     * Recebe gerador de imagens e serializador para publicar auditoria separada do produto.
     */
    public GeracaoAtivosVisuaisProcessor(VisualAssetGenerator generator, ObjectMapper objectMapper) {
        this.generator = generator;
        this.objectMapper = objectMapper;
    }

    /**
     * Retorna a etapa canônica de geração de imagens editoriais.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.GERACAO_ATIVOS_VISUAIS;
    }

    /**
     * Gera todos os ativos visuais obrigatórios e bloqueia montagem se faltar imagem.
     */
    @Override
    public StageResult<PackageAssemblyInput> process(StageContext<PackageAssemblyInput> context) {
        PackageAssemblyInput input = context.input();
        if (input.contentPackage() == null
                || input.contentPackage().visualAssets() == null
                || input.contentPackage().visualAssets().isEmpty()) {
            return StageResult.blocked("FEO sem plano de imagens editoriais para enriquecer o pacote.", List.of());
        }
        List<VisualAsset> generated = new ArrayList<>();
        try {
            for (VisualAssetSpec spec : input.contentPackage().visualAssets()) {
                generated.add(generator.generate(spec));
            }
        } catch (Exception ex) {
            StageArtifact artifact = context.artifactStore().store(
                    "FEO_VISUAL_ASSETS_REJECTED",
                    "feo-visual-assets-rejected.json",
                    "application/json",
                    toJson(input.contentPackage().visualAssets()));
            return StageResult.blocked("Geração de imagens FEO falhou: " + ex.getMessage(), List.of(artifact));
        }
        PackageAssemblyInput output = new PackageAssemblyInput(
                input.context(),
                input.plan(),
                input.contentPackage(),
                generated);
        StageArtifact artifact = context.artifactStore().store(
                "FEO_VISUAL_ASSETS",
                "feo-visual-assets.json",
                "application/json",
                toJson(auditOnly(generated)));
        return StageResult.completedWithNext(
                output,
                List.of(artifact),
                Map.of("visualAssetCount", generated.size(), "qualityGate", "VISUAL_ASSETS_READY"),
                StageCode.MONTAGEM_PACOTE);
    }

    /**
     * Remove bytes das imagens do artefato JSON para manter auditoria leve.
     */
    private List<Map<String, Object>> auditOnly(List<VisualAsset> assets) {
        return assets.stream()
                .map(asset -> Map.<String, Object>of(
                        "code", asset.code(),
                        "title", asset.title(),
                        "assetType", asset.assetType(),
                        "fileName", asset.fileName(),
                        "contentType", asset.contentType(),
                        "model", asset.model(),
                        "prompt", asset.prompt(),
                        "providerRequest", asset.providerRequest(),
                        "providerResponse", asset.providerResponse()))
                .toList();
    }

    /**
     * Serializa objeto como JSON auditável.
     */
    private byte[] toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar imagens FEO", ex);
        }
    }
}
