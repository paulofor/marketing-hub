package com.marketinghub.repository.jpa.agent;

import com.marketinghub.agent.Agent;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório JPA responsável pela persistência de Agent. */
public interface AgentRepository extends JpaRepository<Agent, Long> {

  /** Recupera o agente e seus relacionamentos para exibicao administrativa. */
  Optional<Agent> findDetailedById(Long id);

  /** Recupera a identidade técnica usada por integrações dos agentes. */
  Optional<Agent> findByAgentKey(String agentKey);

  /** Consulta somente o retrato cadastrado para o apelido exato do responsável. */
  @Query(
      "select portrait.url from Agent agent join agent.portraitAsset portrait where agent.nickname = :nickname")
  Optional<String> findPortraitUrlByNickname(@Param("nickname") String nickname);

  /** Bloqueia o cadastro durante a troca do estado operacional PLAY/STOP. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Agent> findLockedById(Long id);

  /** Lista os agentes pelo apelido usado na comunicacao operacional. */
  List<Agent> findAllByOrderByNicknameAsc();

  /** Verifica conflito de apelido ao criar um agente. */
  boolean existsByNicknameIgnoreCase(String nickname);

  /** Verifica conflito de apelido ao editar outro agente. */
  boolean existsByNicknameIgnoreCaseAndIdNot(String nickname, Long id);
}
