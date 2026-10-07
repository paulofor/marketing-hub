import { writeFile } from "node:fs/promises";
import { resolve } from "node:path";
const [directory, sourceHash] = process.argv.slice(2);
if (!/^[a-f0-9]{64}$/.test(sourceHash || ""))
  throw new Error("Fingerprint da candidata ausente.");
await writeFile(
  resolve(directory, "mira-candidate-version-diagnostics.json"),
  JSON.stringify({
    surface: "mira-private-candidate",
    productId: 10,
    productSlug: "pde-planejado-36",
    version: "mira-private-candidate-v3",
    experienceVersion: "mira-private-candidate-v3",
    candidateBuildVersion: "mira-private-candidate-v3",
    frontendSourceSha256: sourceHash,
    generationMode: "DETERMINISTIC_DOCUMENTED_LABELS",
    commercialPublication: false,
    paymentEnabled: false,
    externalAiEnabled: false,
  }),
);
