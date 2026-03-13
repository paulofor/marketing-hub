package com.marketinghub.emailservice.leadportal.email;

/**
 * Tokens disponíveis para personalizar o HTML enviado ao lead.
 */
public enum LeadPortalEmailTemplatePlaceholder {
    LEAD_NAME("nome_cliente", "Nome do cliente", "Substituído automaticamente pelo nome informado pelo lead no formulário."),
    PAYMENT_LINK("link_pagamento", "Link para pagamento", "URL gerada no Mercado Pago para o checkout deste lead."),
    PREVIEW_IMAGE_1("imagem_previa_1", "Imagem prévia 1", "Primeira imagem com marca d'água disponível para visualização online."),
    PREVIEW_IMAGE_2("imagem_previa_2", "Imagem prévia 2", "Segunda URL de imagem com marca d'água quando disponível."),
    PREVIEW_IMAGE_3("imagem_previa_3", "Imagem prévia 3", "Terceira URL de imagem com marca d'água quando houver conteúdo suficiente.");

    private final String key;
    private final String label;
    private final String description;

    LeadPortalEmailTemplatePlaceholder(String key, String label, String description) {
        this.key = key;
        this.label = label;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String token() {
        return "{{" + key + "}}";
    }
}
