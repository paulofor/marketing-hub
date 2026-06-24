# Vitrines

> **Status: OBSOLETO / DESLIGADO.**
>
> Este módulo foi mantido no repositório apenas como histórico de protótipo/sandbox.
> Ele não deve receber novas evoluções, builds automáticos ou deploys até nova decisão de produto.
> O workflow de CI/CD foi removido para evitar publicação acidental de um módulo sem uso confirmado.

Estrutura inicial para o novo produto de vitrines com frontend em React 18 (Vite + TypeScript) e backend em Java 21 com Spring
Boot 3. Ambos possuem Dockerfiles próprios para gerar imagens que podem ser publicadas no mesmo host que executa o Portal Lead.

## Estrutura

- `frontend`: aplicação SPA construída com Vite, usando TanStack Query para chamadas HTTP e Zustand para estado global.
- `backend`: API Spring Boot com endpoint de saúde em `/vitrines/api/health`.
- Cada pasta possui um `Dockerfile` pronto para gerar as imagens publicáveis.

## Visão geral da arquitetura

### Componentes principais

- **Frontend (Vitrine/Portal)**: SPA em React 18 (Vite + TypeScript) que lista conteúdos, indica o que é gratuito ou premium e consome as APIs do backend.
- **API Backend (BFF/serviço de negócios)**: endpoints como `/conteudos`, `/conteudos/{id}` e `/checkout`, concentrando regras de autorização para definir o que cada usuário pode abrir.
- **Serviço de Autenticação/Identidade**: pode iniciar dentro do backend. Emite tokens JWT com roles/permissões embutidas.
- **Banco de Dados**: tabelas de usuários, conteúdos, planos/assinaturas, compras e permissões para sustentar RBAC e vínculos de acesso.
- **Storage de Arquivos**: buckets privados (S3/GCS). A API gera URLs assinadas e temporárias para download/visualização segura de conteúdos premium.
- **Gateway de Pagamento**: Stripe, Mercado Pago, PagSeguro etc., com webhooks notificando o backend na confirmação do pagamento.

### Identificação do lead por e-mail (magic link)

1. Gere um token de uso único (JWT ou similar) com `lead_id`, `email`, expiração curta (15–60 minutos) e, se necessário, uma flag `first_access`.
2. Inclua o token no link enviado por e-mail: `https://sua-vitrine.com/auth/magic?token=<TOKEN>`.
3. Ao clicar, o frontend chama `POST /auth/magic-login?token=...` no backend.
4. O backend valida assinatura, expiração e se o token já foi utilizado.
5. Se ok, cria ou atualiza o usuário, marca o token como usado e retorna a sessão (cookie ou JWT de sessão). A partir daí o lead está logado sem senha.

#### Sandbox `http://vitrineproduto.shop`

- O backend expõe `POST /vitrines/api/auth/magic-link` para gerar um token JWT assinado já apontando para `http://vitrineproduto.shop/auth/magic?token=...`.
- Dados de teste são semeados automaticamente:
  - `cliente@vitrineproduto.shop` (role `CLIENTE`, plano `plan_premium_ads` para abrir conteúdos premium).
  - `lead@vitrineproduto.shop` (role `LEAD`, sem plano vinculado para testar bloqueios).
- Exemplo de chamada local:

```bash
curl -X POST \
  http://localhost:8085/vitrines/api/auth/magic-link \
  -H 'Content-Type: application/json' \
  -d '{"email":"cliente@teste.com","role":"cliente","planId":"plan_consulting"}'
```

O JSON de resposta inclui `token`, `link`, `planId` e `expiresAt`, pronto para ser enviado por e-mail.

### Modelo de permissões

- Roles básicas (RBAC):
  - `ANON`: visitante sem login (opcional exibir cards públicos).
  - `LEAD`: logado sem pagamento (vê cards, mas não abre premium).
  - `CLIENTE`: pagou e abre o que comprou (plano ou conteúdo específico).
  - `ADMIN` (opcional): gerencia conteúdos e usuários.
- Modelo de dados simplificado:
  - `users`: id, email, role.
  - `plans`: id, name, type (assinatura, pagamento único).
  - `contents`: id, title, description, access_type (FREE/PREMIUM), `plan_id` (ou relação N:N) e `file_storage_path`.
  - `purchases`: id, user_id, plan_id, status (PENDING/PAID/CANCELLED), payment_provider e ids externos.
- A autorização pode usar a role e o status da compra (ex.: `CLIENTE` + `purchase.status = PAID`). Para granularidade futura, adicione uma tabela de entitlements.

### Fluxo de listagem vs. abertura de conteúdo

- **Listagem (GET /conteudos)**: retorna cards com título, descrição, rótulo `FREE/PREMIUM`, imagem/capa e um campo `locked` (true/false) calculado com base na role e nas compras do usuário. Permite exibir toda a vitrine estilo Netflix.
- **Abertura (GET /conteudos/{id})**:
  1. Backend checa se o usuário tem permissão (role + compra). Se não tiver, retorna 403 ou link para checkout.
  2. Para conteúdos premium, gera URL assinada e temporária apontando para o storage privado.
  3. Para gratuitos, retorna a URL pública ou o conteúdo diretamente.

## Execução local

### Backend

```bash
cd vitrines/backend
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8085/vitrines/api/health`.

### Frontend

```bash
cd vitrines/frontend
npm install
npm run dev
```

A UI fica disponível em `http://localhost:4173`. Configure a variável `VITE_API_BASE_URL` para apontar para o host do backend,
por exemplo `http://localhost:8085/vitrines/api`.

## Builds e deploy no mesmo host do Portal Lead

### Docker Compose local

Para subir a stack com frontend + backend no mesmo host da proxy pública, use o `docker-compose.yml` na raiz de `vitrines`:

```bash
cd vitrines
docker compose up -d
```

- Backend fica exposto em `http://localhost:8085/vitrines/api/health`.
- Frontend fica exposto em `http://localhost:4173/` e já faz proxy de `/vitrines/api` para o backend via Nginx interno.

Para forçar o uso das imagens publicadas em um registro (ex.: GHCR), combine com `docker-compose.deploy.yml`:

```bash
export VITRINES_BACKEND_IMAGE="<registry>/vitrines-backend:latest"
export VITRINES_FRONTEND_IMAGE="<registry>/vitrines-frontend:latest"
docker compose -f docker-compose.yml -f docker-compose.deploy.yml up -d
```

### Imagens Docker (build manual)

Os Dockerfiles foram preparados para gerar imagens independentes. Substitua `<registry>` pelo registro que publica no mesmo host
do Portal Lead.

```bash
# Backend
cd vitrines/backend
docker build -t <registry>/vitrines-backend:latest .

# Frontend (proxy interno para /vitrines/api)
cd ../frontend
docker build --build-arg VITE_API_BASE_URL=/vitrines/api -t <registry>/vitrines-frontend:latest .
```

### CI/CD

O workflow `.github/workflows/vitrines-ci.yml` executa testes, gera e publica as imagens no GHCR (`vitrines-backend` e `vitrines-frontend`) e, quando o branch é `main`, sincroniza a pasta `vitrines/` para o VPS do Lead Portal, aplicando o stack com Docker Compose.
