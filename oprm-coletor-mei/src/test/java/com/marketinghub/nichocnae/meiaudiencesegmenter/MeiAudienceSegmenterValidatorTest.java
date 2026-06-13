package com.marketinghub.nichocnae.meiaudiencesegmenter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeiAudienceSegmenterValidatorTest {
    private final MeiAudienceSegmenterValidator validator = new MeiAudienceSegmenterValidator();

    @Test
    void deveAceitarSegmentacaoComportamentalSemLinguagemDeSolucao() {
        MeiAudienceSegmentDraft draft = validDraft("Profissionais autônomos com agenda instável");

        assertThatCode(() -> validator.validate(validPending(), draft)).doesNotThrowAnyException();
    }

    @Test
    void deveBloquearSegmentacaoComProdutoOuOferta() {
        MeiAudienceSegmentDraft draft = validDraft("Profissionais buscando produto para vender");

        assertThatThrownBy(() -> validator.validate(validPending(), draft))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linguagem de solução");
    }

    private MeiAudienceSegmenterPending validPending() {
        return new MeiAudienceSegmenterPending(
                10L,
                20L,
                30L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "serviços de beleza",
                "serviços de beleza",
                null,
                null,
                "rotina com atendimento por agenda e deslocamento",
                "clientes chegam por indicação e retorno",
                "WhatsApp e Instagram",
                "dor prática com cancelamentos e retrabalho",
                "insegurança para cobrar",
                "agenda cheia e estabilidade",
                "medo de perder cliente",
                "linguagem de encaixe e indicação",
                "dor prática com cancelamentos e retrabalho",
                "resultado desejado de estabilidade",
                "evidências em fontes brasileiras",
                "exemplo.com.br",
                80,
                70,
                60,
                0,
                Instant.now(),
                List.of(),
                List.of());
    }

    private MeiAudienceSegmentDraft validDraft(String audienceName) {
        return new MeiAudienceSegmentDraft(
                audienceName,
                "termos ocupacionais usados nas fontes",
                "trabalha por conta própria, atende por indicação e organiza agenda manualmente",
                "consegue clientes por indicação, redes locais e retorno de clientes antigos",
                "rotina diária com atendimento, compras, orçamento e remarcações",
                "tarefas recorrentes de agenda, atendimento, cobrança e reposição de material",
                "dor prática com cancelamento, atraso, retrabalho e fluxo irregular",
                "dor emocional com insegurança de renda e reputação local",
                "sonha com estabilidade, reconhecimento e agenda previsível",
                "teme perder clientes, receber calote e ficar sem movimento",
                "frases observadas sobre agenda, cliente sumido e indicação",
                "usa WhatsApp, indicação, redes sociais locais e agenda própria",
                "fontes brasileiras recentes com evidências curtas",
                85,
                75,
                80,
                10,
                5,
                0);
    }
}
