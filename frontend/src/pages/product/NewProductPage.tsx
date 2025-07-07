import { useState } from "react";
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
  });

  const submit = () => {
    create.mutate({
      ...form,
      instagramAccountId: Number(form.instagramAccountId) || undefined,
    });
  };

  return (
    <div>
      <input
        className="form-control mb-2"
        placeholder="Niche"
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
        <option value="">Select Instagram Account</option>
        {accounts?.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name}
          </option>
        ))}
      </select>
      <textarea
        className="form-control mb-2"
        placeholder="Explicit Pain"
        value={form.explicitPain}
        onChange={(e) => setForm({ ...form, explicitPain: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Transformative Promise"
        value={form.promise}
        onChange={(e) => setForm({ ...form, promise: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Unique Mechanism"
        value={form.uniqueMechanism}
        onChange={(e) => setForm({ ...form, uniqueMechanism: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Tripwire Offer"
        value={form.tripwire}
        onChange={(e) => setForm({ ...form, tripwire: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Risk Reversal"
        value={form.riskReversal}
        onChange={(e) => setForm({ ...form, riskReversal: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Social Proof"
        value={form.socialProof}
        onChange={(e) => setForm({ ...form, socialProof: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Checkout Monetization"
        value={form.checkoutMonetization}
        onChange={(e) =>
          setForm({ ...form, checkoutMonetization: e.target.value })
        }
      />
      <textarea
        className="form-control mb-2"
        placeholder="Funnel"
        value={form.funnel}
        onChange={(e) => setForm({ ...form, funnel: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Creative Volume"
        value={form.creativeVolume}
        onChange={(e) => setForm({ ...form, creativeVolume: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Storytelling"
        value={form.storytelling}
        onChange={(e) => setForm({ ...form, storytelling: e.target.value })}
      />
      <button className="btn btn-primary" onClick={submit}>
        Save
      </button>
    </div>
  );
}
