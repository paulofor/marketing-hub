import { useLocation } from "react-router-dom";
import { useMemo } from "react";

const MAX_CAMPAIGN_LENGTH = 190;

export function useCampaignCode(): string | null {
  const location = useLocation();

  return useMemo(() => {
    const searchParams = new URLSearchParams(location.search);
    const candidate = searchParams.get("campaign") ?? searchParams.get("utm_campaign");
    if (!candidate) {
      return null;
    }
    const trimmed = candidate.trim();
    if (!trimmed) {
      return null;
    }
    return trimmed.length > MAX_CAMPAIGN_LENGTH
      ? trimmed.slice(0, MAX_CAMPAIGN_LENGTH)
      : trimmed;
  }, [location.search]);
}
