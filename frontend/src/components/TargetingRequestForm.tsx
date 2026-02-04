import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import type { TargetingAudienceType } from "../api/targeting/types";
import { useCreateTargetingRequest } from "../api/targeting/useCreateTargetingRequest";
import type { TargetingRequestQueryFilters } from "../api/targeting/useTargetingRequests";

interface TargetingRequestFormProps {
  defaultDescricao?: string;
  defaultIdioma?: string;
  defaultPais?: string;
  defaultPublico?: TargetingAudienceType;
  className?: string;
  nicheId?: number;
  hypothesisId?: string;
  queryFilters?: TargetingRequestQueryFilters;
}

interface FormValues {
  descricao: string;
  idioma: string;
  pais: string;
  publico_tipo: TargetingAudienceType;
}

const MAX_LENGTH = 280;
const FORBIDDEN_TERMS = ["sexo", "armas", "violência", "ódio", "drogas"];

function hasPii(text: string): boolean {
  const emailRegex = /[\w.+-]+@[\w-]+\.[\w.-]+/i;
  const phoneRegex = /(\+?\d[\s-]?)?(\(?\d{2,3}\)?[\s-]?)?\d{4,5}[\s-]?\d{4}/;
  return emailRegex.test(text) || phoneRegex.test(text);
}

function containsForbidden(text: string): boolean {
  const lower = text.toLowerCase();
  return FORBIDDEN_TERMS.some((term) => lower.includes(term));
}

export function TargetingRequestForm({
  defaultDescricao,
  defaultIdioma = "pt_BR",
  defaultPais = "BR",
  defaultPublico = "PROSPECT",
  className,
  nicheId,
  hypothesisId,
  queryFilters,
}: TargetingRequestFormProps) {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const mutation = useCreateTargetingRequest(queryFilters);
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      descricao: defaultDescricao ?? "",
      idioma: defaultIdioma,
      pais: defaultPais,
      publico_tipo: defaultPublico,
    },
  });

  const currentLength = watch("descricao")?.length ?? 0;
  const isSubmitting = mutation.isPending;

  const policyText = useMemo(
    () =>
      "As sugestões seguem a política de uso de dados da Meta e serão validadas na API oficial de Targeting.",
    [],
  );

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage(null);
    const descricao = values.descricao?.trim();
    if (!descricao) {
      setErrorMessage("Descreva o nicho ou hipótese.");
      return;
    }
    if (descricao.length > MAX_LENGTH) {
      setErrorMessage(`Limite de ${MAX_LENGTH} caracteres excedido.`);
      return;
    }
    if (hasPii(descricao)) {
      setErrorMessage("Remova dados pessoais (telefone, e-mail, CPF) antes de enviar.");
      return;
    }
    if (containsForbidden(descricao)) {
      setErrorMessage("Detectamos termos proibidos pela Meta. Ajuste a descrição.");
      return;
    }

    try {
      const payload = {
        descricao,
        idioma: values.idioma?.trim() || undefined,
        pais: values.pais?.trim() || undefined,
        publico_tipo: values.publico_tipo,
        niche_id: nicheId,
        hypothesis_id: hypothesisId,
      };
      const response = await mutation.mutateAsync(payload);
      toast.success(
        `Solicitação registrada. Status: ${response.status ?? "PENDING_AI"}. ETA: ${response.etaSeconds ?? 90}s`,
      );
      reset({
        descricao: defaultDescricao ?? "",
        idioma: values.idioma,
        pais: values.pais,
        publico_tipo: values.publico_tipo,
      });
    } catch (error) {
      console.error("Erro ao criar solicitação de targeting", error);
      setErrorMessage("Não foi possível enviar agora. Tente novamente em instantes.");
    }
  });

  return (
    <form onSubmit={onSubmit} className={className}>
      <div className="mb-2">
        <label className="form-label fw-semibold">
          Nicho ou hipótese (será enviada ao AI Worker)
        </label>
        <textarea
          className="form-control"
          placeholder="Ex.: noivas minimalistas em SP, gestores de RH que usam ATS"
          maxLength={MAX_LENGTH}
          rows={3}
          {...register("descricao", { required: true })}
        />
        <div className="d-flex justify-content-between small text-body-secondary mt-1">
          <span>{policyText}</span>
          <span>
            {currentLength}/{MAX_LENGTH}
          </span>
        </div>
        {errors.descricao && (
          <div className="text-danger small mt-1">Descrição obrigatória.</div>
        )}
      </div>
      <div className="row g-2 align-items-end">
        <div className="col-md-3">
          <label className="form-label">Idioma preferencial</label>
          <select className="form-select" {...register("idioma")}> 
            <option value="pt_BR">Português (Brasil)</option>
            <option value="en_US">English</option>
          </select>
        </div>
        <div className="col-md-3">
          <label className="form-label">País</label>
          <input className="form-control" {...register("pais")} />
        </div>
        <div className="col-md-3">
          <label className="form-label">Tipo de público</label>
          <select className="form-select" {...register("publico_tipo")}>
            <option value="PROSPECT">Prospect</option>
            <option value="REMARKETING">Remarketing</option>
          </select>
        </div>
        <div className="col-md-3 d-flex justify-content-end">
          <button type="submit" className="btn btn-primary mt-3 w-100" disabled={isSubmitting}>
            {isSubmitting ? "Enviando..." : "Solicitar ao AI Worker"}
          </button>
        </div>
      </div>
      {errorMessage && <div className="text-danger small mt-2">{errorMessage}</div>}
    </form>
  );
}
