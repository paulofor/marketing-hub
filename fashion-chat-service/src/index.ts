import { CodexAppServerClient } from './codexAppServerClient.js';
import { createApp } from './server.js';

const port = Number.parseInt(process.env.PORT ?? '8094', 10);
const forceFallback = (process.env.FASHION_CHAT_FORCE_FALLBACK ?? 'false').toLowerCase() === 'true';
const codexEnabled = !forceFallback && (process.env.CODEX_APP_SERVER_ENABLED ?? 'false').toLowerCase() === 'true';
const codexAppServerClient = codexEnabled ? new CodexAppServerClient() : undefined;
const app = createApp(codexAppServerClient);

if (codexAppServerClient) {
  codexAppServerClient.start().catch((err) => {
    const message = err instanceof Error ? err.message : String(err);
    console.error(`Falha ao iniciar Codex App Server: ${message}`);
  });
}

const server = app.listen(port, () => {
  console.log(`Fashion chat service listening on port ${port}`);
});

async function shutdown() {
  await codexAppServerClient?.stop();
  server.close(() => process.exit(0));
}

process.once('SIGTERM', () => void shutdown());
process.once('SIGINT', () => void shutdown());
