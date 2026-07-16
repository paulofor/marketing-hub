import { FormEvent, useMemo, useState } from "react";
import axios from "axios";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useGenerateImage } from "../../api/ai/useGenerateImage";

const EXAMPLE_PROMPT =
  "Mulher brasileira elegante, 35 anos, olhando para o espelho antes de sair, ambiente claro, estética premium, fotografia realista para anúncio de produto digital de presença pessoal.";

function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? error.message;
  }
  return error instanceof Error
    ? error.message
    : "Não foi possível gerar a imagem.";
}

export default function ImageGeneratorPage() {
  useBreadcrumbs([{ label: "Gerador de Imagens" }]);

  const [prompt, setPrompt] = useState("");
  const generation = useGenerateImage();
  const result = generation.data;
  const imageSrc = useMemo(() => {
    if (!result?.imageBase64) {
      return null;
    }
    return `data:image/${result.outputFormat};base64,${result.imageBase64}`;
  }, [result]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPrompt = prompt.trim();
    if (!normalizedPrompt || generation.isPending) {
      return;
    }
    generation.mutate({ prompt: normalizedPrompt });
  }

  function handleDownload() {
    if (!imageSrc || !result) {
      return;
    }
    const link = document.createElement("a");
    link.href = imageSrc;
    link.download = `${result.jobId}.${result.outputFormat}`;
    link.click();
  }

  return (
    <div>
      <PageTitle>Gerador de Imagens</PageTitle>
      <p className="text-body-secondary">
        Gere imagens para campanhas, páginas de venda, criativos e materiais de
        produto digital usando IA em modo flex.
      </p>

      <div className="row g-4">
        <div className="col-12 col-xl-5">
          <div className="card">
            <div className="card-body">
              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label
                    className="form-label fw-semibold"
                    htmlFor="image-generator-prompt"
                  >
                    Prompt da imagem
                  </label>
                  <textarea
                    id="image-generator-prompt"
                    className="form-control"
                    rows={10}
                    maxLength={4000}
                    value={prompt}
                    onChange={(event) => setPrompt(event.target.value)}
                    placeholder={EXAMPLE_PROMPT}
                  />
                  <div className="form-text">
                    {prompt.length}/4000 caracteres
                  </div>
                </div>

                {generation.isError ? (
                  <div className="alert alert-danger" role="alert">
                    {errorMessage(generation.error)}
                  </div>
                ) : null}

                <div className="d-flex flex-wrap gap-2">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={
                      generation.isPending || prompt.trim().length === 0
                    }
                  >
                    {generation.isPending ? (
                      <>
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          role="status"
                          aria-hidden="true"
                        />
                        Gerando...
                      </>
                    ) : (
                      "Gerar imagem"
                    )}
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => setPrompt(EXAMPLE_PROMPT)}
                    disabled={generation.isPending}
                  >
                    Usar exemplo
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-7">
          <div className="card">
            <div className="card-body">
              <div className="d-flex align-items-center justify-content-between gap-3 mb-3">
                <div>
                  <h2 className="h5 mb-1">Imagem gerada</h2>
                  <div className="text-body-secondary small">
                    {result
                      ? `${result.model} · ${result.serviceTier} · job ${result.jobId}`
                      : "A imagem aparecerá aqui após a geração."}
                  </div>
                </div>
                <button
                  type="button"
                  className="btn btn-outline-primary"
                  onClick={handleDownload}
                  disabled={!imageSrc}
                >
                  Baixar
                </button>
              </div>

              <div
                className="d-flex align-items-center justify-content-center bg-body-tertiary border rounded overflow-hidden"
                style={{ minHeight: "28rem" }}
              >
                {imageSrc ? (
                  <img
                    src={imageSrc}
                    alt="Resultado gerado por IA"
                    className="img-fluid"
                    style={{ maxHeight: "70vh", objectFit: "contain" }}
                  />
                ) : generation.isPending ? (
                  <div className="text-center text-body-secondary p-4">
                    <span
                      className="spinner-border text-primary mb-3"
                      role="status"
                      aria-hidden="true"
                    />
                    <div>Gerando imagem em modo flex...</div>
                  </div>
                ) : (
                  <div className="text-center text-body-secondary p-4">
                    Informe um prompt objetivo para gerar a primeira imagem.
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
