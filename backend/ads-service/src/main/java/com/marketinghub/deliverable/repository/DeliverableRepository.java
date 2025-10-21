package com.marketinghub.deliverable.repository;

import com.marketinghub.deliverable.Deliverable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository handling persistence for {@link Deliverable} entities.
 */
public interface DeliverableRepository extends JpaRepository<Deliverable, Long> {
    List<Deliverable> findByNicheIdOrderByCreatedAtDesc(Long marketNicheId);

    default List<Deliverable> findAllLatestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
