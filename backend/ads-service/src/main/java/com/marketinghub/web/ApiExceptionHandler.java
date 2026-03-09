package com.marketinghub.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
        ResponseStatusException exception,
        HttpServletRequest request
    ) {
        return buildResponse(exception.getStatusCode(), exception.getReason(), request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(
        MultipartException exception,
        HttpServletRequest request
    ) {
        Throwable rootCause = exception.getMostSpecificCause();
        String rootMessage = rootCause != null ? rootCause.getMessage() : exception.getMessage();
        if (rootMessage == null) {
            rootMessage = "";
        }
        LOGGER.warn("Falha ao processar upload multipart em {}: {}", request.getRequestURI(), rootMessage);

        String message = "Não foi possível processar o upload do arquivo. Tente novamente.";
        if (rootMessage.contains("Stream ended unexpectedly")) {
            message = "O upload do arquivo foi interrompido antes do envio completo. Verifique sua conexão e tente novamente.";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncRequestNotUsableException(
        AsyncRequestNotUsableException exception,
        HttpServletRequest request
    ) {
        LOGGER.warn(
            "Conexão encerrada durante resposta assíncrona. metodo={}, uri={}, query={}, remoteAddr={}, forwardedFor={}, userAgent={}, asyncStarted={}, reason={}",
            request.getMethod(),
            request.getRequestURI(),
            request.getQueryString(),
            request.getRemoteAddr(),
            request.getHeader("X-Forwarded-For"),
            request.getHeader("User-Agent"),
            request.isAsyncStarted(),
            exception.getMessage()
        );

        Throwable rootCause = exception.getMostSpecificCause();
        if (rootCause instanceof ClientAbortException) {
            LOGGER.debug("Detalhes do abort de cliente para {} {}", request.getMethod(), request.getRequestURI(), rootCause);
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
        HttpStatusCode statusCode,
        String message,
        HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", statusCode.value());
        String error = statusCode instanceof HttpStatus httpStatus
            ? httpStatus.getReasonPhrase()
            : statusCode.toString();
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(statusCode).body(body);
    }
}
