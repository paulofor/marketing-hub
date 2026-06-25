package com.marketinghub.oprm.nichocnae.v3.progress.service;

import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Consulta o progresso persistido do pipeline NichoCNAE v3 sem inferir execução no frontend. */
@Service
public class BackendNichoCnaeV3ProgressService {
    private final OprmNichoCnaeV3StageExecutionRepository repository;

    /** Inicializa o service com o repository canônico de execuções v3. */
    public BackendNichoCnaeV3ProgressService(OprmNichoCnaeV3StageExecutionRepository repository) {
        this.repository = repository;
    }

    /** Retorna o job mais recente do CNAE com suas etapas persistidas em ordem de criação. */
    public NichoCnaeV3JobProgressResponse latestByCnae(String cnaeCode) {
        return repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc(cnaeCode, "cnae-intake")
                .map(latest -> new NichoCnaeV3JobProgressResponse(
                        latest.getJobId(),
                        latest.getCnaeCode(),
                        repository.findByJobIdOrderByCreatedAtAsc(latest.getJobId()).stream()
                                .map(this::toStage)
                                .toList()))
                .orElseGet(() -> new NichoCnaeV3JobProgressResponse(null, cnaeCode, List.of()));
    }

    /** Converte a entidade persistida em contrato de progresso para a UI. */
    private NichoCnaeV3StageProgressResponse toStage(OprmNichoCnaeV3StageExecution execution) {
        return new NichoCnaeV3StageProgressResponse(
                execution.getId(),
                execution.getStageCode(),
                execution.getStatus().name(),
                execution.getCreatedAt(),
                execution.getUpdatedAt(),
                execution.getErrorMessage());
    }
}
