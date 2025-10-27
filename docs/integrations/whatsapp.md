# Consolidação de mensagens WhatsApp

A console de WhatsApp centraliza o cadastro da conta Meta Cloud, o envio de mensagens manuais (texto e imagem) e o acompanhamento do histórico recebido pelo webhook oficial.

## Configuração da conta

1. Acesse **Configurações → WhatsApp** no front-end.
2. Informe os campos obrigatórios:
   - **Nome de exibição**: usado para identificar a conta internamente.
   - **Phone Number ID**: identificador retornado pela Meta Cloud API.
   - **Access Token**: token permanente ou de longa duração com permissão para envio pelo número configurado.
3. Opcionalmente preencha:
   - **Telefone comercial** para referência interna.
   - **Business Account ID**.
   - **Verify Token**: token que será validado na assinatura do webhook.
   - **Base URL**: deixe em branco para usar `https://graph.facebook.com/v18.0`.
4. Marque a conta como **ativa** para habilitar o disparo via jornadas e via console. Apenas uma conta pode permanecer ativa; ao ativar uma nova, as demais são desativadas automaticamente.
5. Salve as alterações. O backend persiste os dados em `whatsapp_account`, reutilizando-os em todas as integrações.【F:backend/ads-service/src/main/java/com/marketinghub/whatsapp/service/WhatsAppAccountService.java†L18-L76】【F:backend/ads-service/src/main/resources/db/changelog/V2028_02_01__create_whatsapp_tables.sql†L1-L40】

## Webhook Meta Cloud

- Endpoint de verificação e recepção: `POST`/`GET /api/whatsapp/webhook`.
- Durante a assinatura, informe o **Verify Token** cadastrado e confirme o desafio (`hub.challenge`).【F:backend/ads-service/src/main/java/com/marketinghub/whatsapp/web/WhatsAppWebhookController.java†L17-L44】
- Mensagens e eventos de status são consolidados automaticamente na tabela `whatsapp_message`, preservando contexto, payload bruto e anexos recebidos.【F:backend/ads-service/src/main/java/com/marketinghub/whatsapp/service/WhatsAppMessagingService.java†L129-L207】【F:backend/ads-service/src/main/resources/db/changelog/V2028_02_01__create_whatsapp_tables.sql†L42-L63】

## Envio de mensagens

- A aba **Enviar mensagem** permite disparar textos e imagens on-demand.
- Campos obrigatórios variam conforme o tipo selecionado:
  - **Texto**: número destino e corpo da mensagem.
  - **Imagem**: número destino e URL pública da imagem; legenda opcional.
- Todos os envios são registrados como mensagens de saída e vinculados à conta ativa.【F:frontend/src/pages/whatsapp/WhatsAppConsolePage.tsx†L191-L286】【F:backend/ads-service/src/main/java/com/marketinghub/whatsapp/service/WhatsAppMessagingService.java†L78-L128】

## Integração com jornadas

- O handler de jornada (`WhatsAppChannelHandler`) reutiliza a conta ativa e grava os envios na mesma trilha de auditoria, incluindo suporte a mensagens de texto, imagem e templates com componentes personalizados.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/WhatsAppChannelHandler.java†L21-L157】
- Falhas transitórias (429/5xx) retornam recomendações de retry com base no cabeçalho `Retry-After`; erros definitivos interrompem o passo.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/WhatsAppChannelHandler.java†L60-L111】

## Painel de mensagens

- O painel lista o histórico paginado (25 itens por página) com filtros de direção (entrada/saída).
- Cada registro exibe horário, canal, contato, status, payload e pré-visualização de anexos quando aplicável.【F:frontend/src/pages/whatsapp/WhatsAppConsolePage.tsx†L289-L409】
- O front-end consome `/api/whatsapp/messages` e `/api/whatsapp/messages/send`, invalidando o cache a cada envio ou atualização de conta.【F:frontend/src/api/whatsapp/useWhatsAppMessages.ts†L1-L26】【F:frontend/src/api/whatsapp/useSendWhatsAppMessage.ts†L1-L18】

## Estrutura de dados

- `whatsapp_account`: credenciais, token de verificação e status ativo.
- `whatsapp_message`: registros inbound/outbound, status, payloads, anexos e timestamps de envio/recebimento.
- Índices permitem buscas por `message_id`, `direction` e `account_id` para relatórios futuros.【F:backend/ads-service/src/main/resources/db/changelog/V2028_02_01__create_whatsapp_tables.sql†L1-L69】

## Boas práticas

- Gere tokens de longa duração e rotacione-os periodicamente via Facebook Business Manager.
- Configure o webhook com SSL público e mantenha o Verify Token em segredo.
- Sempre normalize o número destino com código do país (`+55` para Brasil) antes de enviar.
- Para fluxos automáticos, utilize mensagens de template aprovadas e defina `templateComponents` na metadata do passo da jornada quando necessário.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/WhatsAppChannelHandler.java†L98-L123】
