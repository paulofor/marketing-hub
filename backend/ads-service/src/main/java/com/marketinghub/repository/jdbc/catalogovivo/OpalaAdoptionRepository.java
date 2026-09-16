package com.marketinghub.repository.jdbc.catalogovivo;

import com.marketinghub.catalogovivo.v1.service.adoption.OpalaAdoption;
import java.sql.Timestamp;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Responsabilidade: persistir a adesão única por ciclo sem reescrever suas definições históricas.
 */
@Repository
@RequiredArgsConstructor
public class OpalaAdoptionRepository {
  private final JdbcTemplate jdbc;

  /** Consulta a versão do subprocesso que foi fixada pelo operador para este ciclo. */
  public Optional<OpalaAdoption> find(long cycleId) {
    return jdbc
        .query(
            "SELECT * FROM catalogo_vivo_opala_adoption_v1 WHERE cycle_id=?",
            (r, n) ->
                new OpalaAdoption(
                    r.getLong("cycle_id"),
                    r.getLong("process_definition_id"),
                    r.getLong("product_id"),
                    r.getLong("experiment_id"),
                    r.getString("product_version"),
                    r.getLong("cycle_revision"),
                    r.getString("operator_name"),
                    r.getString("reason"),
                    r.getTimestamp("created_at").toInstant()),
            cycleId)
        .stream()
        .findFirst();
  }

  /** Insere uma decisão protegida pelo lock do produto/ciclo e por unicidade física. */
  public void insert(OpalaAdoption adoption) {
    jdbc.update(
        "INSERT INTO catalogo_vivo_opala_adoption_v1(cycle_id,process_definition_id,product_id,experiment_id,product_version,cycle_revision,operator_name,reason,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
        adoption.cycleId(),
        adoption.processDefinitionId(),
        adoption.productId(),
        adoption.experimentId(),
        adoption.productVersion(),
        adoption.cycleRevision(),
        adoption.operatorName(),
        adoption.reason(),
        Timestamp.from(adoption.createdAt()));
  }

  /** Valida o vínculo completo antes de permitir uma execução fora do grafo histórico. */
  public boolean permits(
      long cycleId, long productId, long chainId, long processId, String source) {
    Long count =
        jdbc.queryForObject(
            """
        SELECT COUNT(*) FROM catalogo_vivo_opala_adoption_v1 a
        JOIN learning_sales_cycle_v1 c ON c.id=a.cycle_id
        WHERE a.cycle_id=? AND a.product_id=? AND a.process_definition_id=?
          AND c.product_id=a.product_id AND c.experiment_id=a.experiment_id
          AND c.chain_definition_id=? AND CONCAT('experiment:',c.experiment_id)=?
        """,
            Long.class,
            cycleId,
            productId,
            processId,
            chainId,
            source);
    return count != null && count == 1;
  }
}
