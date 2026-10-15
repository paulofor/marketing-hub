package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Impede que descrições técnicas sejam entregues como roteiro pronto de qualquer produto. */
class AudiovisualBriefContractTest {
  private final ObjectMapper json = new ObjectMapper();
  private static final String BRIEF =
      """
      [ABERTURAS]
      A: O atendimento ficou lindo. E o post? B: Quer mostrar seu trabalho? C: Veja a arte junto da legenda.
      Escolha A: reconhece o momento sem prometer clientes; risco de parecer serviço, resolvido pela prova do kit.
      [FALA_FINAL]
      Anúncio: O atendimento ficou lindo. Mas, na hora de postar, faltou o que escrever?
      Veja este post e a legenda feitos para combinar. Confira a amostra do kit.
      [PROVA_E_LIMITES]
      Arte e legenda de teste da mesma versão; não há prova de clientes, venda ou demanda.
      [DIRECAO_VISUAL]
      TEXTO NA TELA: Veja o kit. AÇÃO VISUAL: mostrar arte e legenda verdadeiras.
      INSTRUÇÕES DE PRODUÇÃO: preservar fontes e sincronizar a fala sem narrar estes rótulos.
      """;

  /** Aceita uma fala separada de direção e preserva o caminho estático existente. */
  @Test
  void acceptsUsableBriefAndNoVideo() {
    assertThatCode(() -> AudiovisualBriefContract.validate(json.getNodeFactory().textNode(BRIEF)))
        .doesNotThrowAnyException();
    assertThatCode(() -> AudiovisualBriefContract.validate(json.nullNode()))
        .doesNotThrowAnyException();
  }

  /** Reproduz a lacuna do briefing descritivo antes de gastar com a montagem. */
  @ParameterizedTest
  @ValueSource(strings = {"Mostrar kit, arquivos e CTA", "Narrar benefícios com cenas bonitas", ""})
  void refusesDescriptionWithoutFinishedSpeech(String brief) {
    assertThatThrownBy(
            () -> AudiovisualBriefContract.validate(json.getNodeFactory().textNode(brief)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Rótulo vazio ou repetido não comprova completude da entrega de Íris. */
  @Test
  void refusesEmptyOrDuplicateSection() {
    String empty =
        BRIEF.replaceAll(
            "(?s)\\[FALA_FINAL\\].*?\\[PROVA_E_LIMITES\\]", "[FALA_FINAL] ... [PROVA_E_LIMITES]");
    assertThatThrownBy(
            () -> AudiovisualBriefContract.validate(json.getNodeFactory().textNode(empty)))
        .hasMessageContaining("vazia");
    assertThatThrownBy(
            () ->
                AudiovisualBriefContract.validate(
                    json.getNodeFactory().textNode(BRIEF + "[FALA_FINAL] Outra fala")))
        .hasMessageContaining("uma seção");
  }
}
