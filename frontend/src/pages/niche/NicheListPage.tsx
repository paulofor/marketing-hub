import { useState } from "react";
import { Link } from "react-router-dom";
import { useNiches } from "../../api/niche/useNiches";
import { useCreateNiche } from "../../api/niche/useCreateNiche";
import { useUpdateNiche } from "../../api/niche/useUpdateNiche";
import PageTitle from "../../components/PageTitle";

export default function NicheListPage() {
  const { data, isLoading } = useNiches();
  const create = useCreateNiche();
  const update = useUpdateNiche();
  const [form, setForm] = useState({
    id: 0,
    name: "",
    description: "",
    demandVolume: "",
    promises: "",
    offers: "",
  });
  const [editing, setEditing] = useState<number | null>(null);

  if (isLoading) return <p>Carregando...</p>;

  const submit = () => {
    if (editing) {
      update.mutate(form);
    } else {
      const { id, ...payload } = form;
      create.mutate(payload);
    }
    setForm({ id: 0, name: "", description: "", demandVolume: "", promises: "", offers: "" });
    setEditing(null);
  };
  return (
    <div>
      <PageTitle>Nichos de Mercado</PageTitle>
      <Link className="btn btn-primary mb-3" to="/niches/new">
        Novo Nicho
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((n) => (
            <tr key={n.id}>
              <td>{n.id}</td>
              <td>{n.name}</td>
              <td>
                <button
                  className="btn btn-sm btn-outline-primary"
                  onClick={() => {
                    setForm(n);
                    setEditing(n.id);
                  }}
                >
                  Editar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="row g-2 mt-2">
        <div className="col-md-3">
          <input
            className="form-control"
            placeholder="nome"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </div>
        <div className="col-md-3">
          <textarea
            className="form-control"
            placeholder="descrição"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            rows={1}
          />
        </div>
        <div className="col-md-2">
          <textarea
            className="form-control"
            placeholder="volume"
            value={form.demandVolume}
            onChange={(e) => setForm({ ...form, demandVolume: e.target.value })}
            rows={1}
          />
        </div>
        <div className="col-md-2">
          <textarea
            className="form-control"
            placeholder="promessas"
            value={form.promises}
            onChange={(e) => setForm({ ...form, promises: e.target.value })}
            rows={1}
          />
        </div>
        <div className="col-md-2">
          <textarea
            className="form-control"
            placeholder="ofertas"
            value={form.offers}
            onChange={(e) => setForm({ ...form, offers: e.target.value })}
            rows={1}
          />
        </div>
        <div className="col-md-1">
          <button className="btn btn-primary w-100" onClick={submit}>
            {editing ? "Atualizar" : "Criar"}
          </button>
        </div>
      </div>
    </div>
  );
}
