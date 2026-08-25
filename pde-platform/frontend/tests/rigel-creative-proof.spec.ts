import { expect, test } from "@playwright/test";
import { mkdir } from "node:fs/promises";

const evidenceDir =
  process.env.RIGEL_CREATIVE_EVIDENCE_DIR ?? "/tmp/rigel-creative-proof";

test("captura prova real da degustacao e da oferta de Rigel", async ({
  page,
}) => {
  await mkdir(evidenceDir, { recursive: true });
  await page.route(
    "**/api/pde/products/kit-whatsapp-pronto/commercial-offer",
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          productSlug: "kit-whatsapp-pronto",
          experienceVersion: "kit-whatsapp-pronto-pde-v2",
          layoutKey: "assisted-service-v2",
          experimentId: 89,
          experimentStatus: "PLANNED",
          acquisitionChannel: "DIRECT_ONE_TO_ONE",
          pain: "Voce perde oportunidades no WhatsApp porque improvisa respostas e follow-ups.",
          proof:
            "Veja uma resposta e duas perguntas personalizadas para um cenario real do seu atendimento.",
          promise:
            "Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.",
          primaryCta: "Quero meu atendimento sob medida",
          priceBrl: 349,
          checkoutUrl: "https://pay.example/kit-whatsapp",
          salesPageUrl: "https://kit-whatsapp-pronto.digicomdigital.com.br",
          targetAudience: "Pequenos prestadores que atendem pelo WhatsApp",
          productFormat: "IMPLANTACAO_PERSONALIZADA",
          deliveryMode: "ASSISTIDA_MANUAL",
          valueUnit:
            "Respostas, perguntas e follow-ups prontos para revisar e usar",
          supplierLegalName: "Fornecedor de Homologacao Ltda.",
          supplierRegistrationNumber: "00.000.000/0001-00",
          supplierAddress: "Endereco de homologacao, 100",
          supportEmail: "teste@sandbox.local",
          termsUrl: "/terms?mh_test=1",
          privacyUrl: "/privacy?mh_test=1",
          refundPolicyUrl: "/refund-policy?mh_test=1",
        }),
      });
    },
  );
  await page.setViewportSize({ width: 1440, height: 1900 });
  await page.goto("/?mh_test=1");
  await expect(
    page.getByRole("heading", {
      name: "Retome conversas no WhatsApp sem improvisar a próxima mensagem",
    }),
  ).toBeVisible();
  await page.screenshot({
    path: `${evidenceDir}/rigel-destination-desktop.png`,
    fullPage: true,
    animations: "disabled",
  });

  const offer = page.getByTestId("commercial-offer");
  await expect(offer).toContainText("R$ 349");
  await expect(offer).toContainText("Pagamento único, sem recorrência");
  await offer.screenshot({
    path: `${evidenceDir}/rigel-offer-proof.png`,
    animations: "disabled",
  });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/?mh_test=1");
  await expect(
    page.getByRole("heading", {
      name: "Retome conversas no WhatsApp sem improvisar a próxima mensagem",
    }),
  ).toBeVisible();
  await page.screenshot({
    path: `${evidenceDir}/rigel-destination-mobile.png`,
    fullPage: true,
    animations: "disabled",
  });

  await page.getByLabel("Qual serviço você oferece?").fill("manicure");
  await page.getByLabel("Situação").selectOption("pedido-de-preco");
  await page
    .getByTestId("assisted-tasting")
    .getByLabel("Tom", { exact: true })
    .selectOption("acolhedor");
  await page.getByRole("button", { name: "Gerar minha amostra" }).click();
  const tastingResult = page.getByTestId("assisted-tasting-result");
  await expect(tastingResult).toContainText("Resposta inicial");
  await expect(tastingResult).toContainText("Pergunta de qualificação");
  await expect(tastingResult.locator("ol li")).toHaveCount(3);
  await tastingResult.scrollIntoViewIfNeeded();
  await tastingResult.screenshot({
    path: `${evidenceDir}/rigel-tasting-proof.png`,
    animations: "disabled",
  });

  await tastingResult
    .locator(":scope > p")
    .nth(0)
    .screenshot({
      path: `${evidenceDir}/rigel-tasting-response.png`,
      animations: "disabled",
    });
  await tastingResult
    .locator(":scope > p")
    .nth(1)
    .screenshot({
      path: `${evidenceDir}/rigel-tasting-question.png`,
      animations: "disabled",
    });
  await tastingResult.locator(":scope > ol").screenshot({
    path: `${evidenceDir}/rigel-tasting-followups.png`,
    animations: "disabled",
  });
});
