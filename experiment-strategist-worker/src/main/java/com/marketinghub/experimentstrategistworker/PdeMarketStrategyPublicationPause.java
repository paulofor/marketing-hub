package com.marketinghub.experimentstrategistworker;

import java.nio.file.Files;
import java.nio.file.Path;

/** Responsabilidade: suspender novas inferências enquanto o publicador troca a imagem de Atena. */
final class PdeMarketStrategyPublicationPause {
  private final Path marker;

  /** Usa o mesmo volume durável do consumidor, preservando a proteção em falha de publicação. */
  PdeMarketStrategyPublicationPause(Path state) {
    marker = state.resolve("publisher-pause.json");
  }

  /** Interrompe novas reservas sem impedir replay do resultado e callback já produzidos. */
  boolean paused() {
    return Files.exists(marker);
  }
}
