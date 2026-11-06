package com.marketinghub.videomanagement.service.provider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;

/**
 * Responsabilidade: sintetizar uma trilha instrumental original, reproduzível e sem API externa.
 */
final class OriginalMusicBed {
  private static final int RATE = 44100;
  private static final double BPM = 94;
  private static final int[][] CHORDS = {
    {48, 55, 59, 64}, {45, 52, 55, 60}, {41, 48, 52, 57}, {43, 50, 55, 59}
  };

  /** Impede instanciação do sintetizador determinístico. */
  private OriginalMusicBed() {}

  /** Escreve PCM mono de duração exata com acordes, arpejo e entrada/saída suaves. */
  static void write(Path target, double seconds) throws IOException {
    if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 180)
      throw new IllegalArgumentException("Duração da trilha fora do contrato.");
    int samples = (int) Math.round(seconds * RATE);
    ByteBuffer wav = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN);
    wav.put(new byte[] {'R', 'I', 'F', 'F'})
        .putInt(36 + samples * 2)
        .put(new byte[] {'W', 'A', 'V', 'E', 'f', 'm', 't', ' '})
        .putInt(16)
        .putShort((short) 1)
        .putShort((short) 1)
        .putInt(RATE)
        .putInt(RATE * 2)
        .putShort((short) 2)
        .putShort((short) 16)
        .put(new byte[] {'d', 'a', 't', 'a'})
        .putInt(samples * 2);
    for (int i = 0; i < samples; i++) {
      double t = i / (double) RATE, beat = t * BPM / 60;
      int[] chord = CHORDS[((int) (beat / 4)) % CHORDS.length];
      double envelope = Math.min(1, t / 1.2) * Math.min(1, (seconds - t) / 2.0);
      double value = 0;
      for (int note : chord) value += .022 * Math.sin(2 * Math.PI * frequency(note) * t);
      double sincePluck = (beat * 2 - Math.floor(beat * 2)) * 30 / BPM;
      int note = chord[((int) (beat * 2)) % chord.length] + 12;
      value +=
          .08
              * Math.sin(2 * Math.PI * frequency(note) * t)
              * (1 - Math.exp(-sincePluck * 90))
              * Math.exp(-sincePluck * 8);
      wav.putShort((short) Math.round(value * envelope * Short.MAX_VALUE));
    }
    Files.write(target, wav.array());
  }

  /** Converte notas MIDI em frequências sem samples, obras ou fonogramas de terceiros. */
  private static double frequency(int note) {
    return 440 * Math.pow(2, (note - 69) / 12.0);
  }
}
