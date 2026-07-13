import cors from 'cors';
import express, { Request, Response } from 'express';
import morgan from 'morgan';
import { CodexAppServerClient } from './codexAppServerClient.js';
import { cancelCodexLogin, logoutCodexAccount, readCodexAccount, startCodexLogin } from './codexAppServerAuth.js';
import { FashionChatService } from './fashionChatService.js';
import { FashionResearchService } from './fashionResearch.js';

export function createApp(codexAppServerClient?: CodexAppServerClient) {
  const app = express();
  const chatService = new FashionChatService(new FashionResearchService(), codexAppServerClient);

  async function buildHealth() {
    if (!codexAppServerClient) {
      return {
        status: 'ok',
        service: 'fashion-chat-service',
        codexAppServer: { status: 'disabled', ready: false, restartAttempts: 0 },
      };
    }
    const codexHealth = codexAppServerClient.health();
    if (!codexAppServerClient.isReady()) {
      return { status: 'degraded', service: 'fashion-chat-service', codexAppServer: codexHealth };
    }
    try {
      const authentication = await codexAppServerClient.readAuthentication();
      return {
        status: authentication.authenticated ? 'ok' : 'degraded',
        service: 'fashion-chat-service',
        codexAppServer: { ...codexHealth, ...authentication },
      };
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      return {
        status: 'degraded',
        service: 'fashion-chat-service',
        codexAppServer: { ...codexHealth, authenticated: false, lastError: message },
      };
    }
  }

  if (process.env.NODE_ENV !== 'test') {
    app.use(morgan('combined'));
  }
  app.use(cors({ origin: true }));
  app.use(express.json({ limit: '200kb' }));

  app.get('/health', async (_req: Request, res: Response) => {
    res.json(await buildHealth());
  });

  app.get('/health/ready', async (_req: Request, res: Response) => {
    const health = await buildHealth();
    const status = health.status === 'ok' ? 200 : 503;
    res.status(status).json(health);
  });

  app.get('/codex-app-server/account/read', async (_req: Request, res: Response) => {
    if (!codexAppServerClient) {
      return res.status(503).json({
        connected: false,
        status: 'unavailable',
        executable: false,
        blockReason: 'CODEX_APP_SERVER_DISABLED',
      });
    }
    const state = await readCodexAccount(codexAppServerClient);
    const status = state.status === 'unavailable' ? 503 : 200;
    return res.status(status).json(state);
  });

  app.post('/codex-app-server/account/login/start', async (req: Request, res: Response) => {
    if (!codexAppServerClient) {
      return res.status(503).json({ status: 'unavailable', blockReason: 'CODEX_APP_SERVER_DISABLED' });
    }
    try {
      const requestedType = validateString(req.body?.type) ?? 'chatgptDeviceCode';
      const state = await startCodexLogin(codexAppServerClient, requestedType);
      return res.status(202).json(state);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'CODEX_LOGIN_FAILED';
      const status = message === 'CODEX_APP_SERVER_UNAVAILABLE' ? 503 : 502;
      return res.status(status).json({ status: 'failed', blockReason: message });
    }
  });

  app.post('/codex-app-server/account/login/cancel', async (req: Request, res: Response) => {
    if (!codexAppServerClient) {
      return res.status(503).json({ status: 'unavailable', blockReason: 'CODEX_APP_SERVER_DISABLED' });
    }
    const loginId = validateString(req.body?.loginId);
    if (!loginId) {
      return res.status(400).json({ status: 'failed', blockReason: 'CODEX_LOGIN_ID_REQUIRED' });
    }
    try {
      await cancelCodexLogin(codexAppServerClient, loginId);
      return res.json({ status: 'cancelled', loginId });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'CODEX_LOGIN_FAILED';
      const status = message === 'CODEX_APP_SERVER_UNAVAILABLE' ? 503 : 502;
      return res.status(status).json({ status: 'failed', blockReason: message });
    }
  });

  app.post('/codex-app-server/account/logout', async (_req: Request, res: Response) => {
    if (!codexAppServerClient) {
      return res.status(503).json({ status: 'unavailable', blockReason: 'CODEX_APP_SERVER_DISABLED' });
    }
    try {
      const state = await logoutCodexAccount(codexAppServerClient);
      return res.json(state);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'CODEX_APP_SERVER_UNAVAILABLE';
      const status = message === 'CODEX_APP_SERVER_UNAVAILABLE' ? 503 : 502;
      return res.status(status).json({ status: 'failed', blockReason: message });
    }
  });

  app.post('/api/fashion-chat/messages', async (req: Request, res: Response) => {
    const message = typeof req.body?.message === 'string' ? req.body.message.trim() : '';
    const customerId = typeof req.body?.customerId === 'string' ? req.body.customerId.trim() : undefined;
    if (!message) {
      return res.status(400).json({ error: 'message e obrigatorio' });
    }
    try {
      const response = await chatService.answer({ message, customerId });
      return res.json(response);
    } catch (err) {
      const messageText = err instanceof Error ? err.message : 'FASHION_CHAT_FAILED';
      console.error(`Fashion chat failed: ${messageText}`, err);
      const status = messageText.startsWith('CODEX_APP_SERVER') || messageText === 'CODEX_NOT_AUTHENTICATED' ? 503 : 500;
      return res.status(status).json({ error: messageText });
    }
  });

  return app;
}

function validateString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}
