package com.marketinghub.facebookads.resumption.service.summary;

import com.marketinghub.facebookads.resumption.service.view.ResumeCampaignView;
import java.math.BigDecimal;

/** Entrega a disponibilidade calculada pelo backend e a autorização mais recente para a tela. */
public record ResumeCampaignSummary(
    boolean applicable,
    boolean available,
    String blocker,
    BigDecimal synchronizedSpend,
    BigDecimal currentLimit,
    BigDecimal zeroResultStopSpend,
    ResumeCampaignView latest) {}
