# Institutional Site

Site institucional estático da Digicom Digital utilizado para credibilidade corporativa, presença pública
da marca e processos de verificação comercial, como Meta Business, WhatsApp Business e provedores de
mensageria. O conteúdo é 100% estático e é servido por um container Nginx com cache e healthcheck.

## Conteúdo

- `public/`: HTML, CSS, JS e assets da Digicom Digital.
- `public/privacy-policy/`: política pública de privacidade da Digicom Digital.
- `nginx/default.conf`: cabeçalho de segurança, cache e endpoint `/healthz`.
- `Dockerfile`: empacota os arquivos estáticos em uma imagem `nginx:alpine` com healthcheck.

## CI/CD

Existe um workflow dedicado no GitHub Actions: **CI – Institutional Site** (`.github/workflows/institutional-site-ci.yml`).

- Em `pull_request` e `push` com alterações em `institutional-site/**`, o pipeline valida o build da imagem Docker.
- Em `push` para `main`, o pipeline faz deploy automático no servidor, sincronizando o conteúdo da pasta `institutional-site/` para `/root/institucional-site`, garantindo que a network Docker compartilhada exista e executando `docker compose build institutional-site && docker compose up -d institutional-site`.
- Caso o deploy precise usar outra pasta no servidor, configure o secret `INSTITUTIONAL_SITE_APP_DIR`.
- Após o deploy em `main`, o workflow valida a disponibilidade pública em `https://www.digicomdigital.com.br/`, `https://www.digicomdigital.com.br/healthz` e também confirma que `http://www.digicomdigital.com.br/` responde ou redireciona corretamente.

## Como executar localmente

```bash
cd institutional-site
docker compose build institutional-site
INSTITUTIONAL_SITE_PORT=8091 docker compose up -d institutional-site
curl -I http://localhost:8091/healthz
```

> Se você estiver no servidor, execute os comandos dentro de `/root/institucional-site` (e não em `/root`) para evitar o erro `no configuration file provided: not found`.

Para encerrar o serviço:

```bash
cd institutional-site
docker compose stop institutional-site && docker compose rm -f institutional-site
```

## Deploy no servidor (191.252.102.54)

1. Faça login via SSH no host e use a pasta de deploy dedicada `/root/institucional-site`.
2. Entre na pasta do deploy e confirme os serviços disponíveis:
   ```bash
   cd /root/institucional-site
   docker compose config --services
   ```
3. Atualize os arquivos (via `git pull` ou sincronização do pipeline), gere a nova imagem e publique o container:
   ```bash
   docker compose build institutional-site
   docker compose up -d institutional-site
   ```
4. O tráfego oficial chega via proxy reverso (porta 80/443) e termina em HTTPS (`https://www.digicomdigital.com.br`). Quando não houver proxy na frente, mantenha a porta configurada (padrão `8091`) liberada para testes diretos.
5. Valide acessos:
   ```bash
   curl -I https://www.digicomdigital.com.br/
   curl -I https://www.digicomdigital.com.br/privacy-policy/
   curl -I https://www.digicomdigital.com.br/healthz
   # fallback técnico direto no host/porta
   curl -I http://191.252.102.54:8091/healthz
   ```

O container não persiste estado. Qualquer alteração no conteúdo requer rebuild e `docker compose up -d`.

## Integração com o proxy do portal de pagamentos

- O arquivo `docker-compose.yml` conecta o serviço `institutional-site` a uma network nomeada `INSTITUTIONAL_SITE_NETWORK` (default `institutional-site-net`).
- Em produção, definimos `INSTITUTIONAL_SITE_NETWORK=public-net` e `INSTITUTIONAL_SITE_NETWORK_EXTERNAL=true` para reutilizar a mesma network externa que já expõe o proxy do `lead-portal-payments-service`.
- O workflow garante que a network exista antes de qualquer `docker compose up`. Localmente você não precisa fazer nada adicional, pois o Compose cria automaticamente a network interna padrão.
- O proxy (`lead-portal-payments-service/docker-compose*.yml`) pode compartilhar a mesma network e apontar um upstream dedicado para `institutional-site`. Assim, `https://www.digicomdigital.com.br` é entregue pelo container estático.
- Sempre que a network compartilhada for recriada manualmente, execute novamente `docker compose up -d institutional-site` para reconectar o container e permitir que o proxy resolva o host `institutional-site`.
