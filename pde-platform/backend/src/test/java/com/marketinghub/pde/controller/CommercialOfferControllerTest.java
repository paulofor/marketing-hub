package com.marketinghub.pde.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.pde.dto.CommercialOfferResponse;
import com.marketinghub.pde.service.CommercialOfferService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o contrato HTTP público que entrega a oferta canônica ao frontend PDE. */
class CommercialOfferControllerTest {

    /** Preserva o nome explícito do produto na rota mesmo quando o bytecode não retém parâmetros. */
    @Test
    void resolvesProductSlugFromPathWithoutCompilerParameterMetadata() throws Exception {
        CommercialOfferService service = mock(CommercialOfferService.class);
        when(service.getOffer("kit-whatsapp-pronto")).thenReturn(offer());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CommercialOfferController(service)).build();

        mvc.perform(get("/api/pde/products/kit-whatsapp-pronto/commercial-offer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productSlug").value("kit-whatsapp-pronto"))
                .andExpect(jsonPath("$.experimentId").value(89))
                .andExpect(jsonPath("$.priceBrl").value(349))
                .andExpect(jsonPath("$.supplierDisplayName").value("Digicom Digital"))
                .andExpect(jsonPath("$.supplierLegalName").doesNotExist())
                .andExpect(jsonPath("$.supplierAddress").doesNotExist());
    }

    /** Monta uma oferta completa de homologação sem depender de integrações externas. */
    private CommercialOfferResponse offer() {
        return new CommercialOfferResponse(
                "kit-whatsapp-pronto",
                "kit-whatsapp-pronto-pde-v2",
                "assisted-service-v2",
                89L,
                "PLANNED",
                "DIRECT_ONE_TO_ONE",
                "Dor real",
                "Prova real",
                "Implantação em até 48 horas",
                "Quero meu atendimento sob medida",
                new BigDecimal("349.00"),
                "https://pay.example/kit",
                "https://kit-whatsapp-pronto.digicomdigital.com.br",
                "Prestadores locais",
                "Implantação personalizada",
                "Serviço assistido",
                "Atendimento pronto",
                "Digicom Digital",
                "00.000.000/0001-00",
                "teste@sandbox.local",
                "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
                "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
                "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy");
    }
}
