import { FormEvent, useMemo, useState } from "react";
import { useInstagramAccounts } from "../api/useInstagramAccounts";
import {
  useCreateInstagramAccount,
  useUpdateInstagramAccount,
  useDeleteInstagramAccount,
  CreateInstagramAccountPayload,
} from "../api/instagramAccountMutations";
import PageTitle from "../components/PageTitle";

interface FormState {
  name: string;
  instagramUserId: string;
  facebookPageId: string;
  adAccountId: string;
  accessToken: string;
  avatarUrl: string;
}

const emptyForm: FormState = {
  name: "",
  instagramUserId: "",
  facebookPageId: "",
  adAccountId: "",
  accessToken: "",
  avatarUrl: "",
};

export default function InstagramAccountsPage() {
  const { data, isLoading, error } = useInstagramAccounts();
  const accounts = useMemo(
    () => (Array.isArray(data) ? data : []),
    [data],
  );
  const createMutation = useCreateInstagramAccount();
  const updateMutation = useUpdateInstagramAccount();
  const deleteMutation = useDeleteInstagramAccount();

  const [form, setForm] = useState<FormState>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const deletingId = deleteMutation.isPending ? deleteMutation.variables ?? null : null;

  if (isLoading) {
    return <p>Carregando...</p>;
  }

  const loadError = error ? (
    <div className="alert alert-danger" role="alert">
      Não foi possível carregar as contas do Instagram.
    </div>
  ) : null;

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const buildPayload = (): CreateInstagramAccountPayload => {
    const payload: CreateInstagramAccountPayload = {
      name: form.name.trim(),
      instagramUserId: form.instagramUserId.trim(),
      facebookPageId: form.facebookPageId.trim(),
      adAccountId: form.adAccountId.trim(),
    };

    if (form.avatarUrl.trim()) {
      payload.avatarUrl = form.avatarUrl.trim();
    }

    if (form.accessToken.trim()) {
      payload.accessToken = form.accessToken.trim();
    }

    return payload;
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const payload = buildPayload();

    if (editingId) {
      updateMutation.mutate(
        { id: editingId, ...payload },
        { onSuccess: resetForm },
      );
    } else {
      createMutation.mutate(payload, { onSuccess: resetForm });
    }
  };

  const handleEdit = (accountId: number) => {
    const account = accounts.find((item) => item.id === accountId);
    if (!account) {
      return;
    }

    setEditingId(account.id);
    setForm({
      name: account.name ?? "",
      instagramUserId: account.instagramUserId ?? "",
      facebookPageId: account.facebookPageId ?? "",
      adAccountId: account.adAccountId ?? "",
      avatarUrl: account.avatarUrl ?? "",
      accessToken: "",
    });
  };

  return (
    <div>
      <PageTitle>Contas do Instagram</PageTitle>
      {loadError}

      <div className="table-responsive">
        <table className="table table-striped align-middle">
          <thead>
            <tr>
              <th scope="col">ID</th>
              <th scope="col">Nome</th>
              <th scope="col">Instagram Business ID</th>
              <th scope="col">Página do Facebook</th>
              <th scope="col">Conta de Anúncios</th>
              <th scope="col">Token</th>
              <th scope="col" className="text-end">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((account) => (
              <tr key={account.id}>
                <td>{account.id}</td>
                <td>
                  <div className="d-flex align-items-center gap-2">
                    {account.avatarUrl && (
                      <img
                        src={account.avatarUrl}
                        alt={account.name}
                        className="rounded-circle"
                        width={32}
                        height={32}
                      />
                    )}
                    <span>{account.name}</span>
                  </div>
                </td>
                <td>{account.instagramUserId}</td>
                <td>{account.facebookPageId}</td>
                <td>{account.adAccountId}</td>
                <td>
                  {account.accessToken ? (
                    <span className="badge bg-success">Configurado</span>
                  ) : (
                    <span className="badge bg-warning text-dark">Pendente</span>
                  )}
                </td>
                <td className="text-end">
                  <div className="btn-group" role="group" aria-label="Ações">
                    <a
                      className="btn btn-sm btn-outline-secondary"
                      href={`/accounts/instagram/${account.id}/posts`}
                    >
                      Posts
                    </a>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-primary"
                      onClick={() => handleEdit(account.id)}
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
                          ></span>
                          Excluindo...
                        </>
                      ) : (
                        "Excluir"
                      )}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form className="card mt-4" onSubmit={handleSubmit}>
        <div className="card-body">
          <h2 className="h5 mb-3">
            {editingId ? "Editar conta do Instagram" : "Cadastrar nova conta"}
          </h2>
          <div className="row g-3">
            <div className="col-md-6">
              <label className="form-label" htmlFor="instagramName">
                Nome <span className="text-danger">*</span>
              </label>
              <input
                id="instagramName"
                className="form-control"
                placeholder="Nome público do perfil"
                value={form.name}
                onChange={(event) =>
                  setForm((current) => ({ ...current, name: event.target.value }))
                }
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="instagramUserId">
                Instagram Business ID <span className="text-danger">*</span>
              </label>
              <input
                id="instagramUserId"
                className="form-control"
                placeholder="ex.: 17841400000000000"
                value={form.instagramUserId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    instagramUserId: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="facebookPageId">
                Página do Facebook <span className="text-danger">*</span>
              </label>
              <input
                id="facebookPageId"
                className="form-control"
                placeholder="ID numérico da página conectada"
                value={form.facebookPageId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    facebookPageId: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="adAccountId">
                Conta de anúncios <span className="text-danger">*</span>
              </label>
              <input
                id="adAccountId"
                className="form-control"
                placeholder="ex.: act_0000000000"
                value={form.adAccountId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    adAccountId: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="accessToken">
                Token de acesso <span className="text-danger">*</span>
              </label>
              <input
                id="accessToken"
                className="form-control"
                placeholder="Use um token com permissões de leitura e publicação"
                value={form.accessToken}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    accessToken: event.target.value,
                  }))
                }
                required={!editingId}
              />
              {editingId ? (
                <small className="form-text text-muted">
                  Deixe em branco para manter o token atual.
                </small>
              ) : (
                <small className="form-text text-muted">
                  O token será armazenado com a moeda padrão em BRL.
                </small>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="avatarUrl">
                Avatar (opcional)
              </label>
              <input
                id="avatarUrl"
                className="form-control"
                placeholder="URL da foto do perfil"
                value={form.avatarUrl}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    avatarUrl: event.target.value,
                  }))
                }
              />
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
              Cancelar edição
            </button>
          ) : (
            <span className="text-muted">
              Todas as contas usam Real brasileiro (BRL) como moeda padrão.
            </span>
          )}
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isSaving}
          >
            {isSaving && (
              <span
                className="spinner-border spinner-border-sm me-2"
                role="status"
                aria-hidden="true"
              ></span>
            )}
            {editingId ? "Salvar alterações" : "Cadastrar conta"}
          </button>
        </div>
      </form>
    </div>
  );
}
