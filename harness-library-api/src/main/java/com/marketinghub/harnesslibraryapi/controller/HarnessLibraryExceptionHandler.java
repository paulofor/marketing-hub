package com.marketinghub.harnesslibraryapi.controller;

import com.marketinghub.harnesslibraryapi.api.ApiErrorResponse;
import com.marketinghub.harnesslibraryapi.client.BackendApiException;
import com.marketinghub.harnesslibraryapi.config.ApiKeyAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz falhas de contrato e integração em respostas públicas estáveis e sanitizadas. */
@RestControllerAdvice
public class HarnessLibraryExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(HarnessLibraryExceptionHandler.class);

  /** Expõe erros de validação de campos sem retornar stack trace ou objeto interno. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleBodyValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .sorted()
            .toList();
    return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "JSON inválido.", details, request);
  }

  /** Expõe erros de cabeçalho, filtro ou variável de caminho. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintValidation(
      ConstraintViolationException ex, HttpServletRequest request) {
    List<String> details =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .distinct()
            .sorted()
            .toList();
    return response(
        HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Parâmetro inválido.", details, request);
  }

  /** Informa JSON ilegível ou campo desconhecido sem ecoar o payload. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableJson(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        "INVALID_JSON",
        "JSON ilegível ou com campo desconhecido.",
        List.of(),
        request);
  }

  /** Preserva o status funcional já sanitizado pelo cliente do backend. */
  @ExceptionHandler(BackendApiException.class)
  public ResponseEntity<ApiErrorResponse> handleBackendFailure(
      BackendApiException ex, HttpServletRequest request) {
    return response(ex.getPublicStatus(), "BACKEND_REJECTED", ex.getMessage(), List.of(), request);
  }

  /** Registra a exceção completa e devolve um erro neutro para falhas não previstas. */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(
      RuntimeException ex, HttpServletRequest request) {
    log.error(
        "Falha não tratada na API da Biblioteca requestId={} method={} path={}",
        requestId(request),
        request.getMethod(),
        request.getRequestURI(),
        ex);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Falha interna ao processar a operação.",
        List.of(),
        request);
  }

  /** Monta o envelope consistente para qualquer falha pública. */
  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status,
      String code,
      String message,
      List<String> details,
      HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(
            new ApiErrorResponse(
                Instant.now(), status.value(), code, message, requestId(request), details));
  }

  /** Lê a correlação criada pelo filtro sem depender de cabeçalho fornecido pelo cliente. */
  private String requestId(HttpServletRequest request) {
    Object value = request.getAttribute(ApiKeyAuthenticationFilter.REQUEST_ID_ATTRIBUTE);
    return value == null ? "unavailable" : value.toString();
  }
}
