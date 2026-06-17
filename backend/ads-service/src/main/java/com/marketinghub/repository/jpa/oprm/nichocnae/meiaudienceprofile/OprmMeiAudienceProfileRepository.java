package com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile;

import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por persistir e consultar perfis de público-alvo MEI/autônomo do OPRM. */
public interface OprmMeiAudienceProfileRepository extends JpaRepository<OprmMeiAudienceProfile, Long> {
  /** Verifica se um ciclo de pesquisa já possui perfil de público-alvo MEI/autônomo persistido. */
  boolean existsByResearchCycleId(Long researchCycleId);

  /** Busca o perfil de público-alvo MEI/autônomo mais recente de um ciclo de pesquisa. */
  Optional<OprmMeiAudienceProfile> findFirstByResearchCycleIdOrderByIdDesc(Long researchCycleId);

  /** Busca o perfil de público-alvo MEI/autônomo mais recente vinculado a um nicho já materializado. */
  Optional<OprmMeiAudienceProfile> findFirstByMarketNicheIdOrderByIdDesc(Long marketNicheId);

  /** Lista perfis de público-alvo MEI/autônomo vinculados a um CNAE. */
  List<OprmMeiAudienceProfile> findByCnaeCodeOrderByCreatedAtDesc(String cnaeCode);

  /** Remove perfis MEI/autônomo de um ciclo antes de reexecutar etapas do mesmo job. */
  void deleteByResearchCycleId(Long researchCycleId);
}
