package com.marketinghub.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: validar a tradução global de exceções HTTP para respostas padronizadas.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    /**
     * Verifica que uma ResponseStatusException preserva o status e a mensagem definidos pela regra de negócio.
     */
    @Test
    void shouldConvertResponseStatusExceptionToBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/creatives");
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontrado");

        var response = handler.handleResponseStatusException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
            .containsEntry("status", HttpStatus.NOT_FOUND.value())
            .containsEntry("message", "Não encontrado")
            .containsEntry("path", "/api/creatives");
    }

    /**
     * Verifica que upload multipart interrompido retorna orientação segura para nova tentativa.
     */
    @Test
    void shouldReturnBadRequestWhenMultipartStreamEndsUnexpectedly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/assets");
        MultipartException exception = new MultipartException(
            "Falha ao parsear request",
            new RuntimeException("Stream ended unexpectedly")
        );

        var response = handler.handleMultipartException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
            .containsEntry("status", HttpStatus.BAD_REQUEST.value())
            .containsEntry(
                "message",
                "O upload do arquivo foi interrompido antes do envio completo. Verifique sua conexão e tente novamente."
            )
            .containsEntry("path", "/api/assets");
    }

    /**
     * Verifica que falhas de validação são tratadas como erro de requisição, não como falha interna.
     */
    @Test
    @DisplayName("Deve converter validação inválida em HTTP 400")
    void shouldReturnBadRequestWhenValidationFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/angles");
        BindException exception = new BindException(new Object(), "request");

        var response = handler.handleValidationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
            .containsEntry("status", HttpStatus.BAD_REQUEST.value())
            .containsEntry("message", "Requisição inválida.")
            .containsEntry("path", "/api/angles");
    }

    /**
     * Verifica que erro inesperado continua retornando HTTP 500 com requestId rastreável.
     */
    @Test
    void shouldReturnRequestIdWhenUnexpectedExceptionBecomesInternalServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/creatives/10/reject");
        request.addHeader("X-Request-Id", "req-500-creative");

        var response = handler.handleUnexpectedException(new RuntimeException("Falha inesperada"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
            .containsEntry("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
            .containsEntry("message", "Erro interno ao processar a solicitação.")
            .containsEntry("path", "/api/creatives/10/reject")
            .containsEntry("requestId", "req-500-creative");
    }
}
