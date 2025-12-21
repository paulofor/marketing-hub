# Lead Portal Payments Service

Microserviço responsável por acompanhar pagamentos via Mercado Pago, registrar compras de pacotes do Lead Portal e entregar por e-mail os arquivos originais (sem marca d'água) ao cliente.

## Principais funcionalidades

- Criação de preferências/links de pagamento no Mercado Pago para um pacote de imagens (`POST /api/v1/payments/checkout`).
- Recebimento de webhooks do Mercado Pago e atualização do status da compra (`POST /api/v1/mercadopago/webhook`).
- Geração de um ZIP com as imagens originais do pacote no bucket R2 configurado.
- Envio automático de e-mail com o link de download após a confirmação do pagamento.
- Endpoint manual para reenvio do e-mail (`POST /api/v1/payments/{purchaseId}/resend`).

## Variáveis de ambiente

| Variável | Descrição | Default |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC do MySQL (ou H2 em dev) | `jdbc:h2:mem:payments` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `""` |
| `MERCADO_PAGO_ACCESS_TOKEN` | Token de acesso do Mercado Pago | **obrigatório** |
| `MERCADO_PAGO_NOTIFICATION_URL` | URL pública do webhook | `http://localhost:8080/api/v1/mercadopago/webhook` |
| `MERCADO_PAGO_SUCCESS_URL` | Redirecionamento após sucesso | `""` |
| `MERCADO_PAGO_FAILURE_URL` | Redirecionamento após falha | `""` |
| `MERCADO_PAGO_PENDING_URL` | Redirecionamento para pagamentos pendentes | `""` |
| `LEAD_PORTAL_STORAGE_BUCKET` | Bucket R2 com os arquivos originais | `leadportal` |
| `LEAD_PORTAL_STORAGE_ENDPOINT` | Endpoint S3/R2 | `""` |
| `LEAD_PORTAL_STORAGE_ACCESS_KEY_ID` | Access key | `""` |
| `LEAD_PORTAL_STORAGE_SECRET_ACCESS_KEY` | Secret | `""` |
| `LEAD_PORTAL_STORAGE_REGION` | Região S3 | `auto` |
| `LEAD_PORTAL_STORAGE_PUBLIC_BASE_URL` | Base pública para gerar links | `""` (se vazio, o ZIP não terá link público) |
| `LEAD_PORTAL_ORIGINALS_PREFIX` | Prefixo para salvar ZIPs de originais | `originals` |
| `SPRING_MAIL_*` | Configuração SMTP para envio dos e-mails | - |
| `EMAIL_FROM_ADDRESS` | Remetente padrão | `no-reply@marketinghub` |
| `EMAIL_REPLY_TO` | Endereço de resposta | `""` |
| `EMAIL_SUBJECT` | Assunto do e-mail | `Suas imagens originais estão prontas` |
| `PAYMENTS_DEFAULT_AMOUNT` | Valor padrão quando o pacote não tem preço | `49.90` |
| `PAYMENTS_DEFAULT_CURRENCY` | Moeda padrão | `BRL` |
| `DELIVERY_ENABLED` | Habilita o scheduler de entrega | `true` |
| `DELIVERY_BATCH_SIZE` | Quantidade de compras processadas por ciclo | `3` |
| `DELIVERY_INITIAL_DELAY` | Delay inicial do scheduler (ms) | `20000` |
| `DELIVERY_FIXED_DELAY` | Intervalo entre ciclos (ms) | `60000` |

## Endpoints

- `POST /api/v1/payments/checkout`: cria a preferência no Mercado Pago e registra a compra. Body mínimo:

```json
{
  "packageId": 123,
  "buyerEmail": "cliente@example.com",
  "buyerName": "Cliente"
}
```

- `POST /api/v1/mercadopago/webhook`: endpoint para receber notificações do Mercado Pago (envie `id` em `data.id` ou como query param `id`).
- `POST /api/v1/payments/{purchaseId}/resend`: força o reenvio do e-mail com os originais.

## Execução local

```bash
cd lead-portal-payments-service
mvn spring-boot:run
```

O schema mínimo para a tabela de compras está em `src/main/resources/schema.sql`. Em produção, a base MySQL já utilizada pelos demais serviços pode ser reaproveitada.

## Execução com Docker Compose

Para rodar o serviço em contêiner localmente:

1. Duplique o arquivo `.env.example` para `.env` e ajuste as variáveis conforme a sua infraestrutura (MySQL, credenciais do Mercado Pago, SMTP, bucket, etc.).
2. Construa e suba o contêiner:

```bash
cd lead-portal-payments-service
docker compose up --build -d
```

O arquivo `docker-compose.yml` carrega automaticamente as variáveis do `.env` e expõe a aplicação em `http://localhost:8092` (pode alterar com `HOST_HTTP_PORT`). O arquivo `docker-compose.deploy.yml` apenas substitui a imagem local pela publicada no registro para ambientes de produção.

## Build da imagem Docker

```bash
docker build -t marketinghub/lead-portal-payments-service:latest -f lead-portal-payments-service/Dockerfile lead-portal-payments-service
```

## CI/CD e publicação automática

O workflow `CI – Lead Portal Payments Service` (`.github/workflows/lead-portal-payments-ci.yml`) executa todo o pipeline sempre que houver alterações no módulo:

1. **Testes** – roda `mvn test` com Java 21.
2. **Build da imagem** – monta a imagem multi-stage e publica no GitHub Container Registry (`ghcr.io/<owner>/lead-portal-payments-service`) com tags `latest` e o SHA do commit, reaproveitando cache remoto.
3. **Deploy** – apenas em pushes para `main`, o GitHub Actions acessa o VPS `191.252.102.54`, sincroniza este diretório via `rsync`, força o login no GHCR, garante que a network Docker (`public-net` por padrão) exista, aplica `docker compose -f docker-compose.yml -f docker-compose.deploy.yml up -d --remove-orphans` e finaliza com `docker image prune -af` para remover imagens antigas.

### Segredos necessários no GitHub

| Segredo | Descrição |
| --- | --- |
| `VPS_SSH_KEY` | Chave privada (ed25519) com acesso root ao host `191.252.102.54`. |
| `LEAD_PORTAL_PAYMENTS_REMOTE_PATH` *(opcional)* | Caminho remoto (default `/root/lead-portal-payments-service`). |
| `LEAD_PORTAL_PAYMENTS_NETWORK` *(opcional)* | Nome da network Docker a ser usada no VPS (default `public-net`). |
| `GHCR_USERNAME` *(opcional)* | Usuário para login no GHCR (por padrão usa o owner do repo). |
| `GHCR_TOKEN` *(opcional)* | Token com permissão `write:packages` para login no GHCR (padrão: `GITHUB_TOKEN`). |

> **Importante:** crie/atualize o arquivo `.env` no servidor com as mesmas chaves do `.env.example` antes de rodar o pipeline pela primeira vez. O deploy não sobe containers sem as variáveis obrigatórias.

## Observações

- O serviço lê as imagens originais direto do bucket R2 usado pelo Lead Portal. Configure as credenciais com as mesmas variáveis do restante do ecossistema (`LEAD_PORTAL_STORAGE_*`).
- O envio de e-mails é síncrono e simples (um link para o ZIP gerado). Se não houver `publicBaseUrl` configurado no bucket, o serviço acusará erro para que o time possa reenviar manualmente.
- O scheduler de entrega roda após uma confirmação de pagamento (`status` `APPROVED`), gera o ZIP e dispara o e-mail automaticamente.
