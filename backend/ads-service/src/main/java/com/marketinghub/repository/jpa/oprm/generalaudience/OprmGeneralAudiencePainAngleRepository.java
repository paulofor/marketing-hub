package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável por dores e ângulos testáveis de públicos gerais OPRM. */
public interface OprmGeneralAudiencePainAngleRepository extends JpaRepository<OprmGeneralAudiencePainAngle, Long> {

    /** Lista ângulos de um subnicho do mais recente para o mais antigo. */
    List<OprmGeneralAudiencePainAngle> findAllBySubnicheIdOrderByUpdatedAtDesc(Long subnicheId);

    /** Conta ângulos de um subnicho que estão em status aprovados para quality gate. */
    long countBySubnicheIdAndStatusIn(Long subnicheId, Collection<OprmGeneralAudiencePainAngleStatus> statuses);
}
