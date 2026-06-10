package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceLandingConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pelos registros OPRM de situação de confirmação de landing/formulário. */
public interface OprmGeneralAudienceLandingConfirmationRepository
        extends JpaRepository<OprmGeneralAudienceLandingConfirmation, Long> {
}
