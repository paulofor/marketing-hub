export interface FashionImageRequest {
  prompt: string;
  sandboxId: string;
  sandboxDir?: string;
}

export interface FashionImageResult {
  imageUrl?: string;
  error?: string;
}

export interface FashionImageGeneratorPort {
  generate(request: FashionImageRequest): Promise<FashionImageResult>;
}
