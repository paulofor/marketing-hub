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
  handle: string;
  code: string;
}

const emptyForm: FormState = {
  name: "",
  handle: "",
  code: "",
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
      handle: form.handle.trim(),
      code: form.code.trim(),
    };

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
      handle: account.handle ?? "",
      code: account.code ?? "",
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
              <th scope="col">Usuário (@)</th>
              <th scope="col">Código</th>
              <th scope="col" className="text-end">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((account) => (
              <tr key={account.id}>
                <td>{account.id}</td>
                <td>{account.name}</td>
                <td>{account.handle}</td>
                <td>{account.code}</td>
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
              <label className="form-label" htmlFor="instagramHandle">
                Usuário (@) <span className="text-danger">*</span>
              </label>
              <input
                id="instagramHandle"
                className="form-control"
                placeholder="ex.: @minhaempresa"
                value={form.handle}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    handle: event.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="instagramCode">
                Código <span className="text-danger">*</span>
              </label>
              <input
                id="instagramCode"
                className="form-control"
                placeholder="Código interno ou identificador"
                value={form.code}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    code: event.target.value,
                  }))
                }
                required
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
            <span className="text-muted">Preencha os dados obrigatórios para salvar.</span>
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
