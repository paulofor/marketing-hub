import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { InstantForm } from "../hypothesis/useInstantFormsByHypothesis";

export function useInstantForm(id?: number | string | null) {
  return useQuery({
    queryKey: ["instant-form", id],
    enabled: id != null && id !== "",
    queryFn: async () => {
      if (id == null || id === "") return null;
      const { data } = await axios.get<InstantForm>(`/api/instant-forms/${id}`);
      return data;
    },
  });
}
