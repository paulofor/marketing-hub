import { readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const contractUrl = new URL(
  "../pde-platform/backend/src/main/resources/contracts/musa-v7-product-v1.json",
  import.meta.url,
);
const outputUrl = new URL(
  "../backend/ads-service/src/main/resources/db/changelog/changesets/2026-09-01-musa-v7-canonical-checkout-binding.sql",
  import.meta.url,
);

const expectedIdentity = {
  slug: "metodo-musa-7-dias",
  experienceVersion: "musa-pde-entry-v7-espelho-antes-de-sair",
  name: "Método MUSA - Presença Elegante em 7 Dias",
  priceLabel: "R$67",
};

const contract = JSON.parse(await readFile(contractUrl, "utf8"));
for (const [field, expected] of Object.entries(expectedIdentity)) {
  if (contract[field] !== expected) {
    throw new Error(
      `Contrato MUSA v7 divergente em ${field}: esperado=${expected} recebido=${contract[field]}`,
    );
  }
}
if (!Array.isArray(contract.missions) || contract.missions.length !== 7) {
  throw new Error("Contrato MUSA v7 deve possuir exatamente sete missões");
}
if (contract.missions.some((mission) => !mission.interaction)) {
  throw new Error("Toda missão MUSA v7 deve possuir interação canônica");
}
const expectedCheckout = {
  provider: "PEPPER",
  checkoutUrl: "https://go.pepper.com.br/owm6x",
  offerReference: "owm6x",
  priceBrl: 67,
  currency: "BRL",
  billingModel: "ONE_TIME",
};
if (
  JSON.stringify(contract.commercialCheckout) !==
  JSON.stringify(expectedCheckout)
) {
  throw new Error("Contrato MUSA v7 diverge do checkout Pepper homologado");
}

const sqlLiteral = JSON.stringify(contract).replaceAll("'", "''");
const sql = `-- Gerado por scripts/generate-musa-v7-canonical-contract-changelog.mjs.
SET @musa_v7_canonical_contract = '${sqlLiteral}';

UPDATE product
SET pde_experience_json = @musa_v7_canonical_contract,
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'metodo-musa-7-dias';

UPDATE pde_production_slot
SET draft_experience_json = @musa_v7_canonical_contract,
    published_experience_json = CASE
      WHEN published_experience_json IS NULL OR published_experience_json = ''
        THEN published_experience_json
      ELSE @musa_v7_canonical_contract
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE product_slug = 'metodo-musa-7-dias'
  AND experience_version = 'musa-pde-entry-v7-espelho-antes-de-sair';
`;

await writeFile(fileURLToPath(outputUrl), sql, "utf8");
