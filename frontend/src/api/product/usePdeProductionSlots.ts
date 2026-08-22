import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type {
  PostDeployPdeProductionSlot,
  SavePdeProductionSlotRequest,
} from "../experiment/usePostDeployMonitor";

export interface PdeSlotValidationFeedback {
  success: boolean;
  message: string;
}

export function pdeSlotValidationFeedback(
  slot: PostDeployPdeProductionSlot,
): PdeSlotValidationFeedback {
  if (slot.validationStatus === "OK") {
    return {
      success: true,
      message: `URL da versão PDE ${slot.slotCode} validada.`,
    };
  }
  return {
    success: false,
    message:
      slot.validationSummary ||
      `A URL da versão PDE ${slot.slotCode} não passou na validação.`,
  };
}

export function useProductPdeProductionSlots(productId?: string | number) {
  return useQuery<PostDeployPdeProductionSlot[]>({
    queryKey: ["products", productId, "pde-production-slots"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<PostDeployPdeProductionSlot[]>(
        `/api/products/${productId}/pde-production-slots`,
      );
      return data;
    },
  });
}

export function useSaveProductPdeProductionSlot(productId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (variables: SavePdeProductionSlotRequest) => {
      const { data } = await axios.post<PostDeployPdeProductionSlot>(
        `/api/products/${productId}/pde-production-slots`,
        variables,
      );
      return data;
    },
    onSuccess: (slot) => {
      toast.success(`Versão PDE ${slot.slotCode} salva no produto.`);
      queryClient.invalidateQueries({
        queryKey: ["products", productId, "pde-production-slots"],
      });
    },
    onError: () => {
      toast.error("Não foi possível salvar a versão PDE do produto agora.");
    },
  });
}

export function useValidateProductPdeProductionSlot(
  productId?: string | number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (slotCode: string) => {
      const { data } = await axios.post<PostDeployPdeProductionSlot>(
        `/api/products/${productId}/pde-production-slots/${slotCode}/validate`,
      );
      return data;
    },
    onSuccess: (slot) => {
      const feedback = pdeSlotValidationFeedback(slot);
      if (feedback.success) {
        toast.success(feedback.message);
      } else {
        toast.error(feedback.message);
      }
      queryClient.invalidateQueries({
        queryKey: ["products", productId, "pde-production-slots"],
      });
    },
    onError: () => {
      toast.error("Não foi possível testar a URL produtiva PDE agora.");
    },
  });
}

export function usePublishProductPdeProductionSlot(
  productId?: string | number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      slotCode,
      experienceJson,
      publishedBy,
    }: {
      slotCode: string;
      experienceJson: string;
      publishedBy?: string;
    }) => {
      const { data } = await axios.post<PostDeployPdeProductionSlot>(
        `/api/products/${productId}/pde-production-slots/${slotCode}/publish`,
        { experienceJson, publishedBy },
      );
      return data;
    },
    onSuccess: (slot) => {
      toast.success(`Contrato PDE ${slot.slotCode} publicado.`);
      queryClient.invalidateQueries({
        queryKey: ["products", productId, "pde-production-slots"],
      });
    },
    onError: () => {
      toast.error("Não foi possível publicar o contrato PDE agora.");
    },
  });
}
