package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.FacebookInstantForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA responsável pela persistência de FacebookInstantForm.
 */
public interface FacebookInstantFormRepository extends JpaRepository<FacebookInstantForm, Long> {
    List<FacebookInstantForm> findByHypothesisId(UUID hypothesisId);

    List<FacebookInstantForm> findByApprovedTrueAndPublishedFalse();

    @Query("select form from fb_instant_form form " +
           "where form.approved = true and form.published = false " +
           "and (form.formId is null or trim(form.formId) = '')")
    List<FacebookInstantForm> findApprovedDraftsWithoutExternalId();
}
