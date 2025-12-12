# Email Service

Microserviço responsável por orquestrar templates do Marketing Hub, montar mensagens com ativos hospedados no Cloudflare
Imagedelivery e realizar o envio automático de e-mails transacionais ou de campanhas.

## Principais funcionalidades

- Consumo de templates HTML hospedados no backend do Marketing Hub;
- Renderização de placeholders (`{{variavel}}` ou `${variavel}`) com dados enviados pelo cliente ou recuperados dinamicamente do backend;
- Download de imagens e anexos armazenados no Cloudflare (inline ou anexos tradicionais);
- Envio de e-mails utilizando SMTP (compatível com provedores como Amazon SES, SendGrid, Postmark, etc.);
- Registro de logs de envio em banco relacional (MySQL em produção, H2 em memória para desenvolvimento);
- Endpoints REST para envio unitário, envio em massa e consulta de status;
- Documentação OpenAPI disponível em `/swagger-ui.html`.

## Estrutura dos endpoints

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `POST` | `/api/v1/emails/send` | Envia um e-mail único |
| `POST` | `/api/v1/emails/bulk` | Envia e-mails em lote (até 500 por requisição) |
| `GET`  | `/api/v1/emails/{requestId}` | Consulta o status de um envio |

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
| `SPRING_DATASOURCE_URL` | URL do banco de dados | `jdbc:h2:mem:emailservice...` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `""` |
| `SPRING_MAIL_HOST` | Host SMTP | `localhost` |
| `SPRING_MAIL_PORT` | Porta SMTP | `1025` |
| `SPRING_MAIL_USERNAME` | Usuário SMTP | `""` |
| `SPRING_MAIL_PASSWORD` | Senha SMTP | `""` |
| `SPRING_MAIL_SMTP_AUTH` | Habilitar autenticação | `false` |
| `SPRING_MAIL_SMTP_STARTTLS_ENABLE` | Habilitar STARTTLS | `false` |
| `MARKETING_HUB_BASE_URL` | Base URL do backend Marketing Hub | `http://191.252.92.222:8000` |
| `MARKETING_HUB_TEMPLATES_PATH` | Caminho do endpoint de templates | `/api/v1/email-templates` |
| `MARKETING_HUB_AUTH_TOKEN` | Token Bearer para autenticação (opcional) | `""` |
| `CLOUDFLARE_BASE_URL` | Base da API de gerenciamento | `https://api.cloudflare.com/client/v4` |
| `CLOUDFLARE_DELIVERY_BASE_URL` | Base de entrega de imagens | `https://imagedelivery.net` |
| `CLOUDFLARE_DELIVERY_HASH` | Hash da conta no Image Delivery | `""` |
| `CLOUDFLARE_DEFAULT_VARIANT` | Variante default | `public` |
| `CLOUDFLARE_AUTH_TOKEN` | Token de acesso à API | `""` |
| `EMAIL_SERVICE_FROM_ADDRESS` | Remetente padrão | `no-reply@marketinghub.com.br` |
| `EMAIL_SERVICE_DRY_RUN` | Se `true`, não envia o e-mail e apenas registra log | `false` |

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
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/marketinghub \
  -e SPRING_DATASOURCE_USERNAME=marketing \
  -e SPRING_DATASOURCE_PASSWORD=senha \
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

---

Para mais detalhes, consulte o código fonte e adapte conforme as necessidades do seu fluxo de marketing digital.
