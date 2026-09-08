package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: serializar snapshots estruturados e recusar auditoria ilegível. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleJson {
  private final ObjectMapper mapper;

  /** Serializa o contrato sem embutir JSON textual na resposta HTTP. */
  public String write(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Módulo ciclos: falha ao serializar evidência de decisão", ex);
      throw new IllegalStateException("Não foi possível preservar a evidência do ciclo.", ex);
    }
  }

  /** Lê uma evidência persistida sem substituir corrupção por aprovação vazia. */
  public JsonNode read(String value) {
    try {
      return mapper.readTree(value);
    } catch (Exception ex) {
      log.error("Módulo ciclos: falha ao ler evidência persistida", ex);
      throw new IllegalStateException("A evidência persistida do ciclo não pode ser lida.", ex);
    }
  }
}
