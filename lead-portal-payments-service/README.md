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
| `SPRING_DATASOURCE_URL` | JDBC do MySQL (ou H2 em dev) | `jdbc:h2:mem:payments` (default) / `jdbc:mysql://...&allowPublicKeyRetrieval=true&connectTimeout=60000&socketTimeout=60000` (prod) |
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
| `SPRING_SQL_INIT_MODE` | Controla execução automática de scripts SQL no startup (`never` recomendado para base produtiva já provisionada) | `never` no profile `prod` |
| `SPRING_JPA_USE_JDBC_METADATA_DEFAULTS` | Define se o Hibernate deve consultar o `DatabaseMetaData` para detectar o dialeto automaticamente (mantenha `false` para evitar dependência do schema `information_schema` em produção) | `false` no profile `prod` |

## Perfis de execução

- **default**: utilizado automaticamente quando nenhum profile é informado. Usa o banco H2 em memória (`jdbc:h2:mem:lead_portal_payments;MODE=MySQL`) e carrega o schema simplificado (`schema-h2.sql`), o que permite subir o serviço sem depender de um MySQL externo.
- **prod**: replica a configuração anterior baseada em MySQL, incluindo a execução de `schema.sql` e do patch incremental em `db/changelog/lead-portal-purchase-add-checkout-expires.sql`. Esse profile já é habilitado no `docker-compose` e pode ser acionado manualmente com `SPRING_PROFILES_ACTIVE=prod` quando você precisar validar contra um banco MySQL real.

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

## Tela intermediária de checkout (`/checkout`)

- O serviço expõe uma página estática em `/checkout` (por padrão https://pagamentopalf.site/checkout) que funciona como ponte entre o e-mail enviado ao cliente e o checkout do Mercado Pago.
- A página espera ao menos o parâmetro `packageId` e consulta o endpoint interno `/api/v1/payments/packages/{packageId}` para obter valor, status e o `checkoutUrl` real.
- Quando chamada com `status`, `collection_status` ou outros parâmetros enviados pelo Mercado Pago, a UI exibe o retorno ao cliente (aprovado, pendente ou falha) reutilizando o mesmo pacote.
- Os parâmetros suportados são:
  - `packageId` (obrigatório): ID do pacote exportado pelo Lead Portal.
  - `purchaseId` (opcional): ID da compra já registrada, usado apenas para exibição.
  - `status`/`collection_status`: string retornada pelo Mercado Pago para mostrar mensagens de sucesso/falha.
  - `autoRedirect` (default `true`): define se a página deve redirecionar automaticamente para o Mercado Pago ao carregar a partir do e-mail.
- Configure `MERCADO_PAGO_SUCCESS_URL`, `MERCADO_PAGO_FAILURE_URL` e `MERCADO_PAGO_PENDING_URL` com `https://pagamentopalf.site/checkout`. O serviço anexa automaticamente `packageId` e `flow=success|failure|pending` antes de enviar a preferência ao Mercado Pago, garantindo que o redirecionamento tenha contexto.
- O `email-service` passa a apontar seus CTAs para essa mesma rota, adicionando `packageId` e `purchaseId`, o que garante uma experiência consistente em qualquer dispositivo.

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

O arquivo `docker-compose.yml` usa as variáveis definidas no `.env` (o Docker Compose já lê esse arquivo automaticamente quando presente) e expõe a aplicação em `http://localhost:8092` (pode alterar com `HOST_HTTP_PORT`). Em produção, o `docker-compose.deploy.yml` apenas substitui a imagem local pela publicada no registro.

### HTTPS e proxy reverso

- O compose agora inclui um **proxy Nginx** (porta 80/443) que termina o TLS com os certificados do Let’s Encrypt montados em `/etc/letsencrypt` e persiste tudo em `/etc/nginx/certs/live/<domínio>`. A configuração padrão está em `nginx.conf` e expõe `pagamentopalf.site` com redirecionamento automático para HTTPS. Em produção, montamos `/etc/nginx/certs` do host (que já possui o certificado válido de `vitrineproduto.online` em `/etc/nginx/certs/live/vitrineproduto.online/`) e mantemos o bind de `/etc/letsencrypt`; em ambiente local continuamos com os certificados autoassinados em `docker/proxy/certs/dev` para não quebrar o container.
> **Nota:** o proxy executa `docker/proxy/ensure-certs.sh` antes de iniciar o Nginx. O script agora itera sobre todos os domínios definidos (por padrão `pagamentopalf.site` e `vitrineproduto.online`), reaproveita os arquivos já disponíveis em `/etc/nginx/certs/live/<domínio>` quando existirem e, caso contrário, copia automaticamente de `/etc/letsencrypt` ou utiliza o fallback autoassinado. Assim que o certificado real for emitido com o profile `certbot`, basta recriar o serviço para que o arquivo atualizado seja detectado.
- O mesmo proxy também publica `https://vitrineproduto.online`, roteando para o container `institutional-site` ligado à network compartilhada (`public-net`). Certifique-se de que o compose do site esteja conectado a essa network (variáveis `INSTITUTIONAL_SITE_NETWORK=public-net` e `INSTITUTIONAL_SITE_NETWORK_EXTERNAL=true`) para que o upstream `institutional-site` seja resolvido corretamente.

- Use o profile `certbot` para emitir/renovar o certificado:

```bash
cd lead-portal-payments-service
CERTBOT_DOMAIN=pagamentopalf.site docker compose --profile certbot run --rm certbot-pagamentos
```

O caminho `./docker/proxy/html` é o webroot para o desafio ACME. Os certificados gerados no host são montados como `read-only` pelo Nginx.
- As URLs padrão do Mercado Pago no `docker-compose.deploy.yml` já apontam para HTTPS (`https://pagamentopalf.site/api/v1/mercadopago/webhook`), então configure o domínio público antes de ativar o fluxo de pagamentos.

## Build da imagem Docker

```bash
docker build -t marketinghub/lead-portal-payments-service:latest -f lead-portal-payments-service/Dockerfile lead-portal-payments-service
```

## CI/CD e publicação automática

O workflow `CI – Lead Portal Payments Service` (`.github/workflows/lead-portal-payments-ci.yml`) executa todo o pipeline sempre que houver alterações no módulo:

1. **Testes** – roda `mvn test` com Java 21.
2. **Build da imagem** – monta a imagem multi-stage e publica no GitHub Container Registry (`ghcr.io/<owner>/lead-portal-payments-service`) com tags `latest` e o SHA do commit, reaproveitando cache remoto.
3. **Deploy** – apenas em pushes para `main`, o GitHub Actions acessa o VPS `191.252.102.54`, cria um backup remoto de `docker/proxy/html`, sincroniza este diretório via `rsync`, força o login no GHCR, garante que a network Docker (`public-net` por padrão) exista, aplica `docker compose -f docker-compose.deploy.yml up -d --remove-orphans` e finaliza com `docker image prune -af` para remover imagens antigas. O compose de deploy é autônomo e usa somente a imagem publicada, sem herdar o `build` do compose local.

O `rsync` continua usando `--delete` para manter o serviço limpo, mas protege contra deleção os ativos comerciais públicos gerados pelo Marketing Hub antes de entrarem no `main`:

- `docker/proxy/html/downloads/**`
- `docker/proxy/html/sales-page-exp*.html`
- `docker/proxy/html/obrigado-exp*.html`

Essa proteção evita apagar páginas de venda, páginas premium de pós-compra e ZIPs de entrega publicados para experimentos em validação.

### Segredos necessários no GitHub

| Segredo | Descrição |
| --- | --- |
| `VPS_SSH_KEY` | Chave privada (ed25519) com acesso root ao host `191.252.102.54`. |
| `LEAD_PORTAL_PAYMENTS_REMOTE_PATH` *(opcional)* | Caminho remoto (default `/root/lead-portal-payments-service`). |
| `LEAD_PORTAL_PAYMENTS_NETWORK` *(opcional)* | Nome da network Docker a ser usada no VPS (default `public-net`). |
| `GHCR_USERNAME` *(opcional)* | Usuário para login no GHCR (por padrão usa o owner do repo). |
| `GHCR_TOKEN` *(opcional)* | Token com permissão `write:packages` para login no GHCR (padrão: `GITHUB_TOKEN`). |

> **Importante:** garanta que as variáveis listadas em `.env.example` estejam definidas no servidor (via `.env` ou exportadas no ambiente) antes de rodar o pipeline. Como o compose base não referencia mais `env_file`, a ausência do `.env` não interrompe o deploy, mas o serviço só funcionará se as credenciais obrigatórias estiverem presentes no ambiente.

## Observações

- O serviço lê as imagens originais direto do bucket R2 usado pelo Lead Portal. Configure as credenciais com as mesmas variáveis do restante do ecossistema (`LEAD_PORTAL_STORAGE_*`).
- O envio de e-mails é síncrono e simples (um link para o ZIP gerado). Se não houver `publicBaseUrl` configurado no bucket, o serviço acusará erro para que o time possa reenviar manualmente.
- O scheduler de entrega roda após uma confirmação de pagamento (`status` `APPROVED`), gera o ZIP e dispara o e-mail automaticamente.
