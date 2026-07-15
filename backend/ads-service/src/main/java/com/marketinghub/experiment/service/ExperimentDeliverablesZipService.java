package com.marketinghub.experiment.service;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageExecution;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.feo.fabricacao.v1.FeoFabricacaoV1StageExecutionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: montar o arquivo ZIP com os entregáveis vinculados a um experimento. */
@Service
public class ExperimentDeliverablesZipService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));
    private static final String STAGE_MONTAGEM = "montagem-pacote";
    private static final TypeReference<List<Map<String, Object>>> ARTIFACT_LIST_TYPE = new TypeReference<>() {};

    private final ExperimentRepository experimentRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeliverablePackageRepository deliverablePackageRepository;
    private final FeoFabricacaoV1StageExecutionRepository feoExecutionRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com os repositórios necessários para coletar os entregáveis. */
    public ExperimentDeliverablesZipService(
            ExperimentRepository experimentRepository,
            DeliverableRepository deliverableRepository,
            DeliverablePackageRepository deliverablePackageRepository,
            FeoFabricacaoV1StageExecutionRepository feoExecutionRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.deliverableRepository = deliverableRepository;
        this.deliverablePackageRepository = deliverablePackageRepository;
        this.feoExecutionRepository = feoExecutionRepository;
        this.objectMapper = objectMapper;
    }

    /** Gera o ZIP de entregáveis do experimento a partir dos dados persistidos. */
    @Transactional(readOnly = true)
    public byte[] generate(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experiment not found: " + experimentId));
        byte[] finalFeoZip = latestFeoFinalZip(experimentId);
        if (finalFeoZip.length > 0) {
            return finalFeoZip;
        }
        Long nicheId = experiment.getNiche() != null ? experiment.getNiche().getId() : null;
        List<Deliverable> deliverables = nicheId != null
                ? deliverableRepository.findByNicheIdOrderByCreatedAtDesc(nicheId)
                : List.of();
        List<DeliverablePackage> packages =
                deliverablePackageRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            writeText(zip, "README.txt", buildReadme(experiment, deliverables, packages));
            writeLandingDeliverables(zip, experiment);
            writeDeliverables(zip, deliverables);
            writePackages(zip, packages);
            writeLatestFeoArtifacts(zip, experimentId);
            zip.finish();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not generate experiment deliverables zip",
                    ex);
        }
    }

    /** Monta o texto de orientação que acompanha o pacote baixado. */
    private String buildReadme(
            Experiment experiment,
            List<Deliverable> deliverables,
            List<DeliverablePackage> packages) {
        StringBuilder text = new StringBuilder();
        text.append("Entregaveis do experimento\n");
        text.append("==========================\n\n");
        text.append("Experimento: ").append(safe(experiment.getName())).append("\n");
        text.append("ID: ").append(experiment.getId()).append("\n");
        text.append("Nicho: ")
                .append(experiment.getNiche() != null ? safe(experiment.getNiche().getName()) : "-")
                .append("\n");
        text.append("Hipotese: ").append(safe(experiment.getHypothesis())).append("\n\n");
        text.append("Conteudo do ZIP:\n");
        text.append("- landing-page-deliverables.json: artefato final do GeraLanding quando existir.\n");
        text.append("- entregaveis/: versoes HTML prontas para leitura dos entregaveis aprovados.\n");
        text.append("- pacotes/: versoes HTML prontas para revisao dos pacotes vinculados.\n");
        text.append("- feo/: artefatos finais premium fabricados pela FEO quando a montagem ja concluiu.\n\n");
        text.append("Totais:\n");
        text.append("- Entregaveis do nicho: ").append(deliverables.size()).append("\n");
        text.append("- Pacotes do experimento: ").append(packages.size()).append("\n");
        return text.toString();
    }

    /** Escreve o artefato final de entregáveis da landing quando ele estiver disponível. */
    private void writeLandingDeliverables(ZipOutputStream zip, Experiment experiment) throws IOException {
        if (StringUtils.hasText(experiment.getLandingPageDeliverables())) {
            writeText(zip, "landing-page-deliverables.json", experiment.getLandingPageDeliverables());
        }
    }

    /** Escreve cada entregável aprovado do nicho em um arquivo HTML pronto para leitura. */
    private void writeDeliverables(ZipOutputStream zip, List<Deliverable> deliverables) throws IOException {
        for (Deliverable deliverable : deliverables) {
            String fileName = "entregaveis/%03d-%s.html".formatted(
                    deliverable.getId(),
                    slug(deliverable.getTitle(), "entregavel"));
            writeText(zip, fileName, renderDeliverable(deliverable));
        }
    }

    /** Escreve cada pacote vinculado ao experimento em um arquivo HTML pronto para revisão. */
    private void writePackages(ZipOutputStream zip, List<DeliverablePackage> packages) throws IOException {
        for (DeliverablePackage pack : packages) {
            String fileName = "pacotes/%03d-%s.html".formatted(
                    pack.getId(),
                    slug(pack.getName(), "pacote"));
            writeText(zip, fileName, renderPackage(pack));
        }
    }

    /** Renderiza um entregável individual em HTML de consumo final. */
    private String renderDeliverable(Deliverable deliverable) {
        StringBuilder body = new StringBuilder();
        appendHero(body, safe(deliverable.getTitle()), "Entregavel do produto");
        appendDefinitionList(body, List.of(
                List.of("ID", String.valueOf(deliverable.getId())),
                List.of("Modelo", safe(deliverable.getModel())),
                List.of("Criado em", safe(formatInstant(deliverable.getCreatedAt()))),
                List.of("Atualizado em", safe(formatInstant(deliverable.getUpdatedAt())))));
        appendSection(body, "Descricao", deliverable.getDescription());
        appendSection(body, "Conteudo pronto para uso", deliverable.getContent());
        appendSection(body, "Prompt e rastreabilidade", deliverable.getPrompt());
        return wrapHtml(deliverable.getTitle(), body.toString());
    }

    /** Renderiza um pacote de entregáveis em HTML de revisão e entrega. */
    private String renderPackage(DeliverablePackage pack) {
        StringBuilder body = new StringBuilder();
        appendHero(body, safe(pack.getName()), "Pacote de entregaveis");
        appendDefinitionList(body, List.of(
                List.of("ID", String.valueOf(pack.getId())),
                List.of("Modelo", safe(pack.getModel())),
                List.of("Criado em", safe(formatInstant(pack.getCreatedAt()))),
                List.of("Atualizado em", safe(formatInstant(pack.getUpdatedAt())))));
        appendSection(body, "Descricao", pack.getDescription());
        appendSection(body, "Prompt e rastreabilidade", pack.getPrompt());
        body.append("<section><h2>Entregaveis vinculados</h2><ul>");
        if (pack.getDeliverables() == null || pack.getDeliverables().isEmpty()) {
            body.append("<li>Nenhum entregavel vinculado.</li>");
        } else {
            for (Deliverable deliverable : pack.getDeliverables()) {
                body.append("<li><strong>").append(escape(safe(deliverable.getTitle()))).append("</strong>");
                if (StringUtils.hasText(deliverable.getDescription())) {
                    body.append(": ").append(escape(deliverable.getDescription().trim()));
                }
                body.append("</li>");
            }
        }
        body.append("</ul></section>");
        return wrapHtml(pack.getName(), body.toString());
    }

    /** Inclui os artefatos finais reais da FEO quando a etapa de montagem já foi concluída. */
    private void writeLatestFeoArtifacts(ZipOutputStream zip, Long experimentId) throws IOException {
        FeoFabricacaoV1StageExecution execution = feoExecutionRepository
                .findFirstByExperimentIdAndStageCodeAndStatusOrderByFinishedAtDesc(
                        experimentId,
                        STAGE_MONTAGEM,
                        FeoFabricacaoV1StageStatus.COMPLETED)
                .orElse(null);
        if (execution == null || !StringUtils.hasText(execution.getArtifactsPayload())) {
            return;
        }
        List<Map<String, Object>> artifacts = readArtifacts(execution.getArtifactsPayload());
        for (Map<String, Object> artifact : artifacts) {
            String name = stringValue(artifact.get("name"), "artefato-feo.bin");
            byte[] content = artifactBytes(artifact.get("content"));
            if (content.length == 0) {
                continue;
            }
            writeBytes(zip, "feo/" + slug(name, "artefato-feo") + extensionFrom(name), content);
        }
    }

    /** Retorna diretamente o ZIP final público da FEO quando ele já existir. */
    private byte[] latestFeoFinalZip(Long experimentId) {
        FeoFabricacaoV1StageExecution execution = feoExecutionRepository
                .findFirstByExperimentIdAndStageCodeAndStatusOrderByFinishedAtDesc(
                        experimentId,
                        STAGE_MONTAGEM,
                        FeoFabricacaoV1StageStatus.COMPLETED)
                .orElse(null);
        if (execution == null || !StringUtils.hasText(execution.getArtifactsPayload())) {
            return new byte[0];
        }
        List<Map<String, Object>> artifacts = readArtifacts(execution.getArtifactsPayload());
        for (Map<String, Object> artifact : artifacts) {
            String type = stringValue(artifact.get("type"), "");
            String name = stringValue(artifact.get("name"), "");
            if ("FINAL_ZIP".equals(type) || name.endsWith(".zip")) {
                byte[] content = artifactBytes(artifact.get("content"));
                if (content.length > 0) {
                    return content;
                }
            }
        }
        return new byte[0];
    }

    /** Escreve um arquivo de texto no ZIP usando UTF-8. */
    private void writeText(ZipOutputStream zip, String name, String content) throws IOException {
        writeBytes(zip, name, (content != null ? content : "").getBytes(StandardCharsets.UTF_8));
    }

    /** Escreve bytes no ZIP preservando o nome informado. */
    private void writeBytes(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(content != null ? content : new byte[0]);
        zip.closeEntry();
    }

    /** Adiciona capa curta ao HTML do arquivo. */
    private void appendHero(StringBuilder html, String title, String subtitle) {
        html.append("<section class=\"cover\"><p class=\"eyebrow\">")
                .append(escape(subtitle))
                .append("</p><h1>")
                .append(escape(title))
                .append("</h1></section>");
    }

    /** Adiciona metadados em grade compacta. */
    private void appendDefinitionList(StringBuilder html, List<List<String>> fields) {
        html.append("<dl class=\"meta\">");
        for (List<String> field : fields) {
            if (field.size() == 2 && StringUtils.hasText(field.get(1)) && !"-".equals(field.get(1))) {
                html.append("<div><dt>")
                        .append(escape(field.get(0)))
                        .append("</dt><dd>")
                        .append(escape(field.get(1)))
                        .append("</dd></div>");
            }
        }
        html.append("</dl>");
    }

    /** Adiciona uma seção HTML quando houver conteúdo. */
    private void appendSection(StringBuilder html, String title, String content) {
        if (StringUtils.hasText(content)) {
            html.append("<section><h2>")
                    .append(escape(title))
                    .append("</h2>")
                    .append(renderTextContent(content.trim()))
                    .append("</section>");
        }
    }

    /** Formata instantes persistidos em UTC para gerar arquivos estáveis. */
    private String formatInstant(java.time.Instant instant) {
        return instant != null ? DATE_TIME_FORMATTER.format(instant) + " UTC" : null;
    }

    /** Retorna texto seguro para campos opcionais. */
    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    /** Converte um texto bruto em blocos HTML simples sem entregar Markdown cru ao comprador. */
    private String renderTextContent(String content) {
        String[] paragraphs = content.split("\\R{2,}");
        StringBuilder html = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.lines().allMatch(line -> line.trim().matches("^([-*•]|\\d+[.)])\\s+.+"))) {
                html.append("<ul>");
                trimmed.lines().forEach(line -> html.append("<li>")
                        .append(escape(line.replaceFirst("^([-*•]|\\d+[.)])\\s+", "").trim()))
                        .append("</li>"));
                html.append("</ul>");
            } else {
                html.append("<p>").append(escape(trimmed).replace("\n", "<br />")).append("</p>");
            }
        }
        return html.toString();
    }

    /** Envolve o corpo em HTML com acabamento de produto final. */
    private String wrapHtml(String title, String body) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="utf-8" />
                  <title>%s</title>
                  <style>
                    body { font-family: Arial, sans-serif; color: #1d2428; margin: 0; background: #f5f7f8; }
                    main { max-width: 900px; margin: 0 auto; background: #fff; min-height: 100vh; padding: 44px 56px; }
                    .cover { border-bottom: 4px solid #0f766e; padding-bottom: 22px; margin-bottom: 26px; }
                    .eyebrow { color: #0f766e; font-weight: 700; text-transform: uppercase; font-size: 12px; letter-spacing: .04em; }
                    h1 { font-size: 34px; margin: 0; color: #123b3a; }
                    h2 { font-size: 21px; color: #123b3a; margin-top: 30px; }
                    p, li, dd { font-size: 15px; line-height: 1.58; }
                    .meta { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
                    .meta div { border: 1px solid #d8e2df; padding: 12px; background: #f8fbfa; }
                    dt { font-size: 11px; text-transform: uppercase; color: #51706b; font-weight: 700; }
                    dd { margin: 4px 0 0; }
                  </style>
                </head>
                <body><main>%s</main></body>
                </html>
                """.formatted(escape(title), body);
    }

    /** Escapa texto para uso seguro em HTML. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Lê a lista de artefatos serializada no callback FEO. */
    private List<Map<String, Object>> readArtifacts(String json) {
        try {
            return objectMapper.readValue(json, ARTIFACT_LIST_TYPE);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read FEO artifacts", ex);
        }
    }

    /** Decodifica o conteúdo de artefato recebido pelo JSON do worker. */
    private byte[] artifactBytes(Object content) {
        if (content instanceof String value && StringUtils.hasText(value)) {
            return Base64.getDecoder().decode(value);
        }
        if (content instanceof List<?> values) {
            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < values.size(); i++) {
                bytes[i] = ((Number) values.get(i)).byteValue();
            }
            return bytes;
        }
        return new byte[0];
    }

    /** Retorna string com fallback para metadados de artefato. */
    private String stringValue(Object value, String fallback) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value);
    }

    /** Preserva extensão original quando o slug remover pontos do nome. */
    private String extensionFrom(String name) {
        int index = name == null ? -1 : name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
    }

    /** Cria um identificador simples para nomes de arquivos dentro do ZIP. */
    private String slug(String value, String fallback) {
        String source = StringUtils.hasText(value) ? value : fallback;
        String normalized = java.text.Normalizer.normalize(source, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }
}
