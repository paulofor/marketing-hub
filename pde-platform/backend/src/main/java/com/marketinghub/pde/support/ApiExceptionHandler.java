package com.marketinghub.pde.support;

import com.marketinghub.pde.service.PdeOperationalHealthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

/** Converte erros da API PDE em respostas simples para o frontend. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final PdeOperationalHealthService operationalHealthService;

    /** Recebe o serviço que transforma falhas HTTP em alertas pós-deploy. */
    public ApiExceptionHandler(PdeOperationalHealthService operationalHealthService) {
        this.operationalHealthService = operationalHealthService;
    }

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

    /** Responde controles internos e de entitlement com proibição explícita. */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleSecurity(SecurityException ex) {
        log.warn("Operação proibida na API PDE: {}", ex.getMessage(), ex);
        return Map.of("error", ex.getMessage());
    }

    /** Preserva o status HTTP deliberado de controles de acesso e outros contratos explícitos. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        log.warn("Resposta controlada na API PDE: status={}, reason={}", ex.getStatusCode(), ex.getReason(), ex);
        String message = ex.getReason() == null || ex.getReason().isBlank()
                ? "A solicitação não foi autorizada"
                : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", message));
    }

    /** Preserva rota inexistente como 404 sem fabricar alerta técnico de falha operacional. */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoResource(NoResourceFoundException ex) {
        log.info("Rota inexistente na API PDE; resourcePath={}", ex.getResourcePath());
        return Map.of("error", "Rota não encontrada");
    }

    /** Registra falha inesperada para impedir leitura comercial baseada em funil quebrado. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpected(Exception ex, HttpServletRequest request) {
        String endpoint = request == null ? "unknown" : request.getRequestURI();
        String method = request == null ? "unknown" : request.getMethod();
        String clientIp = request == null ? "unknown" : resolveClientIp(request);
        log.error(
                "Falha inesperada na API PDE; method={}, endpoint={}, clientIp={}",
                method,
                endpoint,
                clientIp,
                ex);
        operationalHealthService.recordEndpointFailure(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex);
        return Map.of("error", "Falha técnica na API PDE");
    }

    /** Resolve o IP de origem mais útil para diagnosticar falhas recebidas pelo proxy. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
