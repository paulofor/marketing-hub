export async function parseAssetUploadResponse(response: Response): Promise<string> {
  const locationHeader = response.headers.get("Location") ?? response.headers.get("Content-Location");
  if (locationHeader?.trim()) {
    return locationHeader.trim();
  }

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

    if (
      parsed &&
      typeof parsed === "object" &&
      "imageUrl" in parsed &&
      typeof parsed.imageUrl === "string"
    ) {
      return parsed.imageUrl;
    }

    if (
      parsed &&
      typeof parsed === "object" &&
      "data" in parsed &&
      parsed.data &&
      typeof parsed.data === "object" &&
      "url" in parsed.data &&
      typeof parsed.data.url === "string"
    ) {
      return parsed.data.url;
    }
  } catch {
    // Keep raw value when the backend already returns plain text.
  }

  return trimmedBody;
}
