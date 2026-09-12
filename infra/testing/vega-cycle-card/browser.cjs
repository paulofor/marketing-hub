// Reutiliza a homologação dos cards para preservar o mesmo contrato de navegação por processo.
process.env.NEXT_PROCESS_OUTPUT =
  process.env.VEGA_CARD_OUTPUT || "artifacts/vega-cycle-card/browser";
require("../product-next-activity/browser.cjs");
