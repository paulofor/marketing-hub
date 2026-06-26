package com.marketinghub.oprmcoletormei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Valida o escopo de component scan da aplicação do coletor OPRM MEI. */
class OprmColetorMeiApplicationTest {
    /** Confirma que o pacote NichoCNAE v3 entra no contexto Spring da aplicação. */
    @Test
    void shouldScanNichoCnaeVersionThreePackage() {
        SpringBootApplication annotation = OprmColetorMeiApplication.class.getAnnotation(SpringBootApplication.class);

        assertTrue(Arrays.asList(annotation.scanBasePackages()).contains("com.marketinghub.pipelines.nichocnae.v3"));
    }

    /** Confirma que o executor NichoCNAE v2 não é mais carregado operacionalmente pelo coletor. */
    @Test
    void shouldNotScanNichoCnaeVersionTwoPackage() {
        SpringBootApplication annotation = OprmColetorMeiApplication.class.getAnnotation(SpringBootApplication.class);

        assertFalse(Arrays.asList(annotation.scanBasePackages()).contains("com.marketinghub.nichocnae"));
        assertFalse(Arrays.asList(annotation.scanBasePackages()).contains("com.marketinghub.nichocnaev2"));
    }
}
