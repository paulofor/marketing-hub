package com.marketinghub.ads;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FacebookInstantFormRepository extends JpaRepository<FacebookInstantForm, Long> {
    List<FacebookInstantForm> findByHypothesisId(UUID hypothesisId);

    List<FacebookInstantForm> findByApprovedTrueAndPublishedFalse();

    @Query("select form from fb_instant_form form " +
           "where form.approved = true and form.published = false " +
           "and (form.formId is null or trim(form.formId) = '')")
    List<FacebookInstantForm> findApprovedDraftsWithoutExternalId();
}
