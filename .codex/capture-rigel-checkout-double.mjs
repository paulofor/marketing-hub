import { writeFileSync } from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium } = require("@playwright/test");
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN });
const page = await browser.newPage({ viewport: { width: 900, height: 900 } });
await page.setContent(`<!doctype html>
  <html lang="pt-BR"><head><meta charset="utf-8"><title>Checkout local da Rigel</title>
  <style>
    body{margin:0;background:#f5f2eb;color:#14221d;font:18px/1.5 system-ui,sans-serif}
    main{width:min(680px,calc(100% - 40px));margin:48px auto;padding:40px;background:#fff;border:1px solid #d8e1da;border-radius:24px;box-shadow:0 20px 60px #14221d18}
    .qa{padding:9px 12px;border-radius:10px;background:#fff3cd;color:#684f00;font-size:14px;font-weight:800}
    h1{font-size:36px;line-height:1.1}.price{font-size:64px;font-weight:900;color:#14664a}.rule{padding:18px;background:#edf6f0;border-radius:14px}.supplier{margin-top:28px;padding-top:22px;border-top:1px solid #d8e1da;font-size:15px}.target{overflow-wrap:anywhere;color:#55645c;font-size:14px}
  </style></head><body><main>
  <p class="qa">HOMOLOGAÇÃO LOCAL · TEST DOUBLE · nenhum pagamento possível</p>
  <h1>Kit WhatsApp Pronto</h1>
  <p>Implantação personalizada e assistida do atendimento no WhatsApp.</p>
  <p class="price">R$ 349</p>
  <p class="rule"><strong>Pagamento único, sem recorrência.</strong><br>Nenhuma cobrança adicional é criada neste ambiente local.</p>
  <p class="supplier"><strong>Fornecedor:</strong> PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA<br><strong>CNPJ:</strong> 25.215.414/0001-69</p>
  <p class="target"><strong>Destino protegido:</strong> www.mercadopago.com.br · preferência canônica do experimento 89</p>
  </main></body></html>`);
const audit = await page.evaluate(() => ({
  title: document.title,
  text: document.body.innerText.replace(/\s+/g, " ").trim(),
  forms: document.forms.length,
  links: document.links.length,
}));
await page.screenshot({
  path: ".codex/attachments/rigel-checkout-local-double.jpg",
  fullPage: true,
  type: "jpeg",
  quality: 92,
});
writeFileSync(
  ".codex/attachments/rigel-checkout-local-double.json",
  `${JSON.stringify(
    {
      mode: "LOCAL_TEST_DOUBLE_NO_PROVIDER_NO_PAYMENT",
      ...audit,
      expected: {
        experimentId: 89,
        priceBrl: 349,
        billingModel: "ONE_TIME",
        providerHost: "www.mercadopago.com.br",
      },
    },
    null,
    2,
  )}\n`,
);
await browser.close();
console.log(JSON.stringify(audit));
