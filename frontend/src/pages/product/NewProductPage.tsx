import { useState } from "react";
import PageTitle from "../../components/PageTitle";
import { useCreateProduct } from "../../api/product/useCreateProduct";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";

export default function NewProductPage() {
  const create = useCreateProduct();
  const { data: accounts } = useInstagramAccounts();
  const [form, setForm] = useState({
    niche: "",
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
      instagramAccountId: Number(form.instagramAccountId) || undefined,
      aiCost: Number(form.aiCost),
    });
  };

  return (
    <div>
      <PageTitle>Novo Produto</PageTitle>
      <input
        className="form-control mb-2"
        placeholder="Nicho"
        value={form.niche}
        onChange={(e) => setForm({ ...form, niche: e.target.value })}
      />
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
        {accounts?.map((a) => (
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
      <button className="btn btn-primary" onClick={submit}>
        Salvar
      </button>
    </div>
  );
}
