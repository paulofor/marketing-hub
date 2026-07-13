import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { CodexAppServerClient } from './codexAppServerClient.js';
import { FashionResearchService, type FashionResearchResult } from './fashionResearch.js';

export interface FashionChatRequest {
  message: string;
  customerId?: string;
}

export interface FashionChatResponse {
  answer: string;
  mode: 'codex_app_server' | 'local_fallback';
  sandboxId: string;
  research: FashionResearchResult;
}

export class FashionChatService {
  constructor(
    private readonly researchService: FashionResearchService,
    private readonly codexAppServerClient?: CodexAppServerClient,
  ) {}

  async answer(request: FashionChatRequest): Promise<FashionChatResponse> {
    const message = request.message.trim();
    const sandboxDir = await fs.mkdtemp(path.join(os.tmpdir(), 'fashion-chat-'));
    const sandboxId = path.basename(sandboxDir);
    const research = await this.researchService.research(message);
    const prompt = this.buildPrompt(message, request.customerId, research);
    await fs.writeFile(path.join(sandboxDir, 'fashion-question.md'), prompt, 'utf-8');

    if (this.shouldForceFallback() || !this.codexAppServerClient?.isReady()) {
      return { answer: this.buildFallbackAnswer(message, research), mode: 'local_fallback', sandboxId, research };
    }

    try {
      const answer = await this.answerWithCodexAppServer(prompt, sandboxDir);
      return { answer: this.cleanAnswer(answer), mode: 'codex_app_server', sandboxId, research };
    } catch (err) {
      if (this.isRecoverableCodexError(err)) {
        console.warn(`Fashion chat using local fallback: ${err instanceof Error ? err.message : String(err)}`);
        return { answer: this.buildFallbackAnswer(message, research), mode: 'local_fallback', sandboxId, research };
      }
      throw err;
    }
  }

  private async answerWithCodexAppServer(prompt: string, cwd: string): Promise<string> {
    const client = this.codexAppServerClient;
    if (!client) {
      throw new Error('CODEX_APP_SERVER_DISABLED');
    }
    const account = await client.request<Record<string, unknown>>('account/read', { refreshToken: false });
    if (!account?.authMode && !account?.auth_mode && !account?.account) {
      throw new Error('CODEX_NOT_AUTHENTICATED');
    }
    const thread = await client.request<Record<string, unknown>>('thread/start', {
      model: process.env.FASHION_CHAT_CODEX_MODEL?.trim() || 'gpt-5-codex',
      cwd,
      approvalPolicy: 'never',
      sandbox: process.env.CODEX_APP_SERVER_SANDBOX_MODE?.trim() || 'danger-full-access',
      serviceName: 'fashion_chat_service',
    });
    const threadId = this.extractId(thread, ['threadId', 'id']);
    if (!threadId) {
      throw new Error('CODEX_THREAD_START_FAILED');
    }

    let completed = false;
    let finalText = '';
    let failure: string | undefined;
    const unsubscribers = [
      client.onNotification('item/agentMessage/delta', (params) => {
        finalText += this.extractText(params) ?? '';
      }),
      client.onNotification('item/completed', (params) => {
        finalText = this.extractAgentMessageText(params) ?? finalText;
      }),
      client.onNotification('turn/completed', (params) => {
        finalText = this.extractText(params) ?? finalText;
        completed = true;
      }),
      client.onNotification('error', (params) => {
        failure = this.extractText(params) ?? 'CODEX_APP_SERVER_ERROR';
      }),
    ];
    try {
      await client.request<Record<string, unknown>>('turn/start', {
        threadId,
        input: [{ role: 'user', content: prompt }],
      });
      await this.waitForTurn(() => completed, () => failure);
      if (failure) {
        throw new Error(failure);
      }
      return finalText || 'Nao consegui gerar uma resposta neste turno.';
    } finally {
      unsubscribers.forEach((unsubscribe) => unsubscribe());
    }
  }

  private buildPrompt(message: string, customerId: string | undefined, research: FashionResearchResult): string {
    return [
      'Voce e uma consultora de moda do Marketing Hub.',
      'Objetivo: responder ao cliente com orientacao curta, objetiva, elegante e aplicavel.',
      'Nao escreva texto longo. Use no maximo 5 bullets curtos ou 1 paragrafo curto.',
      'Se faltar contexto, de a melhor recomendacao segura e faca uma unica pergunta de refinamento no final.',
      'Nao invente marcas, precos ou tendencias especificas sem evidencia.',
      '',
      `Cliente: ${customerId?.trim() || 'cliente-piloto'}`,
      `Pergunta: ${message}`,
      '',
      `Pesquisa web usada: ${research.query}`,
      research.summary,
      '',
      'Resposta final em portugues do Brasil:',
    ].join('\n');
  }

  private cleanAnswer(answer: string): string {
    return answer.replace(/\s+/g, ' ').trim().slice(0, 1200);
  }

  private shouldForceFallback(): boolean {
    return (process.env.FASHION_CHAT_FORCE_FALLBACK ?? 'false').toLowerCase() === 'true';
  }

  private isRecoverableCodexError(err: unknown): boolean {
    const message = err instanceof Error ? err.message : String(err);
    return (
      message.includes('CODEX_NOT_AUTHENTICATED') ||
      message.includes('CODEX_APP_SERVER') ||
      message.includes('Codex App Server') ||
      message.includes('Timeout em request account/read')
    );
  }

  private buildFallbackAnswer(message: string, research: FashionResearchResult): string {
    const lowerMessage = message.toLowerCase();
    if (this.isGreetingOnly(lowerMessage)) {
      return 'Oi! Me diga a ocasiao, o clima e uma peca que voce quer usar. Com isso eu monto uma recomendacao curta de look, cores e combinacao.';
    }
    const occasion = this.detectOccasion(lowerMessage);
    const palette = this.detectPalette(lowerMessage);
    const sourceHint = research.sources
      .find((source) => source.title !== 'Pesquisa web indisponivel' && source.snippet.trim())
      ?.snippet.replace(/\s+/g, ' ')
      .slice(0, 180);
    const evidence = sourceHint ? ` Considerando a pesquisa, mantenha a proposta atual e facil de executar: ${sourceHint}` : '';

    return this.cleanAnswer(
      [
        `Para ${occasion}, va de base ${palette}, caimento bem ajustado e uma terceira peca leve para deixar o look intencional.`,
        'Combine uma peca principal lisa com textura discreta ou acessorio de cor; isso melhora presenca sem parecer exagerado.',
        'Evite misturar muitas informacoes ao mesmo tempo: escolha uma prioridade entre conforto, elegancia ou impacto visual.',
        `${evidence} Se quiser refinar, me diga ocasiao, clima e uma peca que voce quer usar.`,
      ].join(' '),
    );
  }

  private detectOccasion(message: string): string {
    if (message.includes('trabalho') || message.includes('reuniao') || message.includes('reunião')) {
      return 'um compromisso profissional';
    }
    if (message.includes('festa') || message.includes('casamento') || message.includes('evento')) {
      return 'um evento social';
    }
    if (message.includes('encontro') || message.includes('jantar')) {
      return 'um encontro ou jantar';
    }
    if (message.includes('casual') || message.includes('dia a dia')) {
      return 'um visual casual';
    }
    return 'essa ocasiao';
  }

  private isGreetingOnly(message: string): boolean {
    const normalized = message
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^\w\s]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
    return ['oi', 'ola', 'olá', 'bom dia', 'boa tarde', 'boa noite'].includes(normalized);
  }

  private detectPalette(message: string): string {
    if (message.includes('colorid') || message.includes('cor')) {
      return 'neutra com um ponto de cor';
    }
    if (message.includes('preto')) {
      return 'preta com contraste suave';
    }
    if (message.includes('branco')) {
      return 'clara com contraste em acessorios';
    }
    return 'neutra';
  }

  private async waitForTurn(isCompleted: () => boolean, getFailure: () => string | undefined): Promise<void> {
    const timeoutMs = Number.parseInt(process.env.CODEX_APP_SERVER_TURN_TIMEOUT_MS ?? '180000', 10);
    const start = Date.now();
    while (!isCompleted()) {
      const failure = getFailure();
      if (failure) {
        throw new Error(failure);
      }
      if (Date.now() - start > timeoutMs) {
        throw new Error('CODEX_TURN_TIMEOUT');
      }
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
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

  private extractAgentMessageText(value: unknown): string | undefined {
    if (!value || typeof value !== 'object') {
      return undefined;
    }
    const record = value as Record<string, unknown>;
    return this.extractText(record.item) ?? this.extractText(record.message) ?? this.extractText(record);
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
    for (const key of ['text', 'content', 'message', 'delta', 'summary']) {
      const text = this.extractText(record[key]);
      if (text) {
        return text;
      }
    }
    return undefined;
  }
}
