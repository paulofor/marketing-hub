import cors from 'cors';
import express, { Request, Response } from 'express';
import morgan from 'morgan';
import { CodexAppServerClient } from './codexAppServerClient.js';
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
