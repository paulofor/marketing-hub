import type {
  Experiment,
  ExperimentPlatform,
} from "../../api/experiment/useExperiments";

type CurrentPlan = Pick<
  Experiment,
  "platform" | "dailyBudget" | "mediaSpendLimit" | "startDate" | "endDate"
>;
type EditedPlan = {
  platform: ExperimentPlatform;
  dailyBudget: string;
  mediaSpendLimit: string;
  startDate: string;
  endDate: string;
};

/** Distingue edição de conteúdo de uma alteração financeira; zero legado continua zero. */
export function isMediaPlanEdited(
  current: CurrentPlan,
  edited: EditedPlan,
): boolean {
  const amount = (value?: number | null) =>
    value != null && value > 0 ? String(value) : "";
  const date = (value?: string | null) => value?.slice(0, 10) ?? "";
  return (
    edited.platform !== (current.platform ?? "FACEBOOK") ||
    edited.dailyBudget !== amount(current.dailyBudget) ||
    edited.mediaSpendLimit !== amount(current.mediaSpendLimit) ||
    edited.startDate !== date(current.startDate) ||
    edited.endDate !== date(current.endDate)
  );
}
