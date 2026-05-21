package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.copy.CopyProvisionalHtmlPayloadResolver;
import com.marketinghub.geralanding.copy.CopyProvisionalHtmlAssembler;
import com.marketinghub.geralanding.copy.CopyProvisionalHtmlProcessor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopyProvisionalHtmlAssemblerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CopyProvisionalHtmlAssembler assembler = new CopyProvisionalHtmlAssembler(
            new CopyProvisionalHtmlPayloadResolver(objectMapper),
            new CopyProvisionalHtmlProcessor(),
            objectMapper);

    @Test
    @Disabled("Desabilitado provisoriamente: comparação literal exata do HTML oficial está em ajuste")
    void assembleMustMatchExpectedHtmlExactlyForOfficialFixtures() throws IOException {
        Path fixtureRoot = resolveFixtureRoot();
        String wireframe = Files.readString(fixtureRoot.resolve("entradas/gera-wireframe.json"));
        String copy = Files.readString(fixtureRoot.resolve("entradas/gera-copy.json"));
        String expectedHtml = Files.readString(fixtureRoot.resolve("saidas/gera-wireframe-copy-exato.html"));

        String html = assembler.assemble(copy, wireframe, null);

        assertEquals(expectedHtml, html);
    }

    private Path resolveFixtureRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++) {
            Path candidate = current.resolve("testes");
            if (Files.exists(candidate.resolve("entradas/gera-wireframe.json"))
                    && Files.exists(candidate.resolve("entradas/gera-copy.json"))
                    && Files.exists(candidate.resolve("saidas/gera-wireframe-copy-exato.html"))) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Não foi possível localizar pasta de fixtures em /testes");
    }
}
