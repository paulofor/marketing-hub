import axios from "axios";

export interface AgentPortraitUpload {
  assetId: number;
  url: string;
}

/** Envia a figura mitológica do agente ao storage oficial. */
export async function uploadAgentPortrait(
  file: File,
): Promise<AgentPortraitUpload> {
  const body = new FormData();
  body.append("file", file);
  const { data } = await axios.post<AgentPortraitUpload>(
    "/api/agents/portrait",
    body,
  );
  return data;
}
