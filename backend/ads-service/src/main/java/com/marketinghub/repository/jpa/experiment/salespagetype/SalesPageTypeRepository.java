package com.marketinghub.repository.jpa.experiment.salespagetype;

import com.marketinghub.experiment.salespagetype.SalesPageType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar o catalogo de tipos de pagina de venda. */
public interface SalesPageTypeRepository extends JpaRepository<SalesPageType, String> {
  /** Lista os tipos ativos em ordem alfabetica para selecao operacional. */
  List<SalesPageType> findByActiveTrueOrderByNameAsc();
}
