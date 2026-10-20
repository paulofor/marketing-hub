package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a renovação automática sem depender da fila operacional de campanhas e públicos. */
@Component
public class FacebookTokenRenewalScheduler {
    private final FacebookTokenRenewalService renewalService;

    /** Recebe o serviço responsável pela troca e pelo registro da credencial. */
    public FacebookTokenRenewalScheduler(FacebookTokenRenewalService renewalService) {
        this.renewalService = renewalService;
    }

    /** Confere as contas elegíveis em fila exclusiva, sem alterar o limite operacional vigente. */
    @Scheduled(fixedDelayString = "${facebook.token-renewal.scheduler.delay:21600000}",
            scheduler = "facebookTokenRenewalTaskScheduler")
    public void scheduleRenewal() {
        renewalService.renewTokensIfNeeded();
    }
}
