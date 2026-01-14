export const PROMPT_DOMAINS = [
  {
    value: "NICHE_DETAILED_DESCRIPTION",
    label: "Descrição detalhada de nicho",
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
};
