package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: conferir trilha original reproduzível sem sample, API ou custo de provedor. */
class OriginalMusicBedTest {
  @TempDir Path directory;

  /** Confere os 32 segundos físicos, PCM íntegro, amplitude e identidade determinística. */
  @Test
  void musicIsExactlyThirtyTwoSecondsAndReproducible() throws Exception {
    Path a = directory.resolve("a.wav"), b = directory.resolve("b.wav");
    OriginalMusicBed.write(a, 32);
    OriginalMusicBed.write(b, 32);
    byte[] bytes = Files.readAllBytes(a);
    assertThat(bytes).hasSize(44 + 32 * 44100 * 2).containsExactly(Files.readAllBytes(b));
    try (var stream = AudioSystem.getAudioInputStream(a.toFile())) {
      assertThat(stream.getFormat().getSampleRate()).isEqualTo(44100);
      assertThat(stream.getFrameLength() / stream.getFormat().getFrameRate()).isEqualTo(32);
    }
    var samples = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    samples.position(44);
    int peak = 0;
    while (samples.hasRemaining()) peak = Math.max(peak, Math.abs(samples.getShort()));
    assertThat(peak).isBetween(1000, 16000);
    assertThatThrownBy(() -> OriginalMusicBed.write(a, Double.NaN))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
