import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import NicheDetailPage from "./NicheDetailPage";

const mocks = vi.hoisted(() => ({
  requestPixel: vi.fn(),
}));

vi.mock("../../api/niche/useNiche", () => ({
  useNiche: () => ({
    data: {
      id: 34,
      name: "Pele madura",
      description: "",
      interestsToGenerate: 0,
      jobTitlesToGenerate: 0,
      behaviorsToGenerate: 0,
      hypothesesToGenerate: 0,
      facebookPixelId: null,
      facebookPixelCode: null,
      facebookPixelCreatedAt: null,
      facebookPixelRequestedAt: null,
      facebookPixelRequestStatus: null,
    },
    isLoading: false,
    isFetching: false,
    refetch: vi.fn(),
  }),
}));
vi.mock("../../api/niche/useUpdateNiche", () => ({
  useUpdateNiche: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));
vi.mock("../../api/hypothesis/useHypothesesByNiche", () => ({
  useHypothesesByNiche: () => ({ data: [] }),
}));
vi.mock("../../api/targeting/useTargetingElementsByNiche", () => ({
  useTargetingElementsByNiche: () => ({
    data: [],
    isFetching: false,
    refetch: vi.fn(),
  }),
}));
vi.mock("../../api/chatDialog/useChatDialog", () => ({
  useChatDialog: () => ({ data: null }),
}));
vi.mock("../../api/experiment/useExperimentsByNiche", () => ({
  useExperimentsByNiche: () => ({ data: [] }),
}));
vi.mock("../../api/leadPortal/useLeadPortalFlows", () => ({
  useLeadPortalFlows: () => ({
    data: [],
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
}));
vi.mock("../../api/openAiModel/useOpenAiModels", () => ({
  useOpenAiModels: () => ({ data: [], isLoading: false }),
}));
vi.mock("../../api/informationSource/useInformationSourcesByNiche", () => ({
  useInformationSourcesByNiche: () => ({ data: [] }),
}));
vi.mock("../../api/niche/useRequestFacebookPixel", () => ({
  useRequestFacebookPixel: () => ({
    mutateAsync: mocks.requestPixel,
    isPending: false,
  }),
}));
vi.mock("../../api/informationSource/useCreateInformationSource", () => ({
  useCreateInformationSource: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));
vi.mock("../../app/breadcrumbs", () => ({ useBreadcrumbs: vi.fn() }));
vi.mock("../../components/TargetingElementCard", () => ({
  TargetingElementCard: () => null,
}));
vi.mock("../../components/TargetingGenerationForm", () => ({
  TargetingGenerationForm: () => null,
}));
vi.mock("../../components/TargetingRequestStatusPanel", () => ({
  TargetingRequestStatusPanel: () => null,
}));
vi.mock("../../components/leadPortal/SimpleLeadPortalFormCard", () => ({
  default: () => null,
}));
vi.mock("./NicheLearningDictionaryCard", () => ({
  NicheLearningDictionaryCard: () => null,
}));
vi.mock("./NicheBacklogRecommendationsCard", () => ({
  NicheBacklogRecommendationsCard: () => null,
}));
vi.mock("react-toastify", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

function renderPage() {
  render(
    <MemoryRouter initialEntries={["/niches/34"]}>
      <Routes>
        <Route path="/niches/:nicheId" element={<NicheDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("NicheDetailPage", () => {
  beforeEach(() => {
    mocks.requestPixel.mockReset();
    mocks.requestPixel.mockResolvedValue({});
  });

  it("exibe o comando de criação quando o nicho ainda não possui pixel", async () => {
    renderPage();

    expect(screen.getAllByText("Pixel do Facebook")).not.toHaveLength(0);
    expect(
      screen.getByText("Nenhum pixel gerado ainda para este nicho."),
    ).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Solicitar pixel" }));

    await waitFor(() => expect(mocks.requestPixel).toHaveBeenCalledTimes(1));
  });
});
