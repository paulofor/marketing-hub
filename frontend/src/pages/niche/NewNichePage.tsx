import { useState } from "react";
import { useForm } from "react-hook-form";
import { useCreateNiche } from "../../api/niche/useCreateNiche";
import { useChatDialogs } from "../../api/chatDialog/useChatDialogs";
import PageTitle from "../../components/PageTitle";

export default function NewNichePage() {
  const create = useCreateNiche();
  const { data: chatDialogs } = useChatDialogs();
  const { handleSubmit } = useForm();
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
    chatDialogId: undefined as number | undefined,
    hypothesesToGenerate: 0,
    audiencesToGenerate: 0,
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
      <label className="form-label">Chat GPT Dialog</label>
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
        <option value="">Nenhum</option>
        {chatDialogs?.map((d) => (
          <option key={d.id} value={d.id}>
            {d.theme}
          </option>
        ))}
      </select>
      <input
        type="number"
        className="form-control mb-2"
        placeholder="Qtd. de hipóteses para gerar"
        value={form.hypothesesToGenerate}
        title="Quantidade de hipóteses que o Worker IA irá gerar"
        onChange={(e) =>
          setForm({ ...form, hypothesesToGenerate: Number(e.target.value) })
        }
      />
      <input
        type="number"
        className="form-control mb-2"
        placeholder="Qtd. de públicos para gerar"
        value={form.audiencesToGenerate}
        title="Quantidade de públicos que o Worker IA irá gerar"
        onChange={(e) =>
          setForm({ ...form, audiencesToGenerate: Number(e.target.value) })
        }
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
        onClick={handleSubmit(
          () => submit(),
          (errors) => {
            console.log("Validation errors", errors);
          },
        )}
      >
        Salvar
      </button>
    </div>
  );
}
