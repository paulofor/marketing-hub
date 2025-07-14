import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useUpdateNiche } from "../../api/niche/useUpdateNiche";
import { useNiche } from "../../api/niche/useNiche";
import PageTitle from "../../components/PageTitle";
import { MarketNiche } from "../../api/niche/useNiches";

export default function EditNichePage() {
  const { id } = useParams<{ id: string }>();
  const nicheId = Number(id);
  const { data, isLoading } = useNiche(nicheId);
  const update = useUpdateNiche();
  const navigate = useNavigate();
  const [form, setForm] = useState<MarketNiche>({
    id: nicheId,
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

  useEffect(() => {
    if (data) {
      setForm(data);
    }
  }, [data]);

  const submit = () => {
    update.mutate(form, {
      onSuccess: () => navigate("/niches"),
      onError: () => alert("Erro ao salvar Nicho"),
    });
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Editar Nicho</PageTitle>
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
        onChange={(e) => setForm({ ...form, baseSegmentation: e.target.value })}
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
      <button
        className="btn btn-primary"
        onClick={submit}
        disabled={update.isPending}
      >
        {update.isPending ? (
          <>
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
            />
            Processando...
          </>
        ) : (
          "Salvar"
        )}
      </button>
      {update.isError && (
        <div className="alert alert-danger mt-2">Erro ao salvar Nicho</div>
      )}
    </div>
  );
}
