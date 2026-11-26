package com.marketinghub.product.fichas.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.fichas.v1.service.list.FichasView;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: consultar o acervo imutável do piloto no escopo do cadastro existente. */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "marketinghub.fichas.homologacao", havingValue = "true")
public class FichasService {
  private final ProductRepository products;
  private final ObjectMapper mapper;

  /** Lista revisões exatas do produto e ciclo, sem gravar dados nem despachar agentes. */
  public FichasView list(Long productId, Long ciclo) {
    if (!products.existsById(productId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado");
    var selected = new ArrayList<JsonNode>();
    for (String file : new String[] {"exemplo-bloqueado.json", "exemplo-recuperado.json"}) {
      try (var stream = new ClassPathResource("fichas/v1/" + file).getInputStream()) {
        for (JsonNode card : mapper.readTree(stream)) {
          if (card.path("produtoId").asLong() != productId) continue;
          if (ciclo != null
              && card.has("execucao")
              && card.path("execucao").path("ciclo").asLong() != ciclo) continue;
          selected.add(card);
        }
      } catch (IOException ex) {
        log.error(
            "Falha ao consultar fichas produtoId={} ciclo={} arquivo={}",
            productId,
            ciclo,
            file,
            ex);
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Acervo indisponível", ex);
      }
    }
    // Um recurso só pertence ao ciclo quando uma revisão selecionada o referencia.
    if (ciclo != null) {
      var included = new HashSet<String>();
      selected.stream().filter(c -> c.has("execucao")).forEach(c -> included.add(key(c)));
      boolean changed;
      do {
        changed = false;
        for (var card : selected) {
          if (!included.contains(key(card))) continue;
          for (var link : card.path("vinculos")) changed |= included.add(key(link));
        }
      } while (changed);
      selected.removeIf(c -> !included.contains(key(c)));
    }
    return new FichasView("HOMOLOGACAO_HISTORICA", false, selected);
  }

  /** Identifica uma revisão exata, sem resolver implicitamente para a versão mais recente. */
  private String key(JsonNode node) {
    return node.path("id").asText() + "#r" + node.path("revisao").asInt();
  }
}
