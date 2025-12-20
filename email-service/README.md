# Email Service

Microserviço responsável por orquestrar templates do Marketing Hub, montar mensagens com ativos hospedados no Cloudflare
Imagedelivery e realizar o envio automático de e-mails transacionais ou de campanhas.

## Principais funcionalidades

- Consumo de templates HTML hospedados no backend do Marketing Hub;
- Renderização de placeholders (`{{variavel}}` ou `${variavel}`) com dados enviados pelo cliente ou recuperados dinamicamente do backend;
- Download de imagens e anexos armazenados no Cloudflare (inline ou anexos tradicionais);
- Envio de e-mails utilizando SMTP (compatível com provedores como Amazon SES, SendGrid, Postmark, etc.);
- Registro de logs de envio no banco relacional compartilhado com o backend (MySQL; opcionalmente H2 em memória em desenvolvimento);
- Endpoints REST para envio unitário, envio em massa e consulta de status;
- Documentação OpenAPI disponível em `/swagger-ui.html`.

O serviço compartilha o mesmo banco de dados do backend do Marketing Hub (`marketinghubdb`), utilizando a tabela `email_log` para
registrar os envios.

## Estrutura dos endpoints

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `POST` | `/api/v1/emails/send` | Envia um e-mail único |
| `POST` | `/api/v1/emails/bulk` | Envia e-mails em lote (até 500 por requisição) |
| `GET`  | `/api/v1/emails/{requestId}` | Consulta o status de um envio |
| `GET`  | `/api/v1/tracking/pixel/{requestId}.png` | Retorna o pixel transparente e marca a abertura do e-mail |

### Exemplo de payload

```json
{
  "to": ["cliente@example.com"],
  "cc": ["gestor@example.com"],
  "subject": "Atualização da campanha",
  "templateId": "template-campanha",
  "variables": {
    "nome": "João",
    "link": "https://marketinghub.com/oferta"
  },
  "attachments": [
    {
      "id": "b3c9f6a1-0a2b-43bd",
      "fileName": "banner.png",
      "contentType": "image/png",
      "inline": true,
      "contentId": "banner-principal",
      "variant": "public"
    }
  ]
}
```

## Variáveis de ambiente

| Variável | Descrição | Default |
|----------|-----------|---------|
| `SPRING_DATASOURCE_URL` | URL do banco de dados | `jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb?useSSL=false&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `marketing_hub_user` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `Ab9!rG4wX8_tMq2Bz7#HpK5V` |
| `SPRING_MAIL_HOST` | Host SMTP | `smtp.hostinger.com` |
| `SPRING_MAIL_PORT` | Porta SMTP | `465` |
| `SPRING_MAIL_USERNAME` | Usuário SMTP | `imagens@oportunidadebrasil.shop` |
| `SPRING_MAIL_PASSWORD` | Senha SMTP | `Russo007&` |
| `SPRING_MAIL_SMTP_AUTH` | Habilitar autenticação | `true` |
| `SPRING_MAIL_SMTP_STARTTLS_ENABLE` | Habilitar STARTTLS | `false` |
| `SPRING_MAIL_SMTP_SSL_ENABLE` | Habilitar SSL/TLS (porta 465) | `true` |
| `MARKETING_HUB_BASE_URL` | Base URL do backend Marketing Hub | `http://191.252.92.222:8000` |
| `MARKETING_HUB_CONNECT_TIMEOUT` | Timeout de conexão com o backend (ms) | `2000` |
| `MARKETING_HUB_READ_TIMEOUT` | Timeout de leitura do backend (ms) | `60000` |
| `MARKETING_HUB_TEMPLATES_PATH` | Caminho do endpoint de templates | `/api/v1/email-templates` |
| `MARKETING_HUB_AUTH_TOKEN` | Token Bearer para autenticação (opcional) | `""` |
| `CLOUDFLARE_BASE_URL` | Base da API de gerenciamento | `https://api.cloudflare.com/client/v4` |
| `CLOUDFLARE_DELIVERY_BASE_URL` | Base de entrega de imagens | `https://imagedelivery.net` |
| `CLOUDFLARE_DELIVERY_HASH` | Hash da conta no Image Delivery | `""` |
| `CLOUDFLARE_DEFAULT_VARIANT` | Variante default | `public` |
| `CLOUDFLARE_AUTH_TOKEN` | Token de acesso à API | `""` |
| `EMAIL_SERVICE_FROM_ADDRESS` | Remetente padrão | `imagens@oportunidadebrasil.shop` |
| `EMAIL_SERVICE_DRY_RUN` | Se `true`, não envia o e-mail e apenas registra log | `false` |
| `EMAIL_TRACKING_BASE_URL` | Base URL pública para gerar o pixel de rastreamento | `""` (rastreamento desativado) |
| `LEAD_PORTAL_DISPATCH_ENABLED` | Habilita o polling de pacotes do Lead Portal | `true` |
| `LEAD_PORTAL_DISPATCH_BATCH_SIZE` | Quantos pacotes buscar por ciclo | `3` |
| `LEAD_PORTAL_DISPATCH_INITIAL_DELAY` | Delay inicial do poll (ms) | `20000` |
| `LEAD_PORTAL_DISPATCH_POLL_INTERVAL` | Intervalo entre polls (ms) | `60000` |
| `LEAD_PORTAL_DISPATCH_READ_TIMEOUT` | Timeout de leitura da exportação do Lead Portal (ms) | `180000` |

### Configuração SMTP (Hostinger)

A configuração padrão agora está centralizada em `src/main/resources/application.yml` para evitar divergências com o `docker-compose`. Caso precise sobrescrever em produção ou em um `.env`, use os valores abaixo:

```bash
SPRING_MAIL_HOST=smtp.hostinger.com
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=imagens@oportunidadebrasil.shop
SPRING_MAIL_PASSWORD=Russo007&
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=false
SPRING_MAIL_SMTP_SSL_ENABLE=true
EMAIL_SERVICE_FROM_ADDRESS=imagens@oportunidadebrasil.shop
EMAIL_SERVICE_DRY_RUN=false
EMAIL_TRACKING_BASE_URL=https://email-service.exemplo.com/api/v1/tracking/pixel
```

Se o provedor exigir STARTTLS em vez de SSL/TLS puro, ajuste as variáveis conforme necessário.

## Execução local

```bash
cd email-service
mvn -s settings.xml spring-boot:run
```

> Caso não precise consumir artefatos privados do GitHub Packages, o parâmetro `-s settings.xml` pode ser removido.

A aplicação sobe em `http://localhost:8080` e você pode acessar a documentação em `http://localhost:8080/swagger-ui.html`.

Quando executado via `docker-compose`, o serviço fica disponível na porta `8086` do host para evitar conflitos locais.

### Testes

```bash
cd email-service
mvn -s settings.xml test
```

## Build e imagem Docker

Para gerar a imagem utilizando o Dockerfile multi-stage:

```bash
docker build -t marketinghub/email-service:latest -f email-service/Dockerfile email-service
```

Ao executar, lembre de injetar as configurações necessárias:

```bash
docker run --rm \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb?useSSL=false&serverTimezone=UTC \
  -e SPRING_DATASOURCE_USERNAME=marketing_hub_user \
  -e SPRING_DATASOURCE_PASSWORD=Ab9!rG4wX8_tMq2Bz7#HpK5V \
  -e SPRING_MAIL_HOST=email-smtp.us-east-1.amazonaws.com \
  -e SPRING_MAIL_PORT=587 \
  -e SPRING_MAIL_SMTP_AUTH=true \
  -e SPRING_MAIL_SMTP_STARTTLS_ENABLE=true \
  -e MARKETING_HUB_BASE_URL=https://backend.marketinghub.com.br \
  -e MARKETING_HUB_AUTH_TOKEN=seu-token \
  -e CLOUDFLARE_AUTH_TOKEN=token-cloudflare \
  -e CLOUDFLARE_DELIVERY_HASH=hash-da-conta \
  marketinghub/email-service:latest
```

## Integração com o ecossistema Marketing Hub

- **Marketing Hub Backend**: o serviço consome templates HTML e variáveis dinâmicas via APIs REST. Basta expor rotas compatíveis com os caminhos configurados (`/api/v1/email-templates/{id}` e `/variables`).
- **Cloudflare**: é esperado que os recursos estejam disponíveis via Image Delivery. Informe o `deliveryHash` ou passe a URL completa no payload de anexos.
- **Observabilidade**: o actuator expõe métricas e liveness/readiness (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`).

## Rastreamento de abertura

- Configure `EMAIL_TRACKING_BASE_URL` com a URL pública do serviço apontando para o endpoint `/api/v1/tracking/pixel` (ex.: `https://email-service.exemplo.com/api/v1/tracking/pixel`).
- A cada envio, o serviço injeta uma tag `<img>` invisível (`1x1`) no corpo HTML com o `requestId` do `EmailLog` anexado ao caminho (ex.: `/pixel/{requestId}.png`).
- Quando o pixel é requisitado, o endpoint retorna uma imagem PNG transparente, desabilita cache e registra o momento de abertura no campo `opened_at` do `EmailLog` correspondente.
- Se a base URL não estiver configurada, o pixel não é incluído e nenhum rastreamento é executado.

### Variáveis disponíveis no template

- Variáveis dinâmicas retornadas pelo backend do Marketing Hub para o template solicitado.
- Variáveis enviadas no payload (`variables`) da requisição de envio.
- `requestId`: identificador único do envio gerado pelo serviço.
- `trackingPixelUrl`: URL completa do pixel de rastreamento (disponível apenas quando `EMAIL_TRACKING_BASE_URL` está configurada).

---

Para mais detalhes, consulte o código fonte e adapte conforme as necessidades do seu fluxo de marketing digital.
