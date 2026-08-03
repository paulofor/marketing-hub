const params = new URLSearchParams(location.search);
const paymentId = params.get("payment_id") || params.get("collection_id");
const message = document.querySelector("#message");
const form = document.querySelector("#briefing");

async function confirmPayment() {
  if (!paymentId) throw new Error("Não encontramos o identificador do pagamento. Use o link recebido após a compra.");
  const response = await fetch(`/api/v1/agenda-cheia/post-purchase?payment_id=${encodeURIComponent(paymentId)}`);
  if (!response.ok) throw new Error("O pagamento ainda não foi confirmado. Aguarde alguns instantes e atualize esta página.");
  const status = await response.json();
  if (status.status === "BRIEFING_RECEBIDO") {
    message.textContent = "Seu briefing já foi recebido. Seu kit entrou na fila de produção.";
    return;
  }
  message.textContent = "Pagamento aprovado. Preencha os dados para iniciarmos a personalização.";
  form.classList.remove("hidden");
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const payload = Object.fromEntries(new FormData(form));
  payload.paymentId = paymentId;
  const response = await fetch("/api/v1/agenda-cheia/post-purchase/briefing", {
    method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify(payload)
  });
  if (!response.ok) { message.textContent = "Não foi possível enviar. Confira os campos e tente novamente."; return; }
  form.classList.add("hidden");
  message.textContent = "Briefing recebido! Seu kit entrou na fila de produção. Guarde esta página como confirmação.";
});

confirmPayment().catch((error) => { message.textContent = error.message; });
