const heroMessage = document.getElementById("heroMessage");
const statusPill = document.getElementById("statusPill");
const statusCard = document.getElementById("statusCard");
const statusIcon = document.getElementById("statusIcon");
const statusTitle = document.getElementById("statusTitle");
const statusDescription = document.getElementById("statusDescription");
const detailsCard = document.getElementById("detailsCard");
const warningCard = document.getElementById("warningCard");
const warningMessage = document.getElementById("warningMessage");
const retryButton = document.getElementById("retryButton");
const packageIdEl = document.getElementById("packageId");
const purchaseIdEl = document.getElementById("purchaseId");
const amountEl = document.getElementById("amount");
const expiresAtEl = document.getElementById("expiresAt");
const descriptorEl = document.getElementById("statementDescriptor");
const detailsTitle = document.getElementById("detailsTitle");
const payButton = document.getElementById("payButton");

const params = new URLSearchParams(window.location.search);
const packageId = params.get("packageId");
const purchaseIdParam = params.get("purchaseId");
const explicitStatus = (params.get("status") || params.get("collection_status") || "")
  .toString()
  .toLowerCase();
const shouldAutoRedirect = (params.get("autoRedirect") ?? "true").toLowerCase() !== "false";
const isReturnFlow = Boolean(
  explicitStatus ||
    params.get("collection_id") ||
    params.get("payment_id") ||
    params.get("merchant_order_id")
);
let checkoutUrl;
let redirectTimer;

retryButton?.addEventListener("click", () => {
  window.location.reload();
});

payButton?.addEventListener("click", () => {
  if (checkoutUrl) {
    window.location.href = checkoutUrl;
  }
});

function setStatus(type, title, description) {
  statusIcon.className = `status-icon ${type ?? ""}`.trim();
  statusTitle.textContent = title;
  statusDescription.textContent = description;
  statusPill.classList.remove("success", "warning", "error");
  switch (type) {
    case "success":
      statusPill.textContent = "Pagamento aprovado";
      statusPill.classList.add("success");
      break;
    case "warning":
      statusPill.textContent = "Pagamento pendente";
      statusPill.classList.add("warning");
      break;
    case "error":
      statusPill.textContent = "Pagamento não concluído";
      statusPill.classList.add("error");
      break;
    default:
      statusPill.textContent = "Carregando";
  }
}

function formatAmount(amount, currency = "BRL") {
  if (amount === null || amount === undefined) {
    return "Sob consulta";
  }
  try {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
    }).format(Number(amount));
  } catch (err) {
    return `${amount} ${currency}`.trim();
  }
}

function formatDate(date) {
  if (!date) {
    return "Sem prazo definido";
  }
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) {
    return "Sem prazo definido";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "full",
    timeStyle: "short",
  }).format(parsed);
}

function showWarning(message, type = "error") {
  warningCard.hidden = false;
  warningMessage.textContent = message;
  detailsCard.hidden = true;
  setStatus(type === "error" ? "error" : "warning", "Não conseguimos carregar o checkout", message);
}

async function loadCheckout() {
  if (!packageId) {
    showWarning("O identificador do pacote (packageId) é obrigatório.");
    heroMessage.textContent = "Repare se o link recebido possui o parâmetro packageId.";
    return;
  }

  try {
    const response = await fetch(`/api/v1/payments/packages/${encodeURIComponent(packageId)}`);
    if (!response.ok) {
      const errText = await response.text();
      throw new Error(errText || `Erro ${response.status}`);
    }
    const payload = await response.json();
    hydrate(payload);
  } catch (err) {
    console.error("Falha ao buscar checkout", err);
    showWarning("Não foi possível recuperar o link de pagamento. Tente novamente em instantes.");
  }
}

function hydrate(payload) {
  checkoutUrl = payload.checkoutUrl;
  if (!checkoutUrl) {
    showWarning("Este pacote ainda não possui um link de pagamento ativo.");
    return;
  }

  warningCard.hidden = true;
  detailsCard.hidden = false;

  packageIdEl.textContent = payload.packageId ?? packageId;
  detailsTitle.textContent = `Pacote #${payload.packageId ?? packageId}`;
  purchaseIdEl.textContent = purchaseIdParam ?? payload.purchaseId ?? "—";
  amountEl.textContent = formatAmount(payload.amount, payload.currency);
  expiresAtEl.textContent = formatDate(payload.expiresAt ?? payload.checkoutExpiresAt);
  descriptorEl.textContent = payload.statementDescriptor
    ? `Pagamento processado por ${payload.statementDescriptor}.`
    : "Pagamento processado via Mercado Pago.";

  payButton.disabled = false;

  if (isReturnFlow) {
    applyReturnStatus();
  } else {
    heroMessage.textContent =
      "Revise as informações do pacote e, quando estiver pronto, siga para o Mercado Pago.";
    statusPill.textContent = "Aguardando ação";
    statusPill.classList.remove("success", "warning", "error");
    setStatus(
      "",
      "Tudo pronto para finalizar o pagamento",
      "Clique no botão abaixo para continuar no ambiente seguro do Mercado Pago."
    );
    if (shouldAutoRedirect) {
      statusDescription.textContent =
        "Em instantes você será redirecionado automaticamente. Caso prefira, clique no botão.";
      redirectTimer = window.setTimeout(() => {
        window.location.href = checkoutUrl;
      }, 4000);
    }
  }
}

function applyReturnStatus() {
  switch (explicitStatus) {
    case "approved":
      setStatus(
        "success",
        "Pagamento confirmado!",
        "Recebemos a confirmação do Mercado Pago. Você receberá as imagens originais no seu e-mail em alguns instantes."
      );
      heroMessage.textContent = "Tudo certo! Agora é só aguardar a entrega no seu e-mail.";
      break;
    case "pending":
    case "in_process":
      setStatus(
        "warning",
        "Pagamento em processamento",
        "Assim que o Mercado Pago confirmar o pagamento, enviaremos as instruções para o seu e-mail."
      );
      heroMessage.textContent = "Estamos aguardando a confirmação do Mercado Pago.";
      break;
    case "rejected":
    case "failure":
    case "cancelled":
      setStatus(
        "error",
        "Pagamento não concluído",
        "O Mercado Pago informou que a tentativa não foi finalizada. Você pode tentar novamente clicando no botão abaixo."
      );
      heroMessage.textContent = "Identificamos uma falha na etapa de pagamento.";
      break;
    default:
      setStatus(
        "warning",
        "Estamos validando o status do seu pagamento",
        "Caso a confirmação não ocorra em alguns minutos, tente novamente ou entre em contato com o nosso time."
      );
      heroMessage.textContent = "Ainda não recebemos o status final do Mercado Pago.";
  }
}

window.addEventListener("beforeunload", () => {
  if (redirectTimer) {
    clearTimeout(redirectTimer);
  }
});

loadCheckout();
