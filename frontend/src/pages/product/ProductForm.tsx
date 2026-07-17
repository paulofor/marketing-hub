import { useState } from "react";
import { CreateProduct } from "../../api/product/useCreateProduct";
import { Product } from "../../api/product/useProducts";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import { useNiches } from "../../api/niche/useNiches";

type ProductFormValues = {
  [K in keyof CreateProduct]: string;
};

const defaultForm: ProductFormValues = {
  slug: "",
  name: "",
  publicUrl: "",
  colorPalette: "",
  targetAudience: "",
  languageStyle: "",
  codeModules: "",
  productType: "PDE - Produto Digital Experiencial",
  commercialStatus: "VALIDACAO_COMERCIAL",
  currentPriceBrl: "",
  primaryHypothesisId: "",
  primaryHypothesis: "",
  associatedExperiments: "",
  commercialNotes: "",
  niche: "",
  marketNicheId: "",
  avatar: "",
  instagramAccountId: "",
  explicitPain: "",
  promise: "",
  uniqueMechanism: "",
  tripwire: "",
  riskReversal: "",
  socialProof: "",
  checkoutMonetization: "",
  funnel: "",
  creativeVolume: "",
  storytelling: "",
  aiCost: "",
};

function toFormValues(product?: Product): ProductFormValues {
  if (!product) return defaultForm;
  return {
    slug: product.slug ?? "",
    name: product.name ?? "",
    publicUrl: product.publicUrl ?? "",
    colorPalette: product.colorPalette ?? "",
    targetAudience: product.targetAudience ?? "",
    languageStyle: product.languageStyle ?? "",
    codeModules: product.codeModules ?? "",
    productType: product.productType ?? defaultForm.productType,
    commercialStatus: product.commercialStatus ?? defaultForm.commercialStatus,
    currentPriceBrl:
      product.currentPriceBrl != null ? String(product.currentPriceBrl) : "",
    primaryHypothesisId: product.primaryHypothesisId ?? "",
    primaryHypothesis: product.primaryHypothesis ?? "",
    associatedExperiments: product.associatedExperiments ?? "",
    commercialNotes: product.commercialNotes ?? "",
    niche: product.niche ?? "",
    marketNicheId:
      product.marketNicheId != null ? String(product.marketNicheId) : "",
    avatar: product.avatar ?? "",
    instagramAccountId:
      product.instagramAccountId != null
        ? String(product.instagramAccountId)
        : "",
    explicitPain: product.explicitPain ?? "",
    promise: product.promise ?? "",
    uniqueMechanism: product.uniqueMechanism ?? "",
    tripwire: product.tripwire ?? "",
    riskReversal: product.riskReversal ?? "",
    socialProof: product.socialProof ?? "",
    checkoutMonetization: product.checkoutMonetization ?? "",
    funnel: product.funnel ?? "",
    creativeVolume: product.creativeVolume ?? "",
    storytelling: product.storytelling ?? "",
    aiCost: product.aiCost != null ? String(product.aiCost) : "",
  };
}

function toPayload(form: ProductFormValues): CreateProduct {
  return {
    ...form,
    marketNicheId: Number(form.marketNicheId) || undefined,
    instagramAccountId: Number(form.instagramAccountId) || undefined,
    currentPriceBrl: Number(form.currentPriceBrl) || undefined,
    aiCost: Number(form.aiCost) || 0,
  };
}

type ProductFormProps = {
  initialProduct?: Product;
  isSaving: boolean;
  submitLabel?: string;
  onSubmit: (payload: CreateProduct) => void;
};

export default function ProductForm({
  initialProduct,
  isSaving,
  submitLabel = "Salvar",
  onSubmit,
}: ProductFormProps) {
  const { data: accountsData } = useInstagramAccounts();
  const { data: nichesData } = useNiches();
  const accounts = Array.isArray(accountsData) ? accountsData : [];
  const niches = Array.isArray(nichesData) ? nichesData : [];
  const [form, setForm] = useState<ProductFormValues>(() =>
    toFormValues(initialProduct),
  );

  const setField = (field: keyof ProductFormValues, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const submit = () => {
    onSubmit(toPayload(form));
  };

  return (
    <div>
      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <label className="form-label mb-1">Nome comercial *</label>
          <input
            className="form-control"
            value={form.name}
            onChange={(e) => setField("name", e.target.value)}
          />
        </div>
        <div className="col-12 col-lg-3">
          <label className="form-label mb-1">Slug *</label>
          <input
            className="form-control"
            value={form.slug}
            onChange={(e) => setField("slug", e.target.value)}
          />
        </div>
        <div className="col-12 col-lg-3">
          <label className="form-label mb-1">Preço atual</label>
          <input
            className="form-control"
            inputMode="decimal"
            value={form.currentPriceBrl}
            onChange={(e) => setField("currentPriceBrl", e.target.value)}
          />
        </div>
      </div>
      <label className="form-label mt-3 mb-1">URL pública</label>
      <input
        className="form-control mb-2"
        value={form.publicUrl}
        onChange={(e) => setField("publicUrl", e.target.value)}
      />
      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <label className="form-label mb-1">Tipo</label>
          <input
            className="form-control"
            value={form.productType}
            onChange={(e) => setField("productType", e.target.value)}
          />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label mb-1">Status comercial</label>
          <input
            className="form-control"
            value={form.commercialStatus}
            onChange={(e) => setField("commercialStatus", e.target.value)}
          />
        </div>
      </div>
      <textarea
        className="form-control mt-3 mb-2"
        placeholder="Paleta de cores"
        value={form.colorPalette}
        onChange={(e) => setField("colorPalette", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Público alvo"
        value={form.targetAudience}
        onChange={(e) => setField("targetAudience", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Estilo de linguagem"
        value={form.languageStyle}
        onChange={(e) => setField("languageStyle", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Módulos de código"
        value={form.codeModules}
        onChange={(e) => setField("codeModules", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Hipótese/oferta principal"
        value={form.primaryHypothesis}
        onChange={(e) => setField("primaryHypothesis", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Experimentos associados"
        value={form.associatedExperiments}
        onChange={(e) => setField("associatedExperiments", e.target.value)}
      />
      <label className="form-label mb-1">Nicho *</label>
      <select
        className="form-select mb-2"
        value={form.marketNicheId}
        onChange={(e) =>
          setForm({
            ...form,
            marketNicheId: e.target.value,
            niche:
              niches.find((niche) => String(niche.id) === e.target.value)
                ?.name ?? "",
          })
        }
      >
        <option value="">Selecione o Nicho</option>
        {niches.map((niche) => (
          <option key={niche.id} value={niche.id}>
            {niche.name}
          </option>
        ))}
      </select>
      <input
        className="form-control mb-2"
        placeholder="Avatar"
        value={form.avatar}
        onChange={(e) => setField("avatar", e.target.value)}
      />
      <select
        className="form-select mb-2"
        value={form.instagramAccountId}
        onChange={(e) => setField("instagramAccountId", e.target.value)}
      >
        <option value="">Selecione a Conta do Instagram</option>
        {accounts.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name}
          </option>
        ))}
      </select>
      <textarea
        className="form-control mb-2"
        placeholder="Dor Explícita"
        value={form.explicitPain}
        onChange={(e) => setField("explicitPain", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Promessa Transformadora"
        value={form.promise}
        onChange={(e) => setField("promise", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Mecanismo Único"
        value={form.uniqueMechanism}
        onChange={(e) => setField("uniqueMechanism", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Oferta Tripwire"
        value={form.tripwire}
        onChange={(e) => setField("tripwire", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Reversão de Risco"
        value={form.riskReversal}
        onChange={(e) => setField("riskReversal", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Prova Social"
        value={form.socialProof}
        onChange={(e) => setField("socialProof", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Monetização do Checkout"
        value={form.checkoutMonetization}
        onChange={(e) => setField("checkoutMonetization", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Funil"
        value={form.funnel}
        onChange={(e) => setField("funnel", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Volume Criativo"
        value={form.creativeVolume}
        onChange={(e) => setField("creativeVolume", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Observações comerciais"
        value={form.commercialNotes}
        onChange={(e) => setField("commercialNotes", e.target.value)}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Storytelling"
        value={form.storytelling}
        onChange={(e) => setField("storytelling", e.target.value)}
      />
      <input
        className="form-control mb-2"
        placeholder="Custo de IA"
        value={form.aiCost}
        onChange={(e) => setField("aiCost", e.target.value)}
      />
      <button className="btn btn-primary" onClick={submit} disabled={isSaving}>
        {isSaving ? (
          <>
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
            Salvando...
          </>
        ) : (
          submitLabel
        )}
      </button>
    </div>
  );
}
