# Avatar de Venda — Conversion Events Spec

## Objetivo
Definir os eventos de analytics necessários para medir impacto do Avatar de Venda no funil de produto digital.

## Eventos obrigatórios
| Evento | Quando dispara | Propriedades mínimas |
|---|---|---|
| `avatar_sales_loaded` | widget carregou | `tenant_id`, `offer_id`, `page_type` |
| `avatar_sales_opened` | visitante abriu o widget | `session_id`, `traffic_source` |
| `avatar_sales_first_message_sent` | primeira mensagem do avatar | `character_profile`, `experiment_variant` |
| `avatar_sales_visitor_replied` | visitante respondeu | `message_count`, `intent_guess` |
| `avatar_sales_objection_detected` | objeção classificada | `objection_type`, `confidence` |
| `avatar_sales_proof_shown` | prova exibida | `proof_type`, `proof_id` |
| `avatar_sales_cta_shown` | CTA exibido | `cta_type`, `state` |
| `avatar_sales_cta_clicked` | CTA clicado | `cta_type`, `offer_id` |
| `avatar_sales_handoff_requested` | visitante pediu humano | `reason` |
| `checkout_started_after_avatar` | checkout começou após sessão | `offer_id`, `time_since_avatar_sec` |
| `purchase_completed_after_avatar` | compra concluída | `offer_id`, `order_value` |

## Propriedades padrão
```json
{
  "tenant_id": "tenant_acme",
  "session_id": "sess_123",
  "offer_id": "offer_001",
  "product_id": "prod_001",
  "page_type": "sales_page",
  "traffic_source": "meta_ads",
  "character_profile": "friendly_guide",
  "experiment_variant": "v1_a",
  "timestamp": "2026-03-24T15:00:00Z"
}
```

## Métricas derivadas
- taxa de abertura do avatar;
- taxa de resposta do visitante;
- taxa de objeção por sessão;
- CTR por CTA;
- checkout iniciado após conversa;
- compra após conversa;
- receita por sessão com avatar;
- diferença por personagem e por origem de tráfego.

## Regras de atribuição
- janela padrão de atribuição: 30 minutos após a última interação do avatar;
- se houver clique de CTA, priorizar atribuição por clique;
- se houver múltiplas sessões do avatar, usar a última sessão engajada;
- registrar também grupo de controle sem avatar.

## Experimentos mínimos
### Experimento 1
- controle: página sem avatar;
- variante A: avatar texto + imagem;
- variante B: avatar com microvídeos.

### Experimento 2
- perfil A: Guia Amigável;
- perfil B: Consultor Especialista.

### Experimento 3
- CTA direto para checkout;
- CTA intermediário para ver conteúdo.

## Dashboard mínimo
- sessões com avatar por dia;
- sessões com resposta do visitante;
- objeções mais frequentes;
- CTA mais clicado;
- conversão por perfil;
- conversão por oferta;
- conversão por canal de tráfego.
