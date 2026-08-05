import { FormEvent, useMemo, useState } from "react";
import axios from "axios";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import type {
  GeneratedImageResult,
  GeneratedImageVariant,
} from "../../api/ai/useGenerateImage";
import { useGenerateImage } from "../../api/ai/useGenerateImage";
import { useProducts } from "../../api/product/useProducts";
import { useCommercialPlans } from "../../api/planning/useCommercialPlans";

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

function imageSrc(result: GeneratedImageResult) {
  return `data:image/${result.outputFormat};base64,${result.imageBase64}`;
}

function variantSrc(variant: GeneratedImageVariant) {
  return `data:image/${variant.format};base64,${variant.imageBase64}`;
}

function formatBytes(byteSize: number) {
  if (byteSize >= 1024 * 1024) {
    return `${(byteSize / 1024 / 1024).toFixed(1)} MB`;
  }
  return `${Math.max(1, Math.round(byteSize / 1024))} KB`;
}

function variantLabel(variant: GeneratedImageVariant) {
  const roleLabels: Record<string, string> = {
    original: "Original",
    web: "Web",
    mobile: "Mobile",
  };
  return `${roleLabels[variant.role] ?? variant.role} · ${variant.width}x${variant.height} · ${formatBytes(variant.byteSize)}`;
}

function handleDownload(
  result: GeneratedImageResult,
  variant?: GeneratedImageVariant,
) {
  const link = document.createElement("a");
  link.href = variant ? variantSrc(variant) : imageSrc(result);
  const role = variant?.role ?? "original";
  const format = variant?.format ?? result.outputFormat;
  link.download = `${result.jobId}-${result.model}-${role}.${format}`;
  link.click();
}

export default function ImageGeneratorPage() {
  useBreadcrumbs([{ label: "Gerador de Imagens" }]);

  const [prompt, setPrompt] = useState("");
  const [productId, setProductId] = useState("");
  const [commercialPlanId, setCommercialPlanId] = useState("");
  const [experimentId, setExperimentId] = useState("");
  const productsQuery = useProducts();
  const plansQuery = useCommercialPlans();
  const generation = useGenerateImage();
  const result = generation.data;
  const generatedImages = useMemo(() => result?.images ?? [], [result]);
  const generationFailures = useMemo(() => result?.failures ?? [], [result]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPrompt = prompt.trim();
    if (!normalizedPrompt || !productId || !commercialPlanId || generation.isPending) {
      return;
    }
    generation.mutate({
      productId: Number(productId),
      commercialPlanId: Number(commercialPlanId),
      experimentId: experimentId ? Number(experimentId) : undefined,
      prompt: normalizedPrompt,
    });
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
                  <label className="form-label fw-semibold" htmlFor="image-generator-product">
                    Produto <span className="text-danger">*</span>
                  </label>
                  <select
                    id="image-generator-product"
                    className="form-select"
                    value={productId}
                    onChange={(event) => setProductId(event.target.value)}
                    disabled={generation.isPending || productsQuery.isLoading}
                    required
                  >
                    <option value="">Selecione o produto</option>
                    {(productsQuery.data ?? []).map((product) => (
                      <option key={product.id} value={product.id}>
                        {product.name ?? `Produto ${product.id}`}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="mb-3">
                  <label className="form-label fw-semibold" htmlFor="image-generator-plan">
                    Plano comercial <span className="text-danger">*</span>
                  </label>
                  <select
                    id="image-generator-plan"
                    className="form-select"
                    value={commercialPlanId}
                    onChange={(event) => {
                      setCommercialPlanId(event.target.value);
                      setExperimentId("");
                    }}
                    disabled={generation.isPending || plansQuery.isLoading}
                    required
                  >
                    <option value="">Selecione o plano</option>
                    {(plansQuery.data ?? []).map((plan) => (
                      <option key={plan.id} value={plan.id}>{plan.name}</option>
                    ))}
                  </select>
                </div>

                <div className="mb-3">
                  <label className="form-label fw-semibold" htmlFor="image-generator-experiment">
                    Experimento (opcional)
                  </label>
                  <select
                    id="image-generator-experiment"
                    className="form-select"
                    value={experimentId}
                    onChange={(event) => setExperimentId(event.target.value)}
                    disabled={generation.isPending || !commercialPlanId}
                  >
                    <option value="">Sem experimento</option>
                    {(plansQuery.data ?? [])
                      .filter((plan) => String(plan.id) === commercialPlanId && plan.experimentId)
                      .map((plan) => (
                        <option key={plan.experimentId} value={plan.experimentId ?? ""}>
                          {plan.experimentName ?? `Experimento ${plan.experimentId}`}
                        </option>
                      ))}
                  </select>
                </div>

                <div className="mb-3">
                  <label
                    className="form-label fw-semibold"
                    htmlFor="image-generator-prompt"
                  >
                    Prompt da imagem <span className="text-danger">*</span>
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
                      || !productId || !commercialPlanId
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
                  <h2 className="h5 mb-1">Imagens geradas</h2>
                  <div className="text-body-secondary small">
                    {result
                      ? `Lote ${result.jobId}`
                      : "As duas imagens comparativas aparecerão aqui após a geração."}
                  </div>
                </div>
              </div>

              <div
                className="bg-body-tertiary border rounded overflow-hidden p-3"
                style={{ minHeight: "28rem" }}
              >
                {generationFailures.length > 0 ? (
                  <div className="alert alert-warning" role="status">
                    <div className="fw-semibold mb-1">
                      Parte do lote comparativo falhou.
                    </div>
                    {generationFailures.map((failure) => (
                      <div key={`${failure.model}-${failure.finishedAt}`}>
                        {failure.model}: {failure.message}
                      </div>
                    ))}
                  </div>
                ) : null}

                {generatedImages.length > 0 ? (
                  <div className="row g-3">
                    {generatedImages.map((image) => (
                      <div className="col-12 col-lg-6" key={image.jobId}>
                        <div className="h-100 bg-body border rounded p-3 d-flex flex-column gap-3">
                          <div className="d-flex align-items-start justify-content-between gap-2">
                            <div>
                              <h3 className="h6 mb-1">{image.model}</h3>
                              <div className="text-body-secondary small">
                                {image.serviceTier} · job {image.jobId}
                              </div>
                            </div>
                            <div className="d-flex flex-wrap gap-2 justify-content-end">
                              {(image.variants?.length
                                ? image.variants
                                : [
                                    {
                                      role: "original",
                                      format: image.outputFormat,
                                      imageBase64: image.imageBase64,
                                      width: 0,
                                      height: 0,
                                      byteSize: 0,
                                    },
                                  ]
                              ).map((variant) => (
                                <button
                                  type="button"
                                  className="btn btn-outline-primary btn-sm"
                                  key={variant.role}
                                  onClick={() => handleDownload(image, variant)}
                                  title={variantLabel(variant)}
                                >
                                  {variant.role === "original"
                                    ? "Original"
                                    : variant.role === "web"
                                      ? "Web"
                                      : variant.role === "mobile"
                                        ? "Mobile"
                                        : variant.role}
                                </button>
                              ))}
                            </div>
                          </div>
                          <div className="d-flex align-items-center justify-content-center bg-body border rounded overflow-hidden">
                            <img
                              src={imageSrc(image)}
                              alt={`Resultado gerado por IA com ${image.model}`}
                              className="img-fluid"
                              style={{
                                maxHeight: "56vh",
                                objectFit: "contain",
                              }}
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : generation.isPending ? (
                  <div className="text-center text-body-secondary p-4">
                    <span
                      className="spinner-border text-primary mb-3"
                      role="status"
                      aria-hidden="true"
                    />
                    <div>Gerando duas imagens comparativas em modo flex...</div>
                  </div>
                ) : (
                  <div className="text-center text-body-secondary p-4">
                    Informe um prompt objetivo para gerar as duas imagens.
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
