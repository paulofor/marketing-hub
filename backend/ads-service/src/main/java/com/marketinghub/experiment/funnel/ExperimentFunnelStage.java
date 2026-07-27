package com.marketinghub.experiment.funnel;

/**
 * Etapas padronizadas do funil de vendas de um experimento.
 */
public enum ExperimentFunnelStage {
    VISUALIZACAO_ANUNCIO(1, "Visualização do anúncio"),
    ACESSO_FORM_LEAD(2, "Acesso ao formulário de lead"),
    VISUALIZACAO_FORM(3, "Visualização do formulário"),
    VIDEO_VISTO_PARCIAL(4, "Vídeo visto parcial"),
    VIDEO_VISTO_COMPLETO(5, "Vídeo visto completo"),
    ENVIO_FORM(6, "Envio do formulário"),
    ABERTURA_EMAIL_AMOSTRA(7, "Abertura do e-mail de amostra"),
    ACESSO_CHECKOUT(8, "Acesso ao checkout (Mercado Pago)"),
    COMPRA(9, "Compra"),
    ABERTURA_EMAIL_COMPRA(10, "Abertura do e-mail de compra"),
    DOWNLOAD_MATERIAL_PAGO(11, "Download do material pago");

    private final int order;
    private final String label;

    ExperimentFunnelStage(int order, String label) {
        this.order = order;
        this.label = label;
    }

    public int getOrder() {
        return order;
    }

    public String getLabel() {
        return label;
    }
}
