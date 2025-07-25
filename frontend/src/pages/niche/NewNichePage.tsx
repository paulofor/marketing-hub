import { useState } from "react";
import { useCreateNiche } from "../../api/niche/useCreateNiche";
import PageTitle from "../../components/PageTitle";

export default function NewNichePage() {
  const create = useCreateNiche();
  const [form, setForm] = useState({
    name: "",
    description: "",
    demandVolume: "",
    promises: "",
    offers: "",
    baseSegmentation: "",
    interests: "",
    demographicFilters: "",
    extraTips: "",
  });

  const submit = () => {
    create.mutate(form);
  };

  return (
    <div>
      <PageTitle>Novo Nicho de Mercado</PageTitle>
      <input
        className="form-control mb-2"
        placeholder="Nome"
        value={form.name}
        onChange={(e) => setForm({ ...form, name: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Descrição"
        value={form.description}
        onChange={(e) => setForm({ ...form, description: e.target.value })}
        rows={3}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Volume de Demanda"
        value={form.demandVolume}
        onChange={(e) => setForm({ ...form, demandVolume: e.target.value })}
        rows={3}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Promessas"
        value={form.promises}
        onChange={(e) => setForm({ ...form, promises: e.target.value })}
        rows={3}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Ofertas"
        value={form.offers}
        onChange={(e) => setForm({ ...form, offers: e.target.value })}
        rows={3}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Segmentação-base (Brasil)"
        value={form.baseSegmentation}
        onChange={(e) =>
          setForm({ ...form, baseSegmentation: e.target.value })
        }
        rows={3}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Principais interesses / comportamentos"
        value={form.interests}
        onChange={(e) => setForm({ ...form, interests: e.target.value })}
        rows={3}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Filtros demográficos & cargos"
        value={form.demographicFilters}
        onChange={(e) =>
          setForm({ ...form, demographicFilters: e.target.value })
        }
        rows={3}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Dicas extras"
        value={form.extraTips}
        onChange={(e) => setForm({ ...form, extraTips: e.target.value })}
        rows={3}
      />
      <button className="btn btn-primary" onClick={submit}>
        Salvar
      </button>
    </div>
  );
}
