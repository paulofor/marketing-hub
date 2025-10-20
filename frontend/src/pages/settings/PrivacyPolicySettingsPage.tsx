import { FormEvent, useEffect, useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import {
  usePrivacyPolicySetting,
  useUpdatePrivacyPolicySetting,
} from "../../api/settings/usePrivacyPolicySetting";

interface FeedbackState {
  variant: "success" | "error" | "info";
  message: string;
  description?: string;
}

function isValidHttpsUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "https:";
  } catch {
    return false;
  }
}

function formatUpdatedAt(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function PrivacyPolicySettingsPage() {
  const { data, isLoading, isError } = usePrivacyPolicySetting();
  const updateSetting = useUpdatePrivacyPolicySetting();
  const [urlValue, setUrlValue] = useState("");
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  useEffect(() => {
    if (data) {
      setUrlValue(data.value ?? "");
    }
  }, [data?.value]);

  const currentUrl = useMemo(() => {
    const trimmed = urlValue.trim();
    return trimmed === "" ? null : trimmed;
  }, [urlValue]);

  const updatedAtLabel = useMemo(
    () => formatUpdatedAt(data?.updatedAt ?? null),
    [data?.updatedAt],
  );

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);

    const trimmed = urlValue.trim();
    if (trimmed.length === 0) {
      setFeedback({
        variant: "error",
        message: "Informe a URL da política de privacidade.",
        description:
          "Copie o endereço publicado no bucket (ex.: https://storage.googleapis.com/seu-bucket/politica/index.html).",
      });
      return;
    }

    if (!isValidHttpsUrl(trimmed)) {
      setFeedback({
        variant: "error",
        message: "URL inválida",
        description: "Utilize um endereço completo iniciando com https://.",
      });
      return;
    }

    try {
      const updated = await updateSetting.mutateAsync(trimmed);
      setFeedback({
        variant: "success",
        message: "URL atualizada com sucesso.",
        description: updated.value
          ? "Os formulários instantâneos passarão a utilizar esta página como política padrão."
          : undefined,
      });
    } catch {
      setFeedback({
        variant: "error",
        message: "Não foi possível salvar a URL.",
        description: "Verifique a conexão com o backend e tente novamente em instantes.",
      });
    }
  };

  const renderContent = () => {
    if (isLoading) {
      return <p>Carregando configuração de privacidade...</p>;
    }

    if (isError) {
      return (
        <p className="text-danger mb-0">
          Não foi possível carregar a URL configurada. Recarregue a página ou tente novamente mais tarde.
        </p>
      );
    }

    return (
      <form onSubmit={handleSubmit} className="row gy-4" noValidate>
        <div className="col-12 col-lg-8">
          <label className="form-label fw-semibold" htmlFor="privacyPolicyUrl">
            URL da política de privacidade <span className="text-danger">*</span>
          </label>
          <input
            id="privacyPolicyUrl"
            type="url"
            inputMode="url"
            className="form-control"
            placeholder="https://storage.googleapis.com/bucket/politica/index.html"
            value={urlValue}
            onChange={(event) => setUrlValue(event.target.value)}
            required
          />
          <div className="form-text">
            Publique o arquivo HTML no bucket do GCP e cole aqui o endereço público com HTTPS habilitado.
          </div>
        </div>
        <div className="col-12 col-lg-4">
          <div className="bg-body-tertiary rounded-3 p-3 h-100">
            <h6 className="fw-semibold mb-2">Como será utilizado</h6>
            <p className="text-muted small mb-2">
              O Facebook exige uma política de privacidade válida para publicar instant forms. Caso um formulário não informe um
              link próprio, usaremos a URL cadastrada aqui como padrão.
            </p>
            <dl className="row mb-0 small">
              <dt className="col-sm-5">Última atualização</dt>
              <dd className="col-sm-7">{updatedAtLabel}</dd>
              <dt className="col-sm-5">Pré-visualização</dt>
              <dd className="col-sm-7">
                {currentUrl ? (
                  <a href={currentUrl} target="_blank" rel="noreferrer" className="text-break">
                    Abrir política
                  </a>
                ) : (
                  <span className="text-muted">Defina uma URL para habilitar</span>
                )}
              </dd>
            </dl>
          </div>
        </div>
        <div className="col-12 d-flex gap-2">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={updateSetting.isPending}
          >
            {updateSetting.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
                Salvando...
              </>
            ) : (
              "Salvar URL"
            )}
          </button>
        </div>
      </form>
    );
  };

  return (
    <div className="mt-3">
      <div className="d-flex justify-content-between align-items-center gap-3">
        <PageTitle>Política de privacidade padrão</PageTitle>
      </div>

      {feedback ? (
        <div
          className={`alert alert-${
            feedback.variant === "success"
              ? "success"
              : feedback.variant === "error"
              ? "danger"
              : "info"
          } mt-3`}
          role="alert"
        >
          <strong>{feedback.message}</strong>
          {feedback.description ? <div className="mt-1 mb-0">{feedback.description}</div> : null}
        </div>
      ) : null}

      <section className="card mt-3">
        <div className="card-header">
          <h5 className="mb-1">Registrar URL da política</h5>
          <p className="text-muted small mb-0">
            A URL cadastrada será replicada automaticamente para os formulários instantâneos que não possuírem política própria.
          </p>
        </div>
        <div className="card-body">{renderContent()}</div>
      </section>
    </div>
  );
}
