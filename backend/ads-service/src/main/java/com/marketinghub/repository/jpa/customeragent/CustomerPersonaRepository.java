package com.marketinghub.repository.jpa.customeragent;

import com.marketinghub.customeragent.CustomerPersona;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar versoes persistidas das personas. */
public interface CustomerPersonaRepository extends JpaRepository<CustomerPersona, Long> {
  /** Lista personas ativas na ordem de nome. */
  List<CustomerPersona> findByActiveTrueOrderByNameAsc();
}
