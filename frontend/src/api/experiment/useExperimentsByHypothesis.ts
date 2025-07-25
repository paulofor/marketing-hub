import { useExperimentsByNiche } from "./useExperimentsByNiche";

export function useExperimentsByHypothesis(
  nicheId?: string,
  hypothesisId?: string,
) {
  const { data, ...rest } = useExperimentsByNiche(nicheId);
  const filtered = Array.isArray(data)
    ? data.filter((e) => String(e.hypothesisId) === String(hypothesisId))
    : [];
  return { data: filtered, ...rest };
}
