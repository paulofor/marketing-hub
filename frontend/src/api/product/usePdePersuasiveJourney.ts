import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { PdePersuasiveJourney } from "./pdePersuasiveJourney";

export function usePdePersuasiveJourney(productSlug = "metodo-musa-7-dias") {
  return useQuery<PdePersuasiveJourney>({
    queryKey: ["products", productSlug, "pde-persuasive-journey"],
    queryFn: async () => {
      const { data } = await axios.get<PdePersuasiveJourney>(
        `/api/products/public/${productSlug}/pde-persuasive-journey`,
      );
      return data;
    },
    retry: false,
  });
}
