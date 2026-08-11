package com.marketinghub.agentmemory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a capacidade textual da memória premium e seu changelog MySQL. */
class PremiumAgentMemoryLongtextChangelogTest {

  /** Confirma que conteúdo e evidências extensas usam LONGTEXT no contrato JPA e Liquibase. */
  @Test
  void shouldPersistLongAgentLearningWithoutTruncation() throws Exception {
    assertLongtext(PremiumAgentMemory.class, "content");
    assertLongtext(PremiumAgentMemory.class, "evidence");
    assertLongtext(PremiumAgentMemoryFeedback.class, "evidence");

    String changelog =
        Files.readString(
            Path.of(
                "src/main/resources/db/changelog/changesets/2026-08-11-premium-agent-memory-longtext.yaml"));
    assertThat(changelog)
        .contains("MODIFY COLUMN content_text LONGTEXT NOT NULL")
        .contains("MODIFY COLUMN evidence_text LONGTEXT NOT NULL");

    String master =
        Files.readString(Path.of("src/main/resources/db/changelog/db.changelog-master.yaml"));
    assertThat(master)
        .contains(
            "file: changesets/2026-08-11-premium-agent-memory-longtext.yaml\n"
                + "      relativeToChangelogFile: true");
  }

  /**
   * Valida a definição explícita da coluna para impedir divergência entre Hibernate e Liquibase.
   */
  private void assertLongtext(Class<?> entityType, String fieldName) throws Exception {
    Column column = entityType.getDeclaredField(fieldName).getAnnotation(Column.class);
    assertThat(column.columnDefinition()).isEqualTo("LONGTEXT");
  }
}
