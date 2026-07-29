package com.marketinghub.repository.jpa.pde;

import com.marketinghub.pde.infrastructure.PdeVpsServer;
import com.marketinghub.pde.infrastructure.PdeVpsStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pelo cadastro de VPS usadas pelos PDEs. */
public interface PdeVpsServerRepository extends JpaRepository<PdeVpsServer, Long> {

  /** Lista VPS cadastradas por nome operacional. */
  List<PdeVpsServer> findAllByOrderByNameAsc();

  /** Lista VPS ativas vinculadas ao produto informado. */
  List<PdeVpsServer> findByProductSlugAndStatusIn(
      String productSlug, Collection<PdeVpsStatus> statuses);
}
