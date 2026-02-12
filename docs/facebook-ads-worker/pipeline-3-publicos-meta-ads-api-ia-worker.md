# Pipeline de construção de 3 públicos-alvo com IA Worker (Meta Ads API)

Este documento descreve um **roteiro prático, curto e eficiente (começo → fim)** para construir **3 públicos-alvo** no **Brasil** usando a **Meta Ads API**, com **entrada/saída** por etapa e comandos prontos para **Git Bash** (sem `jq`).  
A diferença deste pipeline é que **as escolhas (seeds, curadoria, composição e calibração)** são feitas por um componente de IA chamado **IA Worker**.

> Fluxo principal (endpoints): **Targeting Search** → **Targeting Suggestions** → **Flexible Targeting (`flexible_spec`)** → **Reach Estimate** → (opcional) **Create Ad Set**.  
> Endpoints opcionais: **Targeting Validation** (valida *IDs*, não o spec inteiro) e **Targeting Description** (descrição humana do spec).

---

## Objetivo final (fim definido)

Você termina com:

1) **3 arquivos JSON** contendo `targeting_spec` (um por público/ad set):  
   - `spec_1.json`, `spec_2.json`, `spec_3.json`

2) **3 respostas de alcance** no Brasil via `reachestimate`:  
   - `reach_1.json`, `reach_2.json`, `reach_3.json`

3) **Trilha de auditoria** do IA Worker (decisões e dados intermediários):  
   - `decision_log.jsonl` (ou `decision_log.json`)  
   - `seed_candidates.json`, `seed_selected.json`  
   - `suggestions_raw.json`, `suggestions_curated.json`  
   - `audience_plan.json`

4) (Opcional) **criação dos 3 Ad Sets** via API (`POST /adsets`) ou uso manual no Ads Manager.

---

## Visão rápida: o que o IA Worker decide (e o que é regra fixa)

### Regras fixas (não negociáveis)
- Geografia: **Brasil** (`geo_locations.countries = ["BR"]`)
- 3 públicos **diferentes entre si** (3 hipóteses)
- Todos os targets são compostos por **IDs oficiais** encontrados via API (não texto “solto”)
- O pipeline sempre salva entradas/saídas em arquivos (reprodutível)

### Decisões do IA Worker (configuráveis)
- Quais **seeds** (palavras) pesquisar
- Qual seed vira o **anchor seed** (ID principal)
- Quais sugestões entram (curadoria)
- Qual “arquetipo” de público usar (ex.: Criadores, Marketing, Decisores — ou outro trio)
- Faixa etária inicial e ajustes
- Limites de tamanho (reach mínimo/máximo) e como calibrar se ficar fora

---

## 0) Preparação do ambiente

### Objetivo
Garantir variáveis e padrão de requests robusto (evita saída vazia e erros silenciosos).

### Entrada
- `ACCESS_TOKEN`
- `AD_ACCOUNT_ID` (sem `act_`)
- `API_VERSION` (ex.: `v24.0`)

### Ação (Git Bash)
```bash
export API_VERSION="v24.0"
export AD_ACCOUNT_ID="1234567890"
export ACCESS_TOKEN="EAAB..."
```

### Saída
- Variáveis prontas para os próximos comandos.

> Recomendação: em todas as chamadas use `-sS --fail` e salve outputs com `> arquivo.json`.

---

## 0.1) Configuração do IA Worker (arquivo de controle)

### Objetivo
Centralizar limites e preferências para o IA Worker tomar decisões consistentes.

### Entrada
- Limites por tipo, thresholds de alcance e idioma/geo

### Ação (criar `ia_worker_config.json`)
```bash
cat > ia_worker_config.json <<'JSON'
{
  "locale": "pt_BR",
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
    "reject_terms": ["Canvas Gaming", "Birthday", "Friends of", "Expats"],
    "prefer_paths_starting_with": ["Interests", "Behaviors", "Demographics"]
  }
}
JSON
```

### Saída
- `ia_worker_config.json` usado em todas as execuções do pipeline.

---

## 1) Definir o ICP (entrada humana) e gerar seeds (decisão do IA Worker)

### Objetivo
Sair do abstrato (“meu mercado”) para um conjunto pequeno de seeds que viram IDs via API.

### Entrada (humana)
Crie um arquivo com o ICP (texto curto):
```bash
cat > icp.md <<'MD'
Produto: Marketing Hub — gera imagens e criativos por IA a partir de foto enviada pelo cliente (para posts, anúncios e diversão).
Mercado: Brasil.
Quem compra: pequenos negócios, social medias, designers, pessoas que querem criativos rápidos e personalizados.
Dor: falta de tempo/habilidade para criar imagens boas e consistentes.
Canais: Instagram, Facebook, anúncios.
MD
```

### Ação (IA Worker)
- Lê `icp.md`
- Gera lista de seeds (keywords curtas) por categoria: interesses, cargos, behaviors
- Salva em `seed_candidates.json`

### Saída (artefato)
- `seed_candidates.json` (exemplo de estrutura)
```json
{
  "interests": ["canva", "photoshop", "graphic design", "image editing", "instagram"],
  "work_positions": ["social media manager", "graphic designer"],
  "behaviors": ["small business owners", "business page admins", "facebook page admins"]
}
```

> Observação: frase longa não vai direto pra segmentação; o IA Worker transforma em seeds curtos para achar IDs.

---

## 2) Converter seeds (texto) em IDs (Targeting Search)

### Objetivo
Encontrar os **IDs oficiais** dos descritores de segmentação.

### Entrada
- Seeds do `seed_candidates.json`
- Para cada seed, o IA Worker define:
  - `type=adinterest` (interesses)
  - `type=adworkposition` (cargos)
  - `type=adbehavior` (comportamentos)

### Ação (Git Bash — exemplo por seed/tipo)
Interesses (ex.: canva):
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adinterest" \
  --data-urlencode "q=canva" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > out_targetingsearch_interest_canva.json
```

Cargos (ex.: social media):
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adworkposition" \
  --data-urlencode "q=social media" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > out_targetingsearch_workpositions_social_media.json
```

Behaviors (ex.: small business owners):
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adbehavior" \
  --data-urlencode "q=small business owners" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > out_targetingsearch_behavior_smb.json
```

### Saída
- Vários arquivos `out_targetingsearch_*.json` com candidatos (IDs).

### Decisão do IA Worker (desambiguação)
O IA Worker escolhe IDs “bons” aplicando regras como:
- Preferir caminhos (`path`) coerentes com o ICP (ex.: “Canva (software)” e não “Canvas Gaming”)
- Rejeitar termos/paths claramente irrelevantes (ex.: “Birthday”, “Friends of”, “Expats”, etc.)
- Se houver vários candidatos, escolher o mais específico (por nome/path) e com melhor sinal para o ICP

### Saída (artefato)
- `seed_selected.json` (IDs selecionados)
```json
{
  "anchor_seed": {"type": "interests", "id": "6015636111201", "name": "Canva (software)"},
  "additional_ids": {
    "work_positions": [{"id":"120762141304604","name":"Social Media Manager"}],
    "behaviors": [{"id":"6002714898572","name":"Small business owners"}]
  }
}
```

---

## 3) Expandir o anchor seed com sugestões (Targeting Suggestions)

### Objetivo
Obter um “mapa do ecossistema” de interesses/behaviors/cargos relacionados ao seed principal.

### Entrada
- `anchor_seed.id` do `seed_selected.json`

### Ação
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsuggestions" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode 'targeting_list=[{"type":"interests","id":"6015636111201"}]' \
  --data-urlencode "limit=150" \
  > suggestions_raw.json
```

### Saída
- `suggestions_raw.json`

### Decisão do IA Worker (curadoria)
O IA Worker filtra e classifica sugestões por:
- Relevância ao ICP (matching de termos e caminhos)
- Tipo (interests vs behaviors vs work_positions)
- Limites do `ia_worker_config.json` (máximo por público)
- Exclusões (“Canvas Gaming”, “Birthday”, “Friends of”, etc. se não fizer sentido)

### Saída (artefato)
- `suggestions_curated.json` (lista enxuta e classificada)

---

## 4) Definir os 3 públicos (planejamento) — decisão do IA Worker

### Objetivo
Criar **3 hipóteses distintas** (3 “baldes”) a partir das sugestões curadas.

### Entrada
- `icp.md`
- `suggestions_curated.json`
- `ia_worker_config.json`

### Ação (IA Worker)
Escolhe 3 arquétipos que façam sentido para o ICP. Para este produto, é comum:

1) **Criadores** (design/edição/foto/vídeo)  
2) **Marketing** (social media, marketing digital, tráfego, conteúdo)  
3) **Decisores SMB** (donos/admins/owners + ferramentas)

Mas isso é **um template**: em outro nicho, o IA Worker pode escolher outro trio.

### Saída (artefato)
- `audience_plan.json` (exemplo)
```json
{
  "audiences": [
    {"name":"Creators", "include_types":["interests","work_positions"]},
    {"name":"Marketing", "include_types":["interests","work_positions"]},
    {"name":"Deciders_SMB", "include_types":["behaviors","interests"]}
  ]
}
```

---

## 5) Montar os 3 `targeting_spec` (Brasil) com `flexible_spec`

### Objetivo
Produzir 3 JSONs finais prontos (um por público), usando `flexible_spec` para combinar blocos.

### Entrada
- `audience_plan.json`
- `suggestions_curated.json`
- `seed_selected.json`
- Regras de geo/idade do `ia_worker_config.json`

### Ação (IA Worker)
Gera os arquivos:

- `spec_1.json`
- `spec_2.json`
- `spec_3.json`

> Recomendação: manter cada público com **poucos itens, bem escolhidos** (ex.: 6–12 interesses; 2–8 cargos; 1–6 behaviors), conforme config.

### Saída
- `spec_1.json`, `spec_2.json`, `spec_3.json`

> Observação importante: `flexible_spec` combina AND/OR entre grupos; use com cuidado para não “apertar demais” o público.

---

## 6) (Opcional) Validar IDs (Targeting Validation)

### Objetivo
Validar rapidamente se uma lista de **IDs** é reconhecida.  
**Não** é validação do `targeting_spec` completo.

### Entrada
- Lista de IDs (ex.: IDs que aparecem no spec)

### Ação
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingvalidation" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "id_list=[6015636111201,6003096002658,6003341931023]" \
  > out_targetingvalidation.json
```

### Saída
- `out_targetingvalidation.json`

---

## 7) Medir alcance e “validar end-to-end” (Reach Estimate)

### Objetivo
Validar se cada público tem tamanho adequado **no Brasil** e identificar specs inconsistentes.

### Entrada
- `spec_1.json`, `spec_2.json`, `spec_3.json`

### Ação (exemplo para cada spec)
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/reachestimate" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "targeting_spec@spec_1.json" \
  > reach_1.json
```

Repita para `spec_2.json` e `spec_3.json`.

### Saída
- `reach_1.json`, `reach_2.json`, `reach_3.json`

### Decisão do IA Worker (calibração automática)
Se algum público cair fora do intervalo (config `reach_thresholds`), o IA Worker aplica um loop de calibração, por exemplo:

- Se **muito pequeno**:
  - Aumentar `age_max` / reduzir ANDs (menos grupos no `flexible_spec`)
  - Trocar cargos muito raros por interesses mais amplos
  - Reduzir quantidade de behaviors exigidos

- Se **muito grande**:
  - Introduzir um segundo bloco relevante (ex.: “interesses + cargos”)
  - Remover interesses genéricos (muito amplos)
  - Ajustar idade (se fizer sentido)

**Saída adicional (artefato)**
- `decision_log.jsonl` com cada ajuste e seu resultado (antes/depois + reach)

---

## 8) (Opcional) Descrição humana do targeting (Targeting Description)

### Objetivo
Gerar uma descrição legível do público (ótimo para auditoria).

### Entrada
- `spec_*.json`

### Ação
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingdescription" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "targeting_spec@spec_2.json" \
  > desc_2.json
```

### Saída
- `desc_2.json`

---

## 9) Checklist de conclusão (pipeline completo)

Fluxo concluído quando existir:

- ✅ `icp.md`
- ✅ `ia_worker_config.json`
- ✅ `seed_candidates.json` + `seed_selected.json`
- ✅ `suggestions_raw.json` + `suggestions_curated.json`
- ✅ `audience_plan.json`
- ✅ `spec_1.json`, `spec_2.json`, `spec_3.json`
- ✅ `reach_1.json`, `reach_2.json`, `reach_3.json`
- ✅ `decision_log.jsonl` (auditável)

---

## 10) (Opcional) Criar Ad Sets via API (fim operacional)

### Objetivo
Criar os 3 Ad Sets no ad account via API, recomendando iniciar com `PAUSED`.

### Entrada
- `campaign_id`
- orçamento, otimização, billing_event etc.
- `targeting_spec` (um dos specs)

### Ação
Consulte a referência “Create an Ad Set” e envie um `POST` para:
- `POST /act_<AD_ACCOUNT_ID>/adsets`

### Saída
- 3 Ad Sets criados (iniciar pausado para revisão).

---

## Erros comuns (e como evitar)

- **Texto como seed final**: sempre converter para **ID** via `targetingsearch`.
- **Ambiguidade Canva vs Canvas**: preferir ID específico e filtrar por `path`/termos rejeitados.
- **Lista enorme de interesses**: curadoria + limites por público.
- **Não restringir BR**: sempre incluir `geo_locations: countries:["BR"]` antes de medir alcance.
- **Confiar no idioma do label**: nomes podem vir em inglês; o que manda é o **ID**.
- **Não registrar decisões**: `decision_log` é obrigatório para reproduzir e depurar.

---

## Referências oficiais (Meta)

- Targeting Search (audiences/reference)  
  https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search/
- Graph API Reference: Ad Account Targetingsearch  
  https://developers.facebook.com/docs/marketing-api/reference/ad-account/targetingsearch/
- Graph API Reference: Ad Account Targetingsuggestions  
  https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingsuggestions/
- Flexible Targeting (`flexible_spec`)  
  https://developers.facebook.com/docs/marketing-api/audiences/reference/flexible-targeting/
- Ad Account Reach Estimate  
  https://developers.facebook.com/docs/marketing-api/reference/ad-account/reachestimate/
- (Opcional) Targeting Validation (valida IDs)  
  https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingvalidation/
- (Opcional) Targeting Description  
  https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-description/
- Create an Ad Set  
  https://developers.facebook.com/docs/marketing-api/get-started/basic-ad-creation/create-an-ad-set/
