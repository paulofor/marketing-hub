package com.marketinghub.experiment.funnel;

/**
 * Etapas padronizadas do funil de vendas de um experimento.
 */
public enum ExperimentFunnelStage {
    VISUALIZACAO_ANUNCIO(1, "Visualização do anúncio"),
    ACESSO_FORM_LEAD(2, "Acesso ao formulário de lead"),
    VISUALIZACAO_FORM(3, "Visualização do formulário"),
    ENVIO_FORM(4, "Envio do formulário"),
    ABERTURA_EMAIL_AMOSTRA(5, "Abertura do e-mail de amostra"),
    ACESSO_CHECKOUT(6, "Acesso ao checkout (Mercado Pago)"),
    COMPRA(7, "Compra"),
    ABERTURA_EMAIL_COMPRA(8, "Abertura do e-mail de compra"),
    DOWNLOAD_MATERIAL_PAGO(9, "Download do material pago");

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
