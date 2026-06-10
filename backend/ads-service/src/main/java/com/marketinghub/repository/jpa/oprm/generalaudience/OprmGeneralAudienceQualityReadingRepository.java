package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceQualityReading;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável pelas leituras de qualidade de públicos gerais do OPRM. */
public interface OprmGeneralAudienceQualityReadingRepository extends JpaRepository<OprmGeneralAudienceQualityReading, Long> {

    /** Lista leituras de qualidade de um subnicho, da mais recente para a mais antiga. */
    List<OprmGeneralAudienceQualityReading> findAllBySubnicheIdOrderByCapturedAtDesc(Long subnicheId);

    /** Busca a leitura de qualidade mais recente de um subnicho. */
    Optional<OprmGeneralAudienceQualityReading> findTopBySubnicheIdOrderByCapturedAtDesc(Long subnicheId);
}
