package com.marketinghub.tiktokadsworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Valida as regras básicas de cadastro e diagnóstico do módulo TikTok Ads. */
class TiktokAccountServiceTest {

    /** Garante persistência de conta e mascaramento de credenciais na resposta. */
    @Test
    void shouldPersistAccountAndReturnMaskedSecrets() throws Exception {
        Path storage = Files.createTempFile("tiktok-accounts", ".json");
        Files.deleteIfExists(storage);
        TiktokAccountService service = new TiktokAccountService(new TiktokAccountRepository(new ObjectMapper(), storage.toString()));

        TiktokAccountResponse response = service.createAccount(new TiktokAccountRequest(
                "Conta teste",
                "123456",
                "token-secreto-123456",
                "app-1",
                "client-key",
                "secret-123",
                true,
                false));

        assertEquals(1L, response.id());
        assertTrue(response.hasAccessToken());
        assertTrue(response.hasAppSecret());
        assertEquals("toke...3456", response.maskedAccessToken());
    }

    /** Garante que publicação automática fica bloqueada sem gate comercial completo. */
    @Test
    void shouldBlockPublicationUntilCommercialGateExists() throws Exception {
        Path storage = Files.createTempFile("tiktok-accounts", ".json");
        Files.deleteIfExists(storage);
        TiktokAccountService service = new TiktokAccountService(new TiktokAccountRepository(new ObjectMapper(), storage.toString()));
        TiktokAccountResponse account = service.createAccount(new TiktokAccountRequest(
                "Conta teste",
                "123456",
                "token-secreto-123456",
                "app-1",
                "client-key",
                "secret-123",
                true,
                true));

        TiktokDiagnosticResponse diagnostic = service.diagnoseAccount(account.id());

        assertEquals("BLOCKED", diagnostic.status());
        assertFalse(diagnostic.blockers().isEmpty());
        assertTrue(diagnostic.blockers().stream().anyMatch(blocker -> blocker.contains("Publicação automática")));
    }
}
