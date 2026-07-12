import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import OpenAI from 'openai';
import { CodexAppServerClient } from './codexAppServerClient.js';
import { FashionResearchService, type FashionResearchResult } from './fashionResearch.js';

export interface FashionChatRequest {
  message: string;
  customerId?: string;
}

export interface FashionChatResponse {
  answer: string;
  mode: 'codex_app_server' | 'openai_direct' | 'deterministic_fallback';
  sandboxId: string;
  research: FashionResearchResult;
  warnings: string[];
}

export class FashionChatService {
  private readonly openai?: OpenAI;
  private readonly model: string;
  private readonly forceFallback: boolean;

  constructor(
    private readonly researchService: FashionResearchService,
    private readonly codexAppServerClient?: CodexAppServerClient,
  ) {
    this.model = process.env.FASHION_CHAT_MODEL?.trim() || 'gpt-4o-mini';
    this.forceFallback = (process.env.FASHION_CHAT_FORCE_FALLBACK ?? 'false').toLowerCase() === 'true';
    if (process.env.OPENAI_API_KEY) {
      this.openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
    }
  }

  async answer(request: FashionChatRequest): Promise<FashionChatResponse> {
    const message = request.message.trim();
    const sandboxDir = await fs.mkdtemp(path.join(os.tmpdir(), 'fashion-chat-'));
    const sandboxId = path.basename(sandboxDir);
    const research = await this.researchService.research(message);
    const prompt = this.buildPrompt(message, request.customerId, research);
    await fs.writeFile(path.join(sandboxDir, 'fashion-question.md'), prompt, 'utf-8');

    const warnings: string[] = [];
    if (!this.forceFallback && this.codexAppServerClient?.isReady()) {
      try {
        const answer = await this.answerWithCodexAppServer(prompt, sandboxDir);
        return { answer: this.cleanAnswer(answer), mode: 'codex_app_server', sandboxId, research, warnings };
      } catch (err) {
        warnings.push(`Codex App Server indisponivel para este turno: ${err instanceof Error ? err.message : String(err)}`);
      }
    } else if (!this.forceFallback) {
      warnings.push('Codex App Server nao esta pronto ou nao foi habilitado.');
    }

    if (!this.forceFallback && this.openai) {
      try {
        const answer = await this.answerWithOpenAI(prompt);
        return { answer: this.cleanAnswer(answer), mode: 'openai_direct', sandboxId, research, warnings };
      } catch (err) {
        warnings.push(`OpenAI direto falhou: ${err instanceof Error ? err.message : String(err)}`);
      }
    }

    return {
      answer: this.cleanAnswer(this.deterministicFallback(message, research)),
      mode: 'deterministic_fallback',
      sandboxId,
      research,
      warnings,
    };
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

  private async answerWithOpenAI(prompt: string): Promise<string> {
    if (!this.openai) {
      throw new Error('OPENAI_API_KEY ausente');
    }
    const response = await this.openai.chat.completions.create({
      model: this.model,
      messages: [{ role: 'user', content: prompt }],
      temperature: 0.3,
      max_tokens: 220,
    });
    return response.choices[0]?.message?.content ?? 'Nao consegui gerar uma resposta objetiva.';
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

  private deterministicFallback(message: string, research: FashionResearchResult): string {
    const lower = message.toLowerCase();
    if (lower.includes('trabalho') || lower.includes('reuniao') || lower.includes('reunião')) {
      return 'Use uma base neutra bem ajustada, como camisa ou blusa lisa, calca de alfaiataria e um terceiro elemento leve. Se quiser parecer mais moderna, escolha um ponto de cor em acessorio ou sapato. Qual e o grau de formalidade da reuniao?';
    }
    if (lower.includes('casamento') || lower.includes('festa')) {
      return 'Priorize tecido com bom caimento, cor que valorize seu tom de pele e acessorios discretos se a roupa ja tiver brilho. Evite branco em casamento e ajuste o look ao horario do evento. E de dia ou a noite?';
    }
    if (lower.includes('cor') || lower.includes('cores')) {
      return 'Comece por uma paleta simples: 2 cores neutras e 1 cor de destaque. Repita a cor de destaque em detalhe pequeno para o look parecer intencional. Voce prefere um visual discreto ou marcante?';
    }
    return `Minha recomendacao inicial: escolha uma peca principal confortavel, combine com neutros de bom caimento e adicione apenas um ponto de personalidade. ${research.sources[0]?.url === 'about:blank' ? 'Para refinar melhor, me diga ocasiao, clima e estilo desejado.' : 'Pela pesquisa, vale priorizar praticidade e coerencia visual. Qual ocasiao voce tem em mente?'}`;
  }

  private cleanAnswer(answer: string): string {
    return answer.replace(/\s+/g, ' ').trim().slice(0, 1200);
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
