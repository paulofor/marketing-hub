package com.marketinghub.feo.fabricacaov1.montagempacote;

import com.marketinghub.feo.fabricacaov1.contract.DeliverableContent;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableSection;
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
import java.util.Map;
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
        byte[] zipBytes = buildZip(input, htmlAsset, pdfAsset, spreadsheetAsset, manifest, report);
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
                    .proof { background: #fff8e8; border-left: 5px solid #d7961f; }
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
                  <h2>Mapa do Kit de Transformacao Aplicavel</h2>
                  <div class="box proof">
                    <p><strong>O produto nao e um PDF bonito.</strong> Ele entrega metodo pratico, plano de execucao, materiais prontos, prova tangivel, ritual de acompanhamento, bonus anti-objecao e reducao clara de esforco.</p>
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
                    <thead><tr><th>Ordem</th><th>Componente</th><th>Entregavel</th><th>Formato</th><th>Papel no produto</th></tr></thead>
                    <tbody>
                """);
        for (DeliverableSpec spec : input.plan().deliverables()) {
            html.append("<tr><td>")
                    .append(escape(spec.consumptionOrder()))
                    .append("</td><td>")
                    .append(escape(componentLabel(spec.componentType())))
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
                  <h2>Workbooks de aplicacao por entregavel</h2>
                """);
        for (DeliverableContent content : input.contentPackage().deliverables()) {
            html.append("<div class=\"box\"><h3>")
                    .append(escape(content.code()))
                    .append(" - ")
                    .append(escape(content.title()))
                    .append("</h3><p>")
                    .append(escape(content.headline()))
                    .append("</p><p><strong>Resultado para o comprador:</strong> ")
                    .append(escape(content.buyerOutcome()))
                    .append("</p><p><strong>Primeira vitoria:</strong> ")
                    .append(escape(content.firstWin()))
                    .append("</p><p><strong>Material pronto para usar:</strong> ")
                    .append(escape(content.readyToUseAsset()))
                    .append("</p><p><strong>Prova tangivel:</strong> ")
                    .append(escape(content.tangibleProof()))
                    .append("</p><p><strong>Ritual ou acompanhamento:</strong> ")
                    .append(escape(content.ritualStep()))
                    .append("</p><p><strong>Bonus anti-objecao:</strong> ")
                    .append(escape(content.antiObjectionBonus()))
                    .append("</p>");
            for (DeliverableSection section : content.sections()) {
                html.append("<h4>")
                        .append(escape(section.title()))
                        .append("</h4><p>")
                        .append(escape(section.explanation()))
                        .append("</p><p><strong>Acao:</strong> ")
                        .append(escape(section.actionStep()))
                        .append("</p>");
            }
            html.append("<p><strong>Checklist de execucao:</strong></p><ul>");
            for (String item : content.checklist()) {
                html.append("<li>").append(escape(item)).append("</li>");
            }
            html.append("</ul><p><strong>Template preenchivel:</strong></p><table><tbody>");
            for (String field : content.templateFields()) {
                html.append("<tr><th>").append(escape(field)).append("</th><td>________________________________________________</td></tr>");
            }
            html.append("</tbody></table><p><strong>Erros a evitar:</strong></p><ul>");
            for (String mistake : content.commonMistakes()) {
                html.append("<li>").append(escape(mistake)).append("</li>");
            }
            html.append("</ul><p><strong>Criterio de conclusao:</strong> ")
                    .append(escape(content.completionCriteria()))
                    .append("</p></div>");
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
                  <h2>Materiais prontos do comprador</h2>
                  <table>
                    <thead><tr><th>Componente</th><th>Arquivo no ZIP</th><th>Uso esperado</th></tr></thead>
                    <tbody>
                """);
        for (DeliverableContent content : input.contentPackage().deliverables()) {
            html.append("<tr><td>")
                    .append(escape(componentLabel(content.componentType())))
                    .append("</td><td>entregaveis/")
                    .append(escape(deliverableFileName(content)))
                    .append("</td><td>")
                    .append(escape(content.readyToUseAsset()))
                    .append("</td></tr>");
        }
        html.append("""
                    </tbody>
                  </table>
                </section>
                <section>
                  <h2>Gate de qualidade comercial</h2>
                  <ul>
                    <li>O cliente entende a promessa em menos de um minuto.</li>
                    <li>Existe uma primeira acao clara antes de qualquer explicacao longa.</li>
                    <li>Cada entregavel reduz dor, esforco ou incerteza de aplicacao.</li>
                    <li>O pacote contem metodo, plano, templates, exemplo preenchido, prova tangivel, ritual e bonus anti-objecao.</li>
                    <li>O pacote nao promete resultado automatico nem altera o mecanismo validado.</li>
                  </ul>
                  <p><strong>Score FEO:</strong> """).append(input.contentPackage().qualityScore()).append("""
                 /100 - """).append(escape(input.contentPackage().qualityGate())).append("""
                </p>
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
        csv.append("ordem,codigo,componente,titulo,formato,papel,primeira_vitoria,material_pronto,prova_tangivel,ritual,bonus_anti_objecao,criterio_conclusao,criterios_qualidade\n");
        Map<String, DeliverableContent> contents = input.contentPackage().deliverables().stream()
                .collect(java.util.stream.Collectors.toMap(DeliverableContent::code, item -> item));
        for (DeliverableSpec spec : input.plan().deliverables()) {
            DeliverableContent content = contents.get(spec.code());
            csv.append(csv(spec.consumptionOrder())).append(',')
                    .append(csv(spec.code())).append(',')
                    .append(csv(componentLabel(spec.componentType()))).append(',')
                    .append(csv(spec.title())).append(',')
                    .append(csv(spec.format())).append(',')
                    .append(csv(spec.role())).append(',')
                    .append(csv(content == null ? "Executar a primeira acao do entregavel." : content.firstWin())).append(',')
                    .append(csv(content == null ? "Material pronto para uso." : content.readyToUseAsset())).append(',')
                    .append(csv(content == null ? "Evidencia de progresso." : content.tangibleProof())).append(',')
                    .append(csv(content == null ? "Checkpoint de aplicacao." : content.ritualStep())).append(',')
                    .append(csv(content == null ? "FAQ operacional." : content.antiObjectionBonus())).append(',')
                    .append(csv(content == null ? "Cliente consegue decidir o proximo passo sem suporte externo."
                            : content.completionCriteria())).append(',')
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
        for (DeliverableContent content : input.contentPackage().deliverables()) {
            byte[] bytes = buildDeliverableHtml(input.context(), content).getBytes(StandardCharsets.UTF_8);
            items.add(new ManifestItem(
                    "entregaveis/" + deliverableFileName(content),
                    "text/html; charset=UTF-8",
                    componentLabel(content.componentType()) + " - " + content.readyToUseAsset(),
                    String.valueOf(order++),
                    sha256(bytes)));
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
            PackageAssemblyInput input,
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
            for (DeliverableContent content : input.contentPackage().deliverables()) {
                addZip(zip,
                        "entregaveis/" + deliverableFileName(content),
                        buildDeliverableHtml(input.context(), content).getBytes(StandardCharsets.UTF_8));
            }
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
     * Monta arquivo HTML individual de cada componente do kit.
     */
    private String buildDeliverableHtml(FabricationContext context, DeliverableContent content) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <html xmlns="http://www.w3.org/1999/xhtml" lang="pt-BR">
                <head>
                  <meta charset="utf-8" />
                  <title>""").append(escape(content.title())).append("""
                </title>
                  <style>
                    body { font-family: Arial, sans-serif; color: #17202a; margin: 32px; line-height: 1.55; }
                    h1 { color: #123b3a; }
                    h2 { color: #1d5f58; border-bottom: 1px solid #d5dfdc; padding-bottom: 6px; }
                    .box { border: 1px solid #d5dfdc; padding: 14px; margin: 12px 0; border-radius: 6px; }
                    th, td { border: 1px solid #dce5e2; padding: 8px; text-align: left; }
                    table { width: 100%; border-collapse: collapse; }
                  </style>
                </head>
                <body>
                  <p><strong>Componente:</strong> """).append(escape(componentLabel(content.componentType()))).append("""
                </p>
                  <h1>""").append(escape(content.title())).append("""
                </h1>
                  <div class="box"><strong>Promessa preservada:</strong> """).append(escape(context.centralPromise())).append("""
                </div>
                  <h2>Resultado para o comprador</h2>
                  <p>""").append(escape(content.buyerOutcome())).append("""
                </p>
                  <h2>Material pronto para usar</h2>
                  <p>""").append(escape(content.readyToUseAsset())).append("""
                </p>
                  <h2>Prova tangivel</h2>
                  <p>""").append(escape(content.tangibleProof())).append("""
                </p>
                  <h2>Ritual de uso</h2>
                  <p>""").append(escape(content.ritualStep())).append("""
                </p>
                  <h2>Bonus anti-objecao</h2>
                  <p>""").append(escape(content.antiObjectionBonus())).append("""
                </p>
                  <h2>Execucao guiada</h2>
                """);
        for (DeliverableSection section : content.sections()) {
            html.append("<div class=\"box\"><h3>")
                    .append(escape(section.title()))
                    .append("</h3><p>")
                    .append(escape(section.explanation()))
                    .append("</p><p><strong>Acao:</strong> ")
                    .append(escape(section.actionStep()))
                    .append("</p></div>");
        }
        html.append("<h2>Template preenchivel</h2><table><tbody>");
        for (String field : content.templateFields()) {
            html.append("<tr><th>").append(escape(field)).append("</th><td>________________________________________________</td></tr>");
        }
        html.append("</tbody></table><h2>Checklist</h2><ul>");
        for (String item : content.checklist()) {
            html.append("<li>").append(escape(item)).append("</li>");
        }
        html.append("</ul><h2>Criterio de conclusao</h2><p>")
                .append(escape(content.completionCriteria()))
                .append("</p></body></html>");
        return html.toString();
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
     * Retorna rotulo legivel para o componente comercial do kit.
     */
    private String componentLabel(String componentType) {
        return switch (componentType == null ? "" : componentType) {
            case "COMECE_AQUI" -> "Comece aqui";
            case "PLANO_EXECUCAO_RAPIDA" -> "Plano de execucao";
            case "CHECKLIST_APLICACAO" -> "Checklist";
            case "TEMPLATES_PRONTOS" -> "Templates prontos";
            case "EXEMPLO_PREENCHIDO" -> "Exemplo preenchido";
            case "PROVA_TANGIVEL" -> "Prova tangivel";
            case "RITUAL_ACOMPANHAMENTO" -> "Ritual de acompanhamento";
            case "BONUS_ANTI_OBJECAO" -> "Bonus anti-objecao";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "Guia de primeiros resultados";
            default -> "Material complementar";
        };
    }

    /**
     * Cria nome estavel do arquivo individual do entregavel.
     */
    private String deliverableFileName(DeliverableContent content) {
        return content.code().toLowerCase() + "-" + slug(content.title()) + ".html";
    }

    /**
     * Normaliza texto para nome de arquivo simples.
     */
    private String slug(String value) {
        String safe = value == null ? "entregavel" : value.toLowerCase();
        safe = java.text.Normalizer.normalize(safe, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        safe = safe.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return safe.isBlank() ? "entregavel" : safe;
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
