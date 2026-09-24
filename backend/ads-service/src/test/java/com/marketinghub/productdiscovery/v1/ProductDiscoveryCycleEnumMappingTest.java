package com.marketinghub.productdiscovery.v1;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

/** Protege os estados evolutivos da descoberta contra ENUM físico gerado pelo Hibernate. */
class ProductDiscoveryCycleEnumMappingTest {

  /** Confirma que todos os enums persistidos do ciclo usam VARCHAR explícito e portável. */
  @Test
  void persistedCycleEnumsUseExplicitVarcharMapping() {
    Arrays.stream(ProductDiscoveryCycle.class.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(Enumerated.class))
        .forEach(this::assertPortableEnumMapping);
  }

  /** Verifica o tipo JDBC e a definição SQL explícita de cada enum persistido. */
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
