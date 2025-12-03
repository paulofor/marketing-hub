import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { apiBaseUrl } from "../../config/api";
import type {
  FlowSubmissionImagePackageStatus,
  LeadPortalImagePackage,
} from "./useLeadPortalSubmissions";

export interface LeadPortalImageReference {
  type: "ORIGINAL" | "GENERATED" | string;
  url?: string | null;
  downloadUrl?: string | null;
  accessType?: string | null;
  assetId?: number | null;
  position?: number | null;
  prompt?: string | null;
  model?: string | null;
  createdAt?: string | null;
  itemId?: number | null;
  storedFileName?: string | null;
}

export interface LeadPortalImagePackageDetail extends LeadPortalImagePackage {
  status: FlowSubmissionImagePackageStatus;
  submission: {
    flowSlug?: string | null;
    name?: string | null;
    email?: string | null;
    phone?: string | null;
    imageQuestionKey?: string | null;
  };
  originalImage?: LeadPortalImageReference | null;
  generatedImages: LeadPortalImageReference[];
}

const DEFAULT_PUBLIC_ASSETS_BASE_URL =
  "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev";

const envAssetsBaseUrl = import.meta.env.VITE_ASSETS_BASE_URL?.trim();

const publicAssetsBaseUrl = (() => {
  if (envAssetsBaseUrl === "api") {
    return apiBaseUrl;
  }

  if (envAssetsBaseUrl && envAssetsBaseUrl.length > 0) {
    return envAssetsBaseUrl;
  }

  return DEFAULT_PUBLIC_ASSETS_BASE_URL;
})();

const normalizedPublicAssetsBaseUrl = publicAssetsBaseUrl.replace(/\/+$/, "");

function buildPublicImageUrl(storedFileName?: string | null) {
  if (!storedFileName) return null;
  const normalizedFileName = storedFileName.startsWith("/")
    ? storedFileName.slice(1)
    : storedFileName;
  return `${normalizedPublicAssetsBaseUrl}/${normalizedFileName}`;
}

const knownAssetHosts = (() => {
  const hosts = new Set<string>();

  try {
    hosts.add(new URL(publicAssetsBaseUrl).hostname);
  } catch {
    // Ignore non-absolute asset base URLs (e.g., relative paths).
  }

  try {
    hosts.add(new URL(apiBaseUrl).hostname);
  } catch {
    // ignore invalid API base URLs (for example, relative paths)
  }

  if (typeof window !== "undefined") {
    hosts.add(window.location.hostname);
  }

  return hosts;
})();

function shouldUsePublicUrl(existingUrl?: string | null) {
  if (!existingUrl) return true;

  try {
    const parsed = new URL(existingUrl);
    return knownAssetHosts.has(parsed.hostname);
  } catch {
    // Relative or malformed URLs are treated as legacy and should be normalized.
    return true;
  }
}

function withPublicImageUrl(
  image?: LeadPortalImageReference | null,
): LeadPortalImageReference | null {
  if (!image) return null;

  const publicUrl = buildPublicImageUrl(image.storedFileName);
  if (!publicUrl) return image;

  return {
    ...image,
    url: shouldUsePublicUrl(image.url) ? publicUrl : image.url,
    downloadUrl: shouldUsePublicUrl(image.downloadUrl)
      ? publicUrl
      : image.downloadUrl,
  };
}

export function useLeadPortalImagePackageDetail(id?: number | null) {
  return useQuery<LeadPortalImagePackageDetail, Error>({
    queryKey: ["lead-portal-image-package", id],
    enabled: typeof id === "number" && id > 0,
    queryFn: async () => {
      if (!id) {
        throw new Error("Identificador do pacote é obrigatório");
      }
      const { data } = await axios.get<LeadPortalImagePackageDetail>(
        `/api/lead-portal/image-packages/${id}`,
      );
      return {
        ...data,
        originalImage: withPublicImageUrl(data.originalImage),
        generatedImages: data.generatedImages
          .map((image) => withPublicImageUrl(image))
          .filter(
            (image): image is LeadPortalImageReference => image !== null,
          ),
      };
    },
    staleTime: 30_000,
  });
}
