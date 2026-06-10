package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência das sementes de público geral do OPRM. */
public interface OprmGeneralAudienceSeedRepository extends JpaRepository<OprmGeneralAudienceSeed, Long> {
    /** Lista as sementes mais recentes para revisão manual do usuário. */
    List<OprmGeneralAudienceSeed> findAllByOrderByUpdatedAtDesc();
}
