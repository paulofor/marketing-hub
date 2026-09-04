package com.marketinghub.harnesslibraryapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.harnesslibraryapi.api.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Protege toda rota v1 com API key e correlação sem expor credenciais. */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
  public static final String API_KEY_HEADER = "X-API-Key";
  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String REQUEST_ID_ATTRIBUTE = "harnessLibraryRequestId";
  private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
  private static final int MAX_JSON_BODY_BYTES = 32 * 1024;
  private static final Pattern REQUEST_ID_PATTERN =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

  private final ObjectMapper objectMapper;
  private final byte[] expectedApiKey;

  /** Resolve a chave pública obrigatória no bootstrap do gateway. */
  @Autowired
  public ApiKeyAuthenticationFilter(
      ObjectMapper objectMapper, HarnessLibraryProperties properties) {
    this(
        objectMapper,
        SecretValueResolver.resolve(
            "HARNESS_LIBRARY_API_KEY", properties.apiKey(), properties.apiKeyFile()));
  }

  /** Permite testar a autenticação sem arquivo ou variável de ambiente. */
  ApiKeyAuthenticationFilter(ObjectMapper objectMapper, byte[] expectedApiKey) {
    this.objectMapper = objectMapper;
    this.expectedApiKey = expectedApiKey.clone();
  }

  /** Restringe a autenticação às rotas operacionais da API. */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/v1/");
  }

  /** Valida a chave em tempo constante e mantém o request ID durante toda a chamada. */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
    request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);
    MDC.put("requestId", requestId);
    try {
      String supplied = request.getHeader(API_KEY_HEADER);
      if (!StringUtils.hasText(supplied)
          || !MessageDigest.isEqual(expectedApiKey, supplied.getBytes(StandardCharsets.UTF_8))) {
        writeError(
            response,
            requestId,
            HttpServletResponse.SC_UNAUTHORIZED,
            "UNAUTHORIZED",
            "Credencial inválida.");
        return;
      }
      HttpServletRequest authenticatedRequest = authenticatedRequest(request, response, requestId);
      if (authenticatedRequest != null) {
        filterChain.doFilter(authenticatedRequest, response);
      }
    } finally {
      MDC.remove("requestId");
    }
  }

  /** Conserva UUID válido do chamador ou cria uma correlação segura para a resposta. */
  private String requestId(String supplied) {
    return StringUtils.hasText(supplied) && REQUEST_ID_PATTERN.matcher(supplied).matches()
        ? supplied
        : UUID.randomUUID().toString();
  }

  /** Captura mutações autenticadas antes da desserialização e impõe limite físico ao JSON. */
  private HttpServletRequest authenticatedRequest(
      HttpServletRequest request, HttpServletResponse response, String requestId)
      throws IOException {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return request;
    }
    if (request.getContentLengthLong() > MAX_JSON_BODY_BYTES) {
      writePayloadTooLarge(response, requestId);
      return null;
    }
    byte[] body = request.getInputStream().readNBytes(MAX_JSON_BODY_BYTES + 1);
    if (body.length > MAX_JSON_BODY_BYTES) {
      writePayloadTooLarge(response, requestId);
      return null;
    }
    String rawPayload =
        new String(body, StandardCharsets.UTF_8)
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    log.info(
        "Payload bruto recebido requestId={} actor={} operation=ingest method={} path={} payload={}",
        requestId,
        request.getHeader("X-Actor"),
        request.getMethod(),
        request.getRequestURI(),
        rawPayload);
    return new CachedBodyRequest(request, body);
  }

  /** Devolve 413 sem registrar nem tentar interpretar o corpo excedente. */
  private void writePayloadTooLarge(HttpServletResponse response, String requestId)
      throws IOException {
    log.warn("Payload excedeu o limite requestId={} maxBytes={}", requestId, MAX_JSON_BODY_BYTES);
    writeError(
        response,
        requestId,
        HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
        "PAYLOAD_TOO_LARGE",
        "JSON excede o limite de 32 KiB.");
  }

  /** Devolve um erro estável sem revelar detalhes da autenticação ou da implementação. */
  private void writeError(
      HttpServletResponse response, String requestId, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiErrorResponse(Instant.now(), status, code, message, requestId, List.of()));
  }

  /** Reapresenta ao MVC os mesmos bytes que já foram auditados na borda autenticada. */
  private static final class CachedBodyRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    /** Preserva a requisição original e uma cópia defensiva do corpo limitado. */
    private CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body.clone();
    }

    /** Cria um novo fluxo para que o conversor JSON leia o corpo integral. */
    @Override
    public ServletInputStream getInputStream() {
      return new CachedBodyServletInputStream(body);
    }

    /** Oferece a mesma repetição para consumidores baseados em Reader. */
    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /** Informa o tamanho real do corpo preservado. */
    @Override
    public int getContentLength() {
      return body.length;
    }

    /** Informa o tamanho real do corpo preservado sem limite de inteiro do contrato Servlet. */
    @Override
    public long getContentLengthLong() {
      return body.length;
    }
  }

  /** Implementa a leitura síncrona dos bytes preservados para o pipeline Servlet. */
  private static final class CachedBodyServletInputStream extends ServletInputStream {
    private final ByteArrayInputStream input;

    /** Inicializa o fluxo isolado que será consumido uma única vez pelo conversor JSON. */
    private CachedBodyServletInputStream(byte[] body) {
      this.input = new ByteArrayInputStream(body);
    }

    /** Lê o próximo byte do corpo preservado. */
    @Override
    public int read() {
      return input.read();
    }

    /** Informa quando todos os bytes já foram consumidos. */
    @Override
    public boolean isFinished() {
      return input.available() == 0;
    }

    /** A leitura em memória está sempre pronta para o consumidor. */
    @Override
    public boolean isReady() {
      return true;
    }

    /** Notifica leitores assíncronos sem bloquear o fluxo síncrono usado pelo MVC. */
    @Override
    public void setReadListener(ReadListener readListener) {
      try {
        if (!isFinished()) {
          readListener.onDataAvailable();
        }
        if (isFinished()) {
          readListener.onAllDataRead();
        }
      } catch (IOException ex) {
        log.error(
            "Falha ao notificar leitura do payload errorLine={} errorClass={} errorMessage={}",
            ex.getStackTrace().length == 0 ? -1 : ex.getStackTrace()[0].getLineNumber(),
            ex.getClass().getName(),
            ex.getMessage(),
            ex);
        readListener.onError(ex);
      }
    }
  }
}
