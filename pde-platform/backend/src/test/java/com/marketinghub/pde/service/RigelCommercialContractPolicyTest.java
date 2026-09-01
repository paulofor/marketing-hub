package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse.DeliveryContractDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.DeliverySectionDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.MissionDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.ServiceScopeDto;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida o piso comercial aplicado ao contrato produtivo exato do Rigel. */
class RigelCommercialContractPolicyTest {

    /** Substitui os três intervalos antigos pelas quantidades exatas da estratégia congelada. */
    @Test
    void enforcesFrozenCommercialFloor() {
        ProductExperienceResponse protectedProduct =
                RigelCommercialContractPolicy.enforce(rigelProduct(10, 20));

        var sections = protectedProduct.missions().get(0).deliveryContract().sections();
        assertThat(sections)
                .extracting(DeliverySectionDto::id, DeliverySectionDto::minItems, DeliverySectionDto::maxItems)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("responses", 15, 15),
                        org.assertj.core.groups.Tuple.tuple("qualificationQuestions", 8, 8),
                        org.assertj.core.groups.Tuple.tuple("followUps", 4, 4));
        assertThat(protectedProduct.serviceScope().includedItems())
                .contains("15 respostas personalizadas")
                .contains("8 perguntas de qualificação")
                .contains("4 follow-ups manuais");
    }

    /** Falha fechado quando o limite máximo recebido não consegue cumprir o escopo congelado. */
    @Test
    void rejectsContractWhoseMaximumIsBelowFrozenFloor() {
        assertThatThrownBy(() -> RigelCommercialContractPolicy.enforce(rigelProduct(10, 14)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não comporta a quantidade comercial");
    }

    /** Monta o contrato mínimo necessário para exercitar a política sem esconder limites antigos. */
    private ProductExperienceResponse rigelProduct(int responseMinimum, int responseMaximum) {
        var delivery = new DeliveryContractDto(List.of(
                new DeliverySectionDto("responses", "Respostas", responseMinimum, responseMaximum),
                new DeliverySectionDto("qualificationQuestions", "Perguntas", 5, 10),
                new DeliverySectionDto("followUps", "Follow-ups", 3, 5)));
        var mission = new MissionDto(
                "entrega-completa-48h",
                5,
                "Kit completo",
                "Princípio",
                "Ação",
                "Evidência",
                "Dica",
                "OPERATION",
                delivery,
                null);
        return new ProductExperienceResponse(
                "kit-whatsapp-pronto",
                "kit-whatsapp-pronto-pde-v2",
                "assisted-service-v2",
                "pde-assisted-service-v2",
                "Kit WhatsApp Pronto",
                "Promessa",
                "Prestadores locais",
                "R$ 349",
                null,
                null,
                List.of(mission),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                "Continuidade",
                new ServiceScopeDto(
                        List.of("10 a 20 respostas", "5 a 10 perguntas", "3 a 5 follow-ups"),
                        List.of("Automação"),
                        "Após pagamento e briefing"),
                List.of(),
                List.of(),
                null,
                null);
    }
}
