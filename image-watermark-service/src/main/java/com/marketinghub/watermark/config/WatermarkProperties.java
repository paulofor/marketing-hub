package com.marketinghub.watermark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermark")
public class WatermarkProperties {

    /** Texto aplicado sobre a imagem. */
    private String text = "PRODUTIVIDADE 360";

    /** Opacidade aplicada no texto da marca d'água (0.0 - 1.0). */
    private double opacity = 0.28;

    /** Fonte utilizada para renderizar a marca d'água. */
    private String fontFamily = "SansSerif";

    /** Prefixo de diretório utilizado ao salvar imagens com marca d'água. */
    private String outputPrefix = "watermarks";

    /** Quantidade máxima de pacotes processados por ciclo. */
    private int batchSize = 5;

    /** Fator multiplicador para o espaçamento entre repetições do texto. */
    private double spacingFactor = 0.85;


    /** Gera uma versão de amostra em miniatura (364x364) em JPEG para visualização. */
    private boolean generateOptimizedCopy = true;

    /** Qualidade JPEG usada na miniatura (0.0 - 1.0). */
    private double optimizedJpegQuality = 0.82;

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

    public double getSpacingFactor() {
        return spacingFactor;
    }

    public void setSpacingFactor(double spacingFactor) {
        this.spacingFactor = spacingFactor;
    }


    public boolean isGenerateOptimizedCopy() {
        return generateOptimizedCopy;
    }

    public void setGenerateOptimizedCopy(boolean generateOptimizedCopy) {
        this.generateOptimizedCopy = generateOptimizedCopy;
    }

    public double getOptimizedJpegQuality() {
        return optimizedJpegQuality;
    }

    public void setOptimizedJpegQuality(double optimizedJpegQuality) {
        this.optimizedJpegQuality = optimizedJpegQuality;
    }

}
