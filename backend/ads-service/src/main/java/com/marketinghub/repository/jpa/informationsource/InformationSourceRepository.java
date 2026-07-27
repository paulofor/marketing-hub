package com.marketinghub.repository.jpa.informationsource;

import com.marketinghub.informationsource.InformationSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for {@link InformationSource} entities. */
public interface InformationSourceRepository extends JpaRepository<InformationSource, Long> {
  List<InformationSource> findByNicheIdOrderByCreatedAtDesc(Long nicheId);
}
