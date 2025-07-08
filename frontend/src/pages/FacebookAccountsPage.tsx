import { useState } from "react";
import { useFacebookAccounts } from "../api/useFacebookAccounts";
import {
  useCreateFacebookAccount,
  useUpdateFacebookAccount,
  useDeleteFacebookAccount,
} from "../api/facebookAccountMutations";
import PageTitle from "../components/PageTitle";

export default function FacebookAccountsPage() {
  const { data, isLoading, error } = useFacebookAccounts();
  const createMutation = useCreateFacebookAccount();
  const updateMutation = useUpdateFacebookAccount();
  const deleteMutation = useDeleteFacebookAccount();

  const [form, setForm] = useState({ id: "", name: "", currency: "" });
  const [editing, setEditing] = useState<boolean | string>(false);

  if (isLoading) return <p>Carregando...</p>;
  if (error) return <p>Erro ao carregar contas</p>;

  const submit = () => {
    if (editing) {
      updateMutation.mutate(form);
    } else {
      createMutation.mutate(form);
    }
    setForm({ id: "", name: "", currency: "" });
    setEditing(false);
  };

  return (
    <div>
      <PageTitle>Contas do Facebook</PageTitle>
      <table className="table table-striped">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>Moeda</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {data?.map(({ id, name, currency }) => (
            <tr key={id}>
              <td>{id}</td>
              <td>{name}</td>
              <td>{currency}</td>
              <td>
                <button
                  className="btn btn-sm btn-outline-primary me-2"
                  onClick={() => {
                    setForm({ id, name, currency });
                    setEditing(id);
                  }}
                >
                  Editar
                </button>
                <button
                  className="btn btn-sm btn-outline-danger"
                  onClick={() => deleteMutation.mutate(id)}
                >
                  Excluir
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row g-2">
        <div className="col-md-2">
          <input
            className="form-control"
            placeholder="id"
            value={form.id}
            onChange={(e) => setForm({ ...form, id: e.target.value })}
          />
        </div>
        <div className="col-md-4">
          <input
            className="form-control"
            placeholder="nome"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </div>
        <div className="col-md-2">
          <input
            className="form-control"
            placeholder="moeda"
            value={form.currency}
            onChange={(e) => setForm({ ...form, currency: e.target.value })}
          />
        </div>
        <div className="col-md-2">
          <button className="btn btn-primary w-100" onClick={submit}>
            {editing ? "Atualizar" : "Criar"}
          </button>
        </div>
      </div>
    </div>
  );
}
