# Pipeline unificado (CANÔNICO): criar 3 públicos-alvo (BR) via Meta Ads API  
## IA Worker (decide) + Facebook Ads Worker (executa)

Este é o **documento único** para o pipeline de criação de públicos. Ele substitui/aposenta:
- `docs/roteiro-criacao-publicos-experimento.md`
- `docs/facebook-ads-worker/pipeline-3-publicos-meta-ads-api-ia-worker.md`

---

## ⚠️ Regras de leitura (IMPORTANTE para modelos que geram código)

Este documento separa **FATOS/REGRAS** vs **EXEMPLOS** para evitar que o gerador de código trate exemplos como “verdade”.

- Tudo marcado com **“EXEMPLO”** é **ilustrativo** e **NÃO** deve ser usado como dado fixo em produção.
- **Nenhum ID de interesse/cargo/behavior em exemplos é garantido** (ou sequer real).  
  ✅ Em produção, **todo ID deve vir da API** (Targeting Search / Suggestions).
- Em blocos de comando/código, os tokens `__ASSIM__` são **placeholders** e devem ser substituídos pelo worker.

**Placeholders padrão (sempre substituir):**
- `__API_VERSION__` (ex.: `v24.0`)
- `__AD_ACCOUNT_ID__` (sem `act_`)
- `__ACCESS_TOKEN__`
- `__LOCALE__` (ex.: `pt_BR`)
- `__SEED__` (texto curto)
- `__ANCHOR_INTEREST_ID__` (ID escolhido via API)
- `__CAMPAIGN_ID__` (se for criar ad set)

---

## Objetivo final (fim definido)

Ao final do fluxo você terá:

1) **3 `targeting_spec` finais (BR)**  
- `spec_1.json`, `spec_2.json`, `spec_3.json`

2) **3 resultados de alcance (BR)**  
- `reach_1.json`, `reach_2.json`, `reach_3.json`

3) **Auditoria / reprodutibilidade**
- `icp.md`
- `ia_worker_config.json`
- `seed_candidates.json`
- `seed_selected.json`
- `suggestions_raw.json`
- `suggestions_curated.json`
- `audience_plan.json`
- `decision_log.jsonl`

4) (Opcional) **3 Ad Sets criados** via API (`POST /adsets`) com `status=PAUSED`.

---

## Quem faz o quê (contrato entre workers)

### FATOS: responsabilidades

**IA Worker (decide)**
- Lê `icp.md` + `ia_worker_config.json`
- Gera seeds (texto curto) para busca
- Desambigua e escolhe **anchor seed** (ID)
- Filtra sugestões e cria **3 hipóteses** (3 públicos diferentes)
- Monta `spec_1..3.json`
- Recalibra (loop) com base no reach

**Facebook Ads Worker (executa)**
- Chama a Meta Ads API e salva JSON bruto
- Endpoints principais:
  - `GET /act_<AD_ACCOUNT_ID>/targetingsearch`
  - `GET /act_<AD_ACCOUNT_ID>/targetingsuggestions`
  - `GET /act_<AD_ACCOUNT_ID>/reachestimate`
- Endpoints opcionais:
  - `GET /act_<AD_ACCOUNT_ID>/targetingvalidation` (sanity check de IDs)
  - `GET /act_<AD_ACCOUNT_ID>/targetingdescription` (auditoria/explicação)
  - `POST /act_<AD_ACCOUNT_ID>/adsets`

---

## Regras fixas (não negociáveis)

### FATOS
- Geografia do experimento: `geo_locations.countries=["BR"]`
- 3 públicos **diferentes entre si** (3 hipóteses)
- IDs usados em targeting **sempre** são obtidos via API (não “texto solto”)
- Todas as etapas devem salvar entrada/saída em arquivo (debug e reprocesso)

---

## 0) Preparação do ambiente

### FATOS — Entrada
- `__ACCESS_TOKEN__`
- `__AD_ACCOUNT_ID__` (sem `act_`)
- `__API_VERSION__`
- `__LOCALE__`

### EXEMPLO — Ação (Git Bash)
```bash
# EXEMPLO — substitua valores
export API_VERSION="__API_VERSION__"
export AD_ACCOUNT_ID="__AD_ACCOUNT_ID__"
export ACCESS_TOKEN="__ACCESS_TOKEN__"
export LOCALE="__LOCALE__"
```

### FATOS — Saída
- Variáveis prontas para o Ads Worker executar requests.

### FATOS — Padrão de request (recomendado)
Use `-sS --fail` e sempre salve a resposta:
```bash
# TEMPLATE (não é exemplo): padrão recomendado
curl -sS --failG "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/targetingsearch"   --data-urlencode "access_token=__ACCESS_TOKEN__"   --data-urlencode "type=adinterest"   --data-urlencode "q=__SEED__"   --data-urlencode "locale=__LOCALE__"   --data-urlencode "limit=25"   --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound"   > out.json
```

---

## 0.1) Configuração do IA Worker (arquivo de controle)

### FATOS — Objetivo
Centralizar limites e preferências para decisões consistentes.

### EXEMPLO — Arquivo `ia_worker_config.json` (ilustrativo)
```bash
cat > ia_worker_config.json <<'JSON'
{
  "locale": "__LOCALE__",
  "geo_countries": ["BR"],
  "age_min_default": 18,
  "age_max_default": 54,

  "limits": {
    "seed_keywords_max": 12,
    "suggestions_fetch_limit": 150,
    "interests_per_audience_max": 12,
    "behaviors_per_audience_max": 6,
    "work_positions_per_audience_max": 8
  },

  "reach_thresholds": {
    "min_lower_bound": 200000,
    "max_upper_bound": 20000000
  },

  "disambiguation": {
    "reject_terms": ["birthday", "friends of", "expats", "canvas gaming"],
    "prefer_paths_starting_with": ["Interests", "Behaviors", "Demographics"]
  }
}
JSON
```

### FATOS — Saída
- Config de limites/heurísticas para o IA Worker.

---

## 1) ICP (entrada humana)

### FATOS — Objetivo
Descrever com clareza: produto, público, dores, país.

### EXEMPLO — `icp.md` (ilustrativo)
```bash
cat > icp.md <<'MD'
Produto: Marketing Hub (IA) — cria imagens personalizadas a partir de foto do cliente.
Mercado: Brasil.
Quem compra: pequenos negócios e profissionais que precisam de criativos.
Uso: posts, anúncios, peças promocionais e diversão.
Dores: falta de tempo/habilidade; custo de designer; velocidade e volume.
MD
```

### FATOS — Saída
- `icp.md`

---

## 2) IA Worker gera seeds (texto curto)

### FATOS — Objetivo
Gerar uma lista **pequena** de termos que têm chance real de virar IDs na API.

### FATOS — Entrada
- `icp.md`
- `ia_worker_config.json`

### EXEMPLO — Saída `seed_candidates.json` (ilustrativo)
```json
{
  "interests": ["__SEED_1__", "__SEED_2__", "__SEED_3__"],
  "work_positions": ["__JOB_QUERY_1__", "__JOB_QUERY_2__"],
  "behaviors": ["__BEHAVIOR_QUERY_1__"]
}
```

---

## 3) Ads Worker faz *Discovery* (mineração): 1 seed → catálogo grande de termos com IDs

### FATOS — Por que mudar esta etapa
Em muitos nichos (ex.: “agronegócio PME”), o texto do seed **não mapeia bem** para itens específicos na Meta.  
Para evitar “ficar preso” caçando manualmente termos, esta etapa passa a operar em **modo discovery**:

- **Entrada mínima**: 1 seed (texto curto)
- **Saída máxima**: muitos candidatos (interesses/cargos/comportamentos/…) **já com IDs**
- Depois o **IA Worker** faz seleção/curadoria e monta os 3 públicos.

> Importante: esta etapa é “alto recall”. Vai vir ruído (ex.: `friends of`, `birthday`, `expats`).  
> O filtro/seleção acontece no IA Worker (Etapas 4–8).

---

### FATOS — Entrada
- `seed_candidates.json` (ou um seed único escolhido pela IA)
- `ia_worker_config.json` (para regras como `limits` e `reject_terms`)

---

### FATOS — Saída
- `out/targeting_discovery_raw.json` (agregado bruto)
- `out/targeting_discovery_dedup.json` (agregado deduplicado; pronto para o IA Worker)

Opcional (mas recomendado):
- `out/discovery/` (um arquivo por chamada)
- `out/discovery_stats.json` (contagem por tipo)

---

### FATOS — Regras de seed (para funcionar melhor)
- Seed deve ser **curta** (1–3 palavras).
- O Ads Worker deve tentar **variações** do seed:
  - sem acento (`agronegocio`)
  - inglês/termos do mercado (`agribusiness`, `agriculture`, `farm`, etc.)
- O Ads Worker pode testar **2 locales** na busca:
  - `pt_BR` e `en_US` (muitos nomes aparecem em inglês mesmo no Brasil; ID manda)

---

## 3.A) Estratégia canônica — chamadas separadas por tipo em `/targetingsearch`

### FATOS
No pipeline em produção, a etapa 3 deve executar **chamadas independentes** por categoria:
- interesses (`type=adinterest`)
- comportamentos (`type=adbehavior`)
- cargos (`type=adworkposition`)

### TEMPLATE — Requests por tipo (um arquivo por tipo)
```bash
# TEMPLATE: substitua os placeholders
SEED_INTEREST="__SEED__"
SEED_BEHAVIOR="__SEED__"
SEED_POSITION="__SEED__"
mkdir -p out/discovery

curl -sS --failG "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/targetingsearch" \
  --data-urlencode "access_token=__ACCESS_TOKEN__" \
  --data-urlencode "type=adinterest" \
  --data-urlencode "q=${SEED_INTEREST}" \
  --data-urlencode "locale=__LOCALE__" \
  --data-urlencode "limit=200" \
  --data-urlencode "fields=id,name,type,path,topic,audience_size_lower_bound,audience_size_upper_bound" \
  > "out/discovery/adinterest_${SEED_INTEREST}.json"

curl -sS --failG "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/targetingsearch" \
  --data-urlencode "access_token=__ACCESS_TOKEN__" \
  --data-urlencode "type=adbehavior" \
  --data-urlencode "q=${SEED_BEHAVIOR}" \
  --data-urlencode "locale=__LOCALE__" \
  --data-urlencode "limit=200" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > "out/discovery/adbehavior_${SEED_BEHAVIOR}.json"

curl -sS --failG "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/targetingsearch" \
  --data-urlencode "access_token=__ACCESS_TOKEN__" \
  --data-urlencode "type=adworkposition" \
  --data-urlencode "q=${SEED_POSITION}" \
  --data-urlencode "locale=__LOCALE__" \
  --data-urlencode "limit=200" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > "out/discovery/adworkposition_${SEED_POSITION}.json"
```

### EXEMPLO — Agregar + deduplicar (sem `jq`)
> EXEMPLO: use Python em **uma linha** (`-c`).  
> (Evita o erro comum de usar heredoc `python - <<PY` e quebrar o stdin do JSON.)

```bash
python -c "import json,glob,os; seed=os.environ.get('SEED','__SEED__'); items=[]; for f in glob.glob('out/discovery/*_'+seed+'.json'):   d=json.load(open(f,encoding='utf-8'));   for x in d.get('data',[]):     x['_src']=os.path.basename(f).replace('.json','');     items.append(x); seen=set(); dedup=[]; for x in items:   k=(x.get('id'), x.get('type'));   if k in seen: continue;   seen.add(k); dedup.append(x); json.dump({'seed': seed, 'items': dedup}, open('out/targeting_discovery_dedup.json','w',encoding='utf-8'), ensure_ascii=False, indent=2); json.dump({'seed': seed, 'count': len(dedup)}, open('out/discovery_stats.json','w',encoding='utf-8'), ensure_ascii=False, indent=2); print('OK', len(dedup))"
```

---

### FATOS — Como o IA Worker usa esta saída
Na etapa seguinte (Etapa 4), o IA Worker recebe `out/targeting_discovery_dedup.json` e:
1) filtra por relevância ao ICP  
2) aplica `reject_terms` (ex.: birthday/friends of/expats/canvas gaming)  
3) escolhe o **anchor seed** (ID) + lista de IDs úteis  
4) segue para `targetingsuggestions` e montagem dos 3 specs

---
## 4) IA Worker escolhe o anchor seed e IDs úteis

### FATOS — Objetivo
Selecionar 1 **anchor interest** (ID) + um conjunto de IDs para compor 3 públicos.

### FATOS — Entrada
- outputs do passo 3 (`out_targetingsearch_*.json`)
- heurísticas de desambiguação (`reject_terms`, `prefer_paths_starting_with`)

### FATOS — Regras de desambiguação
- Preferir `path` coerente com o ICP (ex.: `Interests > ...`)
- Rejeitar termos irrelevantes (ex.: birthday/friends of/expats/canvas gaming)
- Evitar itens muito genéricos quando houver alternativa específica

### EXEMPLO — Saída `seed_selected.json` (ilustrativo)
```json
{
  "anchor_seed": { "type": "interests", "id": "__ANCHOR_INTEREST_ID__", "name": "__ANCHOR_NAME__" },
  "picked_from_query": "__SEED__",
  "extra_ids": {
    "work_positions": [{ "id": "__JOB_ID__", "name": "__JOB_NAME__" }],
    "behaviors": [{ "id": "__BEHAVIOR_ID__", "name": "__BEHAVIOR_NAME__" }]
  }
}
```

---

## 5) Ads Worker expande o anchor seed (Targeting Suggestions)

### FATOS — Objetivo
Expandir o anchor seed em uma lista de sugestões relacionadas.

### FATOS — Entrada
- `seed_selected.json` (`anchor_seed.id`)

### TEMPLATE — Targeting Suggestions
```bash
curl -sS --failG "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/targetingsuggestions"   --data-urlencode "access_token=__ACCESS_TOKEN__"   --data-urlencode "targeting_list=[{"type":"interests","id":"__ANCHOR_INTEREST_ID__"}]"   --data-urlencode "limit=__SUGGESTIONS_LIMIT__"   > suggestions_raw.json
```

### FATOS — Saída
- `suggestions_raw.json` (mistura de interesses/cargos/behaviors etc.)

---

## 6) IA Worker faz curadoria das sugestões

### FATOS — Objetivo
Transformar sugestões brutas em uma lista enxuta, útil e rastreável.

### FATOS — Entrada
- `suggestions_raw.json`
- `icp.md`
- `ia_worker_config.json`

### FATOS — Saída
- `suggestions_curated.json` (filtrado e ranqueado)

### EXEMPLO — Formato de `suggestions_curated.json` (ilustrativo)
```json
{
  "interests": [{ "id": "__ID__", "name": "__NAME__", "score": 0.0 }],
  "work_positions": [{ "id": "__ID__", "name": "__NAME__", "score": 0.0 }],
  "behaviors": [{ "id": "__ID__", "name": "__NAME__", "score": 0.0 }]
}
```

---

## 7) IA Worker define 3 hipóteses (3 públicos diferentes)

### FATOS — Objetivo
Criar 3 públicos **diferentes entre si** para testar hipóteses.

### FATOS — Importante
“Design / Marketing / SMB” foi apenas um trio **de exemplo**.  
O trio real deve ser derivado do seu ICP e das sugestões disponíveis.

### EXEMPLO — Saída `audience_plan.json` (ilustrativo)
```json
{
  "audiences": [
    { "key": "A", "name": "__AUDIENCE_NAME_A__", "strategy": "__STRATEGY_A__" },
    { "key": "B", "name": "__AUDIENCE_NAME_B__", "strategy": "__STRATEGY_B__" },
    { "key": "C", "name": "__AUDIENCE_NAME_C__", "strategy": "__STRATEGY_C__" }
  ]
}
```

---

## 8) IA Worker monta 3 `targeting_spec` finais (com `flexible_spec`)

### FATOS — Objetivo
Gerar `spec_1.json`, `spec_2.json`, `spec_3.json` prontos para `reachestimate` (e depois para criar ad sets).

### FATOS — Entrada
- `seed_selected.json`
- `suggestions_curated.json`
- `audience_plan.json`
- `geo_countries=["BR"]` e faixa etária

### FATOS — Saída
- `spec_1.json`, `spec_2.json`, `spec_3.json`

### EXEMPLO — `spec_1.json` (ilustrativo; substitua IDs)
```json
{
  "geo_locations": { "countries": ["BR"] },
  "age_min": 20,
  "age_max": 55,
  "flexible_spec": [
    { "interests": [{ "id": "__INTEREST_ID__", "name": "__INTEREST_NAME__" }] },
    { "work_positions": [{ "id": "__JOB_ID__", "name": "__JOB_NAME__" }] }
  ]
}
```

---

## 9) (Opcional) Sanity check de IDs (`/targetingvalidation`)

### FATOS — Objetivo
Checagem auxiliar para validar IDs (não substitui `reachestimate`).

### FATOS — Entrada
- lista de IDs extraída dos specs

### TEMPLATE — `targetingvalidation` por `id_list`
```bash
curl -sS --failG "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/targetingvalidation"   --data-urlencode "access_token=__ACCESS_TOKEN__"   --data-urlencode "id_list=["__ID_1__","__ID_2__"]"   > out_targetingvalidation.json
```

### FATOS — Saída
- `out_targetingvalidation.json` (se falhar/oscilar, trate como opcional)

---

## 10) Reach Estimate (validação end-to-end)

### FATOS — Objetivo
Validar se o `targeting_spec` funciona e medir alcance BR.

### FATOS — Entrada
- `spec_1.json`, `spec_2.json`, `spec_3.json`

### TEMPLATE — Reach Estimate
```bash
curl -sS --failG "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/reachestimate"   --data-urlencode "access_token=__ACCESS_TOKEN__"   --data-urlencode "targeting_spec@spec_1.json"   > reach_1.json
```

### FATOS — Saída
- `reach_1.json`, `reach_2.json`, `reach_3.json`

---

## 11) Recalibração automática (IA Worker) — recomendado

### FATOS — Objetivo
Ajustar os specs para ficar dentro de uma faixa desejada de reach (definida no config).

### FATOS — Entrada
- `reach_*.json`
- `ia_worker_config.json`

### FATOS — Saída
- specs atualizados + novas medições
- `decision_log.jsonl` com “antes/depois + motivo”

---

## 12) (Opcional) Criar Ad Sets via API

### FATOS — Objetivo
Criar 3 ad sets (recomendado iniciar `PAUSED`).

### FATOS — Entrada
- `__CAMPAIGN_ID__`
- `spec_*.json`
- orçamento/objetivo/otimização

### EXEMPLO — Create Ad Set (ilustrativo; substituir placeholders)
```bash
curl -sS --failX POST "https://graph.facebook.com/__API_VERSION__/act___AD_ACCOUNT_ID__/adsets"   --data-urlencode "access_token=__ACCESS_TOKEN__"   --data-urlencode "name=__ADSET_NAME__"   --data-urlencode "campaign_id=__CAMPAIGN_ID__"   --data-urlencode "daily_budget=__DAILY_BUDGET__"   --data-urlencode "billing_event=IMPRESSIONS"   --data-urlencode "optimization_goal=REACH"   --data-urlencode "status=PAUSED"   --data-urlencode "targeting@spec_1.json"
```

---

## Checklist de conclusão

- ✅ `icp.md`
- ✅ `ia_worker_config.json`
- ✅ `seed_candidates.json`
- ✅ `seed_selected.json` (anchor seed com ID real)
- ✅ `suggestions_raw.json`
- ✅ `suggestions_curated.json`
- ✅ `audience_plan.json`
- ✅ `spec_1.json`, `spec_2.json`, `spec_3.json`
- ✅ `reach_1.json`, `reach_2.json`, `reach_3.json`
- ✅ `decision_log.jsonl`

---

## Observações rápidas (para evitar “resultados estranhos”)

### FATOS
- Mesmo com `locale=pt_BR`, nomes/path podem vir em inglês. **ID manda.**
- O `type=` usado no `targetingsearch` define onde aquele ID pode entrar no spec.
- ICP (texto) serve para **gerar seeds curtas**, não para virar targeting direto.
- `reachestimate` é a validação prática do `targeting_spec`.

---

## Referências oficiais (Meta)

(Links de referência; não são parte do fluxo executável)
- Targeting Search: https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search/
- Ad Account Targeting Search: https://developers.facebook.com/docs/marketing-api/reference/ad-account/targetingsearch/
- Targeting Suggestions: https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingsuggestions/
- Flexible Targeting (`flexible_spec`): https://developers.facebook.com/docs/marketing-api/audiences/reference/flexible-targeting/
- Reach Estimate: https://developers.facebook.com/docs/marketing-api/reference/ad-account/reachestimate/
- Targeting Validation: https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingvalidation/
- Targeting Description: https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-description/
- Create an Ad Set: https://developers.facebook.com/docs/marketing-api/get-started/basic-ad-creation/create-an-ad-set/
