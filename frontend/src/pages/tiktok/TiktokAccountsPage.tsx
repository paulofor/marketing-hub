import { FormEvent, useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import { useTiktokAccounts } from "../../api/tiktok/useTiktokAccounts";
import type { TiktokAccount } from "../../api/tiktok/useTiktokAccounts";
import {
  TiktokAccountPayload,
  TiktokDiagnosticResponse,
  useCreateTiktokAccount,
  useDeleteTiktokAccount,
  useDiagnoseTiktokAccount,
  useUpdateTiktokAccount,
} from "../../api/tiktok/tiktokAccountMutations";

interface FormState {
  name: string;
  advertiserId: string;
  accessToken: string;
  appId: string;
  clientKey: string;
  appSecret: string;
  metricsEnabled: boolean;
  publicationEnabled: boolean;
}

const emptyForm: FormState = {
  name: "",
  advertiserId: "",
  accessToken: "",
  appId: "",
  clientKey: "",
  appSecret: "",
  metricsEnabled: false,
  publicationEnabled: false,
};

const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});

function formatDateTime(value?: string | null): string {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "-";
  return dateTimeFormatter.format(parsed);
}

function statusBadge(status?: string | null): { className: string; label: string } {
  if (status === "READY_FOR_METRICS") {
    return { className: "text-bg-success", label: "Pronta para métricas" };
  }
  if (status === "BLOCKED") {
    return { className: "text-bg-warning", label: "Bloqueada" };
  }
  return { className: "text-bg-secondary", label: "Pendente" };
}

function RequiredMark() {
  return (
    <span className="ms-1 text-danger">
      <span aria-hidden="true">*</span>
      <span className="visually-hidden">Campo obrigatório</span>
    </span>
  );
}

export default function TiktokAccountsPage() {
  const { data, isLoading, error } = useTiktokAccounts();
  const accounts = useMemo(() => (Array.isArray(data) ? data : []), [data]);
  const createMutation = useCreateTiktokAccount();
  const updateMutation = useUpdateTiktokAccount();
  const deleteMutation = useDeleteTiktokAccount();
  const diagnoseMutation = useDiagnoseTiktokAccount();

  const [form, setForm] = useState<FormState>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [diagnostic, setDiagnostic] = useState<TiktokDiagnosticResponse | null>(null);

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const deletingId = deleteMutation.isPending ? deleteMutation.variables ?? null : null;
  const diagnosingId = diagnoseMutation.isPending ? diagnoseMutation.variables ?? null : null;

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const buildPayload = (): TiktokAccountPayload => ({
    id: editingId ?? undefined,
    name: form.name.trim(),
    advertiserId: form.advertiserId.trim(),
    accessToken: form.accessToken.trim() || null,
    appId: form.appId.trim() || null,
    clientKey: form.clientKey.trim() || null,
    appSecret: form.appSecret.trim() || null,
    metricsEnabled: form.metricsEnabled,
    publicationEnabled: form.publicationEnabled,
  });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const payload = buildPayload();
    if (editingId) {
      updateMutation.mutate(payload, { onSuccess: resetForm });
      return;
    }
    createMutation.mutate(payload, { onSuccess: resetForm });
  };

  const handleEdit = (account: TiktokAccount) => {
    setEditingId(account.id);
    setForm({
      name: account.name ?? "",
      advertiserId: account.advertiserId ?? "",
      accessToken: "",
      appId: account.appId ?? "",
      clientKey: account.clientKey ?? "",
      appSecret: "",
      metricsEnabled: account.metricsEnabled,
      publicationEnabled: account.publicationEnabled,
    });
  };

  const handleDiagnose = (accountId: number) => {
    diagnoseMutation.mutate(accountId, {
      onSuccess: (response) => setDiagnostic(response),
    });
  };

  if (isLoading) {
    return <p>Carregando...</p>;
  }

  return (
    <div>
      <PageTitle>Contas TikTok Ads</PageTitle>

      <div className="alert alert-info" role="status">
        Integração em MVP: cadastre credenciais e valide pré-requisitos. A
        publicação automática fica bloqueada até existir OAuth completo, pixel,
        eventos e gate comercial.
      </div>

      {error ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar as contas TikTok Ads.
        </div>
      ) : null}

      {diagnostic ? (
        <div className="alert alert-secondary" role="status">
          <strong>Diagnóstico:</strong> {diagnostic.message}
          {diagnostic.blockers.length ? (
            <ul className="mb-0 mt-2">
              {diagnostic.blockers.map((blocker) => (
                <li key={blocker}>{blocker}</li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}

      <div className="table-responsive">
        <table className="table table-striped align-middle">
          <thead>
            <tr>
              <th scope="col">Conta</th>
              <th scope="col">Advertiser ID</th>
              <th scope="col">Token</th>
              <th scope="col">Métricas</th>
              <th scope="col">Publicação</th>
              <th scope="col">Diagnóstico</th>
              <th scope="col" className="text-end">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            {accounts.length === 0 ? (
              <tr>
                <td colSpan={7}>Nenhuma conta TikTok Ads cadastrada.</td>
              </tr>
            ) : (
              accounts.map((account) => {
                const badge = statusBadge(account.lastDiagnosticStatus);
                return (
                  <tr key={account.id}>
                    <td>{account.name}</td>
                    <td>{account.advertiserId}</td>
                    <td>{account.maskedAccessToken ?? "Não informado"}</td>
                    <td>{account.metricsEnabled ? "Ativa" : "Inativa"}</td>
                    <td>{account.publicationEnabled ? "Solicitada" : "Bloqueada"}</td>
                    <td>
                      <span className={`badge ${badge.className}`}>{badge.label}</span>
                      <div className="small text-muted">
                        {formatDateTime(account.lastDiagnosticAt)}
                      </div>
                    </td>
                    <td className="text-end">
                      <div className="btn-group" role="group" aria-label="Ações da conta">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-secondary"
                          disabled={diagnoseMutation.isPending && diagnosingId === account.id}
                          onClick={() => handleDiagnose(account.id)}
                        >
                          {diagnoseMutation.isPending && diagnosingId === account.id ? (
                            <>
                              <span
                                className="spinner-border spinner-border-sm me-1"
                                role="status"
                                aria-hidden="true"
                              />
                              Diagnosticando...
                            </>
                          ) : (
                            "Diagnóstico"
                          )}
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-primary"
                          onClick={() => handleEdit(account)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-danger"
                          disabled={deleteMutation.isPending && deletingId === account.id}
                          onClick={() => deleteMutation.mutate(account.id)}
                        >
                          {deleteMutation.isPending && deletingId === account.id ? (
                            <>
                              <span
                                className="spinner-border spinner-border-sm me-1"
                                role="status"
                                aria-hidden="true"
                              />
                              Excluindo...
                            </>
                          ) : (
                            "Excluir"
                          )}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <form className="card mt-4" onSubmit={handleSubmit}>
        <div className="card-body">
          <h2 className="h5 mb-3">
            {editingId ? "Editar conta TikTok Ads" : "Cadastrar conta TikTok Ads"}
          </h2>
          <div className="row g-3">
            <div className="col-md-6">
              <label className="form-label" htmlFor="tiktokName">
                Nome <RequiredMark />
              </label>
              <input
                id="tiktokName"
                className="form-control"
                value={form.name}
                onChange={(event) =>
                  setForm((current) => ({ ...current, name: event.target.value }))
                }
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="tiktokAdvertiserId">
                Advertiser ID <RequiredMark />
              </label>
              <input
                id="tiktokAdvertiserId"
                className="form-control"
                value={form.advertiserId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    advertiserId: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="tiktokAccessToken">
                Access token{editingId ? null : <RequiredMark />}
              </label>
              <input
                id="tiktokAccessToken"
                className="form-control"
                type="password"
                autoComplete="off"
                placeholder={editingId ? "Deixe em branco para manter o token atual" : ""}
                value={form.accessToken}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    accessToken: event.target.value,
                  }))
                }
                required={!editingId}
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="tiktokAppId">
                App ID
              </label>
              <input
                id="tiktokAppId"
                className="form-control"
                value={form.appId}
                onChange={(event) =>
                  setForm((current) => ({ ...current, appId: event.target.value }))
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="tiktokClientKey">
                Client key
              </label>
              <input
                id="tiktokClientKey"
                className="form-control"
                value={form.clientKey}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    clientKey: event.target.value,
                  }))
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="tiktokAppSecret">
                App secret
              </label>
              <input
                id="tiktokAppSecret"
                className="form-control"
                type="password"
                autoComplete="off"
                placeholder={editingId ? "Deixe em branco para manter o segredo atual" : ""}
                value={form.appSecret}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    appSecret: event.target.value,
                  }))
                }
              />
            </div>
            <div className="col-md-6">
              <div className="form-check form-switch">
                <input
                  id="tiktokMetricsEnabled"
                  className="form-check-input"
                  type="checkbox"
                  checked={form.metricsEnabled}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      metricsEnabled: event.target.checked,
                    }))
                  }
                />
                <label className="form-check-label" htmlFor="tiktokMetricsEnabled">
                  Liberar sincronização de métricas
                </label>
              </div>
            </div>
            <div className="col-md-6">
              <div className="form-check form-switch">
                <input
                  id="tiktokPublicationEnabled"
                  className="form-check-input"
                  type="checkbox"
                  checked={form.publicationEnabled}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      publicationEnabled: event.target.checked,
                    }))
                  }
                />
                <label className="form-check-label" htmlFor="tiktokPublicationEnabled">
                  Solicitar publicação automática futura
                </label>
              </div>
            </div>
          </div>
        </div>
        <div className="card-footer d-flex justify-content-between">
          {editingId ? (
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={resetForm}
              disabled={isSaving}
            >
              Cancelar
            </button>
          ) : (
            <span />
          )}
          <button type="submit" className="btn btn-primary" disabled={isSaving}>
            {isSaving ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-1"
                  role="status"
                  aria-hidden="true"
                />
                Salvando...
              </>
            ) : editingId ? (
              "Salvar alterações"
            ) : (
              "Cadastrar conta"
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
