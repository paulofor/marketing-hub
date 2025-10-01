package com.marketinghub.facebookads.web;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.FacebookPageRepository;
import com.marketinghub.ads.FacebookWorkerValidationError;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
        addMessageIfMissing(messages, workerAccount.getAccessToken(), FacebookWorkerValidationError.ACCESS_TOKEN_MISSING);
        addMessageIfMissing(messages, workerAccount.getAdAccountId(), FacebookWorkerValidationError.AD_ACCOUNT_ID_MISSING);
        addMessageIfMissing(
            messages,
            workerAccount.getDefaultWebsiteUrl(),
            FacebookWorkerValidationError.DEFAULT_WEBSITE_URL_MISSING
        );
        addMessageIfMissing(
            messages,
            workerAccount.getAdSetDailyBudget(),
            FacebookWorkerValidationError.AD_SET_DAILY_BUDGET_MISSING
        );
        addMessageIfMissing(
            messages,
            workerAccount.getAdSetBillingEvent(),
            FacebookWorkerValidationError.AD_SET_BILLING_EVENT_MISSING
        );
        addMessageIfMissing(
            messages,
            workerAccount.getAdSetOptimizationGoal(),
            FacebookWorkerValidationError.AD_SET_OPTIMIZATION_GOAL_MISSING
        );
        addMessageIfMissing(
            messages,
            workerAccount.getAdSetDestinationType(),
            FacebookWorkerValidationError.AD_SET_DESTINATION_TYPE_MISSING
        );
        addMessageIfMissing(
            messages,
            workerAccount.getAdSetTargetCountry(),
            FacebookWorkerValidationError.TARGET_COUNTRY_MISSING
        );

        appendRecordedWorkerValidation(messages, workerAccount);

        boolean ready = messages.isEmpty();
        return new FacebookConfigurationStatus.WorkerDiagnostics(
            true,
            ready,
            workerAccount.getId(),
            workerAccount.getName(),
            messages
        );
    }

    private void appendRecordedWorkerValidation(
        List<FacebookConfigurationStatus.DiagnosticMessage> messages,
        FacebookAccount workerAccount
    ) {
        String code = workerAccount.getWorkerLastValidationErrorCode();
        if (!StringUtils.hasText(code)) {
            return;
        }
        FacebookWorkerValidationError error = FacebookWorkerValidationError.fromCode(code);
        if (error == null) {
            return;
        }
        String userMessage = error.userMessage();
        if (!StringUtils.hasText(userMessage)) {
            userMessage = workerAccount.getWorkerLastValidationErrorDetail();
        }
        String formattedTimestamp = null;
        if (workerAccount.getWorkerLastValidationAt() != null) {
            formattedTimestamp = workerAccount
                .getWorkerLastValidationAt()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR")));
        }
        StringBuilder messageBuilder = new StringBuilder();
        if (formattedTimestamp != null) {
            messageBuilder.append("Última validação do worker em ").append(formattedTimestamp).append(": ");
        } else {
            messageBuilder.append("Última validação do worker falhou: ");
        }
        messageBuilder.append(userMessage);
        messages.add(
            0,
            new FacebookConfigurationStatus.DiagnosticMessage(code + "_RECORDED", messageBuilder.toString())
        );
    }

    private void addMessageIfMissing(
        List<FacebookConfigurationStatus.DiagnosticMessage> messages,
        String value,
        FacebookWorkerValidationError error
    ) {
        if (StringUtils.hasText(value)) {
            return;
        }
        messages.add(new FacebookConfigurationStatus.DiagnosticMessage(error.code(), error.userMessage()));
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
