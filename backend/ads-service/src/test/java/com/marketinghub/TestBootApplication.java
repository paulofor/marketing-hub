package com.marketinghub;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Configuração mínima usada pelos testes baseados em {@code @DataJpaTest}.
 *
 * Mantemos um {@link SpringBootApplication} na raiz para que o Spring consiga
 * localizar automaticamente uma {@code @SpringBootConfiguration} sem carregar
 * todo o contexto principal da aplicação.
 */
@SpringBootApplication
public class TestBootApplication {
}
