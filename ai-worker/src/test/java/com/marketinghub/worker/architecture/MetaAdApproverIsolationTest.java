package com.marketinghub.worker.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que a aprovação Meta volte a ser executada pelo AI Worker. */
class MetaAdApproverIsolationTest {
  /** Confirma que o AI Worker não contém pacote ou executor do Aprovador Meta. */
  @Test
  void mustNotContainMetaAdReviewExecutor() throws Exception {
    Path root = Path.of("src/main");
    try (var paths = Files.walk(root)) {
      List<Path> forbidden =
          paths
              .filter(Files::isRegularFile)
              .filter(
                  path -> {
                    String normalized = path.toString().toLowerCase();
                    return normalized.contains("creativereview")
                        || normalized.contains("metaadapprover")
                        || normalized.contains("meta-ad-approver");
                  })
              .toList();
      assertThat(forbidden)
          .as("[ARQUITETURA] o Aprovador Meta pertence somente ao módulo independente")
          .isEmpty();
    }
  }
}
