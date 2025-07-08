import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface SuccessProduct {
  id: number;
  description: string;
  niche: string;
  avatar: string;
  instagramAccountId?: number;
  explicitPain: string;
  promise: string;
  uniqueMechanism: string;
  tripwire: string;
  riskReversal: string;
  socialProof: string;
  checkoutMonetization: string;
  funnel: string;
  creativeVolume: string;
  storytelling: string;
}

export function useSuccessProducts() {
  return useQuery({
    queryKey: ["successProducts"],
    queryFn: async () => {
      const { data } = await axios.get<SuccessProduct[]>(
        "/api/success-products",
      );
      return data;
    },
  });
}
