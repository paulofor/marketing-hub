package com.marketinghub.repository.jpa.agent;

import com.marketinghub.agent.Agent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de Agent. */
public interface AgentRepository extends JpaRepository<Agent, Long> {

  /** Recupera o agente e seus relacionamentos para exibicao administrativa. */
  Optional<Agent> findDetailedById(Long id);

  /** Lista os agentes pelo apelido usado na comunicacao operacional. */
  List<Agent> findAllByOrderByNicknameAsc();

  /** Verifica conflito de apelido ao criar um agente. */
  boolean existsByNicknameIgnoreCase(String nickname);

  /** Verifica conflito de apelido ao editar outro agente. */
  boolean existsByNicknameIgnoreCaseAndIdNot(String nickname, Long id);
}
