package com.marketinghub.pde.support;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte erros da API PDE em respostas simples para o frontend. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Responde erro de regra de negócio com mensagem objetiva. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Falha de regra na API PDE: {}", ex.getMessage(), ex);
        return Map.of("error", ex.getMessage());
    }

    /** Responde erro de validação de entrada sem expor detalhes internos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Falha de validação na API PDE", ex);
        return Map.of("error", "Entrada inválida para a API PDE");
    }
}
