const heroMessage = document.getElementById("heroMessage");
const statusPill = document.getElementById("statusPill");
const statusCard = document.getElementById("statusCard");
const statusIcon = document.getElementById("statusIcon");
const statusTitle = document.getElementById("statusTitle");
const statusDescription = document.getElementById("statusDescription");
const detailsCard = document.getElementById("detailsCard");
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

function showWarning(message, type = "warning") {
  detailsCard.hidden = true;
  setStatus(type === "error" ? "error" : "warning", "Estamos finalizando seu pedido", message);
}

async function loadCheckout() {
  if (!packageId) {
    showWarning("Estamos preparando seu pedido. Em alguns instantes, você receberá a confirmação por e-mail.", "warning");
    heroMessage.textContent = "Verifique sua caixa de entrada e, se usar Gmail, confira também a aba Promoções.";
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
    showWarning("Seu pagamento está em análise final. Assim que concluirmos, enviaremos o produto para o seu e-mail.", "warning");
  }
}

function hydrate(payload) {
  checkoutUrl = payload.checkoutUrl;
  if (!checkoutUrl) {
    showWarning("Seu pedido está sendo preparado para entrega por e-mail.", "warning");
    return;
  }

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
    if (!applyPurchaseStatus(payload.status)) {
      applyReturnStatus();
    }
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

function normalizePurchaseStatus(status) {
  if (!status) {
    return "";
  }
  return status.toString().trim().toUpperCase();
}

function applyPurchaseStatus(status) {
  const normalized = normalizePurchaseStatus(status);
  switch (normalized) {
    case "DELIVERED":
      setStatus(
        "success",
        "Seu produto já foi enviado!",
        "Pagamento aprovado e entrega concluída. Procure agora o e-mail com suas imagens originais. Se usar Gmail, verifique também a aba Promoções."
      );
      heroMessage.textContent = "Parabéns pela compra! Seu produto já está no e-mail.";
      return true;
    case "DELIVERING":
      setStatus(
        "success",
        "Pagamento aprovado!",
        "Ótima compra! Já estamos organizando o envio das imagens originais para o seu e-mail."
      );
      heroMessage.textContent = "Tudo certo por aqui: em instantes, seu produto chega no e-mail.";
      return true;
    case "APPROVED":
      setStatus(
        "success",
        "Pagamento aprovado com sucesso!",
        "Excelente escolha! Recebemos a confirmação e seu produto já está na fila de envio para o e-mail."
      );
      heroMessage.textContent = "Agora é só acompanhar o e-mail — incluindo a aba Promoções no Gmail.";
      return true;
    case "PENDING_PAYMENT":
    case "PREFERENCE_CREATED":
      setStatus(
        "warning",
        "Estamos finalizando seu pedido",
        "Seu pagamento está em processamento final. Assim que confirmado, você receberá o produto por e-mail."
      );
      heroMessage.textContent = "Fique tranquilo: vamos avisar no e-mail assim que a entrega for concluída.";
      return true;
    case "FAILED":
    case "CANCELED":
      setStatus(
        "warning",
        "Estamos validando seu pagamento",
        "Recebemos seu pedido e estamos concluindo a validação. Em breve você receberá uma atualização por e-mail."
      );
      heroMessage.textContent = "Estamos cuidando dos últimos detalhes para liberar seu produto.";
      return true;
    default:
      return false;
  }
}

function applyReturnStatus() {
  switch (explicitStatus) {
    case "approved":
      setStatus(
        "success",
        "Pagamento aprovado com sucesso!",
        "Excelente compra! Em instantes você receberá as imagens originais no e-mail. Se usar Gmail, confira também a aba Promoções."
      );
      heroMessage.textContent = "Parabéns pela compra! Seu produto está a caminho do seu e-mail.";
      break;
    case "pending":
    case "in_process":
      setStatus(
        "warning",
        "Estamos finalizando seu pedido",
        "Seu pagamento está em processamento e o envio será feito por e-mail assim que a confirmação for concluída."
      );
      heroMessage.textContent = "Falta pouco! Acompanhe sua caixa de entrada.";
      break;
    case "rejected":
    case "failure":
    case "cancelled":
      setStatus(
        "warning",
        "Estamos validando seu pagamento",
        "Recebemos sua tentativa e estamos verificando as informações. Em breve você receberá um e-mail com a atualização."
      );
      heroMessage.textContent = "Estamos concluindo a validação para liberar seu produto.";
      break;
    default:
      setStatus(
        "warning",
        "Estamos finalizando seu pedido",
        "Em breve você receberá um e-mail com a confirmação e as instruções de acesso ao produto."
      );
      heroMessage.textContent = "Obrigado pela compra! Estamos preparando tudo para você.";
  }
}

window.addEventListener("beforeunload", () => {
  if (redirectTimer) {
    clearTimeout(redirectTimer);
  }
});

loadCheckout();
