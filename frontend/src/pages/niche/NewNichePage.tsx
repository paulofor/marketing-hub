import { useState } from "react";
import { useForm } from "react-hook-form";
import { useCreateNiche } from "../../api/niche/useCreateNiche";
import { useChatDialogs } from "../../api/chatDialog/useChatDialogs";
import PageTitle from "../../components/PageTitle";
import nicheIcon from "../../assets/icons/niche-icon.svg";

export default function NewNichePage() {
  const create = useCreateNiche();
  const { data: chatDialogs } = useChatDialogs();
  const { handleSubmit } = useForm();
  const [nameOnly, setNameOnly] = useState(false);
  const [form, setForm] = useState({
    name: "",
    description: "",
    interestCategory: "",
    roleCategory: "",
    demandVolume: "",
    promises: "",
    offers: "",
    baseSegmentation: "",
    interests: "",
    demographicFilters: "",
    extraTips: "",
    chatDialogId: undefined as number | undefined,
    hypothesesToGenerate: 0,
    interestsToGenerate: 0,
    jobTitlesToGenerate: 0,
    behaviorsToGenerate: 0,
  });

  const submit = () => {
    const payload = nameOnly ? { name: form.name } : form;
    create.mutate(payload);
  };

  return (
    <div>
      <PageTitle icon={nicheIcon}>Novo Nicho de Mercado</PageTitle>
      <label className="form-label" htmlFor="niche-name">
        Nome *
      </label>
      <input
        id="niche-name"
        className="form-control mb-2"
        placeholder="Nome"
        value={form.name}
        onChange={(e) => setForm({ ...form, name: e.target.value })}
      />
      <div className="form-check mb-3">
        <input
          id="niche-name-only"
          className="form-check-input"
          type="checkbox"
          checked={nameOnly}
          onChange={(e) => setNameOnly(e.target.checked)}
          disabled={create.isPending}
        />
        <label className="form-check-label" htmlFor="niche-name-only">
          Cadastrar somente com nome
        </label>
      </div>
      {!nameOnly ? (
        <>
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
          <input
            className="form-control mb-2"
            placeholder="Categoria de interesse"
            value={form.interestCategory}
            onChange={(e) =>
              setForm({ ...form, interestCategory: e.target.value })
            }
          />
          <input
            className="form-control mb-2"
            placeholder="Categoria de cargo"
            value={form.roleCategory}
            onChange={(e) => setForm({ ...form, roleCategory: e.target.value })}
          />
          <label className="form-label">Chat GPT Dialog</label>
          <select
            className="form-select mb-2"
            value={form.chatDialogId ?? ""}
            onChange={(e) =>
              setForm({
                ...form,
                chatDialogId: e.target.value
                  ? Number(e.target.value)
                  : undefined,
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
          <div className="row g-2 mb-2">
            <div className="col-12 col-md-4">
              <input
                type="number"
                className="form-control"
                placeholder="Qtd. de interesses para gerar"
                value={form.interestsToGenerate}
                title="Quantidade de interesses que o Worker IA irá gerar"
                onChange={(e) =>
                  setForm({
                    ...form,
                    interestsToGenerate: Number(e.target.value),
                  })
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <input
                type="number"
                className="form-control"
                placeholder="Qtd. de cargos para gerar"
                value={form.jobTitlesToGenerate}
                title="Quantidade de cargos que o Worker IA irá gerar"
                onChange={(e) =>
                  setForm({
                    ...form,
                    jobTitlesToGenerate: Number(e.target.value),
                  })
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <input
                type="number"
                className="form-control"
                placeholder="Qtd. de comportamentos para gerar"
                value={form.behaviorsToGenerate}
                title="Quantidade de comportamentos que o Worker IA irá gerar"
                onChange={(e) =>
                  setForm({
                    ...form,
                    behaviorsToGenerate: Number(e.target.value),
                  })
                }
              />
            </div>
          </div>
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
        </>
      ) : null}
      <button
        className="btn btn-primary"
        onClick={handleSubmit(
          () => submit(),
          (errors) => {
            console.log("Validation errors", errors);
          },
        )}
        disabled={create.isPending || !form.name.trim()}
      >
        {create.isPending ? (
          <>
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
            Salvando...
          </>
        ) : (
          "Salvar"
        )}
      </button>
    </div>
  );
}
