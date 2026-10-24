package com.marketinghub.agenttask.audioreview.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import lombok.extern.slf4j.Slf4j;

/** Estima reserva conservadora pela entrada PCM real, sem confundir reserva com cobrança. */
@Slf4j
final class AudioReviewReservation {
  /** Impede instâncias de um cálculo sem estado. */
  private AudioReviewReservation() {}

  /** Usa bytes e duração verificáveis; contratos legados ou ilegíveis mantêm a reserva integral. */
  static BigDecimal estimate(JsonNode request) {
    try {
      byte[] bytes =
          Base64.getDecoder().decode(request.at("/messages/0/content/1/input_audio/data").asText());
      try (var audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))) {
        var format = audio.getFormat();
        long frames = audio.getFrameLength();
        if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
            || format.getChannels() != 1
            || format.getSampleSizeInBits() != 16
            || format.getFrameSize() != 2
            || format.getFrameRate() <= 0
            || frames <= 0
            || frames * 2 != audio.readAllBytes().length) return AudioReviewContract.RESERVATION;
        double seconds = frames / (double) format.getFrameRate();
        if (!Double.isFinite(seconds) || seconds > 180) return AudioReviewContract.RESERVATION;
        // Hipótese conservadora local: 100 tokens/s, dez vezes o observado nos recibos curtos.
        long audioTokens = (long) Math.ceil(seconds * 100) + 256;
        long textTokens =
            request
                    .at("/messages/0/content/0/text")
                    .asText()
                    .getBytes(StandardCharsets.UTF_8)
                    .length
                + 1024L;
        return BigDecimal.valueOf(audioTokens)
            .multiply(new BigDecimal("0.000032"))
            .add(BigDecimal.valueOf(textTokens).multiply(new BigDecimal("0.0000025")))
            .add(new BigDecimal("0.020000"))
            .max(new BigDecimal("0.250000"))
            .min(AudioReviewContract.RESERVATION)
            .setScale(6, RoundingMode.CEILING);
      }
    } catch (Exception ex) {
      log.warn(
          "Não foi possível medir PCM da reserva sonora; mantendo reserva integral sem expor áudio",
          ex);
      return AudioReviewContract.RESERVATION;
    }
  }
}
