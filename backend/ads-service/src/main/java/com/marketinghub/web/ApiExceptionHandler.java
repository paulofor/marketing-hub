package com.marketinghub.web;

import com.marketinghub.salesvideo.exception.VideoModuleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: converter exceções HTTP globais em respostas consistentes e logs rastreáveis.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

  /** Converte falhas conhecidas do módulo de vídeo em resposta HTTP padronizada. */
  @ExceptionHandler(VideoModuleException.class)
  public ResponseEntity<Map<String, Object>> handleVideoModuleException(
      VideoModuleException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getStatus(), exception.getMessage(), request, exception.getErrorCode().name());
  }

  /** Converte ResponseStatusException em resposta HTTP preservando método e endpoint no log. */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatusException(
      ResponseStatusException exception, HttpServletRequest request) {
    LOGGER.warn(
        "ResponseStatusException tratado. status={}, method={}, uri={}, query={}, reason={}",
        exception.getStatusCode().value(),
        request.getMethod(),
        request.getRequestURI(),
        request.getQueryString(),
        exception.getReason());
    return buildResponse(exception.getStatusCode(), exception.getReason(), request, null);
  }

  /** Converte falhas de upload multipart em mensagem segura para o usuário. */
  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<Map<String, Object>> handleMultipartException(
      MultipartException exception, HttpServletRequest request) {
    Throwable rootCause = getMostSpecificCause(exception);
    String rootMessage = rootCause != null ? rootCause.getMessage() : exception.getMessage();
    if (rootMessage == null) {
      rootMessage = "";
    }
    LOGGER.warn(
        "Falha ao processar upload multipart em {}: {}", request.getRequestURI(), rootMessage);

    String message = "Não foi possível processar o upload do arquivo. Tente novamente.";
    if (rootMessage.contains("Stream ended unexpectedly")) {
      message =
          "O upload do arquivo foi interrompido antes do envio completo. Verifique sua conexão e tente novamente.";
    }
    return buildResponse(HttpStatus.BAD_REQUEST, message, request, null);
  }

  /** Converte falhas de validação de entrada em HTTP 400 sem registrar como erro interno. */
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HandlerMethodValidationException.class,
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    BindException.class
  })
  public ResponseEntity<Map<String, Object>> handleValidationException(
      Exception exception, HttpServletRequest request) {
    LOGGER.warn(
        "Requisição inválida rejeitada. method={}, uri={}, query={}, reason={}",
        request.getMethod(),
        request.getRequestURI(),
        request.getQueryString(),
        exception.getMessage());
    return buildResponse(HttpStatus.BAD_REQUEST, "Requisição inválida.", request, null);
  }

  /** Trata abortos de conexão assíncrona sem transformar desconexão do cliente em erro técnico. */
  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public ResponseEntity<Void> handleAsyncRequestNotUsableException(
      AsyncRequestNotUsableException exception, HttpServletRequest request) {
    LOGGER.warn(
        "Conexão encerrada durante resposta assíncrona. metodo={}, uri={}, query={}, remoteAddr={}, forwardedFor={}, userAgent={}, asyncStarted={}, reason={}",
        request.getMethod(),
        request.getRequestURI(),
        request.getQueryString(),
        request.getRemoteAddr(),
        request.getHeader("X-Forwarded-For"),
        request.getHeader("User-Agent"),
        request.isAsyncStarted(),
        exception.getMessage());

    Throwable rootCause = getMostSpecificCause(exception);
    if (rootCause instanceof ClientAbortException) {
      LOGGER.debug(
          "Detalhes do abort de cliente para {} {}",
          request.getMethod(),
          request.getRequestURI(),
          rootCause);
    }

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /** Converte exceções não tratadas em HTTP 500 com requestId para rastreio pelo MCP. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    String requestId = resolveRequestId(request);
    LOGGER.error(
        "Erro HTTP 500 não tratado. requestId={} status=500 method={} endpoint={} uri={} query={} remoteAddr={} forwardedFor={} userAgent={}",
        requestId,
        request.getMethod(),
        request.getRequestURI(),
        request.getRequestURI(),
        request.getQueryString(),
        request.getRemoteAddr(),
        request.getHeader("X-Forwarded-For"),
        request.getHeader("User-Agent"),
        exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Erro interno ao processar a solicitação.",
        request,
        null,
        requestId);
  }

  /** Monta a resposta HTTP padronizada sem requestId explícito. */
  private ResponseEntity<Map<String, Object>> buildResponse(
      HttpStatusCode statusCode, String message, HttpServletRequest request, String errorCode) {
    return buildResponse(statusCode, message, request, errorCode, null);
  }

  /** Monta a resposta HTTP padronizada incluindo requestId quando disponível. */
  private ResponseEntity<Map<String, Object>> buildResponse(
      HttpStatusCode statusCode,
      String message,
      HttpServletRequest request,
      String errorCode,
      String requestId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", OffsetDateTime.now());
    body.put("status", statusCode.value());
    String error =
        statusCode instanceof HttpStatus httpStatus
            ? httpStatus.getReasonPhrase()
            : statusCode.toString();
    body.put("error", error);
    body.put("message", message);
    body.put("path", request.getRequestURI());
    if (errorCode != null) {
      body.put("errorCode", errorCode);
    }
    if (requestId != null) {
      body.put("requestId", requestId);
    }
    return ResponseEntity.status(statusCode).body(body);
  }

  /** Localiza a causa mais específica de uma exceção encadeada. */
  private Throwable getMostSpecificCause(Throwable throwable) {
    Throwable root = throwable;
    while (root != null && root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root;
  }

  /** Resolve ou cria o identificador usado para rastrear a requisição nos logs. */
  private String resolveRequestId(HttpServletRequest request) {
    String requestId = request.getHeader("X-Request-Id");
    if (requestId == null || requestId.isBlank()) {
      requestId = request.getHeader("X-Correlation-Id");
    }
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    return requestId;
  }
}
