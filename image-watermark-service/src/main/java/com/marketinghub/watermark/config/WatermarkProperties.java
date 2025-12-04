package com.marketinghub.watermark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermark")
public class WatermarkProperties {

    /** Texto aplicado sobre a imagem. */
    private String text = "MARKETING HUB DEMO";

    /** Opacidade aplicada no texto da marca d'água (0.0 - 1.0). */
    private double opacity = 0.18;

    /** Fonte utilizada para renderizar a marca d'água. */
    private String fontFamily = "SansSerif";

    /** Prefixo de diretório utilizado ao salvar imagens com marca d'água. */
    private String outputPrefix = "watermarks";

    /** Quantidade máxima de pacotes processados por ciclo. */
    private int batchSize = 5;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getOutputPrefix() {
        return outputPrefix;
    }

    public void setOutputPrefix(String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
