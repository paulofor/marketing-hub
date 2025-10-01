import { useEffect, useMemo, useState } from "react";
import PageTitle from "../components/PageTitle";
import { useFacebookAccounts } from "../api/useFacebookAccounts";
import type { FacebookAccount as RemoteFacebookAccount } from "../api/useFacebookAccounts";
import {
  useCreateFacebookAccount,
  useDeleteFacebookAccount,
  useUpdateFacebookAccount,
  FacebookAccountPayload,
  FinancialStrategyPayload,
} from "../api/facebookAccountMutations";
import { useFacebookPages } from "../api/useFacebookPages";
import {
  useCreateFacebookPage,
  useDeleteFacebookPage,
  useUpdateFacebookPage,
} from "../api/facebookPageMutations";
import { useFacebookConfigurationStatus } from "../api/useFacebookConfigurationStatus";
import FacebookAutomationAlerts from "../components/FacebookAutomationAlerts";

interface FinancialStrategyFormState {
  dailyBudget: string;
  billingEvent: string;
  optimizationGoal: string;
  destinationType: string;
  bidStrategy: string;
  bidAmount: string;
}

interface AccountFormState {
  id?: number;
  name: string;
  authorizedUserId: string;
  authorizedUserName: string;
  authorizedUserEmail: string;
  appId: string;
  appSecret: string;
  appSecretKept: boolean;
  tokenRenewalEnabled: boolean;
  adAccountId: string;
  defaultWebsiteUrl: string;
  defaultCreativeMessageTemplate: string;
  defaultCallToActionType: string;
  financialStrategy: FinancialStrategyFormState;
  workerEnabled: boolean;
  clearAppSecret: boolean;
}

interface PageFormState {
  id?: number;
  pageId: string;
  name: string;
}

const BRAZILIAN_REAL = "BRL";
const BRAZIL = "BR";
const MASKED_APP_SECRET_PLACEHOLDER = "*****";

const emptyFinancialStrategyForm: FinancialStrategyFormState = {
  dailyBudget: "",
  billingEvent: "IMPRESSIONS",
  optimizationGoal: "LINK_CLICKS",
  destinationType: "WEBSITE",
  bidStrategy: "LOWEST_COST_WITHOUT_CAP",
  bidAmount: "",
};

const emptyAccountForm: AccountFormState = {
  name: "",
  authorizedUserId: "",
  authorizedUserName: "",
  authorizedUserEmail: "",
  appId: "",
  appSecret: "",
  appSecretKept: false,
  tokenRenewalEnabled: false,
  adAccountId: "",
  defaultWebsiteUrl: "",
  defaultCreativeMessageTemplate: "%s",
  defaultCallToActionType: "LEARN_MORE",
  financialStrategy: { ...emptyFinancialStrategyForm },
  workerEnabled: false,
  clearAppSecret: false,
};
const emptyPageForm: PageFormState = { pageId: "", name: "" };

const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});

function createEmptyAccountForm(): AccountFormState {
  return {
    ...emptyAccountForm,
    financialStrategy: { ...emptyFinancialStrategyForm },
  };
}

function RequiredMark() {
  return (
    <span className="ms-1 text-danger">
      <span aria-hidden="true">*</span>
      <span className="visually-hidden">Campo obrigatório</span>
    </span>
  );
}

function toFinancialStrategyForm(
  account?: RemoteFacebookAccount,
): FinancialStrategyFormState {
  const strategy = account?.financialStrategy;
  return {
    dailyBudget: strategy?.dailyBudget ?? account?.adSetDailyBudget ?? "",
    billingEvent:
      strategy?.billingEvent ??
      account?.adSetBillingEvent ??
      emptyFinancialStrategyForm.billingEvent,
    optimizationGoal:
      strategy?.optimizationGoal ??
      account?.adSetOptimizationGoal ??
      emptyFinancialStrategyForm.optimizationGoal,
    destinationType:
      strategy?.destinationType ??
      account?.adSetDestinationType ??
      emptyFinancialStrategyForm.destinationType,
    bidStrategy:
      strategy?.bidStrategy ??
      account?.adSetBidStrategy ??
      emptyFinancialStrategyForm.bidStrategy,
    bidAmount: strategy?.bidAmount ?? account?.adSetBidAmount ?? "",
  };
}

function formatDateTime(value?: string | null): string {
  if (!value) return "—";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "—";
  return dateTimeFormatter.format(parsed);
}

function describeTokenExpiration(account: RemoteFacebookAccount): string {
  if (!account.accessToken) {
    return "Token não informado";
  }
  if (!account.tokenExpiresAt) {
    return "Validade não informada";
  }
  if (account.tokenExpired) {
    if (typeof account.tokenExpiresInDays === "number") {
      const absoluteDays = Math.abs(account.tokenExpiresInDays);
      if (absoluteDays === 0) return "Expirou hoje";
      if (absoluteDays === 1) return "Expirou há 1 dia";
      return `Expirou há ${absoluteDays} dias`;
    }
    return "Token expirado";
  }
  if (typeof account.tokenExpiresInDays === "number") {
    if (account.tokenExpiresInDays <= 0) return "Expira hoje";
    if (account.tokenExpiresInDays === 1) return "Expira em 1 dia";
    return `Expira em ${account.tokenExpiresInDays} dias`;
  }
  return "Validade não informada";
}

function describeRenewalStatus(account: RemoteFacebookAccount): string {
  if (!account.tokenRenewalEnabled) {
    return "Renovação automática desativada";
  }
  const status = account.tokenRenewalStatus ?? "NEVER_ATTEMPTED";
  switch (status) {
    case "SUCCESS":
      return "Última renovação concluída";
    case "FAILED":
      return "Falha na última tentativa";
    case "NEVER_ATTEMPTED":
    default:
      return "Nunca tentamos renovar automaticamente";
  }
}

function formatRenewalStatusBadge(account: RemoteFacebookAccount): {
  className: string;
  label: string;
} {
  const status = account.tokenRenewalStatus ?? "NEVER_ATTEMPTED";
  if (!account.tokenRenewalEnabled) {
    return { className: "text-bg-secondary", label: "Automação inativa" };
  }
  if (status === "SUCCESS") {
    return { className: "text-bg-success", label: "Renovação em dia" };
  }
  if (status === "FAILED") {
    return { className: "text-bg-danger", label: "Erro ao renovar" };
  }
  return { className: "text-bg-info", label: "Aguardando primeira tentativa" };
}

export default function FacebookAccountsPage() {
  const { data, isLoading, error } = useFacebookAccounts();
  const accounts = useMemo(() => (Array.isArray(data) ? data : []), [data]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [accountForm, setAccountForm] = useState<AccountFormState>(createEmptyAccountForm);
  const [pageForm, setPageForm] = useState<PageFormState>({ ...emptyPageForm });
  const [deletingAccountId, setDeletingAccountId] = useState<number | null>(null);
  const [deletingPageId, setDeletingPageId] = useState<number | null>(null);

  const createAccountMutation = useCreateFacebookAccount();
  const updateAccountMutation = useUpdateFacebookAccount();
  const deleteAccountMutation = useDeleteFacebookAccount();

  const { data: pagesData, isLoading: pagesLoading } = useFacebookPages(
    selectedAccountId ?? undefined,
  );
  const pages = useMemo(() => (Array.isArray(pagesData) ? pagesData : []), [pagesData]);
  const accountsNeedingRenewal = useMemo(
    () =>
      accounts.filter(
        (account) => Boolean(account.requiresTokenRenewal) || !account.accessToken,
      ),
    [accounts],
  );

  const { data: configuration } = useFacebookConfigurationStatus();

  const accountBeingEdited = useMemo(
    () =>
      typeof accountForm.id === "number"
        ? accounts.find((account) => account.id === accountForm.id) ?? null
        : null,
    [accountForm.id, accounts],
  );

  const createPageMutation = useCreateFacebookPage(selectedAccountId ?? undefined);
  const updatePageMutation = useUpdateFacebookPage(selectedAccountId ?? undefined);
  const deletePageMutation = useDeleteFacebookPage(selectedAccountId ?? undefined);

  useEffect(() => {
    if (accounts.length === 0) {
      setSelectedAccountId(null);
      return;
    }
    if (!selectedAccountId || !accounts.some((account) => account.id === selectedAccountId)) {
      setSelectedAccountId(accounts[0].id);
    }
  }, [accounts, selectedAccountId]);

  useEffect(() => {
    setPageForm({ ...emptyPageForm });
  }, [selectedAccountId]);

  if (isLoading) return <p>Carregando...</p>;
  if (error) return <p>Erro ao carregar contas</p>;

  const isEditingAccount = typeof accountForm.id === "number";
  const isEditingPage = typeof pageForm.id === "number";

  const isAccountMutationPending =
    createAccountMutation.isPending || updateAccountMutation.isPending;
  const isPageMutationPending =
    createPageMutation.isPending || updatePageMutation.isPending;

  const submitAccount = () => {
    if (isAccountMutationPending) return;
    const financialStrategyPayload: FinancialStrategyPayload = {
      dailyBudget: accountForm.financialStrategy.dailyBudget.trim() || null,
      billingEvent: accountForm.financialStrategy.billingEvent.trim() || null,
      optimizationGoal:
        accountForm.financialStrategy.optimizationGoal.trim() || null,
      destinationType:
        accountForm.financialStrategy.destinationType.trim() || null,
      bidStrategy: accountForm.financialStrategy.bidStrategy.trim() || null,
      bidAmount: accountForm.financialStrategy.bidAmount.trim() || null,
      targetCountry: BRAZIL,
    };

    const payload: FacebookAccountPayload = {
      id: accountForm.id,
      name: accountForm.name,
      currency: BRAZILIAN_REAL,
      authorizedUserId: accountForm.authorizedUserId.trim() || null,
      authorizedUserName: accountForm.authorizedUserName.trim() || null,
      authorizedUserEmail: accountForm.authorizedUserEmail.trim() || null,
      appId: accountForm.appId.trim() || null,
      tokenRenewalEnabled: accountForm.tokenRenewalEnabled,
      adAccountId: accountForm.adAccountId.trim() || null,
      defaultWebsiteUrl: accountForm.defaultWebsiteUrl.trim() || null,
      defaultCreativeMessageTemplate:
        accountForm.defaultCreativeMessageTemplate.trim() || "%s",
      defaultCallToActionType:
        accountForm.defaultCallToActionType.trim() || "LEARN_MORE",
      financialStrategy: financialStrategyPayload,
      adSetDailyBudget: financialStrategyPayload.dailyBudget,
      adSetBillingEvent: financialStrategyPayload.billingEvent,
      adSetOptimizationGoal: financialStrategyPayload.optimizationGoal,
      adSetDestinationType: financialStrategyPayload.destinationType,
      adSetBidStrategy: financialStrategyPayload.bidStrategy,
      adSetBidAmount: financialStrategyPayload.bidAmount,
      adSetTargetCountry: financialStrategyPayload.targetCountry,
      workerEnabled: accountForm.workerEnabled,
    };
    if (accountForm.clearAppSecret) {
      payload.appSecret = null;
    } else if (!accountForm.appSecretKept) {
      const trimmedSecret = accountForm.appSecret.trim();
      if (trimmedSecret) {
        payload.appSecret = trimmedSecret;
      }
    }
    const mutation = isEditingAccount ? updateAccountMutation : createAccountMutation;
    mutation.mutate(payload, {
      onSuccess: () => {
        setAccountForm(createEmptyAccountForm());
      },
    });
  };

  const submitPage = () => {
    if (!selectedAccountId) return;
    if (isPageMutationPending) return;
    const payload: PageFormState = {
      id: pageForm.id,
      pageId: pageForm.pageId,
      name: pageForm.name,
    };
    const mutation = isEditingPage ? updatePageMutation : createPageMutation;
    mutation.mutate(payload, {
      onSuccess: () => {
        setPageForm({ ...emptyPageForm });
      },
    });
  };

  const handleDeleteAccount = (id: number) => {
    setDeletingAccountId(id);
    deleteAccountMutation.mutate(id, {
      onSettled: () => setDeletingAccountId(null),
    });
  };

  const handleDeletePage = (pageId: number) => {
    setDeletingPageId(pageId);
    deletePageMutation.mutate(pageId, {
      onSettled: () => setDeletingPageId(null),
    });
  };

  return (
    <div>
      <PageTitle>Contas do Facebook</PageTitle>
      <FacebookAutomationAlerts status={configuration} />
      <div className="alert alert-info" role="alert">
        <h2 className="h6 mb-2">Campos obrigatórios para o Facebook Ads Worker</h2>
        <p className="mb-2">
          Para liberar o worker, mantenha os seguintes campos preenchidos na conta ativa:
        </p>
        <ul className="mb-0 ps-3">
          <li>Token de acesso (gerado automaticamente pelo Marketing Hub)</li>
          <li>ID da conta de anúncios (act_)</li>
          <li>URL padrão do site</li>
          <li>Orçamento diário do conjunto</li>
          <li>Evento de cobrança</li>
          <li>Objetivo de otimização</li>
          <li>Tipo de destino</li>
          <li>País de destino do conjunto (BR)</li>
        </ul>
      </div>
      {accountsNeedingRenewal.length > 0 && (
        <div className="alert alert-warning" role="alert">
          <h2 className="h6 mb-2">Renovação de token necessária</h2>
          <p className="mb-2">
            Atualize o token de acesso de longa duração para evitar que as integrações com o
            Facebook Ads sejam interrompidas.
          </p>
          <ul className="mb-0 ps-3">
            {accountsNeedingRenewal.map((account) => (
              <li key={account.id}>
                <strong>{account.name}</strong>: {describeTokenExpiration(account)}
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="row g-4">
        <div className="col-12">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Contas conectadas</h2>
                <span className="badge text-bg-primary">{accounts.length} conta(s)</span>
              </div>
              <div className="table-responsive mb-3">
                <table className="table table-striped align-middle">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nome</th>
                      <th>Moeda</th>
                      <th>Status do token</th>
                      <th className="text-end">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {accounts.map((account) => {
                      const { id, name, currency } = account;
                      const isDeletingThisAccount =
                        deleteAccountMutation.isPending && deletingAccountId === id;
                      const rowClasses = [
                        selectedAccountId === id ? "table-primary" : "",
                        account.requiresTokenRenewal || !account.accessToken ? "table-warning" : "",
                      ]
                        .filter(Boolean)
                        .join(" ");
                      const badgeClass = !account.accessToken
                        ? "text-bg-danger"
                        : account.tokenExpired
                        ? "text-bg-danger"
                        : account.requiresTokenRenewal
                        ? "text-bg-warning"
                        : "text-bg-success";
                      const badgeLabel = !account.accessToken
                        ? "Token ausente"
                        : account.tokenExpired
                        ? "Token expirado"
                        : account.requiresTokenRenewal
                        ? "Renovar token"
                        : "Token válido";
                      const renewalBadge = formatRenewalStatusBadge(account);
                      return (
                        <tr key={id} className={rowClasses}>
                          <td>{id}</td>
                          <td>{name}</td>
                          <td>{currency}</td>
                          <td>
                            <div className="d-flex flex-column gap-1">
                              <span className={`badge ${badgeClass}`}>{badgeLabel}</span>
                              <small className="text-muted">
                                {describeTokenExpiration(account)}
                              </small>
                              <small className="text-muted">
                                Validade: {formatDateTime(account.tokenExpiresAt)}
                              </small>
                              <span className={`badge ${renewalBadge.className}`}>
                                {renewalBadge.label}
                              </span>
                              <small className="text-muted">
                                {describeRenewalStatus(account)}
                              </small>
                              {account.tokenRenewedAt && (
                                <small className="text-muted">
                                  Última renovação: {formatDateTime(account.tokenRenewedAt)}
                                </small>
                              )}
                              {account.tokenRenewalLastAttemptAt && (
                                <small className="text-muted">
                                  Última tentativa: {formatDateTime(account.tokenRenewalLastAttemptAt)}
                                </small>
                              )}
                              {account.tokenRenewalLastError && (
                                <small className="text-danger">
                                  Erro recente: {account.tokenRenewalLastError}
                                </small>
                              )}
                              {account.authorizedUserName && (
                                <small className="text-muted">
                                  Usuário autorizado: {account.authorizedUserName}
                                  {account.authorizedUserEmail
                                    ? ` (${account.authorizedUserEmail})`
                                    : ""}
                                </small>
                              )}
                              {account.authorizedUserId && (
                                <small className="text-muted">
                                  ID do usuário: {account.authorizedUserId}
                                </small>
                              )}
                            </div>
                          </td>
                          <td className="text-end d-flex justify-content-end gap-2">
                            <button
                              className="btn btn-sm btn-outline-primary"
                              onClick={() => {
                                const strategyForm = toFinancialStrategyForm(account);
                                setAccountForm({
                                  id,
                                  name,
                                  authorizedUserId: account.authorizedUserId ?? "",
                                  authorizedUserName: account.authorizedUserName ?? "",
                                  authorizedUserEmail: account.authorizedUserEmail ?? "",
                                  appId: account.appId ?? "",
                                  appSecret: "",
                                  appSecretKept: Boolean(account.hasAppSecret),
                                  tokenRenewalEnabled: Boolean(account.tokenRenewalEnabled),
                                  adAccountId: account.adAccountId ?? "",
                                  defaultWebsiteUrl: account.defaultWebsiteUrl ?? "",
                                  defaultCreativeMessageTemplate:
                                    account.defaultCreativeMessageTemplate ?? "%s",
                                  defaultCallToActionType:
                                    account.defaultCallToActionType ?? "LEARN_MORE",
                                  financialStrategy: strategyForm,
                                  workerEnabled: Boolean(account.workerEnabled),
                                  clearAppSecret: false,
                                });
                              }}
                              disabled={isDeletingThisAccount}
                            >
                              Editar
                            </button>
                            <button
                              className="btn btn-sm btn-outline-secondary"
                              onClick={() => setSelectedAccountId(id)}
                              disabled={isDeletingThisAccount}
                            >
                              Páginas
                            </button>
                            <button
                              className="btn btn-sm btn-outline-danger"
                              onClick={() => handleDeleteAccount(id)}
                              disabled={deleteAccountMutation.isPending}
                            >
                              {isDeletingThisAccount ? (
                                <>
                                  <span
                                    className="spinner-border spinner-border-sm me-2"
                                    role="status"
                                  />
                                  Excluindo...
                                </>
                              ) : (
                                "Excluir"
                              )}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label">
                    Nome da conta
                    <RequiredMark />
                  </label>
                  <input
                    className="form-control"
                    placeholder="Nome da conta"
                    value={accountForm.name}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        name: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12">
                  <div className="alert alert-info mb-0">
                    <h3 className="h6 mb-1">Token de acesso gerenciado automaticamente</h3>
                    <p className="mb-1">
                      O Marketing Hub gera e renova o token sempre que necessário.
                    </p>
                    {accountBeingEdited ? (
                      <>
                        <p className="mb-1">
                          Status: {describeTokenExpiration(accountBeingEdited)}
                        </p>
                        <p className="mb-0">
                          Validade atual: {formatDateTime(accountBeingEdited.tokenExpiresAt)}
                        </p>
                      </>
                    ) : (
                      <p className="mb-0">
                        Após salvar a conta exibiremos a validade do token gerado automaticamente.
                      </p>
                    )}
                  </div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">
                    ID do aplicativo (App ID)
                    <RequiredMark />
                  </label>
                  <input
                    className="form-control"
                    placeholder="ID do aplicativo registrado no Facebook for Developers"
                    value={accountForm.appId}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        appId: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">
                    Segredo do aplicativo (App Secret)
                    <RequiredMark />
                  </label>
                  <input
                    type="password"
                    className="form-control"
                    placeholder={
                      isEditingAccount && accountBeingEdited?.hasAppSecret
                        ? "Informe um novo segredo para atualizar"
                        : "Cole o segredo do aplicativo"
                    }
                    value={
                      accountForm.appSecretKept
                        ? MASKED_APP_SECRET_PLACEHOLDER
                        : accountForm.appSecret
                    }
                    onFocus={() => {
                      if (accountForm.appSecretKept) {
                        setAccountForm((current) => ({
                          ...current,
                          appSecret: "",
                          appSecretKept: false,
                          clearAppSecret: false,
                        }));
                      }
                    }}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        appSecret: event.target.value,
                        appSecretKept: false,
                        clearAppSecret: false,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                  {isEditingAccount && accountBeingEdited?.hasAppSecret && (
                    <div className="form-check mt-2">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id="clear-app-secret"
                        checked={accountForm.clearAppSecret}
                        onChange={(event) =>
                          setAccountForm((current) => {
                            const checked = event.target.checked;
                            if (checked) {
                              return {
                                ...current,
                                clearAppSecret: true,
                                appSecret: "",
                                appSecretKept: false,
                              };
                            }
                            const shouldRestoreMask =
                              Boolean(accountBeingEdited?.hasAppSecret) &&
                              !current.appSecret;
                            return {
                              ...current,
                              clearAppSecret: false,
                              appSecret: shouldRestoreMask
                                ? ""
                                : current.appSecret,
                              appSecretKept: shouldRestoreMask
                                ? true
                                : current.appSecretKept,
                            };
                          })
                        }
                        disabled={isAccountMutationPending}
                      />
                      <label className="form-check-label" htmlFor="clear-app-secret">
                        Remover segredo salvo
                      </label>
                    </div>
                  )}
                  <div className="form-text">
                    Guardamos o segredo apenas para renovar tokens automaticamente. Ele não é exibido novamente.
                  </div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Renovação automática</label>
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="token-renewal-enabled"
                      checked={accountForm.tokenRenewalEnabled}
                      onChange={(event) =>
                        setAccountForm((current) => ({
                          ...current,
                          tokenRenewalEnabled: event.target.checked,
                        }))
                      }
                      disabled={isAccountMutationPending}
                    />
                    <label className="form-check-label" htmlFor="token-renewal-enabled">
                      Permitir que o Marketing Hub renove o token automaticamente
                    </label>
                  </div>
                  <div className="form-text">
                    Mantenha o App ID e o App Secret atualizados para que o worker consiga solicitar um novo token antes do
                    vencimento.
                  </div>
                </div>
                <div className="col-12">
                  <div className="form-check form-switch">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="worker-enabled"
                      checked={accountForm.workerEnabled}
                      onChange={(event) =>
                        setAccountForm((current) => ({
                          ...current,
                          workerEnabled: event.target.checked,
                        }))
                      }
                      disabled={isAccountMutationPending}
                    />
                    <label className="form-check-label" htmlFor="worker-enabled">
                      Utilizar esta conta no Facebook Ads Worker
                    </label>
                  </div>
                  <div className="form-text">
                    Apenas uma conta pode estar ativa no worker. Ao marcar esta opção, as demais contas serão desativadas.
                  </div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">
                    ID da conta de anúncios (act_)
                    <RequiredMark />
                  </label>
                  <input
                    className="form-control"
                    placeholder="Ex.: 123456789012345"
                    value={accountForm.adAccountId}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        adAccountId: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">
                    URL padrão do site
                    <RequiredMark />
                  </label>
                  <input
                    type="url"
                    className="form-control"
                    placeholder="https://www.exemplo.com/landing-page"
                    value={accountForm.defaultWebsiteUrl}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        defaultWebsiteUrl: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Template da mensagem do criativo</label>
                  <input
                    className="form-control"
                    placeholder="Use %s para inserir o nome do experimento"
                    value={accountForm.defaultCreativeMessageTemplate}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        defaultCreativeMessageTemplate: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Call to Action padrão</label>
                  <input
                    className="form-control"
                    placeholder="LEARN_MORE, SIGN_UP, APPLY_NOW..."
                    value={accountForm.defaultCallToActionType}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        defaultCallToActionType: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12">
                  <div className="border rounded p-3">
                    <h3 className="h6 mb-3">Estratégia financeira</h3>
                    <div className="row g-3">
                      <div className="col-12 col-md-6">
                        <label className="form-label">
                          Orçamento diário do conjunto (centavos)
                          <RequiredMark />
                        </label>
                        <input
                          className="form-control"
                          placeholder="Ex.: 2000 para R$ 20,00"
                          value={accountForm.financialStrategy.dailyBudget}
                          onChange={(event) =>
                            setAccountForm((current) => ({
                              ...current,
                              financialStrategy: {
                                ...current.financialStrategy,
                                dailyBudget: event.target.value,
                              },
                            }))
                          }
                          disabled={isAccountMutationPending}
                        />
                      </div>
                      <div className="col-12 col-md-6">
                        <label className="form-label">
                          Evento de cobrança
                          <RequiredMark />
                        </label>
                        <input
                          className="form-control"
                          placeholder="IMPRESSIONS, LINK_CLICKS..."
                          value={accountForm.financialStrategy.billingEvent}
                          onChange={(event) =>
                            setAccountForm((current) => ({
                              ...current,
                              financialStrategy: {
                                ...current.financialStrategy,
                                billingEvent: event.target.value,
                              },
                            }))
                          }
                          disabled={isAccountMutationPending}
                        />
                      </div>
                      <div className="col-12 col-md-6">
                        <label className="form-label">
                          Objetivo de otimização
                          <RequiredMark />
                        </label>
                        <input
                          className="form-control"
                          placeholder="LINK_CLICKS, REACH..."
                          value={accountForm.financialStrategy.optimizationGoal}
                          onChange={(event) =>
                            setAccountForm((current) => ({
                              ...current,
                              financialStrategy: {
                                ...current.financialStrategy,
                                optimizationGoal: event.target.value,
                              },
                            }))
                          }
                          disabled={isAccountMutationPending}
                        />
                      </div>
                      <div className="col-12 col-md-6">
                        <label className="form-label">
                          Tipo de destino
                          <RequiredMark />
                        </label>
                        <input
                          className="form-control"
                          placeholder="WEBSITE, APP, MESSENGER..."
                          value={accountForm.financialStrategy.destinationType}
                          onChange={(event) =>
                            setAccountForm((current) => ({
                              ...current,
                              financialStrategy: {
                                ...current.financialStrategy,
                                destinationType: event.target.value,
                              },
                            }))
                          }
                          disabled={isAccountMutationPending}
                        />
                      </div>
                      <div className="col-12 col-md-6">
                        <label className="form-label">Estratégia de lance</label>
                        <input
                          className="form-control"
                          placeholder="LOWEST_COST_WITHOUT_CAP, COST_CAP..."
                          value={accountForm.financialStrategy.bidStrategy}
                          onChange={(event) =>
                            setAccountForm((current) => ({
                              ...current,
                              financialStrategy: {
                                ...current.financialStrategy,
                                bidStrategy: event.target.value,
                              },
                            }))
                          }
                          disabled={isAccountMutationPending}
                        />
                      </div>
                      <div className="col-12 col-md-6">
                        <label className="form-label">Valor do lance (centavos, opcional)</label>
                        <input
                          className="form-control"
                          placeholder="Informe apenas quando usar estratégias com limite"
                          value={accountForm.financialStrategy.bidAmount}
                          onChange={(event) =>
                            setAccountForm((current) => ({
                              ...current,
                              financialStrategy: {
                                ...current.financialStrategy,
                                bidAmount: event.target.value,
                              },
                            }))
                          }
                          disabled={isAccountMutationPending}
                        />
                      </div>
                    </div>
                    <div className="form-text mt-2">
                      O país de destino é definido automaticamente como Brasil (BR).
                    </div>
                  </div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">
                    ID do usuário autorizado
                    <RequiredMark />
                  </label>
                  <input
                    className="form-control"
                    placeholder="ID do usuário do Facebook"
                    value={accountForm.authorizedUserId}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        authorizedUserId: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">
                    Nome do usuário autorizado
                    <RequiredMark />
                  </label>
                  <input
                    className="form-control"
                    placeholder="Nome completo"
                    value={accountForm.authorizedUserName}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        authorizedUserName: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">
                    E-mail do usuário autorizado
                    <RequiredMark />
                  </label>
                  <input
                    type="email"
                    className="form-control"
                    placeholder="email@exemplo.com"
                    value={accountForm.authorizedUserEmail}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        authorizedUserEmail: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12">
                  <button
                    className="btn btn-primary w-100"
                    onClick={submitAccount}
                    disabled={isAccountMutationPending}
                  >
                    {isAccountMutationPending ? (
                      <>
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          role="status"
                        />
                        {isEditingAccount ? "Atualizando conta..." : "Adicionando conta..."}
                      </>
                    ) : isEditingAccount ? (
                      "Atualizar conta"
                    ) : (
                      "Adicionar conta"
                    )}
                  </button>
                </div>
              </div>
              <p className="text-muted small mt-2 mb-0">
                A moeda das contas do Facebook é sempre o Real brasileiro (BRL).
              </p>
            </div>
          </div>
        </div>
        <div className="col-12">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Páginas vinculadas</h2>
                <span className="badge text-bg-primary">
                  {selectedAccountId ? pages.length : 0} página(s)
                </span>
              </div>
              {!selectedAccountId ? (
                <p className="text-muted mb-0">
                  Cadastre ou selecione uma conta para configurar as páginas utilizadas nas
                  campanhas.
                </p>
              ) : pagesLoading ? (
                <p>Carregando páginas...</p>
              ) : (
                <div className="table-responsive mb-3">
                  <table className="table table-hover align-middle">
                    <thead>
                      <tr>
                        <th>ID da página</th>
                        <th>Nome</th>
                        <th className="text-end">Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pages.map((page) => (
                        <tr key={page.id}>
                          <td>{page.pageId}</td>
                          <td>{page.name}</td>
                          <td className="text-end d-flex justify-content-end gap-2">
                            {(() => {
                              const isDeletingThisPage =
                                deletePageMutation.isPending && deletingPageId === page.id;
                              return (
                                <>
                                  <button
                                    className="btn btn-sm btn-outline-primary"
                                    onClick={() =>
                                      setPageForm({
                                        id: page.id,
                                        pageId: page.pageId,
                                        name: page.name,
                                      })
                                    }
                                    disabled={isDeletingThisPage}
                                  >
                                    Editar
                                  </button>
                                  <button
                                    className="btn btn-sm btn-outline-danger"
                                    onClick={() => handleDeletePage(page.id)}
                                    disabled={deletePageMutation.isPending}
                                  >
                                    {isDeletingThisPage ? (
                                      <>
                                        <span
                                          className="spinner-border spinner-border-sm me-2"
                                          role="status"
                                        />
                                        Excluindo...
                                      </>
                                    ) : (
                                      "Excluir"
                                    )}
                                  </button>
                                </>
                              );
                            })()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <div className="row g-2">
                <div className="col-md-5">
                  <input
                    className="form-control"
                    placeholder="ID da página"
                    value={pageForm.pageId}
                    onChange={(event) =>
                      setPageForm((current) => ({
                        ...current,
                        pageId: event.target.value,
                      }))
                    }
                    disabled={!selectedAccountId}
                  />
                </div>
                <div className="col-md-4">
                  <input
                    className="form-control"
                    placeholder="Nome da página"
                    value={pageForm.name}
                    onChange={(event) =>
                      setPageForm((current) => ({
                        ...current,
                        name: event.target.value,
                      }))
                    }
                    disabled={!selectedAccountId}
                  />
                </div>
                <div className="col-md-3">
                  <button
                    className="btn btn-primary w-100"
                    onClick={submitPage}
                    disabled={!selectedAccountId || isPageMutationPending}
                  >
                    {isPageMutationPending ? (
                      <>
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          role="status"
                        />
                        {isEditingPage ? "Atualizando página..." : "Adicionando página..."}
                      </>
                    ) : isEditingPage ? (
                      "Atualizar página"
                    ) : (
                      "Adicionar página"
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
