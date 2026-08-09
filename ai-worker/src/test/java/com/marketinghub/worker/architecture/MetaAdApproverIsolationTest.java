package com.marketinghub.worker.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que a aprovação Meta volte a ser executada pelo AI Worker. */
class MetaAdApproverIsolationTest {
  /** Confirma que o AI Worker mantém somente a materialização das correções visuais. */
  @Test
  void mustNotContainMetaAdReviewExecutor() throws Exception {
    Path root = Path.of("src/main/java/com/marketinghub/worker/creativereview");
    try (var files = Files.list(root)) {
      assertThat(files.map(path -> path.getFileName().toString()).toList())
          .as("[ARQUITETURA] a análise do Aprovador Meta pertence ao módulo independente")
          .noneMatch(name -> name.startsWith("CreativeReview"));
    }
  }
}
