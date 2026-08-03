import { FormEvent, useState } from "react";
import { toast } from "react-toastify";
import {
  useActivateTemporaryCheckout,
  useRestoreTemporaryCheckout,
  useTemporaryCheckout,
} from "../../api/leadPortal/useTemporaryCheckout";

const PRODUCT_KEY = "agenda-cheia-nail-design";

export default function TemporaryCheckoutPage() {
  const [amount, setAmount] = useState("0.67");
  const [duration, setDuration] = useState("60");
  const [commercialUrl, setCommercialUrl] = useState("");
  const status = useTemporaryCheckout(PRODUCT_KEY);
  const activate = useActivateTemporaryCheckout();
  const restore = useRestoreTemporaryCheckout();

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      await activate.mutateAsync({
        productKey: PRODUCT_KEY,
        productName: "Agenda Cheia Nail Design",
        testAmount: Number(amount),
        commercialCheckoutUrl: commercialUrl,
        durationMinutes: Number(duration),
      });
      toast.success("Checkout de teste ativado com restauração automática.");
    } catch {
      toast.error("Não foi possível ativar o checkout de teste.");
    }
  }

  async function restoreNow() {
    try {
      await restore.mutateAsync(PRODUCT_KEY);
      toast.success("Checkout comercial restaurado.");
    } catch {
      toast.error("Não foi possível restaurar o checkout comercial.");
    }
  }

  const current = status.data;
  return (
    <main className="container py-4" style={{ maxWidth: 860 }}>
      <p className="text-uppercase text-muted small mb-1">Pagamentos</p>
      <h1>Checkout temporário</h1>
      <p className="text-muted">
        Crie uma cobrança de teste e mantenha um endereço estável que volta
        automaticamente ao checkout comercial.
      </p>

      {current && (
        <section className="card p-3 mb-4" aria-live="polite">
          <div className="d-flex justify-content-between align-items-center gap-3 flex-wrap">
            <div>
              <strong>{current.productName}</strong>
              <div>
                Status:{" "}
                {current.status === "ACTIVE"
                  ? "Teste ativo"
                  : "Comercial restaurado"}
              </div>
              <small>
                Validade: {new Date(current.expiresAt).toLocaleString("pt-BR")}
              </small>
            </div>
            {current.status === "ACTIVE" && (
              <button
                className="btn btn-outline-danger"
                onClick={restoreNow}
                disabled={restore.isPending}
              >
                {restore.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" />
                )}
                Restaurar agora
              </button>
            )}
          </div>
          <label className="form-label mt-3">
            URL estável para usar na página
          </label>
          <input
            className="form-control"
            readOnly
            value={current.redirectUrl}
          />
        </section>
      )}

      <form className="card p-4" onSubmit={submit}>
        <h2 className="h5">Ativar compra de teste</h2>
        <label className="form-label mt-3">Preço de teste *</label>
        <input
          className="form-control"
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          required
        />
        <label className="form-label mt-3">Duração em minutos *</label>
        <input
          className="form-control"
          type="number"
          min="5"
          max="1440"
          value={duration}
          onChange={(event) => setDuration(event.target.value)}
          required
        />
        <label className="form-label mt-3">Checkout comercial de R$ 67 *</label>
        <input
          className="form-control"
          type="url"
          placeholder="https://www.mercadopago.com.br/..."
          value={commercialUrl}
          onChange={(event) => setCommercialUrl(event.target.value)}
          required
        />
        <button
          className="btn btn-primary mt-4"
          type="submit"
          disabled={activate.isPending}
        >
          {activate.isPending && (
            <span className="spinner-border spinner-border-sm me-2" />
          )}
          Criar checkout temporário
        </button>
      </form>
    </main>
  );
}
