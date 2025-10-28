import { CreateLeadPayload, LeadDetails, LeadStatus } from "./types";

const API_BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function createLead(payload: CreateLeadPayload): Promise<LeadDetails> {
  const formData = new FormData();
  formData.append("name", payload.name);
  formData.append("email", payload.email);
  if (payload.notes) {
    formData.append("notes", payload.notes);
  }
  formData.append("image", payload.image);

  const response = await fetch(`${API_BASE_URL}/api/leads`, {
    method: "POST",
    body: formData
  });

  if (!response.ok) {
    const message = await extractError(response);
    throw new Error(message);
  }

  return (await response.json()) as LeadDetails;
}

export async function fetchLead(id: string): Promise<LeadDetails> {
  const response = await fetch(`${API_BASE_URL}/api/leads/${id}`);
  if (!response.ok) {
    const message = await extractError(response);
    throw new Error(message);
  }

  return (await response.json()) as LeadDetails;
}

export async function fetchLeadResult(
  id: string
): Promise<{ status: LeadStatus; result?: string | null; completedAt?: string | null }> {
  const response = await fetch(`${API_BASE_URL}/api/leads/${id}/result`);
  if (!response.ok) {
    const message = await extractError(response);
    throw new Error(message);
  }

  return (await response.json()) as {
    status: LeadStatus;
    result?: string | null;
    completedAt?: string | null;
  };
}

async function extractError(response: Response): Promise<string> {
  try {
    const body = await response.json();
    if (typeof body.error === "string") {
      return body.error;
    }
    if (body.errors && typeof body.errors === "object") {
      return Object.values(body.errors).join(". ");
    }
  } catch (error) {
    console.error("Failed to parse error response", error);
  }
  return `Falha na requisição (${response.status})`;
}
