package com.marketinghub.planning;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: proteger o mapeamento persistente dos contratos operacionais do plano. */
class CommercialPlanMappingTest {

  /** Confirma que textos operacionais completos não voltam ao limite de 512 caracteres. */
  @ParameterizedTest
  @ValueSource(strings = {"nextAction", "currentBlocker", "rootCause"})
  void operationalContextUsesLongText(String fieldName) throws NoSuchFieldException {
    Field field = CommercialPlan.class.getDeclaredField(fieldName);
    Column column = field.getAnnotation(Column.class);

    assertThat(column.columnDefinition()).isEqualTo("LONGTEXT");
  }
}
