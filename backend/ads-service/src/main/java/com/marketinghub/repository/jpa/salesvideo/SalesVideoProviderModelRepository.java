package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoProviderModel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir o catálogo administrável de modelos de vídeo. */
public interface SalesVideoProviderModelRepository
    extends JpaRepository<SalesVideoProviderModel, Long> {

  /** Lista o catálogo em ordem estável para consumo pelas telas. */
  List<SalesVideoProviderModel> findAllByOrderByDisplayNameAsc();
}
