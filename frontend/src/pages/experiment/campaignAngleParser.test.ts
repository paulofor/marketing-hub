import { describe, expect, it } from "vitest";
import {
  hasCampaignAngleContent,
  parseCampaignAnglePayload,
} from "./campaignAngleParser";

describe("campaignAngleParser", () => {
  it("normalizes canonical JSON payloads", () => {
    const payload = JSON.stringify({
      primaryPromise: "Promessa de escala",
      primaryPain: "Sem previsibilidade de novos alunos",
      mechanismSummary: "Playbook automatizado",
      proofUsed: "Baseado em dados internos",
      cta: "Liberar o playbook",
      funnelStage: "Topo",
      tone: "Direto",
    });

    const summary = parseCampaignAnglePayload(payload);

    expect(summary).toMatchObject({
      primaryPromise: "Promessa de escala",
      primaryPain: "Sem previsibilidade de novos alunos",
      mechanismSummary: "Playbook automatizado",
      proofUsed: "Baseado em dados internos",
      cta: "Liberar o playbook",
      funnelStage: "Topo",
      tone: "Direto",
    });
    expect(hasCampaignAngleContent(summary)).toBe(true);
  });

  it("extrai dados mesmo quando o modelo envia JSON em bloco com crases", () => {
    const payload = "```json\n{\n  \"campaignAngle\": \"Promessa priorizada\",\n  \"pain\": \"Sem diferenciação\"\n}\n```";

    const summary = parseCampaignAnglePayload(payload);

    expect(summary).toMatchObject({
      primaryPromise: "Promessa priorizada",
      primaryPain: "Sem diferenciação",
    });
  });

  it("retorna undefined quando não há texto estruturado", () => {
    expect(parseCampaignAnglePayload("   ")).toBeUndefined();
    expect(parseCampaignAnglePayload(undefined)).toBeUndefined();
    expect(hasCampaignAngleContent(undefined)).toBe(false);
  });
});
