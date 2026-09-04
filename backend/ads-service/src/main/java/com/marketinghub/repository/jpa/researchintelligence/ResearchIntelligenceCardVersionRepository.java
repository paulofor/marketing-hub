package com.marketinghub.repository.jpa.researchintelligence;

import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardStatus;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardVersion;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persiste versões editoriais e oferece consultas filtradas para catálogo e gestão. */
public interface ResearchIntelligenceCardVersionRepository
    extends JpaRepository<ResearchIntelligenceCardVersion, Long> {

  /** Recupera o resultado original de uma submissão idempotente. */
  Optional<ResearchIntelligenceCardVersion> findByIdempotencyKey(String idempotencyKey);

  /** Calcula a última versão sob o bloqueio da identidade lógica. */
  @Query(
      "select max(version.versionNumber) from ResearchIntelligenceCardVersion version "
          + "where version.cardKey = :cardKey")
  Optional<Integer> findMaximumVersionNumber(@Param("cardKey") String cardKey);

  /** Localiza uma versão específica sem bloqueio para leitura administrativa. */
  Optional<ResearchIntelligenceCardVersion> findByCardKeyAndVersionNumber(
      String cardKey, Integer versionNumber);

  /** Bloqueia uma versão durante mudança de estado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select version from ResearchIntelligenceCardVersion version "
          + "where version.cardKey = :cardKey and version.versionNumber = :versionNumber")
  Optional<ResearchIntelligenceCardVersion> findVersionForUpdate(
      @Param("cardKey") String cardKey, @Param("versionNumber") Integer versionNumber);

  /** Bloqueia versões ativas anteriores antes da troca atômica. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select version from ResearchIntelligenceCardVersion version "
          + "where version.cardKey = :cardKey and version.status = :status")
  List<ResearchIntelligenceCardVersion> findByCardKeyAndStatusForUpdate(
      @Param("cardKey") String cardKey, @Param("status") ResearchIntelligenceCardStatus status);

  /** Lista cartões ativos que podem ampliar o catálogo empacotado. */
  List<ResearchIntelligenceCardVersion> findByStatusOrderByCardKeyAscVersionNumberAsc(
      ResearchIntelligenceCardStatus status);

  /** Filtra a visão de gestão no banco antes de aplicar o limite solicitado. */
  @Query(
      "select version from ResearchIntelligenceCardVersion version "
          + "where (:status is null or version.status = :status) "
          + "and (:collection is null or version.collection = :collection) "
          + "order by version.createdAt desc, version.id desc")
  List<ResearchIntelligenceCardVersion> findForManagement(
      @Param("status") ResearchIntelligenceCardStatus status,
      @Param("collection") String collection,
      Pageable pageable);
}
