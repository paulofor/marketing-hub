package com.marketinghub.oprm.repository;

import com.marketinghub.oprm.OprmOccupation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OprmOccupationRepository extends JpaRepository<OprmOccupation, UUID> {
    List<OprmOccupation> findAllByOrderByDisplayNameAsc();

    Optional<OprmOccupation> findByOccupationSeedRefIgnoreCase(String occupationSeedRef);
}
