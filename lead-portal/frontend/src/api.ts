import {
  CreateLeadPayload,
  LeadDetails,
  LeadPortalFlow,
  LeadStatus
} from "./types";

function stripTrailingSlash(url: string): string {
  if (url === "/") {
    return "";
  }
  return url.endsWith("/") ? url.slice(0, -1) : url;
}

function resolveApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_URL?.trim();
  if (configured) {
    return stripTrailingSlash(configured);
  }

  const baseUrl = import.meta.env.BASE_URL ?? "/";
  const normalizedBase = stripTrailingSlash(baseUrl);
  return `${normalizedBase}/api`;
}

const API_BASE_URL = resolveApiBaseUrl();

function buildUrl(path: string): string {
  const base = API_BASE_URL;
  const sanitizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${base}${sanitizedPath}`;
}

export async function createLead(payload: CreateLeadPayload): Promise<LeadDetails> {
  const formData = new FormData();
  formData.append("name", payload.name);
  formData.append("email", payload.email);
  if (payload.notes) {
    formData.append("notes", payload.notes);
  }
  formData.append("image", payload.image);

  const response = await fetch(buildUrl("/leads"), {
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
  const response = await fetch(buildUrl(`/leads/${encodeURIComponent(id)}`));
  if (!response.ok) {
    const message = await extractError(response);
    throw new Error(message);
  }

  return (await response.json()) as LeadDetails;
}

export async function fetchLeadResult(
  id: string
): Promise<{ status: LeadStatus; result?: string | null; completedAt?: string | null }> {
  const response = await fetch(buildUrl(`/leads/${encodeURIComponent(id)}/result`));
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

export async function fetchLeadPortalFlow(slug: string): Promise<LeadPortalFlow> {
  const response = await fetch(buildUrl(`/flows/${encodeURIComponent(slug)}`));
  if (!response.ok) {
    const message = await extractError(response);
    throw new Error(message);
  }

  return (await response.json()) as LeadPortalFlow;
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
