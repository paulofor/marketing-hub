import { useState } from "react";
import PageTitle from "../../components/PageTitle";
import { useCreateProduct } from "../../api/product/useCreateProduct";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import { useNiches } from "../../api/niche/useNiches";

export default function NewProductPage() {
  const create = useCreateProduct();
  const { data: accountsData } = useInstagramAccounts();
  const { data: nichesData } = useNiches();
  const accounts = Array.isArray(accountsData) ? accountsData : [];
  const niches = Array.isArray(nichesData) ? nichesData : [];
  const [form, setForm] = useState({
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
  });

  const submit = () => {
    create.mutate({
      ...form,
      marketNicheId: Number(form.marketNicheId) || undefined,
      instagramAccountId: Number(form.instagramAccountId) || undefined,
      currentPriceBrl: Number(form.currentPriceBrl) || undefined,
      aiCost: Number(form.aiCost),
    });
  };

  return (
    <div>
      <PageTitle>Novo Produto Comercial</PageTitle>
      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <label className="form-label mb-1">Nome comercial *</label>
          <input
            className="form-control"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </div>
        <div className="col-12 col-lg-3">
          <label className="form-label mb-1">Slug *</label>
          <input
            className="form-control"
            value={form.slug}
            onChange={(e) => setForm({ ...form, slug: e.target.value })}
          />
        </div>
        <div className="col-12 col-lg-3">
          <label className="form-label mb-1">Preço atual</label>
          <input
            className="form-control"
            inputMode="decimal"
            value={form.currentPriceBrl}
            onChange={(e) => setForm({ ...form, currentPriceBrl: e.target.value })}
          />
        </div>
      </div>
      <label className="form-label mt-3 mb-1">URL pública</label>
      <input
        className="form-control mb-2"
        value={form.publicUrl}
        onChange={(e) => setForm({ ...form, publicUrl: e.target.value })}
      />
      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <label className="form-label mb-1">Tipo</label>
          <input
            className="form-control"
            value={form.productType}
            onChange={(e) => setForm({ ...form, productType: e.target.value })}
          />
        </div>
        <div className="col-12 col-lg-6">
          <label className="form-label mb-1">Status comercial</label>
          <input
            className="form-control"
            value={form.commercialStatus}
            onChange={(e) => setForm({ ...form, commercialStatus: e.target.value })}
          />
        </div>
      </div>
      <textarea
        className="form-control mt-3 mb-2"
        placeholder="Paleta de cores"
        value={form.colorPalette}
        onChange={(e) => setForm({ ...form, colorPalette: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Público alvo"
        value={form.targetAudience}
        onChange={(e) => setForm({ ...form, targetAudience: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Estilo de linguagem"
        value={form.languageStyle}
        onChange={(e) => setForm({ ...form, languageStyle: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Módulos de código"
        value={form.codeModules}
        onChange={(e) => setForm({ ...form, codeModules: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Hipótese/oferta principal"
        value={form.primaryHypothesis}
        onChange={(e) => setForm({ ...form, primaryHypothesis: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Experimentos associados"
        value={form.associatedExperiments}
        onChange={(e) => setForm({ ...form, associatedExperiments: e.target.value })}
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
              niches.find((niche) => String(niche.id) === e.target.value)?.name ?? "",
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
        onChange={(e) => setForm({ ...form, avatar: e.target.value })}
      />
      <select
        className="form-select mb-2"
        value={form.instagramAccountId}
        onChange={(e) =>
          setForm({ ...form, instagramAccountId: e.target.value })
        }
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
        onChange={(e) => setForm({ ...form, explicitPain: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Promessa Transformadora"
        value={form.promise}
        onChange={(e) => setForm({ ...form, promise: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Mecanismo Único"
        value={form.uniqueMechanism}
        onChange={(e) => setForm({ ...form, uniqueMechanism: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Oferta Tripwire"
        value={form.tripwire}
        onChange={(e) => setForm({ ...form, tripwire: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Reversão de Risco"
        value={form.riskReversal}
        onChange={(e) => setForm({ ...form, riskReversal: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Prova Social"
        value={form.socialProof}
        onChange={(e) => setForm({ ...form, socialProof: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Monetização do Checkout"
        value={form.checkoutMonetization}
        onChange={(e) =>
          setForm({ ...form, checkoutMonetization: e.target.value })
        }
      />
      <textarea
        className="form-control mb-2"
        placeholder="Funil"
        value={form.funnel}
        onChange={(e) => setForm({ ...form, funnel: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Volume Criativo"
        value={form.creativeVolume}
        onChange={(e) => setForm({ ...form, creativeVolume: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Observações comerciais"
        value={form.commercialNotes}
        onChange={(e) => setForm({ ...form, commercialNotes: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Storytelling"
        value={form.storytelling}
        onChange={(e) => setForm({ ...form, storytelling: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Custo de IA"
        value={form.aiCost}
        onChange={(e) => setForm({ ...form, aiCost: e.target.value })}
      />
      <button className="btn btn-primary" onClick={submit} disabled={create.isPending}>
        {create.isPending ? (
          <>
            <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
            Salvando...
          </>
        ) : (
          "Salvar"
        )}
      </button>
    </div>
  );
}
