export const PROMPT_DOMAINS = [
  {
    value: "NICHE_DETAILED_DESCRIPTION",
    label: "Descrição detalhada de nicho",
  },
  {
    value: "NICHE_HYPOTHESIS",
    label: "Geração de hipóteses de nicho",
  },
];

export const PROMPT_VARIABLES: Record<string, string[]> = {
  NICHE_DETAILED_DESCRIPTION: [
    "quantity",
    "niche.id",
    "niche.name",
    "niche.description",
    "niche.baseSegmentation",
    "niche.interests",
    "niche.demographicFilters",
    "niche.extraTips",
    "niche.interestCategory",
    "niche.roleCategory",
  ],
  NICHE_HYPOTHESIS: [
    "quantity",
    "niche.id",
    "niche.name",
    "niche.description",
    "niche.baseSegmentation",
    "niche.interests",
    "niche.demographicFilters",
    "niche.extraTips",
    "niche.interestCategory",
    "niche.roleCategory",
    "attributes[].id",
    "attributes[].name",
    "attributes[].description",
    "attributeNames",
    "defaultAttributes",
  ],
};
