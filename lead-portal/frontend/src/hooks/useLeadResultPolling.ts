import { useEffect } from "react";
import { fetchLeadResult } from "../api";
import { LeadDetails } from "../types";

const POLLING_INTERVAL = 3000;

export function useLeadResultPolling(
  lead: LeadDetails | null,
  onUpdate: (lead: LeadDetails | null) => void
) {
  useEffect(() => {
    if (!lead || lead.status === "COMPLETED") {
      return;
    }

    let isCancelled = false;
    const interval = setInterval(async () => {
      try {
        const result = await fetchLeadResult(lead.id);
        if (isCancelled) {
          return;
        }

        const nextResult = result.result ?? lead.result ?? null;
        const nextCompletedAt = result.completedAt
          ?? (result.status === "COMPLETED"
            ? new Date().toISOString()
            : lead.completedAt ?? null);

        const hasStatusChanged = result.status !== lead.status;
        const hasResultChanged = (lead.result ?? null) !== nextResult;
        const hasCompletedAtChanged = (lead.completedAt ?? null) !== nextCompletedAt;

        if (hasStatusChanged || hasResultChanged || hasCompletedAtChanged) {
          onUpdate({
            ...lead,
            status: result.status,
            result: nextResult,
            completedAt: nextCompletedAt
          });
        }
      } catch (error) {
        console.error("Erro ao buscar resultado", error);
      }
    }, POLLING_INTERVAL);

    return () => {
      isCancelled = true;
      clearInterval(interval);
    };
  }, [lead, onUpdate]);
}
