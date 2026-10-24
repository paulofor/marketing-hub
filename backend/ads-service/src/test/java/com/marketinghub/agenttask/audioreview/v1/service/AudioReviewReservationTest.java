package com.marketinghub.agenttask.audioreview.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.Base64;
import javax.sound.sampled.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Confere reserva estimada de PCM real e preserva a proteção dos contratos sem duração confiável.
 */
class AudioReviewReservationTest {
  /** Usa PCM sintético, sem serviço externo nem evidência de clientes. */
  private com.fasterxml.jackson.databind.JsonNode request(int seconds, int textBytes)
      throws Exception {
    var buffer = new ByteArrayOutputStream();
    var format = new AudioFormat(16000, 16, 1, true, false);
    try (var stream =
        new AudioInputStream(
            new ByteArrayInputStream(new byte[seconds * 32000]), format, seconds * 16000)) {
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, buffer);
    }
    var data = new ObjectMapper().createObjectNode();
    var content = data.putArray("messages").addObject().putArray("content");
    content.addObject().put("text", "a".repeat(textBytes));
    content
        .addObject()
        .putObject("input_audio")
        .put("data", Base64.getEncoder().encodeToString(buffer.toByteArray()));
    return data;
  }

  /**
   * Os dois tempos históricos e outra duração mantêm folga frente ao custo observado, sem usar IDs.
   */
  @ParameterizedTest
  @ValueSource(ints = {22, 33, 60})
  void shortAudioRetainsConservativeMargin(int seconds) throws Exception {
    assertThat(AudioReviewReservation.estimate(request(seconds, 3000)))
        .isEqualByComparingTo("0.250000")
        .isGreaterThan(new java.math.BigDecimal("0.020055"));
  }

  /**
   * Contexto e duração aumentam a reserva; não usar a estimativa curta em áudio fora do contrato.
   */
  @Test
  void largerInputAndUnmeasurableAudioRemainProtected() throws Exception {
    assertThat(AudioReviewReservation.estimate(request(120, 50000)))
        .isGreaterThan(new java.math.BigDecimal("0.5"));
    assertThat(AudioReviewReservation.estimate(request(181, 3000)))
        .isEqualTo(AudioReviewContract.RESERVATION);
    assertThat(AudioReviewReservation.estimate(new ObjectMapper().createObjectNode()))
        .isEqualTo(AudioReviewContract.RESERVATION);
  }
}
