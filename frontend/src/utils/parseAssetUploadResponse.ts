export async function parseAssetUploadResponse(response: Response): Promise<string> {
  const rawBody = await response.text();
  const trimmedBody = rawBody.trim();

  if (!trimmedBody) {
    return "";
  }

  const lowerBody = trimmedBody.toLowerCase();
  if (lowerBody.startsWith("<!doctype") || lowerBody.startsWith("<html")) {
    return "";
  }

  try {
    const parsed = JSON.parse(trimmedBody);
    if (typeof parsed === "string") {
      return parsed;
    }
    if (
      parsed &&
      typeof parsed === "object" &&
      "url" in parsed &&
      typeof parsed.url === "string"
    ) {
      return parsed.url;
    }
  } catch {
    // Keep raw value when the backend already returns plain text.
  }

  return trimmedBody;
}

