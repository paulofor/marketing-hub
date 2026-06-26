package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.entity.MoisSalesPage;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável por localizar e salvar a página/produto da biblioteca de vendas MOIS. */
public interface MoisSalesPageRepository extends JpaRepository<MoisSalesPage, Long> {
}
