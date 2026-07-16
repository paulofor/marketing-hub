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
import com.marketinghub.feo.infrastructure.config.FeoProperties;
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
    private final FeoProperties properties;

    /**
     * Recebe gerador de imagens e serializador para publicar auditoria separada do produto.
     */
    public GeracaoAtivosVisuaisProcessor(VisualAssetGenerator generator, ObjectMapper objectMapper, FeoProperties properties) {
        this.generator = generator;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Retorna a etapa canônica de geração de imagens editoriais.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.GERACAO_ATIVOS_VISUAIS;
    }

    /**
     * Gera ativos visuais externos quando habilitado e usa fallback local para impedir pacote final sem imagem.
     */
    @Override
    public StageResult<PackageAssemblyInput> process(StageContext<PackageAssemblyInput> context) {
        PackageAssemblyInput input = context.input();
        if (input.contentPackage() == null
                || input.contentPackage().visualAssets() == null
                || input.contentPackage().visualAssets().isEmpty()) {
            PackageAssemblyInput output = new PackageAssemblyInput(
                    input.context(),
                    input.plan(),
                    input.contentPackage(),
                    List.of());
            return StageResult.completedWithNext(
                    output,
                    List.of(),
                    Map.of("visualAssetCount", 0, "qualityGate", "VISUAL_ASSETS_NOT_PLANNED"),
                    StageCode.MONTAGEM_PACOTE);
        }
        if (!properties.visualAssetsEnabled()) {
            List<VisualAsset> generated = generateTemplateAssets(plannedAssets(input));
            PackageAssemblyInput output = new PackageAssemblyInput(
                    input.context(),
                    input.plan(),
                    input.contentPackage(),
                    generated);
            StageArtifact artifact = context.artifactStore().store(
                    "FEO_VISUAL_ASSETS_TEMPLATE",
                    "feo-visual-assets-template.json",
                    "application/json",
                    toJson(auditOnly(generated)));
            return StageResult.completedWithNext(
                    output,
                    List.of(artifact),
                    Map.of("visualAssetCount", generated.size(), "qualityGate", "VISUAL_ASSETS_TEMPLATE_READY"),
                    StageCode.MONTAGEM_PACOTE);
        }
        List<VisualAsset> generated = new ArrayList<>();
        try {
            for (VisualAssetSpec spec : input.contentPackage().visualAssets()) {
                generated.add(generator.generate(spec));
            }
        } catch (Exception ex) {
            generated = mergeWithTemplateFallback(generated, input.contentPackage().visualAssets());
            StageArtifact artifact = context.artifactStore().store(
                    "FEO_VISUAL_ASSETS_FALLBACK",
                    "feo-visual-assets-fallback.json",
                    "application/json",
                    toJson(auditOnly(generated)));
            PackageAssemblyInput output = new PackageAssemblyInput(
                    input.context(),
                    input.plan(),
                    input.contentPackage(),
                    generated);
            return StageResult.completedWithNext(
                    output,
                    List.of(artifact),
                    Map.of("visualAssetCount", generated.size(), "qualityGate", "VISUAL_ASSETS_EXTERNAL_FAILED_TEMPLATE_READY"),
                    StageCode.MONTAGEM_PACOTE);
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
     * Retorna plano de imagens sem exigir que o pacote tenha essa secao.
     */
    private List<VisualAssetSpec> plannedAssets(PackageAssemblyInput input) {
        if (input.contentPackage() == null || input.contentPackage().visualAssets() == null) {
            return List.of();
        }
        return input.contentPackage().visualAssets();
    }

    /**
     * Gera imagens locais para todas as especificações planejadas.
     */
    private List<VisualAsset> generateTemplateAssets(List<VisualAssetSpec> specs) {
        return specs.stream().map(TemplateVisualAssetFactory::create).toList();
    }

    /**
     * Completa imagens faltantes com fallback local quando a integração externa falha no meio da geração.
     */
    private List<VisualAsset> mergeWithTemplateFallback(List<VisualAsset> generated, List<VisualAssetSpec> specs) {
        List<VisualAsset> completed = new ArrayList<>(generated);
        List<String> generatedCodes = generated.stream().map(VisualAsset::code).toList();
        for (VisualAssetSpec spec : specs) {
            if (!generatedCodes.contains(spec.code())) {
                completed.add(TemplateVisualAssetFactory.create(spec));
            }
        }
        return completed;
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
