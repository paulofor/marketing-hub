import { ConsultantChat } from "./ConsultantChat";
import type { ConsultantTransport } from "./types";

const qaTransport: ConsultantTransport = async ({
  image,
  imageConsent,
  message,
}) => {
  await new Promise((resolve) => window.setTimeout(resolve, 120));
  if (message === "Teste de indisponibilidade") {
    throw new Error("Falha sintética exclusiva da homologação local");
  }
  if (message === "Teste de bloqueio") {
    return {
      message: "Ainda não posso orientar com segurança.",
      recommendation: "Complete o contexto solicitado antes de continuar.",
      why: "O produto deve explicar o bloqueio sem simular uma entrega.",
      nextQuestion: null,
      blocker: {
        blocked: true,
        reason: "Falta uma informação obrigatória.",
        userGuidance: "Abra a orientação e complete o dado pendente.",
        helpLinks: ["https://ajuda.sandbox.local/consultor"],
      },
    };
  }
  return {
    message: "O conjunto já comunica presença e intenção para uma reunião.",
    recommendation:
      image && imageConsent
        ? "Mantenha a base e teste somente um acabamento mais claro próximo ao rosto."
        : "Conte qual peça está causando mais dúvida para eu priorizar um ajuste.",
    why: "Um único ajuste preserva o que funciona e reduz o esforço para decidir.",
    nextQuestion: "Você quer uma alternativa mais discreta ou mais marcante?",
    blocker: { blocked: false, helpLinks: [] },
  };
};

/** Disponibiliza um cenário sem backend ou modelo somente para homologar o kit React localmente. */
export function ConsultantSdkQaPreview() {
  return (
    <main
      style={{
        background: "#f4edf2",
        minHeight: "100dvh",
        padding: "clamp(0px, 3vw, 28px)",
      }}
    >
      <ConsultantChat
        consultantName="Amora"
        greeting="Olá, eu sou a Amora, sua consultora de estilo. Você tem hoje algum evento ou trabalho? Eu posso ajudar."
        transport={qaTransport}
      />
    </main>
  );
}
