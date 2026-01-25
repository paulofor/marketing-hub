# Marketing Hub — Públicos compatíveis com Meta Ads

## Contexto

Hoje o Marketing Hub gera públicos apenas com **nome** e **descrição** textual (`audience.name` e `audience.description`).
O Worker IA aproveita essas descrições para montar um `targetingJson` básico quando precisa planejar ad sets,
mas ainda não armazenamos nem expomos uma segmentação estruturada no cadastro do público. Essa lacuna
impede que o time de mídia escolha o público certo para cada experimento, já que o Facebook Ads exige a
informação em formato de **Targeting Spec** (localizações, interesses, comportamentos, IDs de custom audiences etc.).

## O que o Facebook exige

1. **Estrutura obrigatória do Targeting Spec** — a Meta permite combinar filtros de localização, demografia,
   dispositivos móveis, interesses, comportamentos, públicos personalizados e flexibilização (`flexible_spec`).
   Mesmo quando usamos combinações flexíveis, o payload precisa conter `geo_locations`, `custom_audiences`,
   `product_audience_specs` ou `dynamic_audience_ids` dentro do objeto `targeting`.
   【docs/references/facebook-advanced-targeting.txt†L6-L63】
2. **Exemplos reais incluem IDs numéricos** — os exemplos oficiais mostram `targeting` com chaves como
   `geo_locations.{countries,regions,cities}`, `age_min`, `age_max`, `genders`, `behaviors`, `interests` e
   `relationship_statuses`, sendo que interesses e comportamentos usam objetos `{ "id": 6002714895372, "name": "All travelers" }`.
   【docs/references/facebook-advanced-targeting.txt†L70-L101】
3. **Custom Audiences são arrays de IDs** — inclusão e exclusão aceitam até 500 itens cada e devem receber IDs
   ou objetos com a chave `id`. Descrições livres não são aceitas.
   【docs/references/facebook-advanced-targeting.txt†L114-L123】
4. **Campos avançados dependem do Targeting Search API** — opções como sistemas operacionais, escolas,
   empregadores e localidades precisam ser buscadas via `targeting-search` para obter os códigos oficiais (`key`).
   【docs/references/facebook-advanced-targeting.txt†L45-L63】【docs/references/facebook-advanced-targeting.txt†L101-L112】
5. **Localizações usam hierarquia própria** — países, regiões, cidades, CEPs e grupos têm códigos específicos
   retornados por `type=adgeolocation`, e esses códigos devem ser usados no campo `geo_locations`.
   【docs/references/facebook-targeting-search.txt†L22-L127】

Sem esses IDs não conseguimos publicar ad sets; portanto, o fluxo de geração de públicos precisa entregar um
`targetingSpec` pronto (ou pelo menos um blueprint com os códigos resolvidos).

## Proposta

### 1. Evolução do modelo de dados

| Tabela / entidade | Mudanças propostas |
| --- | --- |
| `audience` | Acrescentar campos `targeting_spec` (JSON), `targeting_status` (enum `DRAFT`, `NEEDS_REVIEW`, `READY`) e `targeting_notes` (texto). Persistiremos também `source` (manual × IA) para auditoria. |
| Novo agregado `audience_targeting_seed` | Lista normalizada dos insumos usados para montar o targeting: `type` (`interest`, `behavior`, `location`, `custom_audience`, `lookalike`, `demographic`, `device`), `value` (nome exibido), `meta_id`/`key` (quando o seed já tiver um código) e `confidence`. Ajuda o analista a revisar (UI) e permite reprocessar caso a Meta invalide um identificador. |
| `ad_set` | Passa a referenciar diretamente `audience.targeting_spec` quando o Worker IA gerar o plano. O campo atual `targetingJson` continua existindo para não quebrar o Worker, mas o backend deve copiar o JSON do público como base. |

### 2. Novo fluxo no Worker IA

1. **Geração textual continua igual** — `NicheAudienceService` segue produzindo `name/description` para brainstorming.
2. **Enriquecimento estruturado** — criar `AudienceTargetingService` com o seguinte comportamento:
   - Buscar públicos aprovados (`audience.approved = true`) que estejam sem `targeting_spec` ou marcados como `DRAFT`.
   - Montar um prompt específico instruindo o modelo a retornar um objeto com: `age_min`, `age_max`, `genders`,
     `geo_seeds` (lista de países/estados/cidades), `interest_seeds`, `behavior_seeds`, `demographic_filters`,
     `custom_audience_hints`, `advantage_plus` (flag).
   - Para cada seed textual, consultar o **Targeting Search API** correspondente:
     - `type=adgeolocation` para localidades (`countries`, `regions`, `cities`, `zip`, `geo_market`).
     - `type=adinterest`/`adTargetingCategory` para interesses, comportamentos, dispositivos e demografia.
   - Persistir o resultado resolvido em `audience_targeting_seed` e montar o `targeting_spec` final respeitando o
     formato aceito (`geo_locations`, `flexible_spec`, `custom_audiences`, `exclusions`, `targeting_automation`).
   - Salvar `targeting_status = NEEDS_REVIEW`, `prompt`, `model` e `meta_response` para auditoria.
3. **Aprovação humana no Marketing Hub** — um usuário revisa na UI (ver item 4) e muda o status para `READY`.
4. **Uso em ad sets** — `AudienceAdSetService` passa a:
   - Priorizar públicos com `targeting_status = READY`.
   - Copiar o JSON e apenas complementar `budget`, `durationDays`, `device_platforms` e criativos.
   - Caso o Worker IA ainda gere ajustes (ex.: localização dinâmica), aplicar diffs antes de chamar `POST /api/adsets`.

### 3. Alterações no backend (`ads-service`)

- **API REST**
  - `GET /api/audiences` passa a retornar os novos campos e, quando `includeTargeting=true`, devolve o JSON completo
    (usar parâmetro para evitar tráfego grande em listagens).
  - `PATCH /api/audiences/{id}/targeting` para aprovação humana (`status`, `notes`, `lastReviewedBy`).
  - `POST /api/audiences/{id}/targeting-seeds/reprocess` para o Worker IA marcar seeds inválidas.
- **Validações**
  - Garantir que `targeting_spec` contenha pelo menos um `geo_locations` válido e um critério primário (interesse,
    comportamento, custom audience ou Advantage+). Isso evita públicos "vazios".
- **Migração**
  - Script para popular `targeting_status = DRAFT` em todos os registros existentes.

### 4. UX no Marketing Hub

- **Painel do nicho/hipótese** ganha uma aba "Segmentação" com:
  - Tabela de seeds resolvidas (tipo, nome, ID meta, origem, confiança).
  - Visualização do JSON em árvore (read-only) + botão "Copiar para Meta".
  - Botões "Aprovar" / "Solicitar novo targeting" (abre modal para justificar).
- **Notificações** quando o Worker gerar um targeting novo aguardando aprovação.
- **Filtros** para encontrar públicos `NEEDS_REVIEW` e priorizar trabalho humano.

### 5. Observabilidade e governança

- Registrar métricas: nº de públicos enriquecidos por hora, taxa de aprovação, seeds sem ID.
- Alertas quando o `Targeting Search` começar a rejeitar termos (ex.: mudança de política para categorias especiais).
- Guardar payloads utilizados nos prompts (`ai_generation` já existe) e o `targeting_spec` final por versão para auditoria.

## Plano de implementação

| Fase | Escopo | Resultado esperado |
| --- | --- | --- |
| 1. Modelagem | Migration + DTO/Entity (`targeting_spec`, `targeting_status`, tabela de seeds). Ajustar APIs e contratos do frontend. | Dados prontos para armazenar targeting estruturado. |
| 2. Worker IA (MVP) | Implementar `AudienceTargetingService` com prompts + mapeamento via Targeting Search (interesses + países). Marcar tudo como `NEEDS_REVIEW`. | Públicos passam a receber targeting espec primário (geo + interesses). |
| 3. UI e aprovação | Novos componentes de revisão no Marketing Hub + endpoint `PATCH /targeting`. | Time de mídia consegue aprovar/editar targeting antes do uso. |
| 4. Integração com Ad Sets | `AudienceAdSetService` usa o `targeting_spec` aprovado e registra diffs aplicados. Ajustar testes automatizados. | Ad sets gerados com o mesmo targeting revisado. |
| 5. Expansão de seeds | Cobrir comportamentos, demografia avançada, custom audiences e Advantage+. Adicionar reprocessamento automático ao detectar IDs obsoletos. | Targeting completo para campanhas mais complexas. |

## Riscos e mitigação

- **Limites do Targeting Search** — Respeitar rate limit aplicando cache e expondo métricas. Em caso de erro,
  manter o seed em `DRAFT` com mensagem para revisão manual.
- **Mudanças de política (Special Ad Category)** — armazenar flag no público/experimento para impedir campos proibidos
  quando `specialAdCategory != NONE`.
- **Consistência entre targeting e prompts** — versionar o `prompt` usado para gerar cada targeting; se mudarmos o template,
  reprocessar apenas públicos `DRAFT/NEEDS_REVIEW`.

Com esse desenho, o Marketing Hub deixa de depender de descrições livres e passa a oferecer públicos realmente compatíveis
com o que o Facebook Ads espera, mantendo o Worker IA como peça central na transformação dos insights em segmentações
operacionais.
