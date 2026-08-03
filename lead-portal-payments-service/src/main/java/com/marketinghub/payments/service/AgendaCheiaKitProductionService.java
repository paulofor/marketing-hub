package com.marketinghub.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.dto.AgendaCheiaDeliveryResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.model.AgendaCheiaDelivery;
import com.marketinghub.payments.repository.AgendaCheiaDeliveryRepository;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Produz, revisa e libera o pacote personalizado Agenda Cheia a partir do briefing pago. */
@Service
public class AgendaCheiaKitProductionService {
    private static final Logger log = LoggerFactory.getLogger(AgendaCheiaKitProductionService.class);
    private static final List<String> FORBIDDEN = List.of("payload", "debug", "prompt", "localhost", "jobid");

    private final AgendaCheiaDeliveryRepository repository;
    private final DigitalProductPostPurchaseEmailService emailService;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;
    private final String publicBaseUrl;

    /** Configura persistência, envio e armazenamento privado dos kits. */
    public AgendaCheiaKitProductionService(
            AgendaCheiaDeliveryRepository repository,
            DigitalProductPostPurchaseEmailService emailService,
            ObjectMapper objectMapper,
            @Value("${agenda-cheia.production.storage-root:/data/agenda-cheia}") String storageRoot,
            @Value("${payments.public-base-url:https://pagamentopalf.site}") String publicBaseUrl) {
        this.repository = repository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /** Executa idempotentemente geração, gate de qualidade e entrega do kit. */
    public AgendaCheiaDeliveryResponse produceAndDeliver(
            AgendaCheiaBriefing briefing, MercadoPagoPaymentDetails payment) {
        AgendaCheiaDelivery delivery = repository.findByBriefingId(briefing.getId())
                .orElseGet(AgendaCheiaDelivery::new);
        if ("ENTREGUE".equals(delivery.getStatus())) {
            return toResponse(delivery);
        }
        initialize(delivery, briefing);
        try {
            delivery.setStatus("EM_PRODUCAO");
            delivery.setStageCode("COMPOSICAO_DO_KIT");
            repository.save(delivery);
            ProductionResult result = generate(briefing, delivery.getDownloadToken());
            delivery.setStageCode("REVISAO_DE_QUALIDADE");
            delivery.setQualityScore(result.qualityScore());
            if (delivery.getQualityScore() < 90) {
                throw new IllegalStateException("O kit não atingiu o padrão mínimo de qualidade");
            }
            delivery.setArtifactPath(result.zipPath().toString());
            delivery.setManifestJson(manifest(result));
            delivery.setFinishedAt(Instant.now());
            delivery.setStageCode("ENTREGA");
            delivery.setStatus("PRONTO_PARA_ENTREGA");
            repository.save(delivery);
            String downloadUrl = downloadUrl(delivery);
            emailService.sendCompletedKit(payment, briefing.getBuyerEmail(), briefing.getProfessionalName(), downloadUrl);
            delivery.setStatus("ENTREGUE");
            delivery.setDeliveredAt(Instant.now());
            delivery.setErrorMessage(null);
            repository.save(delivery);
            log.info("Kit Agenda Cheia produzido e entregue. paymentId={}, briefingId={}, qualityScore={}",
                    briefing.getPaymentId(), briefing.getId(), delivery.getQualityScore());
            return toResponse(delivery);
        } catch (Exception ex) {
            log.error("Falha na produção do Agenda Cheia. paymentId={}, briefingId={}, stage={}",
                    briefing.getPaymentId(), briefing.getId(), delivery.getStageCode(), ex);
            delivery.setStatus("FALHA_TECNICA");
            delivery.setErrorMessage(safeMessage(ex));
            delivery.setFinishedAt(Instant.now());
            repository.save(delivery);
            throw new IllegalStateException("Não foi possível concluir o kit personalizado", ex);
        }
    }

    /** Resolve o arquivo privado apenas por token opaco válido. */
    public Path artifact(String token) {
        AgendaCheiaDelivery delivery = repository.findByDownloadToken(token)
                .filter(item -> "ENTREGUE".equals(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Entrega não encontrada"));
        Path artifact = Path.of(delivery.getArtifactPath()).toAbsolutePath().normalize();
        if (!artifact.startsWith(storageRoot) || !Files.isRegularFile(artifact)) {
            throw new IllegalStateException("Arquivo de entrega indisponível");
        }
        return artifact;
    }

    /** Inicializa os metadados auditáveis sem duplicar a execução. */
    private void initialize(AgendaCheiaDelivery delivery, AgendaCheiaBriefing briefing) {
        if (delivery.getId() == null) {
            delivery.setBriefingId(briefing.getId());
            delivery.setPaymentId(briefing.getPaymentId());
            delivery.setDownloadToken(UUID.randomUUID().toString().replace("-", ""));
            delivery.setStartedAt(Instant.now());
        }
        delivery.setErrorMessage(null);
    }

    /** Gera imagens e textos em diretório temporário e publica um ZIP atômico. */
    private ProductionResult generate(AgendaCheiaBriefing briefing, String token) throws IOException {
        Files.createDirectories(storageRoot);
        Path work = Files.createTempDirectory(storageRoot, "kit-");
        List<Path> images = new ArrayList<>();
        List<String> captions = captions(briefing);
        try {
            for (int index = 0; index < 10; index++) {
                images.add(render(work.resolve("post-%02d.png".formatted(index + 1)), 1080, 1080,
                        briefing, headline(index), index));
                images.add(render(work.resolve("story-%02d.png".formatted(index + 1)), 1080, 1920,
                        briefing, headline(index), index + 10));
            }
            Files.writeString(work.resolve("legendas-prontas.txt"), String.join("\n\n---\n\n", captions), StandardCharsets.UTF_8);
            Files.writeString(work.resolve("mensagens-whatsapp.txt"), whatsappMessages(briefing), StandardCharsets.UTF_8);
            Files.writeString(work.resolve("calendario-7-dias.txt"), calendar(), StandardCharsets.UTF_8);
            Files.writeString(work.resolve("LEIA-ME.txt"), instructions(briefing), StandardCharsets.UTF_8);
            Path temporaryZip = work.resolve("agenda-cheia.zip");
            int qualityScore = reviewImages(images, captions.size(), 5, 7);
            zip(work, temporaryZip);
            Path finalZip = storageRoot.resolve("agenda-cheia-" + token + ".zip");
            Files.move(temporaryZip, finalZip, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            if (Files.size(finalZip) <= 50_000) qualityScore = 0;
            return new ProductionResult(finalZip, captions.size(), 5, 7, qualityScore);
        } finally {
            deleteTree(work);
        }
    }

    /** Renderiza uma arte premium com texto aplicado fora de imagens geradas. */
    private Path render(Path output, int width, int height, AgendaCheiaBriefing briefing,
                        String headline, int variant) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[] palette = palette(briefing.getPreferredColors(), variant);
        graphics.setPaint(new java.awt.GradientPaint(0, 0, palette[0], width, height, palette[1]));
        graphics.fillRect(0, 0, width, height);
        drawNailEditorial(graphics, width, height, palette[2], variant);
        graphics.setColor(new Color(255, 255, 255, 238));
        graphics.fill(new RoundRectangle2D.Float(58, height * .56f, width - 116, height * .35f, 42, 42));
        graphics.setColor(new Color(48, 25, 37));
        graphics.setFont(new Font("SansSerif", Font.BOLD, width / 18));
        drawWrapped(graphics, headline, 102, (int) (height * .63), width - 204, width / 16);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, width / 34));
        graphics.drawString(serviceName(briefing), 102, (int) (height * .83));
        graphics.setFont(new Font("SansSerif", Font.BOLD, width / 38));
        graphics.drawString(publicText(briefing.getProfessionalName()), 70, 92);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, width / 45));
        graphics.drawString(publicText(briefing.getCityRegion()) + "  •  WhatsApp "
                + publicText(briefing.getWhatsapp()), 70, 130);
        graphics.dispose();
        ImageIO.write(image, "png", output.toFile());
        return output;
    }

    /** Desenha uma composição editorial inspirada em unhas modernas sem depender de imagem externa. */
    private void drawNailEditorial(Graphics2D graphics, int width, int height, Color accent, int variant) {
        graphics.setColor(new Color(255, 255, 255, 42));
        graphics.fillOval(width / 2 - 320, height / 8, 650, 650);
        for (int index = 0; index < 5; index++) {
            int x = width / 2 - 260 + index * 115;
            int y = height / 6 + Math.abs(2 - index) * 38;
            graphics.setColor(new Color(238, 190, 178));
            graphics.fillRoundRect(x, y, 92, 330, 70, 70);
            graphics.setColor(index == variant % 5 ? accent : accent.brighter());
            graphics.fillRoundRect(x + 8, y + 5, 76, 145, 62, 62);
            graphics.setColor(new Color(255, 255, 255, 105));
            graphics.fillRoundRect(x + 25, y + 19, 15, 92, 12, 12);
        }
    }

    /** Quebra texto em linhas legíveis dentro da área segura. */
    private void drawWrapped(Graphics2D graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics metrics = graphics.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                graphics.drawString(line.toString(), x, y);
                y += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        graphics.drawString(line.toString(), x, y);
    }

    /** Aplica o gate determinístico de quantidade, dimensões e conteúdo publicável. */
    private int reviewImages(List<Path> images, int captionCount, int messageCount, int calendarDays) throws IOException {
        if (images.size() != 20 || captionCount != 10 || messageCount != 5 || calendarDays != 7) {
            return 0;
        }
        for (Path image : images) {
            BufferedImage decoded = ImageIO.read(image.toFile());
            if (decoded == null || decoded.getWidth() != 1080
                    || (decoded.getHeight() != 1080 && decoded.getHeight() != 1920)) {
                return 0;
            }
        }
        return 100;
    }

    /** Serializa apenas o resumo funcional do pacote final. */
    private String manifest(ProductionResult result) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new Manifest(10, 10, result.captionCount(),
                result.messageCount(), result.calendarDays(), "PNG", "pronto para publicar"));
    }

    /** Compacta todos os artefatos, excluindo o próprio arquivo de saída. */
    private void zip(Path source, Path output) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(output)))) {
            try (var paths = Files.list(source)) {
                for (Path path : paths.filter(item -> !item.equals(output)).sorted().toList()) {
                    zip.putNextEntry(new ZipEntry(path.getFileName().toString()));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }

    /** Remove somente o diretório temporário criado pela execução. */
    private void deleteTree(Path root) {
        try {
            if (!Files.exists(root)) return;
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            log.warn("Não foi possível limpar diretório temporário do Agenda Cheia. path={}", root, ex);
        }
    }

    /** Cria dez legendas com chamada clara e sem garantia de resultado. */
    private List<String> captions(AgendaCheiaBriefing briefing) {
        List<String> result = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            result.add((index + 1) + ". " + headline(index) + "\n\n" + serviceName(briefing)
                    + " em " + publicText(briefing.getCityRegion())
                    + ". Quer consultar horários? Chame no WhatsApp: "
                    + publicText(briefing.getWhatsapp()) + ".");
        }
        return result;
    }

    /** Cria cinco respostas comerciais reutilizáveis no WhatsApp. */
    private String whatsappMessages(AgendaCheiaBriefing briefing) {
        return "1. Oi! Que bom receber sua mensagem. Qual serviço você deseja fazer?\n\n"
                + "2. Tenho opções de horário nesta semana. Qual período funciona melhor para você?\n\n"
                + "3. Trabalho com " + serviceName(briefing) + ". Posso te explicar as opções e valores.\n\n"
                + "4. Quer que eu reserve esse horário enquanto confirmamos os detalhes?\n\n"
                + "5. Posso enviar as próximas disponibilidades pelo WhatsApp?";
    }

    /** Cria o calendário funcional de sete dias. */
    private String calendar() {
        return "Dia 1 — Apresente seu serviço principal\nDia 2 — Mostre agenda aberta\n"
                + "Dia 3 — Publique um detalhe do acabamento\nDia 4 — Convide clientes antigas\n"
                + "Dia 5 — Divulgue horários do fim de semana\nDia 6 — Mostre opções de estilo\n"
                + "Dia 7 — Reforce o contato pelo WhatsApp";
    }

    /** Cria instruções simples de uso e limites realistas do produto. */
    private String instructions(AgendaCheiaBriefing briefing) {
        return "AGENDA CHEIA NAIL DESIGN — KIT PERSONALIZADO\n\nProduzido para: "
                + publicText(briefing.getProfessionalName())
                + "\n\nUse uma arte por dia com a legenda correspondente. "
                + "O kit melhora sua apresentação e cria oportunidades de conversa; não garante clientes ou agendamentos.";
    }

    /** Seleciona a chamada de cada situação comercial. */
    private String headline(int index) {
        return List.of("Agenda aberta esta semana", "Seu próximo nail design começa aqui",
                "Horários disponíveis", "Alongamento com acabamento elegante",
                "Que tal renovar suas unhas?", "Um detalhe muda toda a produção",
                "Volte a cuidar das suas unhas", "Escolha seu estilo favorito",
                "Reserve seu horário pelo WhatsApp", "Sua próxima inspiração está aqui").get(index);
    }

    /** Resolve um serviço curto para não poluir as artes. */
    private String serviceName(AgendaCheiaBriefing briefing) {
        String first = publicText(briefing.getServices().split("[,;\\n]")[0].trim());
        return first.length() > 60 ? first.substring(0, 60) : first;
    }

    /** Resolve paleta estável conforme estilo e variação do kit. */
    private Color[] palette(String preferredColors, int variant) {
        String text = preferredColors == null ? "" : preferredColors.toLowerCase(Locale.ROOT);
        Color base = text.contains("azul") ? new Color(54, 75, 120)
                : text.contains("verde") ? new Color(46, 94, 79)
                : text.contains("preto") ? new Color(35, 31, 38) : new Color(110, 35, 66);
        Color secondary = variant % 2 == 0 ? new Color(226, 166, 176) : new Color(201, 155, 105);
        return new Color[] {base, secondary, new Color(154 + variant * 5 % 80, 66, 101)};
    }

    /** Constrói a URL pública opaca do pacote aprovado. */
    private String downloadUrl(AgendaCheiaDelivery delivery) {
        return publicBaseUrl + "/api/v1/agenda-cheia/post-purchase/deliveries/"
                + delivery.getDownloadToken() + "/download";
    }

    /** Converte a execução para contrato público sem caminho interno ou erro técnico. */
    private AgendaCheiaDeliveryResponse toResponse(AgendaCheiaDelivery delivery) {
        String url = "ENTREGUE".equals(delivery.getStatus()) ? downloadUrl(delivery) : null;
        return new AgendaCheiaDeliveryResponse(delivery.getStatus(), delivery.getStageCode(),
                delivery.getQualityScore(), url, delivery.getFinishedAt(), delivery.getDeliveredAt());
    }

    /** Reduz exceções a uma causa persistível sem dados sensíveis. */
    private String safeMessage(Exception ex) {
        String message = ex.getMessage() == null ? "Falha na produção" : ex.getMessage();
        String normalized = message.toLowerCase(Locale.ROOT);
        return FORBIDDEN.stream().anyMatch(normalized::contains) ? "Falha na produção do kit" : message.substring(0, Math.min(1000, message.length()));
    }

    /** Sanitiza dados do briefing antes de incorporá-los aos artefatos públicos. */
    private String publicText(String value) {
        String result = value == null ? "" : value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").trim();
        for (String forbidden : FORBIDDEN) {
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(forbidden), "");
        }
        return result.replaceAll("\\s+", " ");
    }

    /** Agrupa os artefatos usados pelo gate antes da publicação. */
    private record ProductionResult(Path zipPath, int captionCount, int messageCount, int calendarDays, int qualityScore) {}

    /** Descreve somente o conteúdo comercial entregue. */
    private record Manifest(int posts, int stories, int captions, int whatsappMessages,
                            int calendarDays, String imageFormat, String usage) {}
}
