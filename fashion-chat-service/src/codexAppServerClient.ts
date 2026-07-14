import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import { EventEmitter } from 'node:events';
import readline from 'node:readline';

export type CodexAppServerHealthStatus = 'disabled' | 'starting' | 'ready' | 'degraded' | 'stopped';

type JsonObject = Record<string, unknown>;

type PendingRequest = {
  method: string;
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
  timeout: NodeJS.Timeout;
};

export interface CodexAppServerHealth {
  status: CodexAppServerHealthStatus;
  ready: boolean;
  restartAttempts: number;
  lastError?: string;
  initializedAt?: string;
  authenticated?: boolean;
  authMode?: string;
}

export class CodexAppServerError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CodexAppServerError';
  }
}

export class CodexAppServerClient {
  private readonly command: string;
  private readonly args: string[];
  private readonly env: NodeJS.ProcessEnv;
  private readonly requestTimeoutMs: number;
  private readonly notifications = new EventEmitter();
  private readonly pending = new Map<number, PendingRequest>();
  private process?: ChildProcessWithoutNullStreams;
  private nextId = 1;
  private status: CodexAppServerHealthStatus = 'stopped';
  private startPromise?: Promise<void>;
  private restartAttempts = 0;
  private lastError?: string;
  private initializedAt?: string;

  constructor() {
    this.command = process.env.CODEX_APP_SERVER_COMMAND ?? 'codex';
    this.args = process.env.CODEX_APP_SERVER_ARGS?.trim().split(/\s+/).filter(Boolean) ?? ['app-server', '--listen', 'stdio://'];
    this.env = { ...process.env };
    this.requestTimeoutMs = Number.parseInt(process.env.CODEX_APP_SERVER_REQUEST_TIMEOUT_MS ?? '60000', 10);
    this.notifications.on('error', (params) => {
      this.lastError = `Codex App Server error: ${this.stringifyForLog(params)}`;
      console.warn(this.lastError);
    });
  }

  async start(): Promise<void> {
    if (this.isReady()) {
      return;
    }
    if (this.startPromise) {
      return this.startPromise;
    }
    this.startPromise = this.startProcess();
    try {
      await this.startPromise;
    } finally {
      this.startPromise = undefined;
    }
  }

  async stop(): Promise<void> {
    this.status = 'stopped';
    this.rejectAllPending(new CodexAppServerError('Codex App Server parado'));
    const child = this.process;
    this.process = undefined;
    if (!child || child.killed) {
      return;
    }
    await new Promise<void>((resolve) => {
      child.once('exit', () => resolve());
      child.kill('SIGTERM');
      setTimeout(() => {
        if (!child.killed) {
          child.kill('SIGKILL');
        }
        resolve();
      }, 2000).unref();
    });
  }

  isReady(): boolean {
    return this.status === 'ready' && !!this.process && !this.process.killed;
  }

  health(): CodexAppServerHealth {
    return {
      status: this.status,
      ready: this.isReady(),
      restartAttempts: this.restartAttempts,
      lastError: this.lastError,
      initializedAt: this.initializedAt,
    };
  }

  async request<T>(method: string, params?: unknown): Promise<T> {
    if (!this.isReady()) {
      throw new CodexAppServerError('Codex App Server nao esta pronto');
    }
    return this.sendRequest<T>(method, params);
  }

  async readAuthentication(): Promise<{ authenticated: boolean; authMode?: string }> {
    const account = await this.request<Record<string, unknown>>('account/read', { refreshToken: false });
    const authMode = this.extractAuthMode(account);
    return { authenticated: this.isAuthenticatedAccount(account), authMode };
  }

  onNotification(method: string, listener: (params: unknown) => void): () => void {
    this.notifications.on(method, listener);
    return () => this.notifications.off(method, listener);
  }

  private async startProcess(): Promise<void> {
    this.status = 'starting';
    this.lastError = undefined;
    const child = spawn(this.command, this.args, {
      env: this.env,
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    this.process = child;

    const rl = readline.createInterface({ input: child.stdout });
    rl.on('line', (line) => this.handleLine(line));
    child.stderr.on('data', (chunk) => {
      const text = String(chunk).trim();
      if (text) {
        console.warn(`Codex App Server stderr: ${this.sanitize(text)}`);
      }
    });
    child.once('error', (err) => {
      this.lastError = err.message;
      this.status = 'degraded';
      this.rejectAllPending(err);
    });
    child.once('exit', (code, signal) => {
      rl.close();
      this.status = 'stopped';
      this.lastError = `Codex App Server encerrado code=${code ?? 'n/a'} signal=${signal ?? 'n/a'}`;
      this.rejectAllPending(new CodexAppServerError(this.lastError));
    });

    await this.sendRequest('initialize', {
      clientInfo: { name: 'fashion_chat_service', title: 'Fashion Chat Service', version: '0.1.0' },
    });
    this.sendNotification('initialized', {});
    this.status = 'ready';
    this.initializedAt = new Date().toISOString();
  }

  private sendRequest<T>(method: string, params?: unknown): Promise<T> {
    const id = this.nextId++;
    const child = this.process;
    if (!child || !child.stdin.writable) {
      return Promise.reject(new CodexAppServerError('Codex App Server sem stdin gravavel'));
    }
    const message: JsonObject = { method, id };
    if (params !== undefined) {
      message.params = params;
    }
    return new Promise<T>((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pending.delete(id);
        reject(new CodexAppServerError(`Timeout em request ${method}`));
      }, this.requestTimeoutMs);
      this.pending.set(id, { method, resolve: (value) => resolve(value as T), reject, timeout });
      child.stdin.write(`${JSON.stringify(message)}\n`, (err) => {
        if (err) {
          clearTimeout(timeout);
          this.pending.delete(id);
          reject(err);
        }
      });
    });
  }

  private sendNotification(method: string, params?: unknown): void {
    const child = this.process;
    if (!child || !child.stdin.writable) {
      throw new CodexAppServerError('Codex App Server sem stdin gravavel');
    }
    const message: JsonObject = { method };
    if (params !== undefined) {
      message.params = params;
    }
    child.stdin.write(`${JSON.stringify(message)}\n`);
  }

  private handleLine(line: string): void {
    const trimmed = line.trim();
    if (!trimmed) {
      return;
    }
    let message: JsonObject;
    try {
      message = JSON.parse(trimmed) as JsonObject;
    } catch {
      console.warn(`Codex App Server retornou JSON invalido: ${this.sanitize(trimmed)}`);
      return;
    }
    if (typeof message.id === 'number') {
      const pending = this.pending.get(message.id);
      if (!pending) {
        return;
      }
      clearTimeout(pending.timeout);
      this.pending.delete(message.id);
      if (message.error) {
        pending.reject(new CodexAppServerError(this.stringifyForLog(message.error)));
      } else {
        pending.resolve(message.result ?? message);
      }
      return;
    }
    if (typeof message.method === 'string') {
      this.notifications.emit(message.method, message.params ?? message);
    }
  }

  private rejectAllPending(error: unknown): void {
    for (const pending of this.pending.values()) {
      clearTimeout(pending.timeout);
      pending.reject(error);
    }
    this.pending.clear();
  }

  private stringifyForLog(value: unknown): string {
    return this.sanitize(JSON.stringify(value));
  }

  private extractAuthMode(account: Record<string, unknown>): string | undefined {
    for (const key of ['authMode', 'auth_mode', 'account']) {
      const value = account[key];
      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }
      if (value && typeof value === 'object') {
        return key;
      }
    }
    return undefined;
  }

  private isAuthenticatedAccount(account: Record<string, unknown>): boolean {
    if (this.extractAuthMode(account)) {
      return true;
    }
    return account.connected === true && account.executable === true;
  }

  private sanitize(value: string): string {
    return value.replace(/(token|secret|authorization|api[_-]?key)"?\s*[:=]\s*"[^"]+"/gi, '$1:[redacted]');
  }
}
