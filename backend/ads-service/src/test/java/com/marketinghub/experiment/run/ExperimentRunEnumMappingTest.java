package com.marketinghub.experiment.run;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

/** Protege os enums evolutivos dos runs contra conversão automática para ENUM nativo do MySQL. */
class ExperimentRunEnumMappingTest {

  /** Confirma que todo enum persistido do run usa VARCHAR explícito e portável. */
  @Test
  void persistedRunEnumsUseExplicitVarcharMapping() {
    assertPortableEnumMappings(ExperimentRun.class);
    assertPortableEnumMappings(ExperimentRunGateResult.class);
  }

  /** Valida por reflexão o contrato JPA que impede o Hibernate de recriar ENUM nativo. */
  private void assertPortableEnumMappings(Class<?> entityType) {
    Arrays.stream(entityType.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(Enumerated.class))
        .forEach(this::assertPortableEnumMapping);
  }

  /** Verifica o tipo JDBC e a definição SQL explícita de uma propriedade enumerada. */
  private void assertPortableEnumMapping(Field field) {
    JdbcTypeCode jdbcTypeCode = field.getAnnotation(JdbcTypeCode.class);
    Column column = field.getAnnotation(Column.class);

    assertThat(jdbcTypeCode)
        .as("@JdbcTypeCode de %s.%s", field.getDeclaringClass().getSimpleName(), field.getName())
        .isNotNull();
    assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.VARCHAR);
    assertThat(column)
        .as("@Column de %s.%s", field.getDeclaringClass().getSimpleName(), field.getName())
        .isNotNull();
    assertThat(column.columnDefinition()).startsWith("VARCHAR(");
  }
}
