package com.marketinghub.moisclickbank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Valida o carregamento isolado das credenciais dedicadas da ClickBank. */
class MarketplaceCredentialFileReaderTest {

    /** Garante que o arquivo montado prevaleça sobre valores legados do ambiente. */
    @Test
    void shouldPreferMountedCredentialFile(@TempDir Path directory) throws Exception {
        Path credential = directory.resolve("username");
        Files.writeString(credential, "conta-dedicada\n");

        assertEquals("conta-dedicada", MarketplaceCredentialFileReader.resolve(
                credential.toString(), "usuario-legado", "fallback", "CLICKBANK", "usuario"));
    }

    /** Garante que arquivo configurado, mas ausente, bloqueie o coletor em vez de usar credencial menos segura. */
    @Test
    void shouldFailClosedWhenMountedCredentialFileIsMissing(@TempDir Path directory) {
        assertThrows(IllegalStateException.class, () -> MarketplaceCredentialFileReader.resolve(
                directory.resolve("missing").toString(), "usuario-legado", "fallback", "CLICKBANK", "usuario"));
    }
}
