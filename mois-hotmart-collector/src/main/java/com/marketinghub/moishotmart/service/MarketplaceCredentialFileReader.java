package com.marketinghub.moishotmart.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsável por carregar credenciais de marketplace a partir de arquivos montados somente para leitura. */
final class MarketplaceCredentialFileReader {
    private static final Logger log = LoggerFactory.getLogger(MarketplaceCredentialFileReader.class);

    private MarketplaceCredentialFileReader() {
    }

    /** Prioriza o arquivo seguro e usa os valores legados apenas quando nenhum arquivo foi configurado. */
    static String resolve(String filePath, String directValue, String fallbackValue, String marketplace, String credentialName) {
        if (filePath != null && !filePath.isBlank()) {
            try {
                String value = Files.readString(Path.of(filePath)).trim();
                if (value.isBlank()) {
                    throw new IllegalStateException("Arquivo de credencial vazio para " + marketplace + ": " + credentialName);
                }
                return value;
            } catch (IOException | RuntimeException ex) {
                log.error("Falha ao carregar credencial dedicada. marketplace={}, credencial={}, arquivo={}",
                        marketplace, credentialName, filePath, ex);
                throw new IllegalStateException("Credencial dedicada indisponível para " + marketplace + ": " + credentialName, ex);
            }
        }
        return firstNonBlank(directValue, fallbackValue);
    }

    /** Retorna o primeiro valor configurado sem transformar ausência em segredo válido. */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
