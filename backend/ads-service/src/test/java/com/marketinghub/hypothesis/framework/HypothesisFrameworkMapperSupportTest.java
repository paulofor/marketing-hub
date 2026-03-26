package com.marketinghub.hypothesis.framework;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HypothesisFrameworkMapperSupportTest {
    private HypothesisFrameworkMapperSupport support;

    @BeforeEach
    void setUp() {
        support = new HypothesisFrameworkMapperSupport(new ObjectMapper());
    }

    @Test
    void shouldResolveFrameworkFromLegacyFields() {
        Hypothesis hypothesis = Hypothesis.builder()
                .title("Kit de Matrículas")
                .problem("Pais não respondem")
                .promise("Lotar agenda em 10 dias")
                .mechanism("Sequência de WhatsApp")
                .uniqueMechanism("Copy personalizada por lead")
                .entrega("Envio de diagnóstico + roteiro")
                .offerType(OfferType.LEAD)
                .price(null)
                .build();

        HypothesisFrameworkDto dto = support.resolve(hypothesis);

        assertThat(dto.getPain().getRoot()).isEqualTo("Pais não respondem");
        assertThat(dto.getResult().getDesiredResult()).isEqualTo("Lotar agenda em 10 dias");
        assertThat(dto.getMechanism().getCore()).isEqualTo("Sequência de WhatsApp");
        assertThat(dto.getMechanism().getUnique()).isEqualTo("Copy personalizada por lead");
        assertThat(dto.getProof().getMessage()).isEqualTo("Envio de diagnóstico + roteiro");
        assertThat(dto.getOffer().getName()).isEqualTo("Kit de Matrículas");
        assertThat(dto.getVersion()).isEqualTo("dor-resultado-mecanismo-prova-oferta/v1");
    }

    @Test
    void shouldStoreSnapshotAndSyncLegacyFields() {
        Hypothesis hypothesis = Hypothesis.builder()
                .title("Oferta antiga")
                .problem("Sem narrativa")
                .offerType(OfferType.LEAD)
                .build();

        HypothesisFrameworkDto update = HypothesisFrameworkDto.builder()
                .pain(HypothesisFrameworkDto.Pain.builder()
                        .surface("Demanda sazonal cair")
                        .root("Sem planejamento de rematrícula")
                        .build())
                .offer(HypothesisFrameworkDto.Offer.builder()
                        .name("Plano de rematrícula antecipada")
                        .corePromise("Transformar previsibilidade em 45 dias")
                        .build())
                .build();

        support.applyPartial(hypothesis, update);

        assertThat(hypothesis.getFrameworkJson()).isNotBlank();
        assertThat(hypothesis.getProblem()).isEqualTo("Sem planejamento de rematrícula");
        assertThat(hypothesis.getTitle()).isEqualTo("Plano de rematrícula antecipada");

        HypothesisFrameworkDto stored = support.resolve(hypothesis);
        assertThat(stored.getPain().getRoot()).isEqualTo("Sem planejamento de rematrícula");
        assertThat(stored.getOffer().getName()).isEqualTo("Plano de rematrícula antecipada");
        assertThat(stored.getOffer().getCorePromise()).isEqualTo("Transformar previsibilidade em 45 dias");
    }
}
