import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { CopyCheck, ExternalLink, Save, Send } from "lucide-react";
import {
  useProductPdeProductionSlots,
  usePublishProductPdeProductionSlot,
  useSaveProductPdeProductionSlot,
} from "../../api/product/usePdeProductionSlots";
import { useProducts, type Product } from "../../api/product/useProducts";
import type { PostDeployPdeProductionSlot } from "../../api/experiment/usePostDeployMonitor";
import PageTitle from "../../components/PageTitle";

type FirstFoldCopyForm = {
  headline: string;
  supportingText: string;
  videoKicker: string;
  videoHeadline: string;
  videoSupportingText: string;
  videoExtraText: string;
  videoCtaLabel: string;
  publishedBy: string;
};

const emptyForm: FirstFoldCopyForm = {
  headline: "",
  supportingText: "",
  videoKicker: "",
  videoHeadline: "",
  videoSupportingText: "",
  videoExtraText: "",
  videoCtaLabel: "",
  publishedBy: "Marketing Hub",
};

const defaultMusaCopy: FirstFoldCopyForm = {
  headline: "Você se arruma, mas ainda sente que falta presença?",
  supportingText:
    "Em poucos minutos, o MUSA identifica o detalhe que está deixando sua imagem mais comum do que deveria e mostra um primeiro ajuste para testar hoje, usando o que você já tem.",
  videoKicker: "Prévia MUSA",
  videoHeadline:
    "Antes de pensar em roupa nova, encontre o sinal que apaga sua presença.",
  videoSupportingText:
    "Às vezes o look não está errado. Ele só está sem uma intenção visível. Um acabamento, uma cor, uma combinação ou uma postura podem deixar sua presença mais coerente com muito menos esforço do que você imagina.",
  videoExtraText:
    "O MUSA usa 4 escolhas simples sobre seu espelho, sua rotina e o sinal que você quer transmitir para apontar onde sua imagem perde força e qual microação pode deixar você mais pronta hoje.",
  videoCtaLabel: "Ver meu primeiro ajuste MUSA",
  publishedBy: "Marketing Hub",
};

function isPdeProduct(product: Product) {
  const slug = product.slug?.toLowerCase() ?? "";
  const type = product.productType?.toLowerCase() ?? "";
  const name = product.name?.toLowerCase() ?? "";
  return (
    slug.includes("pde") ||
    type.includes("pde") ||
    Boolean(product.pdeExperienceJson?.trim()) ||
    slug === "metodo-musa-7-dias" ||
    name.includes("método musa") ||
    name.includes("metodo musa")
  );
}

function parseContract(rawJson?: string | null) {
  if (!rawJson?.trim()) return {};
  try {
    const parsed = JSON.parse(rawJson) as Record<string, unknown>;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function textField(value: unknown) {
  return typeof value === "string" ? value : "";
}

function firstFoldFromContract(contract: Record<string, unknown>) {
  const block =
    contract.publicFirstFold && typeof contract.publicFirstFold === "object"
      ? (contract.publicFirstFold as Record<string, unknown>)
      : {};
  return {
    headline: textField(block.headline),
    supportingText: textField(block.supportingText),
    videoKicker: textField(block.videoKicker),
    videoHeadline: textField(block.videoHeadline),
    videoSupportingText: textField(block.videoSupportingText),
    videoExtraText: textField(block.videoExtraText),
    videoCtaLabel: textField(block.videoCtaLabel),
  };
}

function buildContract(
  product: Product,
  slot: PostDeployPdeProductionSlot,
  form: FirstFoldCopyForm,
) {
  const baseContract = parseContract(
    slot.draftExperienceJson ||
      slot.publishedExperienceJson ||
      product.pdeExperienceJson,
  );
  return JSON.stringify(
    {
      ...baseContract,
      slug: product.slug,
      name: baseContract.name || product.name,
      experienceVersion: slot.experienceVersion,
      layoutKey: slot.layoutKey,
      publicFirstFold: {
        headline: form.headline.trim(),
        supportingText: form.supportingText.trim(),
        videoKicker: form.videoKicker.trim(),
        videoHeadline: form.videoHeadline.trim(),
        videoSupportingText: form.videoSupportingText.trim(),
        videoExtraText: form.videoExtraText.trim(),
        videoCtaLabel: form.videoCtaLabel.trim(),
      },
    },
    null,
    2,
  );
}

function formatDate(value?: string | null) {
  if (!value) return "Ainda não publicado";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Data inválida";
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export default function PdeFirstFoldCopyEditorPage() {
  const productsQuery = useProducts();
  const products = productsQuery.data ?? [];
  const pdeProducts = useMemo(() => products.filter(isPdeProduct), [products]);
  const [selectedProductId, setSelectedProductId] = useState("");
  const selectedProduct = pdeProducts.find(
    (product) => String(product.id) === selectedProductId,
  );
  const slotsQuery = useProductPdeProductionSlots(selectedProductId);
  const slots = slotsQuery.data ?? [];
  const [selectedSlotCode, setSelectedSlotCode] = useState("");
  const selectedSlot = slots.find((slot) => slot.slotCode === selectedSlotCode);
  const saveSlot = useSaveProductPdeProductionSlot(selectedProductId);
  const publishSlot = usePublishProductPdeProductionSlot(selectedProductId);
  const [form, setForm] = useState<FirstFoldCopyForm>(emptyForm);

  useEffect(() => {
    if (!selectedProductId && pdeProducts.length > 0) {
      setSelectedProductId(String(pdeProducts[0].id));
    }
  }, [pdeProducts, selectedProductId]);

  useEffect(() => {
    if (!selectedSlotCode && slots.length > 0) {
      setSelectedSlotCode(slots[0].slotCode);
    }
  }, [selectedSlotCode, slots]);

  useEffect(() => {
    if (!selectedProduct || !selectedSlot) return;
    const contract = parseContract(
      selectedSlot.draftExperienceJson ||
        selectedSlot.publishedExperienceJson ||
        selectedProduct.pdeExperienceJson,
    );
    setForm({
      ...defaultMusaCopy,
      ...firstFoldFromContract(contract),
      publishedBy: selectedSlot.publishedBy || "Marketing Hub",
    });
  }, [selectedProduct, selectedSlot]);

  function updateForm(field: keyof FirstFoldCopyForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function saveDraft() {
    if (!selectedProduct || !selectedSlot) return;
    saveSlot.mutate({
      productSlug: selectedProduct.slug || "",
      slotCode: selectedSlot.slotCode,
      domain: selectedSlot.domain,
      publicUrl: selectedSlot.publicUrl,
      backendUrl: selectedSlot.backendUrl || undefined,
      experienceVersion: selectedSlot.experienceVersion,
      layoutKey: selectedSlot.layoutKey,
      targetEnvironment: selectedSlot.targetEnvironment,
      sourceExperimentId: selectedSlot.sourceExperimentId || undefined,
      status: selectedSlot.status,
      notes: selectedSlot.notes || undefined,
      draftExperienceJson: buildContract(selectedProduct, selectedSlot, form),
    });
  }

  function publishCopy() {
    if (!selectedProduct || !selectedSlot) return;
    publishSlot.mutate({
      slotCode: selectedSlot.slotCode,
      experienceJson: buildContract(selectedProduct, selectedSlot, form),
      publishedBy: form.publishedBy.trim() || "Marketing Hub",
    });
  }

  if (productsQuery.isLoading) {
    return <p className="text-muted">Carregando produtos PDE...</p>;
  }

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Copy PDE</PageTitle>
          <p className="text-muted mb-0">
            Editor versionado da primeira dobra pública do PDE: promessa,
            contexto do vídeo e CTA inicial.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          Produtos
        </Link>
      </div>

      <div className="alert alert-info" role="status">
        Use esta tela para mudanças comerciais rápidas. O contrato completo do
        slot continua preservado; aqui só é atualizado o bloco de primeira
        dobra.
      </div>

      <section className="card mb-3">
        <div className="card-body">
          <div className="row g-3">
            <div className="col-12 col-md-5">
              <label className="form-label fw-semibold" htmlFor="pde-copy-product">
                Produto PDE
              </label>
              <select
                id="pde-copy-product"
                className="form-select"
                value={selectedProductId}
                onChange={(event) => {
                  setSelectedProductId(event.target.value);
                  setSelectedSlotCode("");
                }}
              >
                {pdeProducts.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.name || product.slug || `Produto ${product.id}`}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold" htmlFor="pde-copy-slot">
                Versão/slot
              </label>
              <select
                id="pde-copy-slot"
                className="form-select"
                value={selectedSlotCode}
                onChange={(event) => setSelectedSlotCode(event.target.value)}
                disabled={slotsQuery.isLoading || slots.length === 0}
              >
                {slots.map((slot) => (
                  <option key={slot.id} value={slot.slotCode}>
                    {slot.slotCode} · {slot.experienceVersion}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label fw-semibold" htmlFor="pde-copy-publisher">
                Publicado por
              </label>
              <input
                id="pde-copy-publisher"
                className="form-control"
                value={form.publishedBy}
                onChange={(event) => updateForm("publishedBy", event.target.value)}
              />
            </div>
          </div>
          {selectedSlot ? (
            <div className="small text-muted mt-3">
              URL pública:{" "}
              <a href={selectedSlot.publicUrl} target="_blank" rel="noreferrer">
                {selectedSlot.publicUrl}
              </a>{" "}
              · última publicação: {formatDate(selectedSlot.publishedAt)}
            </div>
          ) : (
            <div className="small text-muted mt-3">
              Cadastre uma versão PDE no produto antes de editar a copy.
            </div>
          )}
        </div>
      </section>

      <section className="card">
        <div className="card-body">
          <h2 className="h5 mb-3">Primeira dobra pública</h2>
          <div className="row g-3">
            <div className="col-12">
              <label className="form-label fw-semibold" htmlFor="pde-copy-headline">
                Headline
              </label>
              <input
                id="pde-copy-headline"
                className="form-control"
                value={form.headline}
                onChange={(event) => updateForm("headline", event.target.value)}
              />
            </div>
            <div className="col-12">
              <label className="form-label fw-semibold" htmlFor="pde-copy-supporting">
                Texto de apoio
              </label>
              <textarea
                id="pde-copy-supporting"
                className="form-control"
                rows={3}
                value={form.supportingText}
                onChange={(event) =>
                  updateForm("supportingText", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold" htmlFor="pde-copy-video-kicker">
                Rótulo do vídeo
              </label>
              <input
                id="pde-copy-video-kicker"
                className="form-control"
                value={form.videoKicker}
                onChange={(event) => updateForm("videoKicker", event.target.value)}
              />
            </div>
            <div className="col-12 col-md-8">
              <label className="form-label fw-semibold" htmlFor="pde-copy-video-headline">
                Headline do bloco de vídeo
              </label>
              <input
                id="pde-copy-video-headline"
                className="form-control"
                value={form.videoHeadline}
                onChange={(event) =>
                  updateForm("videoHeadline", event.target.value)
                }
              />
            </div>
            <div className="col-12">
              <label className="form-label fw-semibold" htmlFor="pde-copy-video-supporting">
                Texto do vídeo
              </label>
              <textarea
                id="pde-copy-video-supporting"
                className="form-control"
                rows={3}
                value={form.videoSupportingText}
                onChange={(event) =>
                  updateForm("videoSupportingText", event.target.value)
                }
              />
            </div>
            <div className="col-12">
              <label className="form-label fw-semibold" htmlFor="pde-copy-video-extra">
                Texto adicional
              </label>
              <textarea
                id="pde-copy-video-extra"
                className="form-control"
                rows={2}
                value={form.videoExtraText}
                onChange={(event) =>
                  updateForm("videoExtraText", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-5">
              <label className="form-label fw-semibold" htmlFor="pde-copy-video-cta">
                CTA do vídeo
              </label>
              <input
                id="pde-copy-video-cta"
                className="form-control"
                value={form.videoCtaLabel}
                onChange={(event) =>
                  updateForm("videoCtaLabel", event.target.value)
                }
              />
            </div>
          </div>

          <div className="d-flex flex-wrap gap-2 mt-4">
            <button
              type="button"
              className="btn btn-outline-primary"
              disabled={!selectedSlot || saveSlot.isPending}
              onClick={saveDraft}
            >
              <Save size={16} className="me-1" />
              {saveSlot.isPending ? "Salvando..." : "Salvar rascunho"}
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={!selectedSlot || publishSlot.isPending}
              onClick={publishCopy}
            >
              <Send size={16} className="me-1" />
              {publishSlot.isPending ? "Publicando..." : "Publicar copy"}
            </button>
            {selectedProduct?.slug && selectedSlot ? (
              <a
                className="btn btn-outline-secondary"
                href={`/api/products/public/${selectedProduct.slug}/pde-experience?experienceVersion=${encodeURIComponent(
                  selectedSlot.experienceVersion,
                )}`}
                target="_blank"
                rel="noreferrer"
              >
                <CopyCheck size={16} className="me-1" />
                Ver contrato
              </a>
            ) : null}
            {selectedSlot?.publicUrl ? (
              <a
                className="btn btn-outline-secondary"
                href={selectedSlot.publicUrl}
                target="_blank"
                rel="noreferrer"
              >
                <ExternalLink size={16} className="me-1" />
                Abrir PDE
              </a>
            ) : null}
          </div>
        </div>
      </section>
    </div>
  );
}
