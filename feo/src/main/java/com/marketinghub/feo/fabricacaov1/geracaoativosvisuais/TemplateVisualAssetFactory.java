package com.marketinghub.feo.fabricacaov1.geracaoativosvisuais;

import com.marketinghub.feo.fabricacaov1.contract.VisualAsset;
import com.marketinghub.feo.fabricacaov1.contract.VisualAssetSpec;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Cria imagens editoriais locais quando a geração externa não está disponível.
 */
public final class TemplateVisualAssetFactory {

    private static final Color INK = new Color(43, 32, 36);
    private static final Color BRAND = new Color(122, 36, 68);
    private static final Color GREEN = new Color(47, 89, 82);
    private static final Color GOLD = new Color(214, 167, 92);
    private static final Color SOFT = new Color(255, 250, 246);
    private static final Color BLUSH = new Color(247, 233, 238);
    private static final Color LINE = new Color(234, 216, 207);

    /**
     * Evita instanciação de fábrica utilitária.
     */
    private TemplateVisualAssetFactory() {}

    /**
     * Cria uma imagem PNG grande o suficiente para aparecer claramente no PDF final.
     */
    public static VisualAsset create(VisualAssetSpec spec) {
        boolean cover = "EBOOK_COVER".equals(spec.assetType());
        int width = cover ? 1400 : 1400;
        int height = cover ? 1900 : 950;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            configure(g);
            paintBackground(g, width, height);
            if (cover) {
                paintCover(g, spec, width, height);
            } else if ("BEFORE_AFTER".equals(spec.assetType())) {
                paintBeforeAfter(g, spec, width, height);
            } else if ("CONCEPT_MAP".equals(spec.assetType())) {
                paintConceptMap(g, spec, width, height);
            } else {
                paintInfographic(g, spec, width, height);
            }
            return new VisualAsset(
                    spec.code(),
                    spec.title(),
                    spec.assetType(),
                    "imagens/" + spec.code().toLowerCase() + "-" + slug(spec.title()) + ".png",
                    "image/png",
                    png(image),
                    spec.prompt(),
                    "local-editorial-template",
                    "{\"provider\":\"local-template\"}",
                    "{\"status\":\"generated\"}",
                    List.of("Imagem editorial local gerada como fallback seguro"));
        } finally {
            g.dispose();
        }
    }

    /**
     * Ativa antialiasing para o texto e formas ficarem legíveis.
     */
    private static void configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * Pinta fundo editorial claro com margens e linhas suaves.
     */
    private static void paintBackground(Graphics2D g, int width, int height) {
        g.setColor(SOFT);
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(255, 244, 247));
        g.fillOval(width - 520, -180, 700, 700);
        g.setColor(new Color(255, 248, 232));
        g.fillOval(-260, height - 420, 620, 620);
        g.setColor(LINE);
        g.setStroke(new BasicStroke(4f));
        g.draw(new RoundRectangle2D.Double(48, 48, width - 96, height - 96, 36, 36));
    }

    /**
     * Desenha capa com promessa aspiracional e sensação de produto premium.
     */
    private static void paintCover(Graphics2D g, VisualAssetSpec spec, int width, int height) {
        drawPill(g, "Jornada guiada de 7 dias", 118, 132, BRAND, Color.WHITE);
        drawWrapped(g, "Método MUSA", font(Font.BOLD, 92), BRAND, 118, 330, width - 236, 104);
        drawWrapped(g, "Presença elegante sem gastar muito", font(Font.BOLD, 72), INK, 118, 520, width - 236, 88);
        drawWrapped(g, cleanTitle(spec.title()), font(Font.PLAIN, 38), new Color(92, 73, 80), 118, 740, width - 236, 52);

        paintMirror(g, width - 560, 900, 360, 560);
        drawWrapped(g, "Para quando você se olha pronta e sente: está ok, mas ainda não está marcante.",
                font(Font.BOLD, 42), GREEN, 118, 1180, 650, 58);
        drawPill(g, "menos dúvida", 118, 1570, GREEN, Color.WHITE);
        drawPill(g, "mais intenção", 430, 1570, BRAND, Color.WHITE);
        drawPill(g, "sem compra por impulso", 760, 1570, GOLD.darker(), Color.WHITE);
    }

    /**
     * Desenha infográfico com os pilares práticos do método.
     */
    private static void paintInfographic(Graphics2D g, VisualAssetSpec spec, int width, int height) {
        drawWrapped(g, cleanTitle(spec.title()), font(Font.BOLD, 58), BRAND, 92, 110, width - 184, 70);
        String[] items = {"Espelho real", "Detalhe foco", "Combinação coerente", "Assinatura repetível"};
        Color[] colors = {BRAND, GREEN, GOLD.darker(), new Color(89, 58, 88)};
        for (int i = 0; i < items.length; i++) {
            int x = 110 + i * 305;
            drawCard(g, x, 320, 250, 330, colors[i], items[i], switch (i) {
                case 0 -> "O que apaga sua presença hoje?";
                case 1 -> "Qual ajuste pequeno muda o conjunto?";
                case 2 -> "Roupa, cabelo, pele e acessórios conversam?";
                default -> "O que você consegue repetir sem esforço?";
            });
        }
        drawWrapped(g, "O ganho não vem de comprar mais. Vem de coordenar melhor os sinais que já aparecem em você.",
                font(Font.BOLD, 36), INK, 150, 760, width - 300, 48);
    }

    /**
     * Desenha mapa visual de presença com elementos conectados.
     */
    private static void paintConceptMap(Graphics2D g, VisualAssetSpec spec, int width, int height) {
        drawWrapped(g, cleanTitle(spec.title()), font(Font.BOLD, 56), BRAND, 92, 100, width - 184, 66);
        int centerX = width / 2;
        int centerY = 470;
        drawCircle(g, centerX, centerY, 210, BRAND, "Presença", "intencional");
        String[][] nodes = {
            {"Cabelo", "acabamento"},
            {"Pele", "viço"},
            {"Roupa", "coerência"},
            {"Perfume", "memória"},
            {"Acessórios", "assinatura"},
            {"Ocasião", "contexto"}
        };
        int[][] pos = {{220, 270}, {520, 210}, {900, 230}, {1040, 570}, {650, 690}, {250, 590}};
        for (int i = 0; i < nodes.length; i++) {
            g.setColor(LINE);
            g.setStroke(new BasicStroke(5f));
            g.drawLine(centerX, centerY, pos[i][0] + 95, pos[i][1] + 55);
            drawMiniNode(g, pos[i][0], pos[i][1], nodes[i][0], nodes[i][1]);
        }
    }

    /**
     * Desenha comparação conceitual antes e depois sem prometer transformação automática.
     */
    private static void paintBeforeAfter(Graphics2D g, VisualAssetSpec spec, int width, int height) {
        drawWrapped(g, cleanTitle(spec.title()), font(Font.BOLD, 54), BRAND, 92, 100, width - 184, 66);
        drawPanel(g, 120, 260, 500, 520, "Antes", new String[] {
            "muitas tentativas",
            "compra por impulso",
            "detalhes sem conversa",
            "sensação de quase certo"
        }, new Color(124, 97, 105));
        drawPanel(g, 780, 260, 500, 520, "Depois", new String[] {
            "um foco por vez",
            "reaproveitamento",
            "sinais coordenados",
            "mais segurança ao sair"
        }, GREEN);
        g.setColor(GOLD.darker());
        g.setStroke(new BasicStroke(10f));
        g.drawLine(650, 520, 735, 520);
        g.drawLine(735, 520, 700, 485);
        g.drawLine(735, 520, 700, 555);
    }

    /**
     * Desenha uma representação simples de espelho para a capa.
     */
    private static void paintMirror(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, width, height, 180, 180);
        g.setColor(BRAND);
        g.setStroke(new BasicStroke(10f));
        g.drawRoundRect(x, y, width, height, 180, 180);
        g.setColor(BLUSH);
        g.fillOval(x + 80, y + 90, width - 160, width - 160);
        g.setColor(GREEN);
        g.fillRoundRect(x + 100, y + 360, width - 200, 90, 40, 40);
        g.setColor(GOLD);
        g.fillOval(x + width - 120, y + 360, 46, 46);
    }

    /**
     * Desenha um cartão de pilar do método.
     */
    private static void drawCard(Graphics2D g, int x, int y, int width, int height, Color color, String title, String body) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, width, height, 26, 26);
        g.setColor(color);
        g.setStroke(new BasicStroke(5f));
        g.drawRoundRect(x, y, width, height, 26, 26);
        drawWrapped(g, title, font(Font.BOLD, 32), color, x + 24, y + 58, width - 48, 38);
        drawWrapped(g, body, font(Font.PLAIN, 27), INK, x + 24, y + 160, width - 48, 36);
    }

    /**
     * Desenha o nó central do mapa visual.
     */
    private static void drawCircle(Graphics2D g, int x, int y, int size, Color color, String title, String subtitle) {
        g.setColor(color);
        g.fillOval(x - size / 2, y - size / 2, size, size);
        drawCentered(g, title, font(Font.BOLD, 34), Color.WHITE, x, y - 12);
        drawCentered(g, subtitle, font(Font.PLAIN, 28), Color.WHITE, x, y + 30);
    }

    /**
     * Desenha nó secundário do mapa visual.
     */
    private static void drawMiniNode(Graphics2D g, int x, int y, String title, String subtitle) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, 190, 110, 24, 24);
        g.setColor(GREEN);
        g.setStroke(new BasicStroke(4f));
        g.drawRoundRect(x, y, 190, 110, 24, 24);
        drawCentered(g, title, font(Font.BOLD, 26), GREEN, x + 95, y + 44);
        drawCentered(g, subtitle, font(Font.PLAIN, 22), INK, x + 95, y + 76);
    }

    /**
     * Desenha painel de antes ou depois.
     */
    private static void drawPanel(Graphics2D g, int x, int y, int width, int height, String title, String[] bullets, Color color) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, width, height, 32, 32);
        g.setColor(color);
        g.setStroke(new BasicStroke(6f));
        g.drawRoundRect(x, y, width, height, 32, 32);
        drawCentered(g, title, font(Font.BOLD, 46), color, x + width / 2, y + 92);
        int currentY = y + 180;
        for (String bullet : bullets) {
            g.setColor(color);
            g.fillOval(x + 52, currentY - 22, 22, 22);
            drawWrapped(g, bullet, font(Font.PLAIN, 32), INK, x + 95, currentY, width - 150, 40);
            currentY += 78;
        }
    }

    /**
     * Desenha texto em formato de etiqueta.
     */
    private static void drawPill(Graphics2D g, String text, int x, int y, Color background, Color foreground) {
        g.setFont(font(Font.BOLD, 30));
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(text) + 52;
        g.setColor(background);
        g.fillRoundRect(x, y, width, 62, 31, 31);
        g.setColor(foreground);
        g.drawString(text, x + 26, y + 42);
    }

    /**
     * Centraliza uma linha de texto.
     */
    private static void drawCentered(Graphics2D g, String text, Font font, Color color, int centerX, int baselineY) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
    }

    /**
     * Quebra e desenha texto dentro da largura disponível.
     */
    private static void drawWrapped(Graphics2D g, String text, Font font, Color color, int x, int y, int width, int lineHeight) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        int currentY = y;
        for (String line : wrap(text, metrics, width)) {
            g.drawString(line, x, currentY);
            currentY += lineHeight;
        }
    }

    /**
     * Quebra texto por palavras respeitando a largura de desenho.
     */
    private static List<String> wrap(String text, FontMetrics metrics, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (metrics.stringWidth(candidate) <= width || current.isEmpty()) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    /**
     * Converte a imagem montada para PNG.
     */
    private static byte[] png(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gerar imagem editorial local", ex);
        }
    }

    /**
     * Cria fonte padrão sem depender de arquivo externo.
     */
    private static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    /**
     * Remove termos longos do título visual quando necessário.
     */
    private static String cleanTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Presença elegante";
        }
        return title.replace("Método MUSA - ", "").trim();
    }

    /**
     * Gera nome de arquivo estável e seguro.
     */
    private static String slug(String value) {
        String normalized = Normalizer.normalize(value == null ? "imagem" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String safe = normalized.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return safe.isBlank() ? "imagem" : safe;
    }
}
