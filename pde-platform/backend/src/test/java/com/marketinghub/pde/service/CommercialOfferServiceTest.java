package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Valida a integração da superfície PDE com a oferta canônica do Marketing Hub. */
class CommercialOfferServiceTest {

    /** Deve preservar experimento, preço e checkout recebidos do backend principal. */
    @Test
    void returnsCanonicalMarketingHubOffer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CommercialOfferService service = new CommercialOfferService(builder, "http://marketing-hub");
        server.expect(requestTo(
                        "http://marketing-hub/api/products/public/kit-whatsapp-pronto/commercial-offer"))
                .andRespond(withSuccess(
                        """
                        {
                          "productSlug": "kit-whatsapp-pronto",
                          "experienceVersion": "kit-whatsapp-pronto-pde-v2",
                          "layoutKey": "assisted-service-v2",
                          "experimentId": 89,
                          "experimentStatus": "PLANNED",
                          "acquisitionChannel": "DIRECT_ONE_TO_ONE",
                          "pain": "Conversas improvisadas terminam sem próximo passo",
                          "proof": "Demonstração personalizada do método",
                          "promise": "Implantação personalizada em até 48 horas",
                          "primaryCta": "Quero meu atendimento sob medida",
                          "priceBrl": 349,
                          "checkoutUrl": "https://pay.example/kit",
                          "salesPageUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br",
                          "targetAudience": "Pequenos prestadores",
                          "productFormat": "IMPLANTACAO_PERSONALIZADA",
                          "deliveryMode": "ASSISTIDA_MANUAL",
                          "valueUnit": "Atendimento pronto para usar",
                          "supplierLegalName": "Fornecedor de Homologação Ltda.",
                          "supplierRegistrationNumber": "00.000.000/0001-00",
                          "supplierAddress": "Endereço de homologação, 100",
                          "supportEmail": "teste@sandbox.local",
                          "termsUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
                          "privacyUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
                          "refundPolicyUrl": "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var offer = service.getOffer("kit-whatsapp-pronto");

        assertThat(offer.experimentId()).isEqualTo(89L);
        assertThat(offer.experienceVersion()).isEqualTo("kit-whatsapp-pronto-pde-v2");
        assertThat(offer.priceBrl()).isEqualByComparingTo(new BigDecimal("349"));
        assertThat(offer.checkoutUrl()).isEqualTo("https://pay.example/kit");
        assertThat(offer.supplierRegistrationNumber()).isEqualTo("00.000.000/0001-00");
        server.verify();
    }
}
