import type { Experiment } from "../../api/experiment/useExperiments";

export const CREATIVE_PUBLICATION_COPY_LIMITS = {
  headline: 40,
  primaryText: 125,
  description: 25,
} as const;

export interface CreativePublicationCopy {
  headline: string;
  primaryText: string;
  description: string;
}

/** Conta caracteres como o gate do backend, inclusive fora do plano BMP. */
export const publicationCopyLength = (value?: string | null) =>
  Array.from(value ?? "").length;

/** Recupera a primeira variação persistida sem inventar mensagem na interface. */
export function plannedCreativeCopy(
  experiment: Pick<Experiment, "adCopy">,
): CreativePublicationCopy {
  try {
    const plan = JSON.parse(experiment.adCopy || "{}");
    const variant = (plan.adCopy ?? plan).primaryTextVariants?.[0];
    return {
      headline: variant?.headline || "",
      primaryText: (
        variant?.lengthVariants?.media ||
        variant?.primaryText ||
        ""
      ).replace(/\\n/g, "\n"),
      description: variant?.description || "",
    };
  } catch {
    return { headline: "", primaryText: "", description: "" };
  }
}
