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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;
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
            "feo v1",
            "fabricado pela feo",
            "promessa validada",
            "mecanismo validado",
            "mds",
            "experimento",
            "tráfego",
            "trafego",
            "checkout",
            "sha256",
            "json",
            "ready_for_premium_review");
    private static final List<String> PROHIBITED_FINAL_TERMS = List.of(
            "mecanismo",
            "pesquisa",
            "princípio científico",
            "princípios científicos",
            "princípio de pesquisa",
            "teoria acadêmica",
            "cliente",
            "comprador",
            "compradora",
            "criterios_qualidade",
            "criterio_conclusao",
            "bonus_anti_objecao");

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
        prohibitedTerms = prohibitedFinalTerms(output);
        if (!prohibitedTerms.isEmpty()) {
            return StageResult.blocked(
                    "Pacote final da cliente contém termos técnicos proibidos: " + String.join(", ", prohibitedTerms),
                    List.of());
        }
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
        return prohibitedTerms(text);
    }

    /** Varre os arquivos finais textuais para impedir vazamento de termos internos no produto. */
    private List<String> prohibitedTerms(PackageAssemblyOutput output) {
        StringBuilder text = new StringBuilder();
        text.append(text(output.experienceSite()));
        text.append(text(output.spreadsheet()));
        text.append(textFromZip(output.zipPackage()));
        return prohibitedTerms(text.toString().toLowerCase());
    }

    /** Localiza termos proibidos em texto público do pacote final. */
    private List<String> prohibitedTerms(String text) {
        Set<String> found = new LinkedHashSet<>();
        for (String term : PROHIBITED_CLIENT_TERMS) {
            if (text.contains(term)) {
                found.add(term);
            }
        }
        return new ArrayList<>(found);
    }

    /** Localiza termos de bastidor que nao podem aparecer no ZIP final publico. */
    private List<String> prohibitedFinalTerms(PackageAssemblyOutput output) {
        StringBuilder text = new StringBuilder();
        text.append(text(output.experienceSite()));
        text.append(text(output.spreadsheet()));
        text.append(textFromZip(output.zipPackage()));
        Set<String> found = new LinkedHashSet<>(prohibitedTerms(text.toString().toLowerCase()));
        for (String term : PROHIBITED_FINAL_TERMS) {
            if (text.toString().toLowerCase().contains(term)) {
                found.add(term);
            }
        }
        return new ArrayList<>(found);
    }

    /** Converte ativo textual em string para gate de qualidade. */
    private String text(DigitalAssetFinal asset) {
        if (asset == null || asset.content() == null || asset.contentType() == null) {
            return "";
        }
        if (asset.contentType().startsWith("text/")) {
            return new String(asset.content(), StandardCharsets.UTF_8);
        }
        return "";
    }

    /** Lê arquivos textuais dentro do ZIP final para gate de qualidade. */
    private String textFromZip(DigitalAssetFinal asset) {
        if (asset == null || asset.content() == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(asset.content()), StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (isTextEntry(entry.getName())) {
                    text.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
                }
            }
        } catch (IOException ex) {
            text.append("zip ilegivel");
        }
        return text.toString();
    }

    /** Indica se uma entrada do ZIP deve ser varrida como texto público. */
    private boolean isTextEntry(String name) {
        return name != null && (name.endsWith(".html") || name.endsWith(".txt") || name.endsWith(".csv"));
    }
}
