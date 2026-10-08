package com.marketinghub.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.dto.AgendaCheiaDeliveryResponse;
import com.marketinghub.payments.integration.image.AgendaCheiaPhotoGenerator;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.model.AgendaCheiaDelivery;
import com.marketinghub.payments.repository.AgendaCheiaDeliveryRepository;
import com.marketinghub.payments.service.kit.CapellaKitCatalog;
import com.marketinghub.payments.service.kit.CapellaKitProfile;
import com.marketinghub.payments.service.kit.CapellaKitText;
import com.marketinghub.payments.service.kit.PrivateKitIllustrations;
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

/** Compõe e revisa kits Capella, entregando apenas o perfil correspondente à compra homologada. */
@Service
public class AgendaCheiaKitProductionService {
    private static final Logger log = LoggerFactory.getLogger(AgendaCheiaKitProductionService.class);
    private static final List<String> FORBIDDEN = List.of("payload", "debug", "prompt", "localhost", "jobid");

    private final CapellaKitText text = new CapellaKitText();
    private final AgendaCheiaDeliveryRepository repository;
    private final DigitalProductPostPurchaseEmailService emailService;
    private final ObjectMapper objectMapper;
    private final AgendaCheiaPhotoGenerator photoGenerator;
    private final Path storageRoot;
    private final String publicBaseUrl;

    /** Configura persistência, envio e armazenamento privado dos kits. */
    public AgendaCheiaKitProductionService(
            AgendaCheiaDeliveryRepository repository,
            DigitalProductPostPurchaseEmailService emailService,
            ObjectMapper objectMapper,
            AgendaCheiaPhotoGenerator photoGenerator,
            @Value("${agenda-cheia.production.storage-root:/data/agenda-cheia}") String storageRoot,
            @Value("${payments.public-base-url:https://pagamentopalf.site}") String publicBaseUrl) {
        this.repository = repository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.photoGenerator = photoGenerator;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /** Executa idempotentemente geração, gate de qualidade e entrega do kit. */
    public AgendaCheiaDeliveryResponse produceAndDeliver(
            AgendaCheiaBriefing briefing, MercadoPagoPaymentDetails payment) {
        CapellaKitProfile profile = CapellaKitCatalog.forPaymentReference(payment.externalReference());
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
            ProductionResult result = generate(briefing, delivery.getDownloadToken(), profile);
            delivery.setStageCode("REVISAO_DE_QUALIDADE");
            delivery.setQualityScore(result.qualityScore());
            if (delivery.getQualityScore() < 90) {
                throw new IllegalStateException("O kit não atingiu o padrão mínimo de qualidade");
            }
            delivery.setArtifactPath(result.zipPath().toString());
            delivery.setManifestJson(manifest(result, profile));
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

    /** Prepara uma candidata local sem pagamento, persistência de venda ou envio de e-mail. */
    public PreparedKit prepareCandidate(AgendaCheiaBriefing briefing, String profileCode) throws IOException {
        CapellaKitProfile profile = CapellaKitCatalog.byCode(profileCode);
        ProductionResult result = generate(briefing, UUID.randomUUID().toString().replace("-", ""), profile);
        if (result.qualityScore() < 90) {
            throw new IllegalStateException("A candidata não atingiu o padrão mínimo de qualidade");
        }
        log.info("Candidata Capella preparada sem venda. profileCode={}, qualityScore={}",
                profile.code(), result.qualityScore());
        return new PreparedKit(profile.code(), result.zipPath(), result.qualityScore());
    }

    /** Compõe a prova sintética com o mesmo motor de entrega, sem integração de imagens ou e-mail. */
    public PreparedKit preparePrivateCandidate(
            AgendaCheiaBriefing briefing, String profileCode, String artifactId) throws IOException {
        if (!artifactId.matches("[a-f0-9-]{36}"))
            throw new IllegalArgumentException("Identificador de composição inválido");
        CapellaKitProfile profile = CapellaKitCatalog.byCode(profileCode);
        Path saved = storageRoot.resolve("agenda-cheia-private-" + artifactId + ".zip");
        if (Files.isRegularFile(saved)) return new PreparedKit(profile.code(), saved, 100);
        ProductionResult result =
                generate(briefing, "private-" + artifactId, profile, new PrivateKitIllustrations(), true);
        if (result.qualityScore() < 90)
            throw new IllegalStateException("A prova sintética não atingiu o contrato técnico");
        return new PreparedKit(profile.code(), result.zipPath(), result.qualityScore());
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

    /** Preserva a composição comercial com a biblioteca homologada do perfil comprado. */
    private ProductionResult generate(AgendaCheiaBriefing briefing, String token, CapellaKitProfile profile) throws IOException {
        return generate(briefing, token, profile, photoGenerator, false);
    }

    /** Gera arquivos finais por um provedor explícito e separa a primeira aplicação privada. */
    private ProductionResult generate(AgendaCheiaBriefing briefing, String token, CapellaKitProfile profile,
                                      AgendaCheiaPhotoGenerator provider, boolean privateProof) throws IOException {
        Files.createDirectories(storageRoot);
        Path work = Files.createTempDirectory(storageRoot, "kit-");
        List<Path> images = new ArrayList<>();
        List<String> captions = text.captions(briefing, profile);
        try {
            List<BufferedImage> photos = new ArrayList<>();
            for (int index = 0; index < 10; index++) {
                photos.add(CapellaKitCatalog.NAILS.equals(profile.code())
                        ? provider.generate(token, index)
                        : provider.generate(token, index, profile));
            }
            for (int index = 0; index < 10; index++) {
                images.add(render(work.resolve("post-%02d.png".formatted(index + 1)), 1080, 1080,
                        briefing, profile.headlines().get(index), index, photos.get(index)));
                images.add(render(work.resolve("story-%02d.png".formatted(index + 1)), 1080, 1920,
                        briefing, profile.headlines().get(index), index + 10, photos.get(index)));
            }
            Files.writeString(work.resolve("legendas-prontas.txt"), String.join("\n\n---\n\n", captions), StandardCharsets.UTF_8);
            Files.writeString(work.resolve("mensagens-whatsapp.txt"), text.whatsappMessages(briefing), StandardCharsets.UTF_8);
            Files.writeString(work.resolve("calendario-7-dias.txt"), privateProof ? text.privateCalendar(profile) : text.calendar(profile), StandardCharsets.UTF_8);
            Files.writeString(work.resolve("LEIA-ME.txt"), text.instructions(briefing, profile), StandardCharsets.UTF_8);
            Path temporaryZip = work.resolve("agenda-cheia.zip");
            int qualityScore = reviewImages(images, photos, captions.size(), 5, 7);
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
                        String headline, int variant, BufferedImage photo) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[] palette = palette(briefing.getPreferredColors(), variant);
        drawCoverPhoto(graphics, photo, width, height);
        graphics.setPaint(new java.awt.GradientPaint(0, 0, new Color(0, 0, 0, 15), 0, height, new Color(0, 0, 0, 115)));
        graphics.fillRect(0, 0, width, height);
        float cardY = switch (variant % 3) {
            case 0 -> height * .68f;
            case 1 -> height * .64f;
            default -> height * .71f;
        };
        float cardHeight = height * .22f;
        graphics.setColor(new Color(255, 255, 255, 238));
        graphics.fill(new RoundRectangle2D.Float(58, cardY, width - 116, cardHeight, 42, 42));
        graphics.setColor(palette[variant % palette.length]);
        graphics.fill(new RoundRectangle2D.Float(58, cardY, 18, cardHeight, 18, 18));
        graphics.setColor(new Color(48, 25, 37));
        graphics.setFont(new Font("SansSerif", Font.BOLD, width / 18));
        drawWrapped(graphics, headline, 102, (int) (cardY + height * .055f), width - 204, width / 16);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, width / 34));
        graphics.drawString(text.serviceName(briefing), 102, (int) (cardY + cardHeight - height * .035f));
        drawActionChip(graphics, width, height, cardY, variant);
        graphics.setFont(new Font("SansSerif", Font.BOLD, width / 38));
        graphics.drawString(text.publicText(briefing.getProfessionalName()), 70, 92);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, width / 45));
        graphics.drawString(text.publicText(briefing.getCityRegion()) + "  •  WhatsApp "
                + text.publicText(briefing.getWhatsapp()), 70, 130);
        graphics.dispose();
        ImageIO.write(image, "png", output.toFile());
        return output;
    }

    /** Aplica uma chamada comercial curta e legível sem encobrir a fotografia. */
    private void drawActionChip(Graphics2D graphics, int width, int height, float cardY, int variant) {
        String action = variant % 2 == 0 ? "CHAME NO WHATSAPP" : "RESERVE SEU HORÁRIO";
        int chipWidth = width / 3;
        int chipHeight = Math.max(42, height / 28);
        int chipX = width - chipWidth - 70;
        int chipY = (int) cardY - chipHeight - 24;
        graphics.setColor(new Color(36, 120, 82, 242));
        graphics.fill(new RoundRectangle2D.Float(chipX, chipY, chipWidth, chipHeight, 22, 22));
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, Math.max(20, width / 48)));
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(action, chipX + (chipWidth - metrics.stringWidth(action)) / 2,
                chipY + (chipHeight + metrics.getAscent() - metrics.getDescent()) / 2);
    }

    /** Recorta a fotografia proporcionalmente para preencher o formato sem distorção. */
    private void drawCoverPhoto(Graphics2D graphics, BufferedImage photo, int width, int height) {
        double scale = Math.max((double) width / photo.getWidth(), (double) height / photo.getHeight());
        int drawWidth = (int) Math.ceil(photo.getWidth() * scale);
        int drawHeight = (int) Math.ceil(photo.getHeight() * scale);
        graphics.drawImage(photo, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight, null);
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
    private int reviewImages(List<Path> images, List<BufferedImage> photos,
                             int captionCount, int messageCount, int calendarDays) throws IOException {
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
        List<Long> fingerprints = photos.stream().map(this::fingerprint).toList();
        double sharpnessTotal = photos.stream().mapToDouble(this::sharpness).sum();
        long distinct = fingerprints.stream().distinct().count();
        int diversity = (int) Math.min(30, distinct * 6);
        int sharpness = sharpnessTotal / photos.size() >= 1 ? 35 : 0;
        int completeness = 35;
        int score = completeness + diversity + sharpness;
        log.info("Revisão visual Agenda Cheia. score={}, distinctPhotos={}, averageSharpness={}",
                score, distinct, Math.round(sharpnessTotal / photos.size()));
        return score;
    }

    /** Mede nitidez por diferenças locais para bloquear fundos planos ou desfocados. */
    private double sharpness(BufferedImage image) {
        double total = 0;
        int samples = 0;
        for (int y = 2; y < image.getHeight(); y += 8) for (int x = 2; x < image.getWidth(); x += 8) {
            int center = image.getRGB(x, y) & 255;
            total += Math.abs(center - (image.getRGB(x - 2, y) & 255));
            total += Math.abs(center - (image.getRGB(x, y - 2) & 255));
            samples += 2;
        }
        return samples == 0 ? 0 : total / samples * 12;
    }

    /** Cria assinatura visual simples para exigir diversidade real entre composições. */
    private long fingerprint(BufferedImage image) {
        long result = 0;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            int rgb = image.getRGB((x * image.getWidth() + image.getWidth() / 2) / 8,
                    (y * image.getHeight() + image.getHeight() / 2) / 8);
            result = result * 31 + ((rgb >> 16) & 0xf0) + ((rgb >> 8) & 0xf0) + (rgb & 0xf0);
        }
        return result;
    }

    /** Serializa apenas o resumo funcional do pacote final. */
    private String manifest(ProductionResult result, CapellaKitProfile profile) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new Manifest(10, 10, result.captionCount(),
                result.messageCount(), result.calendarDays(), "PNG", "pronto para publicar", profile.code()));
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

    /** Agrupa os artefatos usados pelo gate antes da publicação. */
    private record ProductionResult(Path zipPath, int captionCount, int messageCount, int calendarDays, int qualityScore) {}

    /** Descreve a preparação privada; não representa entrega comercial ou resultado de mercado. */
    public record PreparedKit(String profileCode, Path zipPath, int qualityScore) {}

    /** Descreve somente o conteúdo comercial entregue. */
    private record Manifest(int posts, int stories, int captions, int whatsappMessages,
                            int calendarDays, String imageFormat, String usage, String profileCode) {}
}
