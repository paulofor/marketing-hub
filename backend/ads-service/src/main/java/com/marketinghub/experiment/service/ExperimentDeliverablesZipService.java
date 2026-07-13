package com.marketinghub.experiment.service;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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

    private final ExperimentRepository experimentRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeliverablePackageRepository deliverablePackageRepository;

    /** Inicializa o serviço com os repositórios necessários para coletar os entregáveis. */
    public ExperimentDeliverablesZipService(
            ExperimentRepository experimentRepository,
            DeliverableRepository deliverableRepository,
            DeliverablePackageRepository deliverablePackageRepository) {
        this.experimentRepository = experimentRepository;
        this.deliverableRepository = deliverableRepository;
        this.deliverablePackageRepository = deliverablePackageRepository;
    }

    /** Gera o ZIP de entregáveis do experimento a partir dos dados persistidos. */
    @Transactional(readOnly = true)
    public byte[] generate(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experiment not found: " + experimentId));
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
        text.append("- entregaveis/: definicoes aprovadas do nicho exibidas na aba Entregaveis.\n");
        text.append("- pacotes/: pacotes de entregaveis vinculados diretamente ao experimento.\n\n");
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

    /** Escreve cada entregável aprovado do nicho em um arquivo Markdown. */
    private void writeDeliverables(ZipOutputStream zip, List<Deliverable> deliverables) throws IOException {
        for (Deliverable deliverable : deliverables) {
            String fileName = "entregaveis/%03d-%s.md".formatted(
                    deliverable.getId(),
                    slug(deliverable.getTitle(), "entregavel"));
            writeText(zip, fileName, renderDeliverable(deliverable));
        }
    }

    /** Escreve cada pacote vinculado ao experimento em um arquivo Markdown. */
    private void writePackages(ZipOutputStream zip, List<DeliverablePackage> packages) throws IOException {
        for (DeliverablePackage pack : packages) {
            String fileName = "pacotes/%03d-%s.md".formatted(
                    pack.getId(),
                    slug(pack.getName(), "pacote"));
            writeText(zip, fileName, renderPackage(pack));
        }
    }

    /** Renderiza um entregável individual em formato legível para download. */
    private String renderDeliverable(Deliverable deliverable) {
        StringBuilder text = new StringBuilder();
        text.append("# ").append(safe(deliverable.getTitle())).append("\n\n");
        appendField(text, "ID", deliverable.getId());
        appendField(text, "Modelo", deliverable.getModel());
        appendField(text, "Criado em", formatInstant(deliverable.getCreatedAt()));
        appendField(text, "Atualizado em", formatInstant(deliverable.getUpdatedAt()));
        appendSection(text, "Descricao", deliverable.getDescription());
        appendSection(text, "Conteudo", deliverable.getContent());
        appendSection(text, "Prompt", deliverable.getPrompt());
        return text.toString();
    }

    /** Renderiza um pacote de entregáveis em formato legível para download. */
    private String renderPackage(DeliverablePackage pack) {
        StringBuilder text = new StringBuilder();
        text.append("# ").append(safe(pack.getName())).append("\n\n");
        appendField(text, "ID", pack.getId());
        appendField(text, "Modelo", pack.getModel());
        appendField(text, "Criado em", formatInstant(pack.getCreatedAt()));
        appendField(text, "Atualizado em", formatInstant(pack.getUpdatedAt()));
        appendSection(text, "Descricao", pack.getDescription());
        appendSection(text, "Prompt", pack.getPrompt());
        text.append("\n## Entregaveis vinculados\n\n");
        if (pack.getDeliverables() == null || pack.getDeliverables().isEmpty()) {
            text.append("- Nenhum entregavel vinculado.\n");
        } else {
            for (Deliverable deliverable : pack.getDeliverables()) {
                text.append("- ").append(safe(deliverable.getTitle()));
                if (StringUtils.hasText(deliverable.getDescription())) {
                    text.append(": ").append(deliverable.getDescription().trim());
                }
                text.append("\n");
            }
        }
        return text.toString();
    }

    /** Escreve um arquivo de texto no ZIP usando UTF-8. */
    private void writeText(ZipOutputStream zip, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write((content != null ? content : "").getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Adiciona um campo simples ao texto quando houver valor. */
    private void appendField(StringBuilder text, String label, Object value) {
        if (value != null && StringUtils.hasText(value.toString())) {
            text.append("- ").append(label).append(": ").append(value).append("\n");
        }
    }

    /** Adiciona uma seção Markdown quando houver conteúdo. */
    private void appendSection(StringBuilder text, String title, String content) {
        if (StringUtils.hasText(content)) {
            text.append("\n## ").append(title).append("\n\n");
            text.append(content.trim()).append("\n");
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
