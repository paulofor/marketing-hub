import { describe, expect, it } from "vitest";
import {
  createSimpleFormTemplateQuestions,
  type SimpleFlowTemplateConfig,
} from "./SimpleLeadPortalFormCard";

const baseConfig: SimpleFlowTemplateConfig = {
  formTemplate: "PREMIUM_SAMPLE",
  workQuestionTitle: "",
  optionsQuestionTitle: "Qual estilo combina mais com o seu estúdio?",
  optionsQuestionValues:
    "Elegante e minimalista\nDelicado e feminino\nModerno e marcante",
  otherOptionsTitle: "",
  headerTitle: "Veja seu estúdio em 3 artes premium",
  headerSubtitle: "Escolha um estilo e receba sua amostra.",
  headerPromise: "Avalie antes de comprar.",
  realExamplesTitle: "Exemplos",
  realExamplesSubtitle: "Veja a qualidade.",
  realExampleCard1Title: "Agenda aberta",
  realExampleCard1Subtitle: "Post para captar horários.",
  realExampleCard2Title: "Prova do trabalho",
  realExampleCard2Subtitle: "Post para valorizar o resultado.",
  realExampleCard3Title: "Chamada para WhatsApp",
  realExampleCard3Subtitle: "Post para iniciar conversas.",
  bulletSectionTitle: "Você recebe",
  bulletItem1: "3 artes",
  bulletItem2: "Prévia imediata",
  bulletItem3: "Entrega por e-mail",
  card1ImageUrl: "/assets/exemplo-1.png",
  card2ImageUrl: "/assets/exemplo-2.png",
  card3ImageUrl: "/assets/exemplo-3.png",
  card1OverlayText: "",
  card2OverlayText: "",
  card3OverlayText: "",
};

describe("modelo de amostra premium do Lead Portal", () => {
  it("coleta somente nome do estúdio, e-mail e um estilo visual", () => {
    const questions = createSimpleFormTemplateQuestions(baseConfig);
    const requiredQuestions = questions.filter((question) => question.required);

    expect(requiredQuestions.map((question) => question.dataKey)).toEqual([
      "nome",
      "email",
      "estilo_visual",
    ]);
    expect(requiredQuestions[2]).toMatchObject({
      type: "SINGLE_CHOICE",
      options: [
        "Elegante e minimalista",
        "Delicado e feminino",
        "Moderno e marcante",
      ],
    });
    expect(
      questions.some((question) => question.dataKey === "local_trabalho"),
    ).toBe(false);
    expect(
      questions.some((question) => question.dataKey === "outras_opcoes"),
    ).toBe(false);
  });
});
