import { useEffect, useMemo, useState } from "react";
import PageTitle from "../components/PageTitle";
import { useFacebookAccounts } from "../api/useFacebookAccounts";
import type { FacebookAccount as RemoteFacebookAccount } from "../api/useFacebookAccounts";
import {
  useCreateFacebookAccount,
  useDeleteFacebookAccount,
  useUpdateFacebookAccount,
  FacebookAccountPayload,
} from "../api/facebookAccountMutations";
import { useFacebookPages } from "../api/useFacebookPages";
import {
  useCreateFacebookPage,
  useDeleteFacebookPage,
  useUpdateFacebookPage,
} from "../api/facebookPageMutations";

interface AccountFormState {
  id?: number;
  name: string;
  accessToken: string;
  tokenExpiresAt: string;
  authorizedUserId: string;
  authorizedUserName: string;
  authorizedUserEmail: string;
  appId: string;
  businessManagerAppId: string;
  appSecret: string;
  tokenRenewalEnabled: boolean;
  clearAppSecret: boolean;
}

interface PageFormState {
  id?: number;
  pageId: string;
  name: string;
}

const BRAZILIAN_REAL = "BRL";

const emptyAccountForm: AccountFormState = {
  name: "",
  accessToken: "",
  tokenExpiresAt: "",
  authorizedUserId: "",
  authorizedUserName: "",
  authorizedUserEmail: "",
  appId: "",
  businessManagerAppId: "",
  appSecret: "",
  tokenRenewalEnabled: false,
  clearAppSecret: false,
};
const emptyPageForm: PageFormState = { pageId: "", name: "" };

const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});

function toInputDateValue(value?: string | null): string {
  if (!value) return "";
  return value.slice(0, 16);
}

function toBackendDateValue(value?: string): string | null {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
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
  const [accountForm, setAccountForm] = useState<AccountFormState>({ ...emptyAccountForm });
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
    const payload: FacebookAccountPayload = {
      id: accountForm.id,
      name: accountForm.name,
      currency: BRAZILIAN_REAL,
      accessToken: accountForm.accessToken.trim() || null,
      tokenExpiresAt: toBackendDateValue(accountForm.tokenExpiresAt),
      authorizedUserId: accountForm.authorizedUserId.trim() || null,
      authorizedUserName: accountForm.authorizedUserName.trim() || null,
      authorizedUserEmail: accountForm.authorizedUserEmail.trim() || null,
      appId: accountForm.appId.trim() || null,
      businessManagerAppId: accountForm.businessManagerAppId.trim() || null,
      tokenRenewalEnabled: accountForm.tokenRenewalEnabled,
    };
    if (accountForm.clearAppSecret) {
      payload.appSecret = null;
    } else if (accountForm.appSecret.trim()) {
      payload.appSecret = accountForm.appSecret.trim();
    }
    const mutation = isEditingAccount ? updateAccountMutation : createAccountMutation;
    mutation.mutate(payload, {
      onSuccess: () => {
        setAccountForm({ ...emptyAccountForm });
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
                                setAccountForm({
                                  id,
                                  name,
                                  accessToken: account.accessToken ?? "",
                                  tokenExpiresAt: toInputDateValue(account.tokenExpiresAt ?? undefined),
                                  authorizedUserId: account.authorizedUserId ?? "",
                                  authorizedUserName: account.authorizedUserName ?? "",
                                  authorizedUserEmail: account.authorizedUserEmail ?? "",
                                  appId: account.appId ?? "",
                                  businessManagerAppId:
                                    account.businessManagerAppId ?? "",
                                  appSecret: "",
                                  tokenRenewalEnabled: Boolean(account.tokenRenewalEnabled),
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
                  <label className="form-label">Nome da conta</label>
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
                  <label className="form-label">Token de acesso (longa duração)</label>
                  <textarea
                    className="form-control"
                    placeholder="Cole o token de acesso gerado no Business Manager"
                    rows={3}
                    value={accountForm.accessToken}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        accessToken: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                  <div className="form-text">
                    Utilize sempre um token de longa duração para que o Marketing Hub possa
                    renovar o acesso automaticamente.
                  </div>
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">ID do aplicativo (App ID)</label>
                  <input
                    className="form-control"
                    placeholder="ID do aplicativo vinculado ao Business Manager"
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
                  <label className="form-label">ID do aplicativo vinculado ao Business Manager</label>
                  <input
                    className="form-control"
                    placeholder="ID do aplicativo no Business Manager"
                    value={accountForm.businessManagerAppId}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        businessManagerAppId: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                </div>
                <div className="col-12 col-md-6">
                  <label className="form-label">Segredo do aplicativo (App Secret)</label>
                  <input
                    type="password"
                    className="form-control"
                    placeholder={
                      isEditingAccount && accounts.find((acc) => acc.id === accountForm.id)?.hasAppSecret
                        ? "Informe um novo segredo para atualizar"
                        : "Cole o segredo do aplicativo"
                    }
                    value={accountForm.appSecret}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        appSecret: event.target.value,
                        clearAppSecret: false,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                  {isEditingAccount &&
                    accounts.find((acc) => acc.id === accountForm.id)?.hasAppSecret && (
                      <div className="form-check mt-2">
                        <input
                          className="form-check-input"
                          type="checkbox"
                          id="clear-app-secret"
                          checked={accountForm.clearAppSecret}
                          onChange={(event) =>
                            setAccountForm((current) => ({
                              ...current,
                              clearAppSecret: event.target.checked,
                              appSecret: event.target.checked ? "" : current.appSecret,
                            }))
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
                  <label className="form-label">Validade do token</label>
                  <input
                    type="datetime-local"
                    className="form-control"
                    value={accountForm.tokenExpiresAt}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        tokenExpiresAt: event.target.value,
                      }))
                    }
                    disabled={isAccountMutationPending}
                  />
                  <div className="form-text">
                    Exibiremos um alerta 7 dias antes do vencimento.
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
                <div className="col-12 col-md-6">
                  <label className="form-label">ID do usuário autorizado</label>
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
                  <label className="form-label">Nome do usuário autorizado</label>
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
                  <label className="form-label">E-mail do usuário autorizado</label>
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
