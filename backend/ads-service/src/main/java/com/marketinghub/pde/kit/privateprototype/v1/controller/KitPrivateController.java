package com.marketinghub.pde.kit.privateprototype.v1.controller;

import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrivateService;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: expor a experiência privada e os callbacks do compositor pelo backend
 * principal.
 */
@RestController
@RequestMapping("/api/pde/kit/private/v1")
@Slf4j
public class KitPrivateController {
  private final KitPrivateService service;
  private final String pdeToken;
  private final String compositorToken;

  /** Recebe as credenciais existentes, separando o compositor do emissor de acessos de QA. */
  public KitPrivateController(
      KitPrivateService service,
      @Value("${integrations.pde-platform.internal-token:${PDE_INTERNAL_API_TOKEN:}}")
          String pdeToken,
      @Value("${integrations.lead-portal-payments.auth-token:${LEAD_PORTAL_PAYMENTS_AUTH_TOKEN:}}")
          String compositorToken) {
    this.service = service;
    this.pdeToken = pdeToken;
    this.compositorToken = compositorToken;
  }

  /** Entrega a interface versionada, sem informação privada e sem emitir evento de funil. */
  @GetMapping(value = "/prototype", produces = "text/html;charset=UTF-8")
  public ResponseEntity<byte[]> page() {
    return resource("prototype.html", "text/html;charset=UTF-8");
  }

  /** Entrega o código do cliente privado, mantendo toda decisão de fila no backend. */
  @GetMapping(value = "/prototype.js", produces = "text/javascript;charset=UTF-8")
  public ResponseEntity<byte[]> script() {
    return resource("prototype.js", "text/javascript;charset=UTF-8");
  }

  /** Entrega os estilos responsivos da experiência sem dependência de CDN externa. */
  @GetMapping(value = "/prototype.css", produces = "text/css;charset=UTF-8")
  public ResponseEntity<byte[]> style() {
    return resource("prototype.css", "text/css;charset=UTF-8");
  }

  /**
   * Permite ao operador preparar um acesso sintético pelo mesmo padrão da administração dos ciclos.
   */
  @PostMapping("/admin/sessions")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Preparar acesso de QA sem inferência, compra ou gasto de mídia")
  public ResponseEntity<Created> adminCreate(@Valid @RequestBody Create input) {
    return privateResponse(service.create(input));
  }

  /** Emite acesso para o executor de homologação após autenticação da integração PDE. */
  @PostMapping("/internal/sessions")
  public ResponseEntity<Created> create(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @Valid @RequestBody Create input) {
    authorize(token, pdeToken);
    return privateResponse(service.create(input));
  }

  /** Recupera exclusivamente o pacote da sessão autenticada. */
  @GetMapping("/session")
  public ResponseEntity<SessionView> session(@RequestHeader("X-Kit-Session") String token) {
    return privateResponse(service.session(token));
  }

  /** Confere a entrada antes de reservar o trabalho de composição no backend. */
  @PutMapping("/input")
  public ResponseEntity<SessionView> input(
      @RequestHeader("X-Kit-Session") String token, @Valid @RequestBody Input input) {
    return privateResponse(service.input(token, input));
  }

  /** Recebe somente ações reais desta sessão, marcadas como simulação técnica. */
  @PostMapping("/events")
  public ResponseEntity<SessionView> event(
      @RequestHeader("X-Kit-Session") String token, @Valid @RequestBody Event input) {
    return privateResponse(service.event(token, input));
  }

  /** Entrega o ZIP íntegro somente ao acesso autorizado e dentro do limite de transferências. */
  @GetMapping("/download")
  public ResponseEntity<byte[]> download(@RequestHeader("X-Kit-Session") String token) {
    return ResponseEntity.ok()
        .headers(privateHeaders())
        .contentType(MediaType.parseMediaType("application/zip"))
        .header("Content-Disposition", "attachment; filename=kit-privado.zip")
        .body(service.download(token));
  }

  /** Entrega a imagem pelo acesso opaco, sem disponibilizar URL pública do artefato. */
  @GetMapping("/assets/{name}")
  public ResponseEntity<byte[]> asset(
      @RequestHeader("X-Kit-Session") String token, @PathVariable String name) {
    return ResponseEntity.ok()
        .headers(privateHeaders())
        .contentType(MediaType.IMAGE_PNG)
        .body(service.asset(token, name));
  }

  /** Revoga a sessão preservando sua trilha de auditoria. */
  @DeleteMapping("/internal/sessions/{id}")
  public void revoke(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable String id) {
    authorize(token, pdeToken);
    service.revoke(id);
  }

  /** Expõe provas persistidas sem credenciais ou conteúdo binário. */
  @GetMapping("/internal/cycles/{cycleId}/report")
  public Report report(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable Long cycleId) {
    authorize(token, pdeToken);
    return service.report(cycleId);
  }

  /** Oferece ao compositor somente reservas válidas criadas pelo backend. */
  @GetMapping("/stage-executions/pending")
  public List<Pending> pending(
      @RequestHeader(value = "X-Payments-Auth", required = false) String token) {
    authorize(token, compositorToken);
    return service.pending();
  }

  /** Reserva uma tentativa de composição sem criar nova etapa no executor. */
  @PostMapping("/stage-executions/{id}/claim")
  public Pending claim(
      @RequestHeader(value = "X-Payments-Auth", required = false) String token,
      @PathVariable String id,
      @Valid @RequestBody Claim input) {
    authorize(token, compositorToken);
    return service.claim(id, input);
  }

  /** Aceita o callback apenas após validar quantidade, dimensões, hash e vínculo do arquivo. */
  @PostMapping("/stage-executions/{id}/result")
  public void complete(
      @RequestHeader(value = "X-Payments-Auth", required = false) String token,
      @PathVariable String id,
      @Valid @RequestBody Result input) {
    authorize(token, compositorToken);
    service.complete(id, input);
  }

  /** Preserva a falha terminal, sem duplicar custo ou apagar a reserva anterior. */
  @PostMapping("/stage-executions/{id}/failure")
  public void fail(
      @RequestHeader(value = "X-Payments-Auth", required = false) String token,
      @PathVariable String id,
      @Valid @RequestBody Failure input) {
    authorize(token, compositorToken);
    service.fail(id, input);
  }

  /** Compara credenciais em tempo constante e recusa configuração vazia. */
  private void authorize(String presented, String expected) {
    if (expected == null
        || expected.isBlank()
        || presented == null
        || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna necessária.");
  }

  /** Impede cache, referência e indexação de qualquer sessão ou artefato privado. */
  private HttpHeaders privateHeaders() {
    var h = new HttpHeaders();
    h.setCacheControl("no-store");
    h.add("Referrer-Policy", "no-referrer");
    h.add("X-Robots-Tag", "noindex, nofollow");
    h.add("X-Content-Type-Options", "nosniff");
    return h;
  }

  /** Aplica proteção uniforme aos contratos privados. */
  private <T> ResponseEntity<T> privateResponse(T value) {
    return ResponseEntity.ok().headers(privateHeaders()).body(value);
  }

  /** Carrega recursos versionados sem aceitar caminho fornecido pelo navegador. */
  private ResponseEntity<byte[]> resource(String name, String type) {
    try {
      var bytes = new ClassPathResource("private-kit-v1/" + name).getContentAsByteArray();
      return ResponseEntity.ok()
          .headers(privateHeaders())
          .header(
              "Content-Security-Policy",
              "default-src 'self'; img-src 'self' blob:; script-src 'self'; style-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'")
          .contentType(MediaType.parseMediaType(type))
          .body(bytes);
    } catch (IOException ex) {
      log.error("Kit privado: recurso versionado ausente file={}", name, ex);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Experiência privada indisponível.");
    }
  }
}
