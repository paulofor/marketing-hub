package com.marketinghub.tiktokadsworker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Aplica as regras operacionais de cadastro e diagnóstico de contas TikTok Ads. */
@Service
public class TiktokAccountService {
    private final TiktokAccountRepository repository;

    /** Inicializa o serviço com o repositório de contas TikTok. */
    public TiktokAccountService(TiktokAccountRepository repository) {
        this.repository = repository;
    }

    /** Lista as contas cadastradas com dados sensíveis mascarados. */
    public List<TiktokAccountResponse> listAccounts() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    /** Cria uma nova conta TikTok Ads. */
    public TiktokAccountResponse createAccount(TiktokAccountRequest request) {
        Instant now = Instant.now();
        TiktokAccount account = new TiktokAccount();
        applyRequest(account, request);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        account.setLastDiagnosticStatus("PENDING");
        account.setLastDiagnosticMessage("Diagnóstico ainda não executado.");
        return toResponse(repository.save(account));
    }

    /** Atualiza uma conta existente preservando segredos quando o formulário não reenviar novos valores. */
    public TiktokAccountResponse updateAccount(Long id, TiktokAccountRequest request) {
        TiktokAccount account = findRequired(id);
        String previousAccessToken = account.getAccessToken();
        String previousAppSecret = account.getAppSecret();
        applyRequest(account, request);
        if (!StringUtils.hasText(request.accessToken())) {
            account.setAccessToken(previousAccessToken);
        }
        if (!StringUtils.hasText(request.appSecret())) {
            account.setAppSecret(previousAppSecret);
        }
        account.setUpdatedAt(Instant.now());
        return toResponse(repository.save(account));
    }

    /** Remove uma conta TikTok Ads cadastrada. */
    public void deleteAccount(Long id) {
        findRequired(id);
        repository.deleteById(id);
    }

    /** Executa diagnóstico de pré-requisitos antes de liberar métricas ou publicação. */
    public TiktokDiagnosticResponse diagnoseAccount(Long id) {
        TiktokAccount account = findRequired(id);
        List<String> checks = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        checks.add("Conta encontrada no módulo TikTok Ads.");
        addRequiredCheck("Advertiser ID informado.", "Informe o advertiser ID da conta TikTok Ads.", account.getAdvertiserId(), checks, blockers);
        addRequiredCheck("Access token informado.", "Informe um access token válido antes de sincronizar métricas.", account.getAccessToken(), checks, blockers);
        addRequiredCheck("App ID informado.", "Informe o app ID para preparar OAuth e renovação futura.", account.getAppId(), checks, blockers);
        addRequiredCheck("Client key informada.", "Informe a client key do aplicativo TikTok.", account.getClientKey(), checks, blockers);
        addRequiredCheck("App secret informado.", "Informe o app secret para preparar OAuth e diagnóstico real.", account.getAppSecret(), checks, blockers);
        if (!account.isMetricsEnabled()) {
            blockers.add("Ative sincronização de métricas somente depois de validar token e eventos.");
        } else {
            checks.add("Sincronização de métricas habilitada para próxima fase.");
        }
        if (account.isPublicationEnabled()) {
            blockers.add("Publicação automática deve permanecer bloqueada até existir gate comercial e OAuth completo.");
        } else {
            checks.add("Publicação automática bloqueada, evitando gasto prematuro.");
        }

        Instant checkedAt = Instant.now();
        String status = blockers.isEmpty() ? "READY_FOR_METRICS" : "BLOCKED";
        String message = blockers.isEmpty()
                ? "Conta pronta para a próxima fase de sincronização de métricas."
                : "Conta ainda não está pronta para operar TikTok Ads.";
        account.setLastDiagnosticAt(checkedAt);
        account.setLastDiagnosticStatus(status);
        account.setLastDiagnosticMessage(message);
        repository.save(account);
        return new TiktokDiagnosticResponse(account.getId(), status, message, checks, blockers, checkedAt);
    }

    /** Aplica os campos editáveis do formulário na entidade de conta. */
    private void applyRequest(TiktokAccount account, TiktokAccountRequest request) {
        account.setName(request.name().trim());
        account.setAdvertiserId(request.advertiserId().trim());
        account.setAccessToken(trimToNull(request.accessToken()));
        account.setAppId(trimToNull(request.appId()));
        account.setClientKey(trimToNull(request.clientKey()));
        account.setAppSecret(trimToNull(request.appSecret()));
        account.setMetricsEnabled(Boolean.TRUE.equals(request.metricsEnabled()));
        account.setPublicationEnabled(Boolean.TRUE.equals(request.publicationEnabled()));
    }

    /** Registra sucesso ou bloqueio para um campo obrigatório. */
    private void addRequiredCheck(String success, String blocker, String value, List<String> checks, List<String> blockers) {
        if (StringUtils.hasText(value)) {
            checks.add(success);
            return;
        }
        blockers.add(blocker);
    }

    /** Busca uma conta existente ou falha quando o identificador é inválido. */
    private TiktokAccount findRequired(Long id) {
        return repository.findById(id).orElseThrow(() -> new TiktokAccountNotFoundException(id));
    }

    /** Converte texto vazio para nulo. */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /** Monta o contrato seguro de saída para o frontend. */
    private TiktokAccountResponse toResponse(TiktokAccount account) {
        return new TiktokAccountResponse(
                account.getId(),
                account.getName(),
                account.getAdvertiserId(),
                StringUtils.hasText(account.getAccessToken()),
                mask(account.getAccessToken()),
                account.getAppId(),
                account.getClientKey(),
                StringUtils.hasText(account.getAppSecret()),
                account.isMetricsEnabled(),
                account.isPublicationEnabled(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getLastDiagnosticAt(),
                account.getLastDiagnosticStatus(),
                account.getLastDiagnosticMessage());
    }

    /** Mascara tokens e segredos antes de retornar dados à tela. */
    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}
