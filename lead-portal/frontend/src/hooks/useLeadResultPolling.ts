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
        if (!isCancelled && result.status === "COMPLETED") {
          onUpdate({
            ...lead,
            status: result.status,
            result: result.result ?? null,
            completedAt: result.completedAt ?? new Date().toISOString()
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
