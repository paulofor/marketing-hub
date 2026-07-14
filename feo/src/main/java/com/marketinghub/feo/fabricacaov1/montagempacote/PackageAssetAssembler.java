package com.marketinghub.feo.fabricacaov1.montagempacote;

import com.marketinghub.feo.fabricacaov1.contract.DeliverableSpec;
import com.marketinghub.feo.fabricacaov1.contract.DigitalAssetFinal;
import com.marketinghub.feo.fabricacaov1.contract.FabricationContext;
import com.marketinghub.feo.fabricacaov1.contract.FabricationReport;
import com.marketinghub.feo.fabricacaov1.contract.ManifestItem;
import com.marketinghub.feo.fabricacaov1.contract.OfferDeliveryManifest;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyInput;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyOutput;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Monta arquivos finais da FEO sem chamar banco, storage ou outra etapa concreta.
 */
@Component
public class PackageAssetAssembler {

    private static final Logger log = LoggerFactory.getLogger(PackageAssetAssembler.class);

    /**
     * Converte plano aprovado em pacote final de entrega.
     */
    public PackageAssemblyOutput assemble(PackageAssemblyInput input) {
        String html = buildHtml(input);
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
        byte[] pdfBytes = renderPdf(html);
        byte[] spreadsheetBytes = buildSpreadsheet(input).getBytes(StandardCharsets.UTF_8);
        DigitalAssetFinal htmlAsset = asset("01-pacote-final.html", "text/html; charset=UTF-8", htmlBytes);
        DigitalAssetFinal pdfAsset = asset("02-pacote-final.pdf", "application/pdf", pdfBytes);
        DigitalAssetFinal spreadsheetAsset = asset("03-manifesto-entregaveis.csv", "text/csv; charset=UTF-8", spreadsheetBytes);
        OfferDeliveryManifest manifest = manifest(input, List.of(htmlAsset, pdfAsset, spreadsheetAsset));
        FabricationReport report = report(input, manifest);
        byte[] zipBytes = buildZip(htmlAsset, pdfAsset, spreadsheetAsset, manifest, report);
        DigitalAssetFinal zipAsset = asset("00-pacote-entregaveis-feo.zip", "application/zip", zipBytes);
        return new PackageAssemblyOutput(manifest, report, htmlAsset, pdfAsset, spreadsheetAsset, zipAsset);
    }

    /**
     * Monta HTML com acabamento editorial simples e utilizavel como preview.
     */
    private String buildHtml(PackageAssemblyInput input) {
        FabricationContext context = input.context();
        StringBuilder html = new StringBuilder();
        html.append("""
                <html xmlns="http://www.w3.org/1999/xhtml" lang="pt-BR">
                <head>
                  <meta charset="utf-8" />
                  <title>""").append(escape(context.offerName())).append("""
                </title>
                  <style>
                    body { font-family: Arial, sans-serif; color: #17202a; margin: 40px; line-height: 1.48; }
                    .cover { border-bottom: 4px solid #138a72; padding-bottom: 24px; margin-bottom: 28px; }
                    h1 { font-size: 30px; margin: 0 0 12px; color: #123b3a; }
                    h2 { color: #123b3a; margin-top: 28px; }
                    h3 { margin-bottom: 8px; color: #1d5f58; }
                    .pill { display: inline-block; background: #e8f5f1; padding: 6px 10px; border-radius: 4px; margin: 4px 6px 4px 0; }
                    .box { border: 1px solid #d5dfdc; padding: 16px; margin: 14px 0; border-radius: 6px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 12px; }
                    th, td { border: 1px solid #dce5e2; padding: 8px; text-align: left; vertical-align: top; }
                    th { background: #f2f7f5; }
                  </style>
                </head>
                <body>
                <section class="cover">
                  <p class="pill">Produto digital final</p>
                  <p class="pill">Fabricado pela FEO v1</p>
                  <h1>""").append(escape(context.offerName())).append("""
                </h1>
                  <p><strong>Nicho:</strong> """).append(escape(context.niche())).append("""
                </p>
                  <p><strong>Promessa validada:</strong> """).append(escape(context.centralPromise())).append("""
                </p>
                  <p><strong>Resultado prometido:</strong> """).append(escape(context.promisedResult())).append("""
                </p>
                </section>
                <section>
                  <h2>Mecanismo central</h2>
                  <div class="box">""").append(escape(context.coreMechanism())).append("""
                </div>
                  <h2>Como usar este pacote</h2>
                  <ol>
                    <li>Comece pelo entregavel principal para gerar a primeira clareza pratica.</li>
                    <li>Use a planilha/manifesto para acompanhar o progresso de aplicacao.</li>
                    <li>Execute o plano em uma janela curta e revise o resultado antes de ampliar escopo.</li>
                  </ol>
                </section>
                <section>
                  <h2>Entregaveis</h2>
                  <table>
                    <thead><tr><th>Ordem</th><th>Entregavel</th><th>Formato</th><th>Papel no produto</th></tr></thead>
                    <tbody>
                """);
        for (DeliverableSpec spec : input.plan().deliverables()) {
            html.append("<tr><td>")
                    .append(escape(spec.consumptionOrder()))
                    .append("</td><td>")
                    .append(escape(spec.title()))
                    .append("</td><td>")
                    .append(escape(spec.format()))
                    .append("</td><td>")
                    .append(escape(spec.role()))
                    .append("</td></tr>");
        }
        html.append("""
                    </tbody>
                  </table>
                </section>
                <section>
                  <h2>Execucao guiada</h2>
                """);
        for (DeliverableSpec spec : input.plan().deliverables()) {
            html.append("<div class=\"box\"><h3>")
                    .append(escape(spec.consumptionOrder()))
                    .append(". ")
                    .append(escape(spec.title()))
                    .append("</h3><p>")
                    .append(escape(spec.role()))
                    .append("</p><ul>");
            for (String section : spec.sections()) {
                html.append("<li>").append(escape(section)).append("</li>");
            }
            html.append("</ul></div>");
        }
        html.append("""
                </section>
                <section>
                  <h2>Limites de promessa</h2>
                  <p>Este pacote materializa a oferta validada. Ele nao promete resultado automatico, nao altera o mecanismo validado e depende da aplicacao correta pelo cliente.</p>
                </section>
                </body>
                </html>
                """);
        return html.toString();
    }

    /**
     * Renderiza PDF a partir do HTML final.
     */
    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception ex) {
            log.error("Falha ao renderizar PDF da FEO", ex);
            throw new IllegalStateException("Falha ao renderizar PDF da FEO", ex);
        }
    }

    /**
     * Monta CSV abrivel em Google Sheets ou Excel.
     */
    private String buildSpreadsheet(PackageAssemblyInput input) {
        StringBuilder csv = new StringBuilder();
        csv.append("ordem,codigo,titulo,formato,papel,criterios_qualidade\n");
        for (DeliverableSpec spec : input.plan().deliverables()) {
            csv.append(csv(spec.consumptionOrder())).append(',')
                    .append(csv(spec.code())).append(',')
                    .append(csv(spec.title())).append(',')
                    .append(csv(spec.format())).append(',')
                    .append(csv(spec.role())).append(',')
                    .append(csv(String.join(" | ", spec.qualityCriteria()))).append('\n');
        }
        return csv.toString();
    }

    /**
     * Cria manifesto do pacote gerado.
     */
    private OfferDeliveryManifest manifest(PackageAssemblyInput input, List<DigitalAssetFinal> assets) {
        List<ManifestItem> items = new ArrayList<>();
        int order = 1;
        for (DigitalAssetFinal asset : assets) {
            items.add(new ManifestItem(
                    asset.name(),
                    asset.contentType(),
                    "Arquivo final para consumo ou revisao do produto",
                    String.valueOf(order++),
                    asset.sha256()));
        }
        return new OfferDeliveryManifest(input.context().requestId(), input.plan().packageTitle(), items);
    }

    /**
     * Cria relatorio de fabricacao orientado ao usuario.
     */
    private FabricationReport report(PackageAssemblyInput input, OfferDeliveryManifest manifest) {
        return new FabricationReport(
                input.context().requestId(),
                "COMPLETED",
                "Pacote pronto para revisao humana e entrega controlada.",
                input.context().validationSignals(),
                manifest.items().stream().map(ManifestItem::fileName).toList(),
                List.of(
                        "Revisar visualmente o PDF antes de publicar",
                        "Importar o CSV no Google Sheets quando precisar de planilha colaborativa",
                        "Conectar o ZIP ao fluxo oficial de entrega do comprador"));
    }

    /**
     * Compacta os arquivos finais e documentos de controle.
     */
    private byte[] buildZip(
            DigitalAssetFinal html,
            DigitalAssetFinal pdf,
            DigitalAssetFinal spreadsheet,
            OfferDeliveryManifest manifest,
            FabricationReport report) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            addZip(zip, html.name(), html.content());
            addZip(zip, pdf.name(), pdf.content());
            addZip(zip, spreadsheet.name(), spreadsheet.content());
            addZip(zip, "manifesto.txt", manifest.toString().getBytes(StandardCharsets.UTF_8));
            addZip(zip, "relatorio-fabricacao.txt", report.toString().getBytes(StandardCharsets.UTF_8));
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException ex) {
            log.error("Falha ao montar ZIP da FEO", ex);
            throw new IllegalStateException("Falha ao montar ZIP da FEO", ex);
        }
    }

    /**
     * Adiciona um arquivo ao pacote ZIP.
     */
    private void addZip(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    /**
     * Cria ativo final com hash e notas de qualidade.
     */
    private DigitalAssetFinal asset(String name, String contentType, byte[] content) {
        return new DigitalAssetFinal(
                name,
                contentType,
                content,
                sha256(content),
                List.of("Arquivo gerado pela FEO v1", "Pronto para revisao antes da entrega final"));
    }

    /**
     * Escapa texto para uso seguro em HTML.
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Escapa texto para CSV.
     */
    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    /**
     * Calcula SHA-256 para rastreabilidade dos arquivos finais.
     */
    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }
}
