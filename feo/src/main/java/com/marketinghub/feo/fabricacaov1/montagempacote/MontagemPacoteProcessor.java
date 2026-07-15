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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Monta o pacote final de entrega com arquivos profissionais.
 */
@Component
public class MontagemPacoteProcessor implements StageProcessor<PackageAssemblyInput, PackageAssemblyOutput> {

    private static final List<String> PROHIBITED_CLIENT_TERMS = List.of(
            "ctr",
            "cpl",
            "pré-venda",
            "pre-venda",
            "score feo",
            "fabricado pela feo",
            "promessa validada",
            "mecanismo validado",
            "experimento",
            "tráfego",
            "trafego",
            "checkout",
            "sha256",
            "json",
            "ready_for_premium_review");

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
     * Gera experiencia guiada, PDF, planilha CSV, manifesto e ZIP do produto final.
     */
    @Override
    public StageResult<PackageAssemblyOutput> process(StageContext<PackageAssemblyInput> context) {
        PackageAssemblyInput input = context.input();
        if (input.contentPackage() == null || input.contentPackage().deliverables().isEmpty()) {
            return StageResult.blocked("Montagem FEO sem conteúdos finais redigidos.", List.of());
        }
        List<String> prohibitedTerms = prohibitedTerms(input);
        if (!prohibitedTerms.isEmpty()) {
            return StageResult.blocked(
                    "Pacote da cliente contém termos técnicos proibidos: " + String.join(", ", prohibitedTerms),
                    List.of());
        }
        PackageAssemblyOutput output = assembler.assemble(input);
        List<StageArtifact> artifacts = List.of(
                toArtifact(context, "FINAL_EXPERIENCE_SITE", output.experienceSite()),
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

    /**
     * Varre conteúdo público para impedir vazamento de auditoria técnica para a compradora.
     */
    private List<String> prohibitedTerms(PackageAssemblyInput input) {
        String text = input.contentPackage().deliverables().toString().toLowerCase();
        List<String> found = new ArrayList<>();
        for (String term : PROHIBITED_CLIENT_TERMS) {
            if (text.contains(term)) {
                found.add(term);
            }
        }
        return found;
    }
}
