package com.marketinghub.oprm.web;

import com.marketinghub.oprm.dto.OprmApiErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackageClasses = {
        OprmJobController.class,
        OprmArtifactController.class,
        OprmWorkspaceController.class,
        OprmFeedbackController.class,
        OprmHeartbeatController.class,
        com.marketinghub.oprm.niche.web.OprmNicheIngestionController.class,
        com.marketinghub.oprm.niche.web.OprmNicheCatalogIngestionController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OprmApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<OprmApiErrorResponseDto> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String errorCode = status == HttpStatus.CONFLICT ? "OPRM_CONFLICT" : "OPRM_REQUEST_ERROR";
        return buildResponse(exception.getStatusCode().value(), errorCode, exception.getReason(), request, null);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<OprmApiErrorResponseDto> handleValidation(
            Exception exception,
            HttpServletRequest request
    ) {
        String details = switch (exception) {
            case MethodArgumentNotValidException ex -> ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.joining("; "));
            case HandlerMethodValidationException ex -> ex.getAllValidationResults()
                    .stream()
                    .flatMap(result -> result.getResolvableErrors().stream())
                    .map(error -> error.getDefaultMessage() == null ? "validation error" : error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            default -> exception.getMessage();
        };
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "OPRM_VALIDATION_ERROR",
                "payload inválido para o endpoint OPRM",
                request,
                details
        );
    }

    private ResponseEntity<OprmApiErrorResponseDto> buildResponse(
            int statusCode,
            String code,
            String message,
            HttpServletRequest request,
            String details
    ) {
        String correlationId = request.getHeader("X-Correlation-Id");
        OprmApiErrorResponseDto response = new OprmApiErrorResponseDto(
                code,
                message == null ? "erro na requisição OPRM" : message,
                correlationId,
                details
        );
        return ResponseEntity.status(statusCode).body(response);
    }

    private String formatFieldError(FieldError error) {
        String defaultMessage = error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
        return error.getField() + ": " + defaultMessage;
    }
}
