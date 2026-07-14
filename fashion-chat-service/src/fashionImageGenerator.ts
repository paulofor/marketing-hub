export interface FashionImageRequest {
  prompt: string;
  sandboxId: string;
}

export interface FashionImageResult {
  imageUrl?: string;
  error?: string;
}

export class FashionImageGenerator {
  private readonly apiKey = process.env.OPENAI_API_KEY?.trim();
  private readonly baseUrl = (process.env.OPENAI_BASE_URL?.trim() || 'https://api.openai.com/v1').replace(/\/$/, '');
  private readonly model = process.env.FASHION_CHAT_IMAGE_MODEL?.trim() || process.env.OPENAI_IMAGE_MODEL?.trim() || 'gpt-image-1';

  isEnabled(): boolean {
    return (process.env.FASHION_CHAT_GENERATE_IMAGES ?? 'false').toLowerCase() === 'true' && Boolean(this.apiKey);
  }

  async generate(request: FashionImageRequest): Promise<FashionImageResult> {
    if (!this.isEnabled()) {
      return {};
    }
    const body: Record<string, unknown> = {
      model: this.model,
      prompt: request.prompt,
      n: 1,
      size: process.env.FASHION_CHAT_IMAGE_SIZE?.trim() || '1024x1024',
    };
    if (this.supportsResponseFormat(this.model)) {
      body.response_format = 'b64_json';
    }
    try {
      console.info(`Fashion chat image request sandboxId=${request.sandboxId} model=${this.model}`);
      const response = await fetch(`${this.baseUrl}/images/generations`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${this.apiKey}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      });
      const payload = await response.json().catch(() => undefined);
      console.info(`Fashion chat image response sandboxId=${request.sandboxId} status=${response.status}`);
      if (!response.ok) {
        return { error: this.extractError(payload) || `OPENAI_IMAGE_HTTP_${response.status}` };
      }
      const imageUrl = this.extractImageUrl(payload);
      if (!imageUrl) {
        return { error: 'OPENAI_IMAGE_EMPTY_PAYLOAD' };
      }
      return { imageUrl };
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(`Fashion chat image generation failed sandboxId=${request.sandboxId}: ${message}`, err);
      return { error: message };
    }
  }

  private extractBase64(payload: unknown): string | undefined {
    if (!payload || typeof payload !== 'object') {
      return undefined;
    }
    const data = (payload as { data?: unknown }).data;
    if (!Array.isArray(data)) {
      return undefined;
    }
    const first = data[0] as { b64_json?: unknown } | undefined;
    return typeof first?.b64_json === 'string' && first.b64_json.trim() ? first.b64_json : undefined;
  }

  private extractImageUrl(payload: unknown): string | undefined {
    const b64 = this.extractBase64(payload);
    if (b64) {
      return `data:image/png;base64,${b64}`;
    }
    if (!payload || typeof payload !== 'object') {
      return undefined;
    }
    const data = (payload as { data?: unknown }).data;
    if (!Array.isArray(data)) {
      return undefined;
    }
    const first = data[0] as { url?: unknown } | undefined;
    return typeof first?.url === 'string' && first.url.trim() ? first.url : undefined;
  }

  private supportsResponseFormat(model: string): boolean {
    return !model.toLowerCase().startsWith('gpt-image-');
  }

  private extractError(payload: unknown): string | undefined {
    if (!payload || typeof payload !== 'object') {
      return undefined;
    }
    const error = (payload as { error?: unknown }).error;
    if (!error || typeof error !== 'object') {
      return undefined;
    }
    const message = (error as { message?: unknown }).message;
    return typeof message === 'string' ? message : undefined;
  }
}
