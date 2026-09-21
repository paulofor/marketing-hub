const params = new URLSearchParams(location.search);
const paymentId = params.get("payment_id") || params.get("collection_id");
const message = document.querySelector("#message");
const form = document.querySelector("#briefing");
const paymentStatus = document.querySelector("#payment-status");
const submitButton = form.querySelector('button[type="submit"]');
const kitTitle = document.querySelector("#kit-title");
const kitIntro = document.querySelector("#kit-intro");
let submitting = false;

function showStatus(status) {
  form.classList.add("hidden");
  if (status.status === "ENTREGUE") {
    kitTitle.textContent = "Seu kit está pronto.";
    kitIntro.textContent = "Baixe e guarde os arquivos pelo link enviado ao seu e-mail. Escolha a primeira arte e use a legenda para divulgar seu trabalho.";
    message.textContent = "Seu kit foi entregue! Confira o link de download no e-mail informado, inclusive na pasta de spam. Se precisar, fale com nosso atendimento abaixo.";
  } else if (status.status === "BRIEFING_RECEBIDO") {
    kitTitle.textContent = "Estamos preparando seu kit.";
    message.textContent = "Seu briefing já foi recebido. A entrega será feita por e-mail em até 3 dias úteis após o recebimento de todas as informações.";
  } else if (status.status === "AGUARDANDO_BRIEFING") {
    kitTitle.textContent = "Agora vamos personalizar seu kit.";
    message.textContent = "Pagamento aprovado. Preencha os dados para iniciarmos a personalização.";
    form.classList.remove("hidden");
  } else {
    throw new Error("Não foi possível confirmar a situação do kit. Fale com nosso atendimento abaixo.");
  }
  paymentStatus.textContent = "✓ Pagamento confirmado";
}

async function confirmPayment() {
  if (!paymentId) throw new Error("Não encontramos o identificador do pagamento. Use o link recebido após a compra.");
  const response = await fetch(`/api/v1/agenda-cheia/post-purchase?payment_id=${encodeURIComponent(paymentId)}`);
  if (!response.ok) throw new Error("O pagamento ainda não foi confirmado. Aguarde alguns instantes e atualize esta página.");
  showStatus(await response.json());
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (submitting) return;
  submitting = true;
  submitButton.disabled = true;
  submitButton.textContent = "Enviando briefing…";
  form.setAttribute("aria-busy", "true");
  const payload = Object.fromEntries(new FormData(form));
  payload.paymentId = paymentId;
  try {
    const response = await fetch("/api/v1/agenda-cheia/post-purchase/briefing", {
      method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error("Não foi possível enviar. Confira os campos e tente novamente.");
    showStatus(await response.json());
  } catch (error) {
    message.textContent = "Não foi possível confirmar o envio. Seus dados continuam nesta página. Atualize a situação antes de tentar novamente ou fale com nosso atendimento abaixo.";
  } finally {
    submitting = false;
    submitButton.disabled = false;
    submitButton.textContent = "Enviar briefing e iniciar meu kit";
    form.setAttribute("aria-busy", "false");
  }
});

confirmPayment().catch((error) => { message.textContent = error.message; });
