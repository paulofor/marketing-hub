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
     * Monta HTML com acabamento editorial e conteudo aplicavel pelo cliente final.
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
                    body { font-family: Arial, sans-serif; color: #17202a; margin: 36px; line-height: 1.55; }
                    .cover { border-bottom: 5px solid #138a72; padding-bottom: 24px; margin-bottom: 28px; }
                    h1 { font-size: 32px; margin: 0 0 12px; color: #123b3a; }
                    h2 { color: #123b3a; margin-top: 30px; border-bottom: 1px solid #d5dfdc; padding-bottom: 6px; }
                    h3 { margin-bottom: 8px; color: #1d5f58; }
                    .pill { display: inline-block; background: #e8f5f1; padding: 6px 10px; border-radius: 4px; margin: 4px 6px 4px 0; }
                    .box { border: 1px solid #d5dfdc; padding: 16px; margin: 14px 0; border-radius: 6px; }
                    .premium { background: #f5fbf8; border-left: 5px solid #138a72; }
                    .grid { display: table; width: 100%; border-spacing: 10px; }
                    .cell { display: table-cell; width: 33%; border: 1px solid #dce5e2; padding: 12px; vertical-align: top; }
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
                  <h2>Experiencia de entrega premium</h2>
                  <div class="box premium">
                    <p>Este pacote foi organizado para ser usado como produto final, nao como relatorio interno. O cliente deve conseguir abrir, entender a promessa, executar os passos e perceber progresso sem depender de explicacao adicional.</p>
                  </div>
                  <div class="grid">
                    <div class="cell"><strong>Aplicacao rapida</strong><br />Primeira acao em ate 20 minutos para reduzir ansiedade e aumentar percepcao de valor.</div>
                    <div class="cell"><strong>Progresso visivel</strong><br />Checklist e criterios de conclusao para o cliente saber quando terminou cada etapa.</div>
                    <div class="cell"><strong>Promessa preservada</strong><br />Todo conteudo fica dentro do mecanismo e do resultado validados pelo experimento.</div>
                  </div>
                  <h2>Mecanismo central</h2>
                  <div class="box">""").append(escape(context.coreMechanism())).append("""
                </div>
                  <h2>Prova e limites de confianca</h2>
                  <div class="box">""").append(escape(context.proofSummary())).append("""
                </div>
                  <h2>Como usar este pacote</h2>
                  <ol>
                    <li>Leia o diagnostico inicial e marque o ponto de partida real.</li>
                    <li>Execute o entregavel principal antes de consumir bonus ou materiais complementares.</li>
                    <li>Preencha o template de aplicacao para transformar leitura em decisao concreta.</li>
                    <li>Use os criterios de conclusao para validar se houve progresso percebido.</li>
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
                  <h2>Diagnostico inicial</h2>
                  <div class="box premium">
                    <p><strong>Sintoma principal:</strong> o cliente sente dor porque ainda nao consegue transformar a situacao atual em uma sequencia simples de acao.</p>
                    <p><strong>Resultado de entrada:</strong> ao final do primeiro ciclo, o cliente deve saber exatamente qual acao tomar, qual erro evitar e qual criterio observar.</p>
                    <p><strong>Regra de foco:</strong> remover complexidade antes de adicionar profundidade. O produto deve economizar esforco mental.</p>
                  </div>
                  <h2>Execucao guiada por entregavel</h2>
                """);
        for (DeliverableSpec spec : input.plan().deliverables()) {
            html.append("<div class=\"box\"><h3>")
                    .append(escape(spec.consumptionOrder()))
                    .append(". ")
                    .append(escape(spec.title()))
                    .append("</h3><p>")
                    .append(escape(spec.role()))
                    .append("</p><p><strong>Resultado esperado:</strong> ")
                    .append(escape(resultFor(spec, context)))
                    .append("</p><p><strong>Passo de aplicacao:</strong></p><ol>")
                    .append("<li>Separe uma situacao real do seu contexto atual.</li>")
                    .append("<li>Use o mecanismo central para escolher uma decisao simples.</li>")
                    .append("<li>Registre o antes, a acao e o criterio que indicara progresso.</li>")
                    .append("</ol><p><strong>Template preenchivel:</strong></p><table><tbody>")
                    .append("<tr><th>Situacao atual</th><td>________________________________________________</td></tr>")
                    .append("<tr><th>Acao escolhida</th><td>________________________________________________</td></tr>")
                    .append("<tr><th>Barreira que pode atrapalhar</th><td>________________________________________________</td></tr>")
                    .append("<tr><th>Criterio de conclusao</th><td>________________________________________________</td></tr>")
                    .append("</tbody></table><p><strong>Componentes do entregavel:</strong></p><ul>");
            for (String section : spec.sections()) {
                html.append("<li>").append(escape(section)).append("</li>");
            }
            html.append("</ul></div>");
        }
        html.append("""
                </section>
                <section>
                  <h2>Plano de aplicacao de 7 dias</h2>
                  <table>
                    <thead><tr><th>Dia</th><th>Acao</th><th>Evidencia de progresso</th></tr></thead>
                    <tbody>
                      <tr><td>1</td><td>Diagnosticar a situacao atual e escolher um unico foco.</td><td>Problema descrito em uma frase.</td></tr>
                      <tr><td>2</td><td>Aplicar o entregavel principal no foco escolhido.</td><td>Primeira decisao registrada.</td></tr>
                      <tr><td>3</td><td>Remover uma friccao ou tarefa desnecessaria.</td><td>Tempo, duvida ou esforco reduzido.</td></tr>
                      <tr><td>4</td><td>Usar o checklist para revisar a execucao.</td><td>Itens criticos marcados.</td></tr>
                      <tr><td>5</td><td>Ajustar o plano com base no que funcionou.</td><td>Proxima acao definida.</td></tr>
                      <tr><td>6</td><td>Repetir a parte que gerou maior clareza.</td><td>Padrao percebido.</td></tr>
                      <tr><td>7</td><td>Fechar o ciclo com conclusao e proxima melhoria.</td><td>Resumo final e continuidade.</td></tr>
                    </tbody>
                  </table>
                </section>
                <section>
                  <h2>Gate de qualidade comercial</h2>
                  <ul>
                    <li>O cliente entende a promessa em menos de um minuto.</li>
                    <li>Existe uma primeira acao clara antes de qualquer explicacao longa.</li>
                    <li>Cada entregavel reduz dor, esforco ou incerteza de aplicacao.</li>
                    <li>O pacote nao promete resultado automatico nem altera o mecanismo validado.</li>
                  </ul>
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
     * Define resultado pratico esperado para um entregavel especifico.
     */
    private String resultFor(DeliverableSpec spec, FabricationContext context) {
        String format = spec.format() == null ? "" : spec.format();
        if (format.contains("CSV")) {
            return "transformar informacoes soltas em acompanhamento preenchivel e verificavel.";
        }
        if (format.contains("PDF")) {
            return "guiar uma decisao pratica alinhada ao resultado prometido: " + context.promisedResult();
        }
        return "entregar clareza operacional sem exigir conhecimento tecnico do cliente.";
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
        csv.append("ordem,codigo,titulo,formato,papel,acao_7_dias,criterio_conclusao,criterios_qualidade\n");
        for (DeliverableSpec spec : input.plan().deliverables()) {
            csv.append(csv(spec.consumptionOrder())).append(',')
                    .append(csv(spec.code())).append(',')
                    .append(csv(spec.title())).append(',')
                    .append(csv(spec.format())).append(',')
                    .append(csv(spec.role())).append(',')
                    .append(csv("Aplicar em uma situacao real e registrar antes, acao e evidencia.")).append(',')
                    .append(csv("Cliente consegue decidir o proximo passo sem suporte externo.")).append(',')
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
                "READY_FOR_PREMIUM_REVIEW",
                input.context().validationSignals(),
                manifest.items().stream().map(ManifestItem::fileName).toList(),
                List.of(
                        "Revisar capa, promessa e primeiro exercicio antes de publicar",
                        "Importar o CSV no Google Sheets para acompanhar aplicacao do cliente",
                        "Conectar o ZIP ao fluxo oficial de entrega do comprador somente apos revisao premium"));
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
