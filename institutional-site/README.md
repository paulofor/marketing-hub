# Institutional Site

Site institucional estático do Marketing Hub utilizado para fins de credibilidade corporativa, análises
manuais e processos de verificação como o onboarding do Amazon SES. O conteúdo é 100% estático e é
servido por um container Nginx otimizado para cache e verificações de saúde.

## Conteúdo

- `public/`: HTML, CSS, JS e assets. Pode ser ajustado diretamente para atualizar cópia ou branding.
- `public/privacy-policy/`: reutiliza a política oficial de privacidade.
- `nginx/default.conf`: cabeçalho de segurança, cache e endpoint `/healthz`.
- `Dockerfile`: empacota os arquivos estáticos em uma imagem `nginx:alpine` com healthcheck.


## CI/CD

Existe um workflow dedicado no GitHub Actions: **CI – Institutional Site** (`.github/workflows/institutional-site-ci.yml`).

- Em `pull_request` e `push` com alterações em `institutional-site/**`, o pipeline valida o build da imagem Docker.
- Em `push` para `main`, o pipeline faz deploy automático no servidor, executando `git pull`, `docker compose build institutional-site` e `docker compose up -d institutional-site`.
- Caso o repositório no servidor não esteja em `/opt/marketinghub/app`, configure o secret `INSTITUTIONAL_SITE_APP_DIR`.
- Após deploy em `main`, o workflow valida disponibilidade pública em `http://www.vitrineproduto.online` e no endpoint `/healthz`.

## Como executar localmente

```bash
# Na raiz do repositório
docker compose build institutional-site
INSTITUTIONAL_SITE_PORT=8091 docker compose up -d institutional-site
curl -I http://localhost:8091/healthz
```

Para encerrar o serviço:

```bash
docker compose stop institutional-site && docker compose rm -f institutional-site
```

## Deploy no servidor (191.252.102.54)

1. Faça login via SSH no mesmo host que roda `lead-portal-payments-service`.
2. Atualize o repositório e gere a nova imagem:
   ```bash
   git pull origin main
   docker compose build institutional-site
   ```
3. Publique o container reutilizando a stack existente:
   ```bash
   docker compose up -d institutional-site
   ```
4. Publicação oficial: `http://www.vitrineproduto.online` apontando para o IP `191.252.102.54` (direto ou via proxy).
   Mantenha a porta configurada (padrão `8091`) liberada quando não houver proxy na frente.
5. Valide acessos:
   ```bash
   curl -I http://www.vitrineproduto.online/
   curl -I http://www.vitrineproduto.online/privacy-policy/
   curl -I http://www.vitrineproduto.online/healthz
   # fallback técnico direto no host/porta
   curl -I http://191.252.102.54:8091/healthz
   ```

O container não persiste estado. Qualquer alteração no conteúdo requer rebuild e `docker compose up -d`.
