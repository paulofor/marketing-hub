import fs from 'node:fs/promises';
import path from 'node:path';
import { CodexAppServerClient } from './codexAppServerClient.js';
import type { FashionImageGeneratorPort, FashionImageRequest, FashionImageResult } from './fashionImageGenerator.js';

type CodexImageGeneration = {
  savedPath?: string;
  result?: string;
  status?: string;
  revisedPrompt?: string;
};

export class CodexFashionImageGenerator implements FashionImageGeneratorPort {
  constructor(private readonly codexAppServerClient?: CodexAppServerClient) {}

  async generate(request: FashionImageRequest): Promise<FashionImageResult> {
    if (!request.sandboxDir) {
      return { error: 'FASHION_IMAGE_SANDBOX_DIR_REQUIRED' };
    }
    const client = this.codexAppServerClient;
    if (!client?.isReady()) {
      return { error: 'CODEX_APP_SERVER_UNAVAILABLE_FOR_IMAGE' };
    }
    try {
      const account = await client.request<Record<string, unknown>>('account/read', { refreshToken: false });
      if (!this.isAuthenticatedAccount(account)) {
        return { error: 'CODEX_NOT_AUTHENTICATED_FOR_IMAGE' };
      }
      const generation = await this.generateWithCodex(request, client);
      const imagePath = generation.savedPath ?? (await this.findGeneratedImage(request.sandboxDir));
      if (!imagePath) {
        return { error: generation.result || 'CODEX_IMAGE_EMPTY_PAYLOAD' };
      }
      return { imageUrl: await this.readImageAsDataUrl(imagePath, request.sandboxDir) };
    } catch (err) {
      return { error: err instanceof Error ? err.message : String(err) };
    }
  }

  private async generateWithCodex(
    request: FashionImageRequest,
    client: CodexAppServerClient,
  ): Promise<CodexImageGeneration> {
    const thread = await client.request<Record<string, unknown>>('thread/start', {
      model: process.env.FASHION_CHAT_CODEX_IMAGE_MODEL?.trim() || process.env.FASHION_CHAT_CODEX_MODEL?.trim() || 'gpt-5.5',
      cwd: request.sandboxDir,
      approvalPolicy: 'never',
      sandbox: process.env.CODEX_APP_SERVER_SANDBOX_MODE?.trim() || 'danger-full-access',
      serviceName: 'fashion_chat_image_service',
    });
    const threadId = this.extractId(thread, ['threadId', 'id']) ?? this.extractNestedId(thread, 'thread', ['threadId', 'id']);
    if (!threadId) {
      throw new Error('CODEX_IMAGE_THREAD_START_FAILED');
    }

    let completed = false;
    let failure: string | undefined;
    let latestGeneration: CodexImageGeneration = {};
    const unsubscribers = [
      client.onNotification('item/completed', (params) => {
        latestGeneration = this.extractImageGeneration(params) ?? latestGeneration;
      }),
      client.onNotification('turn/completed', (params) => {
        latestGeneration = this.extractImageGeneration(params) ?? latestGeneration;
        completed = true;
      }),
      client.onNotification('error', (params) => {
        failure = this.extractText(params) ?? 'CODEX_IMAGE_GENERATION_ERROR';
      }),
    ];
    try {
      await client.request<Record<string, unknown>>('turn/start', {
        threadId,
        input: [{ type: 'text', text: this.buildImageTurnPrompt(request.prompt) }],
      });
      await this.waitForTurn(() => completed, () => failure);
      if (failure) {
        throw new Error(failure);
      }
      return latestGeneration;
    } finally {
      unsubscribers.forEach((unsubscribe) => unsubscribe());
    }
  }

  private buildImageTurnPrompt(prompt: string): string {
    return [
      'Gere uma imagem agora usando a capacidade nativa de geração de imagem do Codex/App Server.',
      'Salve a imagem como arquivo PNG na sandbox atual com o nome fashion-visual.png.',
      'Nao use APIs externas, nao use chave OPENAI_API_KEY e nao responda apenas com prompt.',
      'A imagem deve ser croqui editorial premium de moda, fiel ao briefing abaixo.',
      '',
      prompt,
      '',
      'Ao terminar, responda somente um JSON curto com status e caminho salvo, se disponivel.',
    ].join('\n');
  }

  private extractImageGeneration(value: unknown): CodexImageGeneration | undefined {
    const found = this.findImageGenerationObject(value);
    if (!found) {
      return undefined;
    }
    return {
      savedPath: this.extractPath(found.savedPath),
      result: typeof found.result === 'string' ? found.result : undefined,
      status: typeof found.status === 'string' ? found.status : undefined,
      revisedPrompt:
        typeof found.revisedPrompt === 'string'
          ? found.revisedPrompt
          : typeof found.revised_prompt === 'string'
            ? found.revised_prompt
            : undefined,
    };
  }

  private findImageGenerationObject(value: unknown): Record<string, unknown> | undefined {
    if (!value || typeof value !== 'object') {
      return undefined;
    }
    if (Array.isArray(value)) {
      for (const item of value) {
        const found = this.findImageGenerationObject(item);
        if (found) {
          return found;
        }
      }
      return undefined;
    }
    const record = value as Record<string, unknown>;
    if (record.type === 'imageGeneration' || record.type === 'image_generation_call') {
      return record;
    }
    for (const item of Object.values(record)) {
      const found = this.findImageGenerationObject(item);
      if (found) {
        return found;
      }
    }
    return undefined;
  }

  private extractPath(value: unknown): string | undefined {
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
    if (value && typeof value === 'object') {
      const record = value as Record<string, unknown>;
      return this.extractPath(record.path) ?? this.extractPath(record.value);
    }
    return undefined;
  }

  private async findGeneratedImage(sandboxDir: string): Promise<string | undefined> {
    const preferred = path.join(sandboxDir, 'fashion-visual.png');
    try {
      await fs.access(preferred);
      return preferred;
    } catch {
      return undefined;
    }
  }

  private async readImageAsDataUrl(imagePath: string, sandboxDir: string): Promise<string> {
    const resolvedSandbox = path.resolve(sandboxDir);
    const resolvedImage = path.resolve(imagePath);
    if (!resolvedImage.startsWith(`${resolvedSandbox}${path.sep}`) && resolvedImage !== resolvedSandbox) {
      throw new Error('CODEX_IMAGE_PATH_OUTSIDE_SANDBOX');
    }
    const buffer = await fs.readFile(resolvedImage);
    return `data:${this.detectMimeType(resolvedImage, buffer)};base64,${buffer.toString('base64')}`;
  }

  private detectMimeType(filePath: string, buffer: Buffer): string {
    const lower = filePath.toLowerCase();
    if (buffer.length >= 8 && buffer.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) {
      return 'image/png';
    }
    if (buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff) {
      return 'image/jpeg';
    }
    if (lower.endsWith('.webp')) {
      return 'image/webp';
    }
    return 'image/png';
  }

  private async waitForTurn(isCompleted: () => boolean, getFailure: () => string | undefined): Promise<void> {
    const timeoutMs = Number.parseInt(process.env.CODEX_APP_SERVER_IMAGE_TURN_TIMEOUT_MS ?? process.env.CODEX_APP_SERVER_TURN_TIMEOUT_MS ?? '180000', 10);
    const start = Date.now();
    while (!isCompleted()) {
      const failure = getFailure();
      if (failure) {
        throw new Error(failure);
      }
      if (Date.now() - start > timeoutMs) {
        throw new Error('CODEX_IMAGE_TURN_TIMEOUT');
      }
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
  }

  private isAuthenticatedAccount(account: Record<string, unknown>): boolean {
    return Boolean(
      account.authMode ||
        account.auth_mode ||
        account.account ||
        account.login ||
        (account.connected === true && account.executable === true),
    );
  }

  private extractId(value: Record<string, unknown>, keys: string[]): string | undefined {
    for (const key of keys) {
      const candidate = value[key];
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate.trim();
      }
    }
    return undefined;
  }

  private extractNestedId(value: Record<string, unknown>, key: string, idKeys: string[]): string | undefined {
    const nested = value[key];
    if (!nested || typeof nested !== 'object') {
      return undefined;
    }
    return this.extractId(nested as Record<string, unknown>, idKeys);
  }

  private extractText(value: unknown): string | undefined {
    if (typeof value === 'string') {
      return value;
    }
    if (Array.isArray(value)) {
      return value.map((item) => this.extractText(item)).filter(Boolean).join('');
    }
    if (!value || typeof value !== 'object') {
      return undefined;
    }
    const record = value as Record<string, unknown>;
    for (const key of ['text', 'content', 'message', 'delta', 'summary', 'error']) {
      const text = this.extractText(record[key]);
      if (text) {
        return text;
      }
    }
    return undefined;
  }
}
