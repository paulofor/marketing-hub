package com.marketinghub.payments.integration.image;

import java.awt.image.BufferedImage;
import com.marketinghub.payments.service.kit.CapellaKitProfile;
import com.marketinghub.payments.service.kit.CapellaKitCatalog;

/** Fornece fotografias da profissão contratada para a composição dos kits Capella. */
public interface AgendaCheiaPhotoGenerator {
    /** Gera uma fotografia sem texto para a variação comercial solicitada. */
    BufferedImage generate(String executionId, int variant);

    /** Impede que uma integração antiga forneça unhas para uma profissão diferente. */
    default BufferedImage generate(String executionId, int variant, CapellaKitProfile profile) {
        if (!CapellaKitCatalog.NAILS.equals(profile.code())) {
            throw new IllegalStateException("Integração fotográfica não homologada para este perfil");
        }
        return generate(executionId, variant);
    }
}
