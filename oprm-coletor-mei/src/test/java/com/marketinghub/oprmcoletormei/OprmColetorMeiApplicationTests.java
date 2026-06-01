package com.marketinghub.oprmcoletormei;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Valida que o contexto Spring do coletor OPRM sobe sem disparar integrações externas durante testes.
 */
@SpringBootTest(properties = "oprm.cnae-enrichment.startup-catch-up.enabled=false")
class OprmColetorMeiApplicationTests {

    /** Confirma o carregamento mínimo do ApplicationContext do coletor OPRM. */
    @Test
    void contextLoads() {
    }
}
