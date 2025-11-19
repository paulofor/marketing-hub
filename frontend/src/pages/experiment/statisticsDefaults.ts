const STATISTIC_TIERS = [
  { minimumBudget: 300, sampleSize: 300, mdePercent: 9 },
  { minimumBudget: 150, sampleSize: 150, mdePercent: 12 },
  { minimumBudget: 0, sampleSize: 100, mdePercent: 14 },
] as const;

export interface BudgetStatisticsDefaults {
  sampleSize: number;
  mdePercent: number;
}

export function getStatisticsDefaultsForBudget(
  dailyBudget: number,
): BudgetStatisticsDefaults | null {
  if (!Number.isFinite(dailyBudget) || dailyBudget <= 0) {
    return null;
  }

  const tier = STATISTIC_TIERS.find((candidate) => dailyBudget >= candidate.minimumBudget);
  if (!tier) {
    return null;
  }

  return {
    sampleSize: tier.sampleSize,
    mdePercent: tier.mdePercent,
  };
}
