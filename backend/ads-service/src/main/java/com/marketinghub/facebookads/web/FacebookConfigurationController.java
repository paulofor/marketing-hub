package com.marketinghub.facebookads.web;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.FacebookPageRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/facebook")
public class FacebookConfigurationController {
    private final FacebookPageRepository pageRepository;
    private final FacebookAccountRepository accountRepository;

    public FacebookConfigurationController(
        FacebookPageRepository pageRepository,
        FacebookAccountRepository accountRepository
    ) {
        this.pageRepository = pageRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/configuration-status")
    public FacebookConfigurationStatus configurationStatus() {
        boolean hasConfiguredPages = pageRepository.count() > 0;
        List<FacebookAccount> accounts = accountRepository.findAll();
        FacebookConfigurationStatus.WorkerDiagnostics worker = buildWorkerDiagnostics(accounts);
        FacebookConfigurationStatus.TokenRenewalDiagnostics tokenRenewal = buildTokenRenewalDiagnostics(accounts);
        return new FacebookConfigurationStatus(hasConfiguredPages, worker, tokenRenewal);
    }

    private FacebookConfigurationStatus.WorkerDiagnostics buildWorkerDiagnostics(List<FacebookAccount> accounts) {
        FacebookAccount workerAccount = accounts
            .stream()
            .filter(FacebookAccount::isWorkerEnabled)
            .findFirst()
            .orElse(null);

        if (workerAccount == null) {
            List<FacebookConfigurationStatus.DiagnosticMessage> messages = List.of(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "NO_WORKER_ACCOUNT",
                    "Nenhuma conta está habilitada no Facebook Ads Worker. Acesse Contas do Facebook e marque a opção \"Utilizar esta conta no worker\"."
                )
            );
            return new FacebookConfigurationStatus.WorkerDiagnostics(false, false, null, null, messages);
        }

        List<FacebookConfigurationStatus.DiagnosticMessage> messages = new ArrayList<>();
        if (!StringUtils.hasText(workerAccount.getAccessToken())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "ACCESS_TOKEN_MISSING",
                    "Informe um token de acesso válido na conta selecionada para o worker."
                )
            );
        }
        if (!StringUtils.hasText(workerAccount.getAdAccountId())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "AD_ACCOUNT_ID_MISSING",
                    "Preencha o ID da conta de anúncios (act_...) na conta ativa do worker."
                )
            );
        }
        if (!StringUtils.hasText(workerAccount.getDefaultWebsiteUrl())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "DEFAULT_WEBSITE_URL_MISSING",
                    "Informe a URL padrão do site para que o worker possa criar anúncios."
                )
            );
        }
        if (!StringUtils.hasText(workerAccount.getAdSetDailyBudget())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "AD_SET_DAILY_BUDGET_MISSING",
                    "Defina o orçamento diário do conjunto de anúncios em centavos."
                )
            );
        }
        if (!StringUtils.hasText(workerAccount.getAdSetBillingEvent())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "AD_SET_BILLING_EVENT_MISSING",
                    "Informe o evento de cobrança padrão (por exemplo, IMPRESSIONS)."
                )
            );
        }
        if (!StringUtils.hasText(workerAccount.getAdSetOptimizationGoal())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "AD_SET_OPTIMIZATION_GOAL_MISSING",
                    "Defina o objetivo de otimização do conjunto (por exemplo, LINK_CLICKS)."
                )
            );
        }
        if (!StringUtils.hasText(workerAccount.getAdSetDestinationType())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "AD_SET_DESTINATION_TYPE_MISSING",
                    "Informe o tipo de destino padrão (WEBSITE, APP, MESSENGER...)."
                )
            );
        }
        if (!StringUtils.hasText(workerAccount.getAdSetTargetCountry())) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "AD_SET_TARGET_COUNTRY_MISSING",
                    "Informe pelo menos um país de segmentação para o conjunto de anúncios."
                )
            );
        }

        boolean ready = messages.isEmpty();
        return new FacebookConfigurationStatus.WorkerDiagnostics(
            true,
            ready,
            workerAccount.getId(),
            workerAccount.getName(),
            messages
        );
    }

    private FacebookConfigurationStatus.TokenRenewalDiagnostics buildTokenRenewalDiagnostics(List<FacebookAccount> accounts) {
        List<FacebookAccount> enabledAccounts = accounts
            .stream()
            .filter(FacebookAccount::isTokenRenewalEnabled)
            .toList();

        List<FacebookConfigurationStatus.TokenRenewalAccountStatus> statuses = enabledAccounts
            .stream()
            .map(this::buildTokenRenewalAccountStatus)
            .toList();

        long eligibleCount = statuses
            .stream()
            .filter(FacebookConfigurationStatus.TokenRenewalAccountStatus::eligible)
            .count();

        return new FacebookConfigurationStatus.TokenRenewalDiagnostics(
            enabledAccounts.size(),
            (int) eligibleCount,
            statuses
        );
    }

    private FacebookConfigurationStatus.TokenRenewalAccountStatus buildTokenRenewalAccountStatus(
        FacebookAccount account
    ) {
        List<FacebookConfigurationStatus.DiagnosticMessage> messages = new ArrayList<>();

        boolean hasAccessToken = StringUtils.hasText(account.getAccessToken());
        if (!hasAccessToken) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "ACCESS_TOKEN_MISSING",
                    "Informe um token de acesso válido para permitir a renovação automática."
                )
            );
        }

        boolean hasAppId = StringUtils.hasText(account.getAppId());
        if (!hasAppId) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "APP_ID_MISSING",
                    "Cadastre o App ID do Facebook utilizado para gerar o token."
                )
            );
        }

        boolean hasAppSecret = account.hasAppSecret();
        if (!hasAppSecret) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "APP_SECRET_MISSING",
                    "Informe o App Secret para que possamos renovar o token automaticamente."
                )
            );
        }

        boolean renewalRequired = account.isTokenRenewalRequired();
        boolean eligible = hasAccessToken && hasAppId && hasAppSecret && renewalRequired;

        if (eligible) {
            messages.add(
                new FacebookConfigurationStatus.DiagnosticMessage(
                    "READY",
                    "Pronto para renovação automática. O worker atualizará este token quando necessário."
                )
            );
        } else if (hasAccessToken && hasAppId && hasAppSecret && !renewalRequired) {
            Long expiresInDays = account.getTokenExpiresInDays();
            String detail;
            if (expiresInDays == null) {
                detail = "O token atual não possui data de expiração informada. O worker aguardará até que haja necessidade de renovação.";
            } else if (expiresInDays > 1) {
                detail = String.format(
                    Locale.ROOT,
                    "Token válido por mais %d dias. A renovação automática acontecerá quando estiver próximo do vencimento.",
                    expiresInDays
                );
            } else if (expiresInDays == 1) {
                detail = "Token válido por mais 1 dia. A renovação automática será tentada na próxima execução.";
            } else if (expiresInDays == 0) {
                detail = "Token vence hoje. A renovação automática será tentada na próxima execução.";
            } else {
                detail = "Token já expirado. O worker tentará renovar na próxima execução.";
            }
            messages.add(new FacebookConfigurationStatus.DiagnosticMessage("WAITING_THRESHOLD", detail));
        }

        return new FacebookConfigurationStatus.TokenRenewalAccountStatus(
            account.getId(),
            account.getName(),
            eligible,
            messages
        );
    }

    public record FacebookConfigurationStatus(
        boolean hasConfiguredPages,
        WorkerDiagnostics worker,
        TokenRenewalDiagnostics tokenRenewal
    ) {
        public record WorkerDiagnostics(
            boolean hasAccount,
            boolean ready,
            Long accountId,
            String accountName,
            List<DiagnosticMessage> messages
        ) {}

        public record TokenRenewalDiagnostics(
            int enabledAccounts,
            int eligibleAccounts,
            List<TokenRenewalAccountStatus> accounts
        ) {}

        public record TokenRenewalAccountStatus(
            Long accountId,
            String accountName,
            boolean eligible,
            List<DiagnosticMessage> messages
        ) {}

        public record DiagnosticMessage(String code, String message) {}
    }
}
