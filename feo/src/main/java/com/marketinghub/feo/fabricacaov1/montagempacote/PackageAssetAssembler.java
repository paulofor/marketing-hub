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
import com.marketinghub.feo.fabricacaov1.contract.VisualAsset;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
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
        String ebookHtml = buildHtml(input);
        byte[] pdfBytes = renderPdf(ebookHtml);
        byte[] experienceBytes = buildExperienceSite(input).getBytes(StandardCharsets.UTF_8);
        byte[] spreadsheetBytes = buildSpreadsheet(input).getBytes(StandardCharsets.UTF_8);
        DigitalAssetFinal experienceAsset = asset("01-experiencia-guiada/index.html", "text/html; charset=UTF-8", experienceBytes);
        DigitalAssetFinal pdfAsset = asset("02-ebook-principal.pdf", "application/pdf", pdfBytes);
        DigitalAssetFinal spreadsheetAsset = asset("03-plano-checklists-e-templates.csv", "text/csv; charset=UTF-8", spreadsheetBytes);
        OfferDeliveryManifest manifest = manifest(input, List.of(experienceAsset, pdfAsset, spreadsheetAsset));
        FabricationReport report = report(input, manifest);
        byte[] zipBytes = buildZip(input, experienceAsset, pdfAsset, spreadsheetAsset, manifest, report);
        DigitalAssetFinal zipAsset = asset("00-metodo-musa-produto-digital-experiencial.zip", "application/zip", zipBytes);
        return new PackageAssemblyOutput(manifest, report, experienceAsset, pdfAsset, spreadsheetAsset, zipAsset);
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
                    .cover { border-bottom: 5px solid #138a72; padding-bottom: 24px; margin-bottom: 28px; page-break-after: always; }
                    .cover-image { width: 100%; max-height: 860px; margin-bottom: 22px; }
                    .visual { width: 100%; max-height: 720px; border: 1px solid #d5dfdc; margin: 14px 0; }
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
                """);
        VisualAsset cover = visualByType(input, "EBOOK_COVER");
        if (cover != null) {
            html.append("<img class=\"cover-image\" src=\"")
                    .append(dataUri(cover))
                    .append("\" alt=\"")
                    .append(escape(cover.title()))
                    .append("\" />");
        }
        html.append("""
                  <p class="pill">E-book prático</p>
                  <p class="pill">Plano guiado de 7 dias</p>
                  <h1>""").append(escape(context.offerName())).append("""
                </h1>
                  <p><strong>Para quem é:</strong> """).append(escape(context.niche())).append("""
                </p>
                  <p><strong>Promessa:</strong> """).append(escape(context.centralPromise())).append("""
                </p>
                  <p><strong>Resultado buscado:</strong> """).append(escape(context.promisedResult())).append("""
                </p>
                  <p><strong>Base do método:</strong> princípios práticos derivados da pesquisa MDS, traduzidos em decisões simples de cabelo, pele, roupa, perfume, ocasião e compra consciente.</p>
                </section>
                <section>
                  <h2>Comece por aqui</h2>
                  <div class="box premium">
                    <p>Este material foi feito para aplicação prática. Abra o plano de 7 dias, escolha um ajuste pequeno e use os checklists para sair da tentativa solta para uma presença mais intencional.</p>
                  </div>
                  <div class="grid">
                    <div class="cell"><strong>Aplicacao rapida</strong><br />Primeira acao em ate 20 minutos para reduzir ansiedade e aumentar percepcao de valor.</div>
                    <div class="cell"><strong>Progresso visivel</strong><br />Checklist e criterios de conclusao para o cliente saber quando terminou cada etapa.</div>
                    <div class="cell"><strong>Clareza de escolha</strong><br />Você entende o que manter, o que ajustar e o que evitar comprar por impulso.</div>
                  </div>
                  <h2>O que você recebe</h2>
                  <div class="box proof">
                    <p>Um e-book com plano de execução, checklists, templates, exemplos preenchidos, figuras de apoio, guia anti-impulso e uma rotina simples para perceber avanço em 7 dias.</p>
                  </div>
                  <h2>Mecanismo central</h2>
                  <div class="box">""").append(escape(context.coreMechanism())).append("""
                </div>
                  <h2>Como a pesquisa vira transformação prática</h2>
                  <div class="box premium">
                    <p>Os princípios científicos não aparecem como aula técnica. Eles foram convertidos em regras de aplicação: reduzir esforço mental, organizar sinais visuais, criar comparação antes/depois e facilitar escolhas com o que você já tem.</p>
                    <p>O foco do método é transformar conhecimento em decisão prática, não em excesso de informação.</p>
                  </div>
                """);
        appendVisual(html, input, "INFOGRAPHIC");
        appendVisual(html, input, "CONCEPT_MAP");
        appendVisual(html, input, "BEFORE_AFTER");
        html.append("""
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
                    .append("</p><p><strong>Princípio aplicado:</strong> ")
                    .append(escape(content.appliedPrinciple()))
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
                  <h2>Checklist final de aplicação</h2>
                  <ul>
                    <li>Você escolheu um foco de presença para os próximos 7 dias.</li>
                    <li>Você marcou os detalhes que mais geram ruído hoje.</li>
                    <li>Você definiu uma combinação possível com o que já tem.</li>
                    <li>Você separou o que pode comprar depois do que pode reaproveitar agora.</li>
                    <li>Você registrou um antes/depois simples para perceber progresso.</li>
                  </ul>
                </section>
                <section>
                  <h2>Observação honesta</h2>
                  <p>O material não promete transformação automática. Ele organiza decisões pequenas e aplicáveis para você reduzir tentativa, compra por impulso e incoerência visual.</p>
                </section>
                </body>
                </html>
                """);
        return html.toString();
    }

    /**
     * Monta a experiencia guiada do PDE como produto principal consumido pela compradora.
     */
    private String buildExperienceSite(PackageAssemblyInput input) {
        FabricationContext context = input.context();
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>""").append(escape(context.offerName())).append("""
                </title>
                  <style>
                    :root { --ink:#16211f; --muted:#64716d; --brand:#176b5d; --soft:#edf7f3; --line:#d8e5e0; --gold:#b77b22; }
                    * { box-sizing: border-box; }
                    body { margin: 0; font-family: Arial, sans-serif; color: var(--ink); background: #fbfcfb; line-height: 1.5; }
                    header, main, footer { max-width: 1120px; margin: 0 auto; padding: 28px 20px; }
                    header { display: grid; grid-template-columns: 1.1fr .9fr; gap: 28px; align-items: center; min-height: 92vh; }
                    h1 { font-size: 44px; line-height: 1.05; margin: 12px 0; color: #103c36; letter-spacing: 0; }
                    h2 { font-size: 25px; margin: 0 0 14px; color: #103c36; }
                    h3 { margin: 0 0 8px; color: #174b43; }
                    p { margin: 0 0 12px; }
                    .eyebrow { color: var(--brand); font-weight: 700; text-transform: uppercase; font-size: 12px; letter-spacing: 0; }
                    .hero-img, .visual { width: 100%; border: 1px solid var(--line); border-radius: 8px; background: white; }
                    .promise { font-size: 18px; color: #33413d; max-width: 680px; }
                    .cta-row { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 18px; }
                    .btn { border: 0; background: var(--brand); color: white; padding: 12px 16px; border-radius: 6px; font-weight: 700; text-decoration: none; }
                    .ghost { background: white; color: var(--brand); border: 1px solid var(--brand); }
                    section { padding: 28px 0; border-top: 1px solid var(--line); }
                    .grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
                    .card { background: white; border: 1px solid var(--line); border-radius: 8px; padding: 16px; min-height: 132px; }
                    .mission { display: grid; grid-template-columns: 80px 1fr; gap: 14px; align-items: start; }
                    .day { background: var(--soft); color: var(--brand); border-radius: 8px; padding: 12px; font-weight: 700; text-align: center; }
                    .check { display: flex; gap: 10px; align-items: start; margin: 8px 0; }
                    .check input { margin-top: 4px; }
                    .library { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
                    .note { background: #fff8ea; border-left: 5px solid var(--gold); padding: 14px; border-radius: 6px; }
                    @media (max-width: 760px) { header, .grid, .library { grid-template-columns: 1fr; } h1 { font-size: 34px; } header { min-height: auto; } }
                  </style>
                </head>
                <body>
                  <header>
                    <div>
                      <div class="eyebrow">Produto Digital Experiencial</div>
                      <h1>""").append(escape(context.offerName())).append("""
                </h1>
                      <p class="promise">""").append(escape(context.centralPromise())).append("""
                </p>
                      <div class="cta-row">
                        <a class="btn" href="#diagnostico">Começar diagnóstico</a>
                        <a class="btn ghost" href="#jornada">Ver jornada de 7 dias</a>
                      </div>
                    </div>
                    <div>
                """);
        VisualAsset cover = visualByType(input, "EBOOK_COVER");
        if (cover != null) {
            html.append("<img class=\"hero-img\" src=\"../")
                    .append(escape(cover.fileName()))
                    .append("\" alt=\"")
                    .append(escape(cover.title()))
                    .append("\" />");
        }
        html.append("""
                    </div>
                  </header>
                  <main>
                    <section id="diagnostico">
                      <h2>Dia 0 - Diagnóstico MUSA</h2>
                      <p>Antes de mexer em roupa, maquiagem ou compra, marque o que hoje mais cria ruído na sua presença.</p>
                      <div class="grid">
                        <label class="card"><input type="checkbox" /> Meu visual parece quase certo, mas falta coerência.</label>
                        <label class="card"><input type="checkbox" /> Eu compro itens novos, mas continuo sem saber combinar.</label>
                        <label class="card"><input type="checkbox" /> Cabelo, pele, roupa, perfume ou acessórios não conversam entre si.</label>
                      </div>
                    </section>
                    <section>
                      <h2>Mecanismo aplicado</h2>
                      <p>""").append(escape(context.coreMechanism())).append("""
                </p>
                      <div class="note">A ciência do MDS entra aqui como decisão prática: reduzir esforço mental, organizar sinais visuais e criar pequenas evidências de progresso.</div>
                """);
        appendExperienceVisual(html, input, "CONCEPT_MAP");
        html.append("""
                    </section>
                    <section id="jornada">
                      <h2>Jornada guiada de 7 dias</h2>
                """);
        for (int day = 1; day <= 7; day++) {
            html.append("<div class=\"card mission\"><div class=\"day\">Dia ")
                    .append(day)
                    .append("</div><div><h3>")
                    .append(escape(dayTitle(day)))
                    .append("</h3><p>")
                    .append(escape(dayAction(day)))
                    .append("</p><div class=\"check\"><input type=\"checkbox\" /><span>")
                    .append(escape(dayCheckpoint(day)))
                    .append("</span></div></div></div>");
        }
        html.append("""
                    </section>
                    <section>
                      <h2>Prova visual da transformação</h2>
                      <p>Use as figuras abaixo como referência conceitual. O objetivo é coerência e intenção, não luxo caro nem transformação radical.</p>
                """);
        appendExperienceVisual(html, input, "INFOGRAPHIC");
        appendExperienceVisual(html, input, "BEFORE_AFTER");
        html.append("""
                    </section>
                    <section>
                      <h2>Biblioteca de apoio</h2>
                      <div class="library">
                        <div class="card"><h3>02-ebook-principal.pdf</h3><p>Guia editorial com capa, figuras internas e explicação prática do método.</p></div>
                        <div class="card"><h3>03-plano-checklists-e-templates.csv</h3><p>Planilha para preencher ações, evidências, checkpoints e próximos ajustes.</p></div>
                """);
        for (DeliverableContent content : input.contentPackage().deliverables()) {
            html.append("<div class=\"card\"><h3>")
                    .append(escape(content.title()))
                    .append("</h3><p>")
                    .append(escape(content.firstWin()))
                    .append("</p></div>");
        }
        html.append("""
                      </div>
                    </section>
                    <section>
                      <h2>Fechamento</h2>
                      <p>Ao final dos 7 dias, registre o que ficou mais coerente, o que você deixou de comprar por impulso e qual próximo microajuste manterá sua presença alinhada.</p>
                    </section>
                  </main>
                  <footer>
                    <p>Experiência guiada do Método MUSA. Use os arquivos de apoio dentro deste pacote para preencher seu plano.</p>
                  </footer>
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
        for (VisualAsset visualAsset : input.visualAssets()) {
            items.add(new ManifestItem(
                    visualAsset.fileName(),
                    visualAsset.contentType(),
                    "Imagem editorial: " + visualAsset.title(),
                    String.valueOf(order++),
                    sha256(visualAsset.content())));
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
            DigitalAssetFinal experience,
            DigitalAssetFinal pdf,
            DigitalAssetFinal spreadsheet,
            OfferDeliveryManifest manifest,
            FabricationReport report) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            addZip(zip, experience.name(), experience.content());
            addZip(zip, pdf.name(), pdf.content());
            addZip(zip, spreadsheet.name(), spreadsheet.content());
            for (VisualAsset visualAsset : input.visualAssets()) {
                addZip(zip, visualAsset.fileName(), visualAsset.content());
            }
            addZip(zip, "README.txt", readme(input, manifest).getBytes(StandardCharsets.UTF_8));
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException ex) {
            log.error("Falha ao montar ZIP da FEO", ex);
            throw new IllegalStateException("Falha ao montar ZIP da FEO", ex);
        }
    }

    /**
     * Cria orientação simples para a compradora abrir o pacote.
     */
    private String readme(PackageAssemblyInput input, OfferDeliveryManifest manifest) {
        return "Metodo MUSA - Produto Digital Experiencial\n\n"
                + "Comece pelo arquivo 01-experiencia-guiada/index.html.\n"
                + "Use a experiencia para fazer o diagnostico, cumprir as missoes de 7 dias e marcar seu progresso.\n"
                + "Depois consulte 02-ebook-principal.pdf para aprofundar o metodo.\n"
                + "Use 03-plano-checklists-e-templates.csv para preencher seu plano.\n"
                + "As imagens da pasta imagens/ sao figuras de apoio da experiencia e do e-book.\n\n"
                + "Arquivos do pacote:\n"
                + String.join("\n", manifest.items().stream().map(ManifestItem::fileName).toList())
                + "\n\nPromessa: " + input.context().centralPromise() + "\n";
    }

    /**
     * Adiciona imagem editorial ao PDF quando o ativo existir.
     */
    private void appendVisual(StringBuilder html, PackageAssemblyInput input, String assetType) {
        VisualAsset visual = visualByType(input, assetType);
        if (visual == null) {
            return;
        }
        html.append("<h2>")
                .append(escape(visual.title()))
                .append("</h2><img class=\"visual\" src=\"")
                .append(dataUri(visual))
                .append("\" alt=\"")
                .append(escape(visual.title()))
                .append("\" />");
    }

    /**
     * Adiciona imagem na experiencia guiada usando caminho relativo dentro do ZIP.
     */
    private void appendExperienceVisual(StringBuilder html, PackageAssemblyInput input, String assetType) {
        VisualAsset visual = visualByType(input, assetType);
        if (visual == null) {
            return;
        }
        html.append("<img class=\"visual\" src=\"../")
                .append(escape(visual.fileName()))
                .append("\" alt=\"")
                .append(escape(visual.title()))
                .append("\" />");
    }

    /**
     * Define o titulo comercial de cada dia da experiencia.
     */
    private String dayTitle(int day) {
        return switch (day) {
            case 1 -> "Escolher o foco de presença";
            case 2 -> "Organizar cabelo, pele e detalhe principal";
            case 3 -> "Montar uma combinação coerente";
            case 4 -> "Criar assinatura olfativa acessível";
            case 5 -> "Usar o checklist de 12 minutos";
            case 6 -> "Evitar uma compra por impulso";
            case 7 -> "Registrar antes/depois e próximos passos";
            default -> "Microajuste de presença";
        };
    }

    /**
     * Define a ação prática de cada dia da experiência.
     */
    private String dayAction(int day) {
        return switch (day) {
            case 1 -> "Marque o ruído visual mais forte e escolha uma única área para ajustar primeiro.";
            case 2 -> "Faça um ajuste simples de cabelo, pele, unha ou acabamento antes de pensar em comprar algo.";
            case 3 -> "Separe uma roupa possível com o que você já tem e escolha um ponto de cor ou contraste.";
            case 4 -> "Defina um perfume ou cheiro-base para uma ocasião real da sua semana.";
            case 5 -> "Passe pelo checklist rápido antes de sair ou gravar conteúdo.";
            case 6 -> "Revise uma compra desejada e veja se ela resolve o foco escolhido ou só aumenta tentativa.";
            case 7 -> "Compare seu ponto de partida com o estado atual e escolha o próximo microajuste.";
            default -> "Execute uma ação pequena e registre a evidência de progresso.";
        };
    }

    /**
     * Define o checkpoint de conclusão de cada dia da experiência.
     */
    private String dayCheckpoint(int day) {
        return switch (day) {
            case 1 -> "Tenho uma frase clara sobre o que quero ajustar.";
            case 2 -> "Fiz um ajuste visível sem depender de compra.";
            case 3 -> "Tenho uma combinação possível para repetir.";
            case 4 -> "Escolhi uma assinatura olfativa por ocasião.";
            case 5 -> "Completei o checklist sem travar.";
            case 6 -> "Decidi reaproveitar, adiar ou comprar com intenção.";
            case 7 -> "Registrei uma evidência de progresso e o próximo passo.";
            default -> "Concluí a microação do dia.";
        };
    }

    /**
     * Busca a primeira imagem do tipo solicitado.
     */
    private VisualAsset visualByType(PackageAssemblyInput input, String assetType) {
        if (input.visualAssets() == null) {
            return null;
        }
        return input.visualAssets().stream()
                .filter(asset -> assetType.equals(asset.assetType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Converte a imagem em URI embutida para renderização do PDF.
     */
    private String dataUri(VisualAsset visual) {
        return "data:" + visual.contentType() + ";base64," + Base64.getEncoder().encodeToString(visual.content());
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
                  <h2>Principio aplicado</h2>
                  <p>""").append(escape(content.appliedPrinciple())).append("""
                </p>
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
            case "DIAGNOSTICO_GUIADO" -> "Diagnostico guiado";
            case "MISSOES_7_DIAS" -> "Missoes de 7 dias";
            case "PAINEL_PROGRESSO" -> "Painel de progresso";
            case "PLANO_EXECUCAO_RAPIDA" -> "Plano de execucao";
            case "CHECKLIST_APLICACAO" -> "Checklist";
            case "TEMPLATES_PRONTOS" -> "Templates prontos";
            case "EXEMPLO_PREENCHIDO" -> "Exemplo preenchido";
            case "PROVA_TANGIVEL" -> "Prova tangivel";
            case "BIBLIOTECA_APOIO" -> "Biblioteca de apoio";
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
