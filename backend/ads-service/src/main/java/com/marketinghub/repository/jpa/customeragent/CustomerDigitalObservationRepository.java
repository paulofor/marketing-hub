package com.marketinghub.repository.jpa.customeragent;

import com.marketinghub.customeragent.CustomerDigitalObservation;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar experiencias digitais observacionais persistidas. */
public interface CustomerDigitalObservationRepository
    extends JpaRepository<CustomerDigitalObservation, Long> {
  /** Lista observacoes pelo estado operacional e ordem de criacao. */
  List<CustomerDigitalObservation> findByStatusOrderByCreatedAtAsc(String status, Pageable page);
}
