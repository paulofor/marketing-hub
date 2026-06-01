package com.marketinghub.repository.jpa.oprm;

import com.marketinghub.oprm.OprmOccupation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmOccupation.
 */
public interface OprmOccupationRepository extends JpaRepository<OprmOccupation, UUID> {
    List<OprmOccupation> findAllByOrderByDisplayNameAsc();

    Optional<OprmOccupation> findByOccupationSeedRefIgnoreCase(String occupationSeedRef);
}
