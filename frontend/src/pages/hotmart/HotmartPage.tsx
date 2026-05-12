import { FormEvent, useEffect, useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import {
  useHotmartAccessTokenSetting,
  useUpdateHotmartAccessTokenSetting,
} from "../../api/settings/useHotmartAccessTokenSetting";

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

export default function HotmartPage() {
  const { data, isLoading, isError } = useHotmartAccessTokenSetting();
  const updateSetting = useUpdateHotmartAccessTokenSetting();
  const [tokenValue, setTokenValue] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    if (data) {
      setTokenValue(data.value ?? "");
    }
  }, [data?.value]);

  const updatedAt = useMemo(() => formatUpdatedAt(data?.updatedAt), [data?.updatedAt]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);

    const trimmed = tokenValue.trim();
    if (!trimmed) {
      setFeedback("Informe o token JWT de acesso da Hotmart.");
      return;
    }

    try {
      await updateSetting.mutateAsync(trimmed);
      setFeedback("Token salvo com sucesso.");
    } catch {
      setFeedback("Não foi possível salvar o token. Tente novamente.");
    }
  };

  return (
    <div className="mt-3">
      <PageTitle>Hotmart</PageTitle>

      <section className="card mt-3 shadow-sm border-0">
        <div className="card-header">
          <h5 className="mb-1">Token de acesso</h5>
          <p className="text-muted small mb-0">
            Cole aqui o JWT obtido no login da Hotmart para uso nas integrações deste módulo.
          </p>
        </div>
        <div className="card-body">
          {isLoading ? <p className="mb-0">Carregando token...</p> : null}
          {isError ? (
            <p className="text-danger mb-0">Não foi possível carregar o token salvo.</p>
          ) : null}

          {!isLoading && !isError ? (
            <form onSubmit={handleSubmit} className="d-flex flex-column gap-3" noValidate>
              <div>
                <label htmlFor="hotmartAccessToken" className="form-label fw-semibold">
                  JWT Hotmart <span className="text-danger">*</span>
                </label>
                <textarea
                  id="hotmartAccessToken"
                  className="form-control"
                  rows={6}
                  value={tokenValue}
                  onChange={(event) => setTokenValue(event.target.value)}
                  placeholder="Cole aqui o token JWT"
                  required
                />
                <div className="form-text">Última atualização: {updatedAt}</div>
              </div>

              <div className="d-flex gap-2">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={updateSetting.isPending}
                >
                  {updateSetting.isPending ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                      Salvando...
                    </>
                  ) : (
                    "Salvar"
                  )}
                </button>
              </div>
            </form>
          ) : null}

          {feedback ? <div className="alert alert-info mt-3 mb-0">{feedback}</div> : null}
        </div>
      </section>
    </div>
  );
}
