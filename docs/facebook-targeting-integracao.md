# Integração de interesses/comportamentos/cargos com a API de Targeting do Facebook

## Contexto e problema

Os logs atuais do **Facebook Ads Worker** indicam que parte das sugestões enviadas para criação de públicos
(geradas manualmente ou pelo AI Worker) não correspondem a opções válidas da Meta. Isso gera mensagens como
"interesse/comportamento/cargo não encontrado na API da Meta" e impede a criação do conjunto de anúncios. As
sugestões são texto livre e só são validadas tardiamente, já no momento de montar o targeting, o que aumenta
retrabalho e confusão para o cliente no front-end.

## Objetivo

Garantir que interesses, comportamentos e cargos apresentados ao cliente e utilizados na criação de anúncios
sejam *sempre* opções válidas da Meta Ads, mantendo a autonomia do AI Worker para gerar hipóteses, mas
aterrando-as nas categorias reais expostas pela Graph API.

## Referência oficial da Meta

A Meta expõe o endpoint `/search` com `type=adinterest`, `type=adbehavior` e `type=adworkposition` (entre
outros) descrito na documentação oficial de Targeting Search:
<https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search/>

## Visão de arquitetura proposta

```
Front-end  →  Backend (API pública)  →  AI Worker  →  Resolver de Targeting (Facebook Ads Worker)  →  Meta Graph API
                                 ↑                        ↓
                                 └────────────── Persistência/Status ────────────────┘
```

### 1) Solicitação do cliente
- O cliente solicita ideias de targeting (por nicho ou hipótese) via front-end.
- O backend cria um registro de "solicitação de targeting" com status `PENDING_AI`.

### 2) Geração de hipóteses (AI Worker)
- O AI Worker recebe a solicitação e gera *candidatos* (palavras-chave/frases) para cada tipo: interesse,
  comportamento e cargo.
- Ele retorna ao backend a lista de candidatos com metadados (ex.: explicação do raciocínio, score semântico).
- Status passa para `PENDING_FACEBOOK_MATCH`.

### 3) Resolução e validação (Facebook Ads Worker)
- Novo componente **TargetingResolverService** (dentro do Facebook Ads Worker) recebe os candidatos e faz
  consultas determinísticas à Graph API usando `targetingsearch`:
  - `type=adinterest` para interesses
  - `type=adbehavior` para comportamentos
  - `type=adworkposition` para cargos
  - Parâmetros padrão: `ad_account_id`, `locale=pt_BR`, `country=BR`, `limit` configurável
- O resolver faz *grounding* e ranqueia resultados por similaridade (match exato > prefixo > fuzziness),
  retornando apenas IDs válidos, nome e `audience_size`.
- Cache local no worker para evitar chamadas repetidas e reduzir latência.
- Cada candidato vira uma ou mais **opções resolvidas** com status `VALIDATED`. Se não houver match,
  mantemos `NO_MATCH` com motivo para feedback.

### 4) Persistência e disponibilização
- O backend expõe endpoints para:
  - Listar solicitações e seus candidatos (com status, ID do Facebook, nome e atributos relevantes).
  - Reprocessar um candidato (caso o cliente edite texto ou escolha outro idioma/país).
- O front-end apresenta somente opções `VALIDATED`, mas permite ver rascunhos e motivos de rejeição.

### 5) Uso na criação de anúncios
- `FacebookAdsService` passa a consumir exclusivamente objetos `TargetingOption` persistidos (contendo `id`,
  `type`, `name`, `audience_size`), em vez de texto livre. Isso elimina falhas na etapa de criação do ad set.

## Modelo de dados sugerido

- `targeting_requests` (id, tipo de entrada: nicho/hipótese, status, criado_por, timestamps)
- `targeting_candidates` (id, request_id, texto_sugerido, tipo: interest/behavior/work_position,
  origem: AI/usuario, status: PENDING_FACEBOOK_MATCH|VALIDATED|NO_MATCH, motivo_rejeicao)
- `targeting_options` (id, candidate_id, facebook_id, nome, type, audience_size, path/hierarchy, score)

## Contratos principais entre serviços

- **Backend → AI Worker**: `POST /targeting/{requestId}/candidates` com descrição do nicho/hipótese.
- **Backend → Facebook Ads Worker**: `POST /targeting/{requestId}/resolve` com candidatos agregados.
- **Facebook Ads Worker → Backend**: `PATCH /targeting/{candidateId}` atualizando status, facebook_id,
  nome, audience_size e score de relevância.

## Estratégia de fallback e UX

- Tentativas em sequência de locales: `pt_BR` → `en_US` → sem locale para ampliar cobertura.
- Caso `NO_MATCH`, exibir ao cliente o motivo e sugerir termos relacionados retornados pela Graph API.
- Limitar quantidade por tipo (ex.: top 20) para evitar sobrecarregar a tela e o ad set.

## Plano de adoção incremental

1. **Fix rápido**: corrigir mensagens de erro (já aplicado) para mostrar o valor que falhou.
2. **Novo Resolver**: implementar o serviço de `targetingsearch` genérico no Facebook Ads Worker e endpoint
   interno para ser chamado pelo backend.
3. **Persistência e contratos**: criar tabelas e endpoints no backend conforme modelo acima.
4. **UI/UX**: atualizar front-end para mostrar apenas opções validadas, status de cada candidato e motivos de
   rejeição; permitir reprocessar ou editar texto.
5. **Governança**: métricas (taxa de match, tempo de resolução) e alertas no Prometheus/Grafana.

Essa abordagem aproxima o AI Worker das possibilidades reais da Meta, entrega transparência ao cliente e
reduz falhas na criação de conjuntos de anúncios.
