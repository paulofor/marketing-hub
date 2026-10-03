package com.marketinghub.payments.service.kit;

/** Apresenta somente o contexto funcional da profissão correspondente ao pagamento confirmado. */
public record BriefingKitProfile(String code, String profession, String productName, String serviceExamples) {
    /** Retira caminhos e repertório interno do contrato enviado à tela. */
    public static BriefingKitProfile from(CapellaKitProfile profile) {
        return new BriefingKitProfile(profile.code(), profile.profession(), profile.productName(), profile.serviceExamples());
    }
}
