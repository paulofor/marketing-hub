package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de Prompt.
 */
public interface PromptRepository extends JpaRepository<Prompt, Long> {
    List<Prompt> findByDomainOrderByUpdatedAtDesc(String domain);

    List<Prompt> findAllByOrderByUpdatedAtDesc();

    Optional<Prompt> findFirstByDomainAndActiveTrueOrderByUpdatedAtDesc(String domain);

    boolean existsByDomainIgnoreCase(String domain);

    @Modifying
    @Query("update Prompt p set p.active = false where p.domain = :domain and p.id <> :id")
    void deactivateOthers(@Param("domain") String domain, @Param("id") Long id);
}
