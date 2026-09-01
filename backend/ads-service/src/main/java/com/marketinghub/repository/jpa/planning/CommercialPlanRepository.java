package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consultar planos comerciais. */
public interface CommercialPlanRepository extends JpaRepository<CommercialPlan, Long> {
  /** Lista planos comerciais por status, priorizando os mais recentes. */
  List<CommercialPlan> findByStatusOrderByUpdatedAtDesc(CommercialPlanStatus status);

  /** Localiza os planos que governam um experimento direto ou de seu portfólio. */
  @Query(
      "select distinct p from CommercialPlan p left join p.experiments e "
          + "where p.experiment.id = :experimentId or e.id = :experimentId order by p.updatedAt desc")
  List<CommercialPlan> findByExperimentReference(@Param("experimentId") Long experimentId);

  /** Localiza os planos que governam um produto por hipótese ou experimento associado. */
  @Query(
      "select distinct p from CommercialPlan p "
          + "left join p.hypothesis h left join h.product hp "
          + "left join p.experiment pe left join pe.product pep "
          + "left join p.experiments e left join e.product ep "
          + "where hp.id = :productId or pep.id = :productId or ep.id = :productId "
          + "order by p.updatedAt desc")
  List<CommercialPlan> findByProductId(@Param("productId") Long productId);

  /** Localiza apenas os identificadores dos planos usados pela medição da cadeia do produto. */
  @Query(
      "select distinct p.id from CommercialPlan p "
          + "left join p.hypothesis h left join h.product hp "
          + "left join p.experiment pe left join pe.product pep "
          + "left join p.experiments e left join e.product ep "
          + "where hp.id = :productId or pep.id = :productId or ep.id = :productId")
  List<Long> findIdsByProductId(@Param("productId") Long productId);
}
