package com.marketinghub.payments.service.kit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Carrega os contratos versionados de kit sem promover candidatas a ofertas comerciais. */
public final class CapellaKitCatalog {
    public static final String NAILS = "nails-v1";
    public static final String BARBER = "barber-v1";
    private static final Logger log = LoggerFactory.getLogger(CapellaKitCatalog.class);
    private static final Map<String, CapellaKitProfile> PROFILES = Map.of(
            NAILS, load(NAILS), BARBER, load(BARBER));

    /** Impede instanciação de um catálogo imutável compartilhado. */
    private CapellaKitCatalog() {}

    /** Resolve somente perfis explicitamente versionados, sem fallback de outra profissão. */
    public static CapellaKitProfile byCode(String code) {
        CapellaKitProfile profile = code == null ? null : PROFILES.get(code);
        if (profile == null) throw new IllegalArgumentException("Perfil de kit não suportado");
        return profile;
    }

    /** Preserva a identidade comprada; barbearia continua candidata sem autorização comercial. */
    public static CapellaKitProfile forPaymentReference(String reference) {
        if (!"agenda-cheia-nail-design".equalsIgnoreCase(reference)) {
            throw new IllegalArgumentException("Pagamento não pertence a uma oferta Capella homologada");
        }
        return byCode(NAILS);
    }

    /** Lê um contrato do classpath e confirma a identidade do arquivo versionado. */
    private static CapellaKitProfile load(String code) {
        try (var input = CapellaKitCatalog.class.getResourceAsStream("/kits/capella/" + code + ".json")) {
            if (input == null) throw new IOException("Contrato de kit ausente");
            CapellaKitProfile profile = new ObjectMapper().readValue(input, CapellaKitProfile.class);
            if (!code.equals(profile.code())) throw new IOException("Identidade do contrato divergente");
            return profile;
        } catch (IOException ex) {
            log.error("Falha ao carregar contrato Capella. profileCode={}", code, ex);
            throw new IllegalStateException("Contrato de kit indisponível", ex);
        }
    }
}
