# Institutional Site

Site institucional estático do Marketing Hub utilizado para fins de credibilidade corporativa, análises
manuais e processos de verificação como o onboarding do Amazon SES. O conteúdo é 100% estático e é
servido por um container Nginx otimizado para cache e verificações de saúde.

## Conteúdo

- `public/`: HTML, CSS, JS e assets. Pode ser ajustado diretamente para atualizar cópia ou branding.
- `public/privacy-policy/`: reutiliza a política oficial de privacidade.
- `nginx/default.conf`: cabeçalho de segurança, cache e endpoint `/healthz`.
- `Dockerfile`: empacota os arquivos estáticos em uma imagem `nginx:alpine` com healthcheck.

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
4. Aponte o DNS público para o IP 191.252.102.54 (ou exponha via proxy existente) e libere a porta
   configurada (padrão `8091`).
5. Valide acessos:
   ```bash
   curl -I http://191.252.102.54:8091/
   curl -I http://191.252.102.54:8091/privacy-policy/
   curl -I http://191.252.102.54:8091/healthz
   ```

O container não persiste estado. Qualquer alteração no conteúdo requer rebuild e `docker compose up -d`.
