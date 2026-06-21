package com.marketinghub.experiment.service.generatepromise.latestdraft;

import com.marketinghub.experiment.service.generatepromise.ExperimentPromiseOptionDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Resposta com a solicitação de promessa mais recente que pode ser retomada pela tela. */
public record ExperimentPromiseOptionsDraftResponse(
        Long requestId,
        String status,
        Long nicheId,
        UUID hypothesisId,
        String currentSinglePain,
        String currentFreeReward,
        String currentFunnelPromise,
        String currentPrimaryCta,
        List<ExperimentPromiseOptionDto> options,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {}
