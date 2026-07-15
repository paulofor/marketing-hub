package com.marketinghub.feo.fabricacaov1.montagempacote;

import com.marketinghub.feo.fabricacaov1.contract.DigitalAssetFinal;
import com.marketinghub.feo.fabricacaov1.contract.FabricationReport;
import com.marketinghub.feo.fabricacaov1.contract.ManifestItem;
import com.marketinghub.feo.fabricacaov1.contract.OfferDeliveryManifest;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyInput;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyOutput;
import com.marketinghub.feo.fabricacaov1.pipeline.StageArtifact;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageProcessor;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Monta o pacote final de entrega com arquivos profissionais.
 */
@Component
public class MontagemPacoteProcessor implements StageProcessor<PackageAssemblyInput, PackageAssemblyOutput> {

    private final PackageAssetAssembler assembler;

    /**
     * Recebe o montador deterministico dos arquivos finais.
     */
    public MontagemPacoteProcessor(PackageAssetAssembler assembler) {
        this.assembler = assembler;
    }

    /**
     * Retorna a etapa canonica de montagem final.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.MONTAGEM_PACOTE;
    }

    /**
     * Gera HTML, PDF, planilha CSV, manifesto e ZIP do produto final.
     */
    @Override
    public StageResult<PackageAssemblyOutput> process(StageContext<PackageAssemblyInput> context) {
        PackageAssemblyInput input = context.input();
        if (input.contentPackage() == null || input.contentPackage().deliverables().isEmpty()) {
            return StageResult.blocked("Montagem FEO sem conteúdos finais redigidos.", List.of());
        }
        PackageAssemblyOutput output = assembler.assemble(input);
        List<StageArtifact> artifacts = List.of(
                toArtifact(context, "FINAL_HTML", output.html()),
                toArtifact(context, "FINAL_PDF", output.pdf()),
                toArtifact(context, "FINAL_SPREADSHEET", output.spreadsheet()),
                toArtifact(context, "FINAL_ZIP", output.zipPackage()));
        return StageResult.completed(
                output,
                artifacts,
                Map.of(
                        "generatedFileCount", output.manifest().items().size(),
                        "commercialGate", output.report().commercialDecision()));
    }

    /**
     * Converte ativo final em artefato auditavel da etapa.
     */
    private StageArtifact toArtifact(StageContext<PackageAssemblyInput> context, String type, DigitalAssetFinal asset) {
        return context.artifactStore().store(type, asset.name(), asset.contentType(), asset.content());
    }
}
