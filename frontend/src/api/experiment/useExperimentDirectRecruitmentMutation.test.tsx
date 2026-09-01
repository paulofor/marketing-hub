import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import axios from "axios";
import { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useSubmitDirectRecruitment } from "./useExperimentDirectRecruitment";

vi.mock("axios");

/** Cria um provedor de consultas sem retry para o teste da mutação pública. */
function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe("useSubmitDirectRecruitment", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("remove a identidade em claro antes de chamar o backend", async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        submissionId: 20,
        status: "QUALIFIED",
        qualified: true,
        message: "Perfil aderente",
        offerUrl: "https://rigel.example",
        remainingContacts: 14,
        sampleComplete: false,
      },
    });
    const { result } = renderHook(
      () =>
        useSubmitDirectRecruitment("11111111-2222-4333-8444-555555555555", 89),
      { wrapper },
    );

    await act(async () => {
      await result.current.mutateAsync({
        contactReference: "maria@example.com",
        submissionKey: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
        serviceSegment: "CONSULTING",
        weeklyConversationsRange: "ONE_TO_TEN",
        usesWhatsapp: true,
        decisionMaker: true,
        wantsPersonalizedImplementation: true,
        consentAccepted: true,
        consentVersion: "consent-v1",
      });
    });

    const payload = vi.mocked(axios.post).mock.calls[0][1] as Record<
      string,
      unknown
    >;
    expect(JSON.stringify(payload)).not.toContain("maria@example.com");
    expect(payload).not.toHaveProperty("contactReference");
    expect(payload.contactFingerprint).toBe(
      "ab11b96325a932145fce3c6d9629880f164cce846ad3eb81b4aa589bb6301534",
    );
  });
});
