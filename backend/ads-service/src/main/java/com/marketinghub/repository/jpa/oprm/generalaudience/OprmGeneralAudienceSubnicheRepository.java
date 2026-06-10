package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência dos subnichos de públicos gerais do OPRM. */
public interface OprmGeneralAudienceSubnicheRepository extends JpaRepository<OprmGeneralAudienceSubniche, Long> {
    /** Lista subnichos de uma semente com os itens mais recentes primeiro. */
    List<OprmGeneralAudienceSubniche> findAllBySeedIdOrderByUpdatedAtDesc(Long seedId);
}
