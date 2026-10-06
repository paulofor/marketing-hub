package com.marketinghub.pde.mira.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.mira.privateprototype.v1.service.MiraPrivateService;
import com.marketinghub.pde.mira.privateprototype.v1.service.contract.MiraPrivateContract.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.mira.MiraPrivateSessionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: prevenir regressões de identidade, segurança, retomada e limites da candidata.
 */
class MiraPrivateServiceTest {
  private MiraPrivateSessionRepository repository;
  private LearningSalesCycleRepository cycles;
  private ProductRepository products;
  private MiraPrivateService service;
  private Map<String, MiraPrivateSession> rows;

  /** Monta dependências locais e duas identidades para detectar exceções por identificador. */
  @BeforeEach
  void setup() {
    repository = mock(MiraPrivateSessionRepository.class);
    cycles = mock(LearningSalesCycleRepository.class);
    products = mock(ProductRepository.class);
    rows = new HashMap<>();
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            a -> {
              var row = (MiraPrivateSession) a.getArgument(0);
              rows.put(row.getId(), row);
              return row;
            });
    when(repository.findBySessionHash(any()))
        .thenAnswer(
            a ->
                rows.values().stream()
                    .filter(r -> r.getSessionHash().equals(a.getArgument(0)))
                    .findFirst());
    when(repository.findLockedById(any()))
        .thenAnswer(a -> Optional.ofNullable(rows.get(a.getArgument(0))));
    when(repository.findByCycleIdOrderByCreatedAtAsc(any()))
        .thenAnswer(
            a ->
                rows.values().stream()
                    .filter(r -> r.getCycleId().equals(a.getArgument(0)))
                    .toList());
    when(cycles.findLockedById(any()))
        .thenAnswer(
            a -> {
              long id = a.getArgument(0);
              var c = new LearningSalesCycle();
              c.setId(id);
              c.setProductId(id + 100);
              c.setExperimentId(id + 200);
              c.setStage("ADJUSTMENT");
              c.setStatus("OPEN");
              c.setProductVersion(MiraPrivateService.VERSION);
              return Optional.of(c);
            });
    when(products.findById(any()))
        .thenAnswer(
            a -> {
              var p = new Product();
              p.setId(a.getArgument(0));
              p.setSlug(MiraPrivateService.PRODUCT_SLUG);
              return Optional.of(p);
            });
    service = new MiraPrivateService(repository, cycles, products, new ObjectMapper());
  }

  /** Cria pacote sintético com identidades diferentes do caso operacional. */
  private String create(long cycle, String condition, String scenario) {
    return service
        .create(
            new Create(
                cycle,
                "experiment:" + (cycle + 200),
                MiraPrivateService.VERSION,
                condition,
                scenario,
                "DESKTOP_1440"))
        .sessionToken();
  }

  /** Usa instruções sintéticas explícitas sem declarar evidência documental ou clínica. */
  private Input input(String name) {
    return new Input(
        "Organizar os produtos que já tenho", List.of(new ProductInput(name, "Limpar e enxaguar")));
  }

  /** Confirma uma entrada única, idempotência, retomada e limite de duas organizações úteis. */
  @Test
  void oneProductAndLostResponseReuseTheResult() {
    String token = create(6, "REDUCED", "ADHERENT");
    service.input(token, input("Produto A"));
    var first = service.generate(token);
    assertThat(first.routine()).hasSize(1);
    assertThat(first.organizationsUsed()).isEqualTo(1);
    assertThat(service.generate(token)).isEqualTo(first);
    service = new MiraPrivateService(repository, cycles, products, new ObjectMapper());
    assertThat(service.session(token)).isEqualTo(first);
    assertThat(service.input(token, input("Produto A"))).isEqualTo(first);
    service.input(token, input("Produto B"));
    assertThat(service.generate(token).organizationsUsed()).isEqualTo(2);
    assertThatThrownBy(() -> service.input(token, input("Produto C")))
        .hasMessageContaining("duas organizações");
    assertThat(service.session(token).routine().getFirst().productName()).isEqualTo("Produto B");
  }

  /** Mantém o primeiro resultado consumível durante correção ou bloqueio da segunda organização. */
  @Test
  void preservesUsefulResultWhenSecondInputIsBlocked() {
    String token = create(51, "REDUCED", "RECOVERY");
    service.input(token, input("Primeiro produto"));
    var first = service.generate(token);
    var draft =
        service.input(token, new Input("Diagnosticar manchas", input("Outro produto").products()));
    assertThat(draft.previousResults()).hasSize(1);
    var blocked = service.generate(token);
    assertThat(blocked.status()).isEqualTo("BLOCKED");
    assertThat(blocked.organizationsUsed()).isEqualTo(1);
    assertThat(blocked.previousResults().getFirst().routine()).isEqualTo(first.routine());
    service.input(token, input("Segundo produto"));
    var second = service.generate(token);
    assertThat(second.previousResults().getFirst().routine()).isEqualTo(first.routine());
    assertThat(second.organizationsUsed()).isEqualTo(2);
  }

  /** Garante qualidade idêntica para inventários iguais nas duas condições e outro ciclo. */
  @Test
  void equalInventoryProducesEqualOutputAcrossConditionsAndIdentities() {
    String reference = create(18, "REFERENCE", "ADHERENT"),
        reduced = create(29, "REDUCED", "ADHERENT");
    var input =
        new Input(
            "Organizar meus cuidados",
            List.of(
                new ProductInput("Limpeza", "Limpar e enxaguar"),
                new ProductInput("Hidratação", "Aplicar após a limpeza")));
    service.input(reference, input);
    service.input(reduced, input);
    assertThat(service.generate(reference).routine())
        .isEqualTo(service.generate(reduced).routine());
    assertThat(service.report(18L).sessions()).hasSize(1);
    assertThat(service.report(29L).sessions()).hasSize(1);
  }

  /** Preserva bloqueio clínico e permite corrigir texto sem consumir organização útil. */
  @Test
  void clinicalAndUndocumentedInputsRemainBlockedWithoutConsumption() {
    String token = create(7, "REDUCED", "SAFETY");
    service.input(token, new Input("Diagnosticar manchas", input("Produto A").products()));
    var blocked = service.generate(token);
    assertThat(blocked.status()).isEqualTo("BLOCKED");
    assertThat(blocked.organizationsUsed()).isZero();
    assertThat(blocked.routine()).isEmpty();
    service.event(token, new Event("SAFETY_LIMIT_BLOCKED"));
    service.event(token, new Event("AGENT_SCENARIO_COMPLETED"));
    assertThat(service.session(token).blocker()).contains("clínica");
    String recovery = create(9, "REDUCED", "RECOVERY");
    service.input(
        recovery,
        new Input(
            "Organizar cuidados",
            List.of(new ProductInput("Sem documento", "Orientação desconhecida"))));
    assertThat(service.generate(recovery).organizationsUsed()).isZero();
    service.input(recovery, input("Documentado"));
    assertThat(service.generate(recovery).status()).isEqualTo("READY");
  }

  /** Impede consumo depois do prazo, acesso sem segredo, expiração e revogação. */
  @Test
  void invalidExpiredRevokedAndLateSessionsAreRejected() {
    String token = create(8, "REDUCED", "ADHERENT");
    var row = rows.values().iterator().next();
    assertThatThrownBy(() -> service.session(row.getId())).hasMessageContaining("inválido");
    service.input(token, input("Produto"));
    row.setPayloadJson(
        row.getPayloadJson()
            .replaceAll(
                "\\\"firstInteractionAt\\\":\\\"[^\\\"]+\\\"",
                "\"firstInteractionAt\":\"2020-01-01T00:00:00Z\""));
    assertThat(service.generate(token).blocker()).contains("tempo");
    row.setExpiresAt(Instant.now().minusSeconds(1));
    assertThatThrownBy(() -> service.session(token)).hasMessageContaining("expirado");
    row.setExpiresAt(Instant.now().plusSeconds(100));
    service.revoke(row.getId());
    assertThatThrownBy(() -> service.session(token)).hasMessageContaining("revogado");
  }

  /** Impede retentativa ou consumo após fechamento do ciclo sem apagar resultado anterior. */
  @Test
  void closedCyclePreservesResultsButRejectsMutations() {
    String token = create(45, "REDUCED", "ADHERENT");
    service.input(token, input("Produto A"));
    var before = service.generate(token);
    var closed = new LearningSalesCycle();
    closed.setId(45L);
    closed.setStatus("CLOSED");
    closed.setProductVersion(MiraPrivateService.VERSION);
    when(cycles.findLockedById(45L)).thenReturn(Optional.of(closed));
    assertThatThrownBy(() -> service.generate(token)).hasMessageContaining("encerrou");
    assertThatThrownBy(() -> service.input(token, input("Outro"))).hasMessageContaining("encerrou");
    assertThat(service.session(token)).isEqualTo(before);
  }

  /** Recusa produto, experimento e versão divergentes antes da emissão do pacote. */
  @Test
  void mismatchedContextCannotIssueAccess() {
    assertThatThrownBy(
            () ->
                service.create(
                    new Create(
                        6L,
                        "experiment:999",
                        MiraPrivateService.VERSION,
                        "REDUCED",
                        "ADHERENT",
                        "DESKTOP_1440")))
        .hasMessageContaining("referência");
    assertThatThrownBy(
            () ->
                service.create(
                    new Create(
                        6L,
                        "experiment:206",
                        "mira-commercial-v1",
                        "REDUCED",
                        "ADHERENT",
                        "DESKTOP_1440")))
        .hasMessageContaining("versão");
    var foreign = new Product();
    foreign.setSlug("outro-produto");
    when(products.findById(any())).thenReturn(Optional.of(foreign));
    assertThatThrownBy(() -> create(6, "REDUCED", "ADHERENT"))
        .hasMessageContaining("outro produto");
    assertThat(rows).isEmpty();
  }
}
