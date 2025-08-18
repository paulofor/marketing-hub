import { useState } from "react";
import { useCreateNiche, CreateNiche } from "../../api/niche/useCreateNiche";
import PageTitle from "../../components/PageTitle";
import { useChatDialogs } from "../../api/chatDialog/useChatDialogs";

export default function NewNichePage() {
  const create = useCreateNiche();
  const [form, setForm] = useState<CreateNiche>({
    name: "",
    description: "",
    demandVolume: "",
    promises: "",
    offers: "",
    baseSegmentation: "",
    interests: "",
    demographicFilters: "",
    extraTips: "",
    chatDialogId: undefined,
  });
  const { data: dialogs } = useChatDialogs();

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
      <select
        className="form-select mb-2"
        value={form.chatDialogId ?? ""}
        onChange={(e) =>
          setForm({
            ...form,
            chatDialogId: e.target.value ? Number(e.target.value) : undefined,
          })
        }
      >
        <option value="">Diálogo do ChatGPT (opcional)</option>
        {dialogs?.map((d) => (
          <option key={d.id} value={d.id}>
            {d.theme || d.id}
          </option>
        ))}
      </select>
      <button className="btn btn-primary" onClick={submit}>
        Salvar
      </button>
    </div>
  );
}
