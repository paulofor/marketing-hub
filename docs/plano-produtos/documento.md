# Área de membros externa com Kiwify + Java + Docker

> Documento de pesquisa e implementação para criar uma área de membros própria, em Java, recebendo eventos da Kiwify por webhook e rodando em container Docker.
>
> Data da pesquisa: 2026-05-08.

## 1. Resumo executivo

A Kiwify já oferece uma Área de Membros própria, mas também permite operar com uma **área de membros externa**. A forma mais simples e flexível de implementar uma área externa própria é:

1. usar a Kiwify como checkout, cobrança e origem dos eventos de venda/assinatura;
2. criar um endpoint HTTPS no seu backend Java para receber webhooks da Kiwify;
3. mapear eventos como `compra_aprovada`, `compra_reembolsada`, `chargeback`, `subscription_canceled`, `subscription_late` e `subscription_renewed` para liberar, pausar ou remover acesso;
4. armazenar todos os eventos recebidos de forma idempotente;
5. consultar a API pública da Kiwify quando precisar reconciliar dados, validar vendas ou fazer backfill.

Minha recomendação técnica é implementar o backend com **Spring Boot + PostgreSQL + Docker Compose**, com uma camada de autenticação própria para alunos e um módulo separado para processar webhooks.

---

## 2. O que foi encontrado na pesquisa sobre Kiwify

### 2.1 Área de membros externa

A documentação de integração com Memberkit confirma que a Memberkit é tratada como uma **área de membros externa** para hospedar cursos caso você não queira usar a Área de Membros da Kiwify.

Ponto importante: na integração Memberkit, a Kiwify informa que apenas produtos criados com a opção **“Área de membros externa”** aparecem para esse tipo de integração. Se o produto foi criado usando a área de membros da própria Kiwify, a documentação diz que não há como mudar o produto existente; a alternativa é criar um novo produto.

Na prática, para uma implementação própria, o conceito é parecido: você deve pensar no seu sistema Java como a área externa responsável por liberar conteúdo, enquanto a Kiwify fica responsável por venda, pagamento e eventos.

### 2.2 Webhooks da Kiwify

A Kiwify possui webhooks para enviar dados de um aplicativo para outro quando certos eventos acontecem. A própria documentação descreve webhook como uma forma de a Kiwify “conversar” com outro sistema, por exemplo quando uma compra é aprovada. Os webhooks são enviados em formato JSON.

A configuração manual fica em:

```text
Dashboard Kiwify → Apps → Webhooks → Criar Webhook
```

No webhook você escolhe:

- nome da configuração;
- URL que receberá os dados;
- produto ou todos os produtos;
- eventos que disparam o webhook;
- token/segredo da integração.

Eventos citados na API de criação de webhook:

```text
boleto_gerado
pix_gerado
carrinho_abandonado
compra_recusada
compra_aprovada
compra_reembolsada
chargeback
subscription_canceled
subscription_late
subscription_renewed
```

### 2.3 API pública da Kiwify

A Kiwify tem uma API pública REST, com base URL:

```text
https://public-api.kiwify.com
```

A autenticação usa token Bearer OAuth. Para chamadas autenticadas, a documentação exige:

```text
Authorization: Bearer <token>
x-kiwify-account-id: <account_id>
```

A API retorna JSON e possui rate limit documentado de 100 chamadas por minuto. Ela também permite consultar produtos e vendas, o que é útil para reconciliação.

Endpoints úteis para a área externa:

```text
POST /v1/oauth/token       # gerar token OAuth
GET  /v1/products          # listar produtos
GET  /v1/sales             # listar vendas
GET  /v1/sales/{id}        # consultar venda específica
POST /v1/webhooks          # criar webhook via API, se quiser automatizar
```

---

## 3. Arquitetura recomendada

```text
+-------------------+         JSON Webhook          +---------------------------+
|      Kiwify       |  ---------------------------> | Java API / Spring Boot    |
| Checkout/Pagto    |                               | /webhooks/kiwify          |
+-------------------+                               +------------+--------------+
                                                                  |
                                                                  v
                                                     +---------------------------+
                                                     | PostgreSQL                |
                                                     | users, memberships,      |
                                                     | webhook_events, audit    |
                                                     +------------+--------------+
                                                                  |
                                                                  v
                                                     +---------------------------+
                                                     | Área de membros web/mobile|
                                                     | Login, cursos, aulas     |
                                                     +---------------------------+
```

Componentes:

- **Kiwify**: checkout, cobrança, renovação, reembolso, chargeback e eventos.
- **Webhook Receiver**: endpoint público que recebe eventos Kiwify.
- **Access Service**: decide se libera, suspende ou remove acesso.
- **PostgreSQL**: banco principal para alunos, produtos, matrículas e eventos.
- **Área de membros**: interface onde o aluno acessa cursos/aulas.
- **Kiwify API Client**: cliente opcional para consultar vendas/produtos e reconciliar estados.

---

## 4. Mapeamento de eventos para regras de acesso

| Evento Kiwify | Ação recomendada no sistema externo |
|---|---|
| `compra_aprovada` | Criar usuário se não existir e liberar acesso ao produto comprado. |
| `subscription_renewed` | Renovar ou manter acesso ativo. Atualizar próxima data de expiração, se aplicável. |
| `subscription_late` | Marcar assinatura como atrasada. Você pode manter acesso em período de tolerância ou bloquear, conforme sua regra comercial. |
| `subscription_canceled` | Marcar assinatura como cancelada. Decidir se remove acesso imediatamente ou ao fim do período pago. |
| `compra_reembolsada` | Remover ou suspender acesso relacionado à venda reembolsada. |
| `chargeback` | Remover acesso imediatamente e registrar alerta antifraude. |
| `compra_recusada` | Não liberar acesso. Opcionalmente registrar tentativa para suporte/CRM. |
| `boleto_gerado` / `pix_gerado` | Não liberar acesso ainda; aguardar aprovação/pagamento. |
| `carrinho_abandonado` | Não liberar acesso; opcionalmente enviar para recuperação de carrinho. |

Regra de ouro: **libere acesso apenas depois de evento de pagamento aprovado ou venda validada como paga**.

---

## 5. Modelo de dados sugerido

### 5.1 `users`

```sql
create table users (
  id uuid primary key,
  email varchar(255) not null unique,
  name varchar(255),
  phone varchar(50),
  password_hash varchar(255),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
```

### 5.2 `products`

```sql
create table products (
  id uuid primary key,
  kiwify_product_id varchar(100) not null unique,
  name varchar(255) not null,
  slug varchar(255) not null unique,
  active boolean not null default true,
  created_at timestamptz not null default now()
);
```

### 5.3 `memberships`

```sql
create table memberships (
  id uuid primary key,
  user_id uuid not null references users(id),
  product_id uuid not null references products(id),
  kiwify_order_id varchar(100),
  status varchar(50) not null,
  starts_at timestamptz not null default now(),
  ends_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, product_id, kiwify_order_id)
);
```

Statuses sugeridos:

```text
ACTIVE
PAST_DUE
CANCELED
REFUNDED
CHARGEBACK
EXPIRED
```

### 5.4 `webhook_events`

```sql
create table webhook_events (
  id uuid primary key,
  provider varchar(50) not null,
  event_type varchar(100) not null,
  external_event_id varchar(150),
  external_order_id varchar(150),
  raw_payload jsonb not null,
  processed_at timestamptz,
  processing_status varchar(50) not null default 'RECEIVED',
  error_message text,
  created_at timestamptz not null default now(),
  unique(provider, external_event_id)
);
```

Observação: se o webhook não trouxer um ID único de evento, use uma chave idempotente derivada, por exemplo:

```text
provider + event_type + order_id + updated_at/status
```

---

## 6. Implementação Java com Spring Boot

### 6.1 Dependências recomendadas

No `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>

  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>

  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
</dependencies>
```

### 6.2 Configuração por ambiente

`application.yml`:

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/members}
    username: ${DATABASE_USER:members}
    password: ${DATABASE_PASSWORD:members}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

kiwify:
  webhook-token: ${KIWIFY_WEBHOOK_TOKEN}
  api:
    base-url: ${KIWIFY_API_BASE_URL:https://public-api.kiwify.com}
    client-id: ${KIWIFY_CLIENT_ID:}
    client-secret: ${KIWIFY_CLIENT_SECRET:}
    account-id: ${KIWIFY_ACCOUNT_ID:}
```

### 6.3 Controller do webhook

Use `JsonNode` no começo para ser resiliente a variações de payload.

```java
package com.example.members.kiwify;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/kiwify")
public class KiwifyWebhookController {

    private final KiwifyWebhookService service;
    private final KiwifyWebhookSecurity security;

    public KiwifyWebhookController(KiwifyWebhookService service, KiwifyWebhookSecurity security) {
        this.service = service;
        this.security = security;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody JsonNode payload, HttpServletRequest request) {
        security.validate(request, payload);
        service.receive(payload);
        return ResponseEntity.ok().build();
    }
}
```

### 6.4 Validação do token do webhook

A documentação mostra que a configuração do webhook possui um campo `token`, mas você deve confirmar no teste real da Kiwify como esse token chega ao seu servidor: header, query string ou campo no JSON.

Uma abordagem segura é aceitar apenas chamadas HTTPS e validar o segredo compartilhado conforme o formato observado no teste.

```java
package com.example.members.kiwify;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class KiwifyWebhookSecurity {

    private final String expectedToken;

    public KiwifyWebhookSecurity(@Value("${kiwify.webhook-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void validate(HttpServletRequest request, JsonNode payload) {
        // Ajuste o nome do header/campo depois de testar no painel da Kiwify.
        String received = request.getHeader("X-Kiwify-Token");

        if (!StringUtils.hasText(received) && payload.hasNonNull("token")) {
            received = payload.get("token").asText();
        }

        if (!StringUtils.hasText(received) || !received.equals(expectedToken)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid Kiwify webhook token");
        }
    }
}
```

### 6.5 Serviço idempotente de processamento

```java
package com.example.members.kiwify;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class KiwifyWebhookService {

    private final WebhookEventRepository events;
    private final MembershipService memberships;

    public KiwifyWebhookService(WebhookEventRepository events, MembershipService memberships) {
        this.events = events;
        this.memberships = memberships;
    }

    @Transactional
    public void receive(JsonNode payload) {
        String eventType = extractEventType(payload);
        String orderId = extractOrderId(payload);
        String externalEventId = extractEventIdOrBuildIdempotencyKey(payload, eventType, orderId);

        if (events.existsByProviderAndExternalEventId("KIWIFY", externalEventId)) {
            return;
        }

        WebhookEvent event = new WebhookEvent();
        event.setId(UUID.randomUUID());
        event.setProvider("KIWIFY");
        event.setEventType(eventType);
        event.setExternalEventId(externalEventId);
        event.setExternalOrderId(orderId);
        event.setRawPayload(payload.toString());
        event.setProcessingStatus("RECEIVED");
        events.save(event);

        switch (eventType) {
            case "compra_aprovada" -> memberships.grantAccessFromKiwify(payload);
            case "subscription_renewed" -> memberships.renewAccessFromKiwify(payload);
            case "subscription_late" -> memberships.markPastDueFromKiwify(payload);
            case "subscription_canceled" -> memberships.cancelFromKiwify(payload);
            case "compra_reembolsada" -> memberships.refundFromKiwify(payload);
            case "chargeback" -> memberships.chargebackFromKiwify(payload);
            default -> {
                // Eventos como boleto_gerado, pix_gerado, compra_recusada e carrinho_abandonado
                // podem ser apenas registrados, sem liberar acesso.
            }
        }

        event.setProcessingStatus("PROCESSED");
        event.setProcessedAt(OffsetDateTime.now());
        events.save(event);
    }

    private String extractEventType(JsonNode payload) {
        // Ajuste após observar o JSON real enviado pela Kiwify.
        return payload.path("event").asText(payload.path("trigger").asText("unknown"));
    }

    private String extractOrderId(JsonNode payload) {
        return payload.path("order_id").asText(
            payload.path("sale").path("id").asText(
                payload.path("id").asText(null)
            )
        );
    }

    private String extractEventIdOrBuildIdempotencyKey(JsonNode payload, String eventType, String orderId) {
        String eventId = payload.path("event_id").asText(null);
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }
        String updatedAt = payload.path("updated_at").asText(payload.path("sale").path("updated_at").asText(""));
        return eventType + ":" + orderId + ":" + updatedAt;
    }
}
```

### 6.6 Cliente para API da Kiwify

Use a API da Kiwify principalmente para:

- validar uma venda crítica antes de liberar acesso;
- reconciliar assinaturas/vendas;
- buscar produtos e montar tabela de relacionamento;
- rodar backfill caso um webhook falhe.

Exemplo simplificado com `WebClient`:

```java
package com.example.members.kiwify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KiwifyApiClient {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String accountId;

    public KiwifyApiClient(
        @Value("${kiwify.api.base-url}") String baseUrl,
        @Value("${kiwify.api.client-id}") String clientId,
        @Value("${kiwify.api.client-secret}") String clientSecret,
        @Value("${kiwify.api.account-id}") String accountId
    ) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accountId = accountId;
    }

    public String generateAccessToken() {
        return webClient.post()
            .uri("/v1/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters
                .fromFormData("client_id", clientId)
                .with("client_secret", clientSecret))
            .retrieve()
            .bodyToMono(KiwifyTokenResponse.class)
            .map(KiwifyTokenResponse::accessToken)
            .block();
    }

    public String getSaleRawJson(String saleId, String accessToken) {
        return webClient.get()
            .uri("/v1/sales/{id}", saleId)
            .header("Authorization", "Bearer " + accessToken)
            .header("x-kiwify-account-id", accountId)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    public record KiwifyTokenResponse(String access_token) {
        public String accessToken() {
            return access_token;
        }
    }
}
```

Em produção, não gere token a cada requisição. Faça cache do token até próximo do vencimento.

---

## 7. Dockerização

### 7.1 Dockerfile

```dockerfile
# Etapa de build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa de runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 7.2 `docker-compose.yml`

```yaml
services:
  app:
    build: .
    container_name: members-api
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SERVER_PORT: 8080
      DATABASE_URL: jdbc:postgresql://postgres:5432/members
      DATABASE_USER: members
      DATABASE_PASSWORD: members_secret
      KIWIFY_WEBHOOK_TOKEN: troque-este-token
      KIWIFY_API_BASE_URL: https://public-api.kiwify.com
      KIWIFY_CLIENT_ID: ${KIWIFY_CLIENT_ID}
      KIWIFY_CLIENT_SECRET: ${KIWIFY_CLIENT_SECRET}
      KIWIFY_ACCOUNT_ID: ${KIWIFY_ACCOUNT_ID}
    depends_on:
      - postgres

  postgres:
    image: postgres:16-alpine
    container_name: members-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: members
      POSTGRES_USER: members
      POSTGRES_PASSWORD: members_secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres_data:
```

### 7.3 Comandos

```bash
# Build e subida local
docker compose up --build

# Ver logs
docker compose logs -f app

# Teste de saúde, se você criar /health
curl http://localhost:8080/health
```

### 7.4 HTTPS em produção

A Kiwify deve chamar uma URL pública. Em produção, use HTTPS:

```text
https://membros.seudominio.com/webhooks/kiwify
```

Você pode colocar Nginx, Traefik, Caddy ou um load balancer cloud na frente do container Java. Não exponha webhook sensível apenas em HTTP.

---

## 8. Passo a passo de implantação

### Fase 1 — Preparar produto e eventos na Kiwify

1. Crie ou revise o produto na Kiwify.
2. Se a intenção for área externa desde o início, crie o produto já com a opção de área externa quando aplicável.
3. Defina quais eventos serão enviados.
4. Comece com estes eventos mínimos:

```text
compra_aprovada
compra_reembolsada
chargeback
subscription_canceled
subscription_late
subscription_renewed
```

### Fase 2 — Subir backend Java

1. Criar projeto Spring Boot.
2. Configurar PostgreSQL e Flyway.
3. Implementar `/webhooks/kiwify`.
4. Salvar payload bruto em `webhook_events`.
5. Criar regras de acesso em `memberships`.
6. Criar endpoint de login para o aluno.

### Fase 3 — Testar webhook

1. Primeiro teste com Request Bin/Webhook.site para observar o payload.
2. Depois aponte para seu endpoint Java.
3. No controller, registre o payload bruto.
4. Ajuste nomes dos campos usados para extrair evento, venda, produto, cliente e e-mail.
5. Valide o token/segredo conforme o formato real recebido.
6. Simule reenvio do mesmo evento para confirmar idempotência.

### Fase 4 — Reconciliar dados

Crie um job diário ou manual para:

1. consultar vendas na Kiwify por período;
2. comparar com `memberships` locais;
3. corrigir acessos ausentes ou indevidos;
4. alertar divergências.

---

## 9. Cuidados importantes

### 9.1 Idempotência

Webhooks podem ser reenviados. Seu endpoint não pode criar dois acessos para a mesma venda. Sempre salve uma chave única por evento.

### 9.2 Segurança

- Exija HTTPS.
- Valide token/segredo do webhook.
- Não exponha `client_secret` em código, imagem Docker ou repositório.
- Use variáveis de ambiente ou secret manager.
- Evite logs com CPF, telefone, endereço ou dados pessoais completos.
- Proteja endpoints administrativos.

### 9.3 LGPD

A API de vendas pode retornar dados pessoais do comprador, como nome, e-mail, CPF, telefone e endereço. Guarde apenas o necessário para a área de membros. Evite armazenar CPF se não for indispensável.

### 9.4 Observabilidade

Registre:

- evento recebido;
- tipo do evento;
- ID da venda;
- status de processamento;
- erro, se houver;
- tempo de processamento.

Use esses registros para suporte: “aluno pagou, mas não recebeu acesso”.

---

## 10. Exemplo de fluxo real

### Compra aprovada

```text
1. Cliente compra na Kiwify.
2. Kiwify dispara webhook compra_aprovada.
3. Java recebe JSON e valida token.
4. Java salva evento em webhook_events.
5. Java cria ou atualiza users pelo e-mail do comprador.
6. Java encontra o produto pelo kiwify_product_id.
7. Java cria membership ACTIVE.
8. Aluno recebe e-mail de primeiro acesso ou magic link.
```

### Reembolso

```text
1. Venda é reembolsada na Kiwify.
2. Kiwify dispara compra_reembolsada.
3. Java encontra membership por order_id/produto.
4. Java altera status para REFUNDED.
5. Aluno perde acesso ao conteúdo.
```

### Assinatura atrasada

```text
1. Cobrança de assinatura atrasa.
2. Kiwify dispara subscription_late.
3. Java marca membership como PAST_DUE.
4. Sistema aplica regra definida:
   - manter acesso por X dias; ou
   - bloquear imediatamente; ou
   - liberar apenas conteúdos básicos.
```

---

## 11. Estrutura inicial de projeto

```text
members-api/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/members/
        │   ├── MembersApplication.java
        │   ├── kiwify/
        │   │   ├── KiwifyWebhookController.java
        │   │   ├── KiwifyWebhookSecurity.java
        │   │   ├── KiwifyWebhookService.java
        │   │   └── KiwifyApiClient.java
        │   ├── membership/
        │   │   ├── Membership.java
        │   │   ├── MembershipService.java
        │   │   └── MembershipRepository.java
        │   └── user/
        │       ├── User.java
        │       ├── UserService.java
        │       └── UserRepository.java
        └── resources/
            ├── application.yml
            └── db/migration/
                ├── V1__create_users.sql
                ├── V2__create_products.sql
                ├── V3__create_memberships.sql
                └── V4__create_webhook_events.sql
```

---

## 12. Checklist de produção

- [ ] Produto criado/configurado na Kiwify para área externa, quando aplicável.
- [ ] Webhook criado em `Apps → Webhooks`.
- [ ] Endpoint público HTTPS funcionando.
- [ ] Token/segredo validado.
- [ ] Payload bruto salvo.
- [ ] Processamento idempotente.
- [ ] Eventos de reembolso, chargeback e cancelamento tratados.
- [ ] Job de reconciliação com API Kiwify.
- [ ] Logs sem dados sensíveis desnecessários.
- [ ] Backup do PostgreSQL.
- [ ] Monitoramento de erro no endpoint de webhook.
- [ ] Processo manual de suporte para liberar acesso em caso de falha.

---

## 13. Limitações e pontos a confirmar em teste real

1. A documentação pública mostra o campo `token` na configuração do webhook, mas não encontrei no material consultado a especificação oficial completa de como esse token é enviado ao seu endpoint. Confirme usando o botão de teste do webhook e ajuste a validação.
2. O payload real dos webhooks pode variar por evento. Por isso, comece salvando `raw_payload` e usando `JsonNode` até estabilizar os DTOs.
3. Para assinatura cancelada, a regra “bloquear imediatamente” ou “bloquear no fim do período pago” depende da sua política comercial.
4. Evite depender só de webhook. Use a API de vendas para reconciliação periódica.

---

## 14. Fontes consultadas

- Kiwify Help — Como funcionam os webhooks: https://ajuda.kiwify.com.br/pt-br/article/como-funcionam-os-webhooks-2ydtgl/
- Kiwify Help — Como integrar com o Memberkit: https://ajuda.kiwify.com.br/pt-br/article/como-integrar-com-o-memberkit-1dfehr3/
- Kiwify Help — Como funciona a API da Kiwify: https://ajuda.kiwify.com.br/pt-br/article/como-funciona-a-api-da-kiwify-1iosjhu/
- Kiwify API — Informações Gerais: https://docs.kiwify.com.br/api-reference/general
- Kiwify API — Criar webhook: https://docs.kiwify.com.br/api-reference/webhooks/create
- Kiwify API — Listar vendas: https://docs.kiwify.com.br/api-reference/sales/list
- Kiwify API — Consultar venda: https://docs.kiwify.com.br/api-reference/sales/single
- Kiwify API — Listar produtos: https://docs.kiwify.com.br/api-reference/products/list
