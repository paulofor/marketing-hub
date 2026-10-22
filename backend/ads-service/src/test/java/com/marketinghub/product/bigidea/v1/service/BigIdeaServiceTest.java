package com.marketinghub.product.bigidea.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import com.marketinghub.product.bigidea.v1.*;
import com.marketinghub.product.bigidea.v1.service.save.*;
import com.marketinghub.repository.jpa.product.*;
import jakarta.validation.Validation;
import java.util.*;
import org.junit.jupiter.api.*;

/** Verifica revisão, isolamento e recuperação da direção comercial sem chamadas externas. */
class BigIdeaServiceTest {
  ProductRepository products;
  ProductBigIdeaRepository revisions;
  ProductBigIdeaBindingRepository bindings;
  BigIdeaService service;
  Product product;
  List<ProductBigIdea> saved;

  /** Prepara produtos e persistência sintéticos com versões independentes. */
  @BeforeEach
  void setup() {
    products = mock(ProductRepository.class);
    revisions = mock(ProductBigIdeaRepository.class);
    bindings = mock(ProductBigIdeaBindingRepository.class);
    service =
        new BigIdeaService(
            products,
            revisions,
            bindings,
            new ObjectMapper().findAndRegisterModules(),
            Validation.buildDefaultValidatorFactory().getValidator());
    product = new Product();
    product.setId(701L);
    saved = new ArrayList<>();
    when(products.findById(701L)).thenReturn(Optional.of(product));
    when(products.findLockedById(701L)).thenReturn(Optional.of(product));
    when(revisions.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              ProductBigIdea idea = i.getArgument(0);
              idea.setId((long) saved.size() + 1);
              saved.add(idea);
              return idea;
            });
    when(revisions.findByProductIdOrderByRevisionDesc(701L)).thenAnswer(i -> saved.reversed());
  }

  /** Fornece uma hipótese de valor com limitações explícitas. */
  static BigIdeaContent content(String idea) {
    return new BigIdeaContent(
        idea,
        "Profissional com agenda vazia",
        "Publicar com menos esforço",
        "Kit pronto para usar",
        "Peças adaptadas à agenda",
        "Hipótese, sem vendas medidas",
        "Não garante agendamentos");
  }

  /** Cria, evolui e preserva a revisão anterior integralmente. */
  @Test
  void keepsImmutableHistory() {
    var first =
        service.save(
            701L,
            new SaveBigIdeaRequest(
                0, content("Conteúdo pronto para a agenda real"), "Direção inicial"),
            "Operador");
    var second =
        service.save(
            701L,
            new SaveBigIdeaRequest(
                1, content("A agenda vira uma pauta pronta"), "Tornar o mecanismo claro"),
            "Operador");
    assertThat(first.revision()).isEqualTo(1);
    assertThat(second.revision()).isEqualTo(2);
    assertThat(service.get(701L).history()).containsExactly(second, first);
    assertThat(first.content().centralIdea()).isEqualTo("Conteúdo pronto para a agenda real");
  }

  /** Recusa sobrescrita por edição antiga e valida conteúdo antes da persistência. */
  @Test
  void rejectsStaleAndEmptyWrites() {
    service.save(701L, new SaveBigIdeaRequest(0, content("Direção A"), "Inicial"), "Operador");
    assertThatThrownBy(
            () ->
                service.save(
                    701L,
                    new SaveBigIdeaRequest(0, content("Direção B"), "Outro motivo"),
                    "Operador"))
        .hasMessageContaining("409");
    assertThatThrownBy(
            () -> service.save(701L, new SaveBigIdeaRequest(1, content("  "), "Teste"), "Operador"))
        .hasMessageContaining("400");
    assertThat(saved).hasSize(1);
  }

  /** Reutiliza resposta perdida sem duplicar a revisão nem inferência. */
  @Test
  void retriesIdenticalWriteIdempotently() {
    var request = new SaveBigIdeaRequest(0, content("Direção A"), "Inicial");
    assertThat(service.save(701L, request, "Operador"))
        .isEqualTo(service.save(701L, request, "Operador"));
    assertThat(saved).hasSize(1);
  }

  /** Preserva versão fixada do experimento após evolução e isola outro produto. */
  @Test
  void preservesExperimentReferenceAndIsolation() {
    var first =
        service.save(701L, new SaveBigIdeaRequest(0, content("Direção A"), "Inicial"), "Operador");
    var binding = new ProductBigIdeaBinding();
    binding.setBigIdea(product.getBigIdea());
    binding.setProductId(701L);
    when(bindings.findByProductIdAndSourceReference(701L, "experiment:901"))
        .thenReturn(Optional.of(binding));
    service.save(701L, new SaveBigIdeaRequest(1, content("Direção B"), "Aprendizado"), "Operador");
    assertThat(service.pin(701L, "experiment:901@agent-validation-v1")).isEqualTo(first);
    assertThat(service.pin(701L, "experiment:902").revision()).isEqualTo(2);
    assertThatThrownBy(() -> service.pin(702L, "experiment:901")).hasMessageContaining("404");
  }

  /** Mantém produtos sem ideia explícitos sem bloquear leitura de legado. */
  @Test
  void absenceDoesNotFabricateDirection() {
    assertThat(service.get(701L).status()).isEqualTo("MISSING");
    assertThat(service.pin(701L, "experiment:901")).isNull();
    verify(bindings, never()).save(any());
  }

  /** Permite primeira direção de Atena e protege uma definição concorrente do operador. */
  @Test
  void atenaOnlyInitializesOnce() {
    var idea = content("Direção A");
    service.initializeFromStrategy(701L, idea, 501L);
    service.initializeFromStrategy(701L, idea, 501L);
    assertThat(saved).hasSize(1);
    assertThat(saved.getFirst().getSourceTaskId()).isEqualTo(501L);
    assertThatThrownBy(() -> service.initializeFromStrategy(701L, content("Direção B"), 502L))
        .hasMessageContaining("409");
  }
}
