package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSourceEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável por evidências agregadas dos públicos gerais OPRM. */
public interface OprmGeneralAudienceSourceEvidenceRepository extends JpaRepository<OprmGeneralAudienceSourceEvidence, Long> {

    /** Lista evidências de uma semente do registro mais recente para o mais antigo. */
    List<OprmGeneralAudienceSourceEvidence> findAllBySeedIdOrderByCapturedAtDesc(Long seedId);

    /** Lista evidências de um subnicho do registro mais recente para o mais antigo. */
    List<OprmGeneralAudienceSourceEvidence> findAllBySubnicheIdOrderByCapturedAtDesc(Long subnicheId);

    /** Conta evidências agregadas associadas diretamente ao subnicho. */
    long countBySubnicheId(Long subnicheId);
}
