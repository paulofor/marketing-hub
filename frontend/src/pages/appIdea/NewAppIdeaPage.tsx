import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Sparkles } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useCreateAppIdea } from "../../api/appIdea/useCreateAppIdea";

interface AppIdeaForm {
  name: string;
  niche: string;
  targetAudience?: string;
  problemToSolve?: string;
  valueProposition?: string;
  coreFeatures?: string;
  differentiator?: string;
  monetization?: string;
  goToMarket?: string;
  technologyStack?: string;
  model?: string;
  prompt?: string;
}

export default function NewAppIdeaPage() {
  const create = useCreateAppIdea();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AppIdeaForm>({
    defaultValues: {
      name: "",
      niche: "",
      targetAudience: "",
      problemToSolve: "",
      valueProposition: "",
      coreFeatures: "",
      differentiator: "",
      monetization: "",
      goToMarket: "",
      technologyStack: "",
      model: "",
      prompt: "",
    },
  });

  useEffect(() => {
    if (create.isSuccess) {
      reset();
    }
  }, [create.isSuccess, reset]);

  const onSubmit = handleSubmit(
    (values) => {
      create.mutate(values);
    },
    (validationErrors) => {
      console.log("Validation errors", validationErrors);
    },
  );

  const creationError =
    create.isError && create.error instanceof Error
      ? create.error.message
      : create.isError
      ? "Não foi possível salvar a ideia de aplicativo."
      : null;

  return (
    <div className="pb-4">
      <div className="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Nova Ideia de Aplicativo</PageTitle>
          <p className="text-secondary mb-0">
            Estruture um aplicativo como produto digital, mapeando dores, proposta de valor e monetização para o nicho escolhido.
          </p>
        </div>
        <Sparkles size={32} className="text-warning" aria-hidden="true" />
      </div>
      <form className="row g-4" onSubmit={onSubmit}>
        <div className="col-12 col-lg-6">
          <label className="form-label fw-semibold">Nome da ideia *</label>
          <input
            className={`form-control${errors.name ? " is-invalid" : ""}`}
            placeholder="Nome do aplicativo"
            {...register("name", { required: true })}
          />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label fw-semibold">Nicho *</label>
          <input
            className={`form-control${errors.niche ? " is-invalid" : ""}`}
            placeholder="Nicho prioritário"
            {...register("niche", { required: true })}
          />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label fw-semibold">Público-alvo</label>
          <input
            className="form-control"
            placeholder="Segmento atendido"
            {...register("targetAudience")}
          />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label fw-semibold">Stack tecnológica sugerida</label>
          <input
            className="form-control"
            placeholder="Tecnologias base"
            {...register("technologyStack")}
          />
        </div>
        <div className="col-12 col-xl-6">
          <label className="form-label fw-semibold">Problema principal</label>
          <textarea
            className="form-control"
            rows={4}
            placeholder="Resumo da dor explícita que o app resolve"
            {...register("problemToSolve")}
          />
        </div>
        <div className="col-12 col-xl-6">
          <label className="form-label fw-semibold">Proposta de valor</label>
          <textarea
            className="form-control"
            rows={4}
            placeholder="Transformação entregue pelo aplicativo"
            {...register("valueProposition")}
          />
        </div>
        <div className="col-12 col-xl-6">
          <label className="form-label fw-semibold">Funcionalidades-chave</label>
          <textarea
            className="form-control"
            rows={4}
            placeholder="Lista de funcionalidades obrigatórias"
            {...register("coreFeatures")}
          />
        </div>
        <div className="col-12 col-xl-6">
          <label className="form-label fw-semibold">Diferenciais competitivos</label>
          <textarea
            className="form-control"
            rows={4}
            placeholder="Por que o aplicativo se destaca"
            {...register("differentiator")}
          />
        </div>
        <div className="col-12 col-xl-6">
          <label className="form-label fw-semibold">Modelo de monetização</label>
          <textarea
            className="form-control"
            rows={4}
            placeholder="Assinaturas, planos, upsells ou vendas internas"
            {...register("monetization")}
          />
        </div>
        <div className="col-12 col-xl-6">
          <label className="form-label fw-semibold">Estratégia de go-to-market</label>
          <textarea
            className="form-control"
            rows={4}
            placeholder="Canais de aquisição, lançamento e retenção"
            {...register("goToMarket")}
          />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label fw-semibold">Modelo IA utilizado</label>
          <input
            className="form-control"
            placeholder="Ex: gpt-4o-mini, claude-3.5"
            {...register("model")}
          />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label fw-semibold">Prompt de geração</label>
          <textarea
            className="form-control"
            rows={3}
            placeholder="Prompt ou briefing usado para gerar a ideia"
            {...register("prompt")}
          />
        </div>
        {creationError ? (
          <div className="col-12">
            <div className="alert alert-danger mb-0" role="alert">
              {creationError}
            </div>
          </div>
        ) : null}
        {create.isSuccess ? (
          <div className="col-12">
            <div className="alert alert-success mb-0" role="alert">
              Ideia de aplicativo salva com sucesso!
            </div>
          </div>
        ) : null}
        <div className="col-12 d-flex justify-content-end">
          <button
            type="submit"
            className="btn btn-primary d-inline-flex align-items-center gap-2"
            disabled={create.isPending}
          >
            {create.isPending ? (
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
            ) : null}
            Salvar Ideia
          </button>
        </div>
      </form>
    </div>
  );
}
