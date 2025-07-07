import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Product {
  id: number;
  niche: string;
  avatar: string;
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

export function useProducts() {
  return useQuery({
    queryKey: ["products"],
    queryFn: async () => {
      const { data } = await axios.get<Product[]>("/api/products");
      return data;
    },
  });
}
