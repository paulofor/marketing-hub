package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceFacebookAdsData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por armazenar dados OPRM que o Facebook Ads buscará via backend. */
public interface OprmGeneralAudienceFacebookAdsDataRepository extends JpaRepository<OprmGeneralAudienceFacebookAdsData, Long> {

    /** Remove os dados anteriores de um ângulo antes de registrar uma nova versão revisada. */
    void deleteByPainAngle_Id(Long painAngleId);

    /** Lista dados de público registrados para um ângulo de dor. */
    List<OprmGeneralAudienceFacebookAdsData> findAllByPainAngle_IdOrderByCreatedAtAsc(Long painAngleId);
}
