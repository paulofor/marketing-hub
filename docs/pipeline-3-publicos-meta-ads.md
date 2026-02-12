# Pipeline unificado: construção de 3 públicos-alvo (Meta Ads API) — IA Worker + Facebook Ads Worker

Este documento unifica e corrige os dois roteiros existentes, mantendo o que funcionou na prática e removendo caminhos que geraram ruído (ex.: sugestões via `/search?type=adinterestsuggestion` como “Canvas Gaming”, aniversários etc.).  
O objetivo é um **fluxo reprodutível (começo → fim)** para sair de um **ICP** (texto) e chegar em **3 `targeting_spec` + reach estimate no Brasil**, com decisões tomadas pelo **IA Worker** e execução de chamadas pela **Facebook Ads Worker**.

> Fluxo principal (endpoints): **Targeting Search** → **Targeting Suggestions** → **IA monta `flexible_spec`** → **Reach Estimate** → (opcional) **Create Ad Set**.  
> Endpoints opcionais: **Targeting Validation** (sanity check de IDs) e **Targeting Description** (auditoria humana).

---

## 1) Resultado final (fim definido)

Você termina com:

1) **3 arquivos JSON** (`targeting_spec`) — um por público/ad set  
- `spec_1.json`, `spec_2.json`, `spec_3.json`

2) **3 respostas de alcance (Brasil)**  
- `reach_1.json`, `reach_2.json`, `reach_3.json`

3) **Trilha de auditoria e reprodutibilidade** (essencial para depuração)  
- `icp.md` (entrada humana)  
- `ia_worker_config.json` (regras/limites)  
- `seed_candidates.json` (seeds sugeridos pela IA)  
- `seed_selected.json` (IDs selecionados / anchor seed)  
- `suggestions_raw.json` (sugestões brutas)  
- `suggestions_curated.json` (curadoria)  
- `audience_plan.json` (3 hipóteses / arquétipos)  
- `decision_log.jsonl` (decisões e ajustes com antes/depois)

4) (Opcional) **3 Ad Sets criados** via API (`POST /adsets`) ou manual no Ads Manager.

---

## 2) Papéis e contrato entre Workers

### IA Worker (decide)
Responsável por decisões e curadoria, nunca por chamadas à API:

- Lê `icp.md` e gera seeds (palavras-curtas) por tipo
- Desambigua candidatos (ex.: “Canva (software)” ≠ “Canvas Gaming”)
- Escolhe 3 hipóteses (arquétipos) e monta `audience_plan.json`
- Monta e ajusta `spec_1..3.json` (inclusive recalibração por reach)
- Escreve logs e artefatos intermediários (audit trail)

### Facebook Ads Worker (executa)
Responsável por executar e salvar chamadas à Meta:

- Executa `/targetingsearch` para seeds (interests / work_positions / behaviors)
- Executa `/targetingsuggestions` para expandir o anchor seed
- Executa `/reachestimate` para validar end-to-end cada spec
- (Opcional) executa `/targetingvalidation`, `/targetingdescription` e `POST /adsets`
- Salva sempre o JSON bruto (entrada/saída) e status da request

> Regra de ouro: **o IA Worker nunca “chuta” ID**. Todo ID deve vir da API via Ads Worker.

---

## 3) Pré-requisitos

### Variáveis de ambiente (Git Bash)
**Entrada**
- `ACCESS_TOKEN`
- `AD_ACCOUNT_ID` (sem `act_`)
- `API_VERSION` (ex.: `v24.0`)
- `LOCALE` (ex.: `pt_BR`)

**Ação**
```bash
export API_VERSION="v24.0"
export AD_ACCOUNT_ID="1234567890"
export ACCESS_TOKEN="EAAB..."
export LOCALE="pt_BR"
```

**Saída**
- Ambiente pronto para todas as chamadas.

### Padrão de curl (evita “vazio silencioso”)
Use sempre:
- `-sS` (silencioso mas mostra erro)
- `--fail` (código != 200 vira erro)
- `> arquivo.json` (persistência)

Exemplo:
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch"   --data-urlencode "access_token=${ACCESS_TOKEN}"   --data-urlencode "type=adinterest"   --data-urlencode "q=canva"   --data-urlencode "locale=${LOCALE}"   --data-urlencode "limit=25"   --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound"   > out_targetingsearch_interest_canva.json
```

---

## 4) Pipeline (começo → fim), com entrada e saída por etapa

### Etapa 1 — Definir ICP (entrada humana)
**Objetivo:** transformar “nicho/mercado” em uma descrição curta, clara e acionável.

**Entrada:** texto (markdown) com produto, público, dor, país.

**Ação**
```bash
cat > icp.md <<'MD'
Produto: Marketing Hub — gera imagens e criativos por IA a partir de foto enviada pelo cliente.
Mercado: Brasil.
Quem compra: pequenos negócios, social medias, designers, empreendedores.
Uso: posts, anúncios, peças promocionais e diversão.
Dores: falta de tempo/habilidade para criar imagens consistentes; custo de designer; velocidade.
Canais: Instagram, Facebook, tráfego pago.
MD
```

**Saída:** `icp.md`

---

### Etapa 2 — IA Worker gera seeds (texto curto)
**Objetivo:** gerar uma lista pequena de termos que tenham chance real de virar IDs.

**Entrada:** `icp.md`, `ia_worker_config.json` (opcional nesta fase).

**Ação (IA Worker)**
- Gera seeds por tipo:
  - `interests`: ferramentas, temas, plataformas, “jobs-to-be-done”
  - `work_positions`: cargos prováveis (B2B)
  - `behaviors`: sinais de intenção (admins/owners etc.)

**Saída:** `seed_candidates.json`
```json
{
  "interests": ["canva", "photoshop", "graphic design", "image editing", "instagram"],
  "work_positions": ["social media manager", "graphic designer"],
  "behaviors": ["small business owners", "business page admins", "facebook page admins"]
}
```

---

### Etapa 3 — Ads Worker converte seeds em IDs (Targeting Search)
**Objetivo:** obter **IDs oficiais** para cada seed.

**Entrada:** `seed_candidates.json`

**Ação (Ads Worker)**
1) Para cada seed em `interests`, chamar `type=adinterest`
2) Para cada seed em `work_positions`, chamar `type=adworkposition`
3) Para cada seed em `behaviors`, chamar `type=adbehavior`
4) Salvar cada retorno bruto em arquivo

**Comandos (templates)**
Interesses:
```bash
SEED="canva"
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch"   --data-urlencode "access_token=${ACCESS_TOKEN}"   --data-urlencode "type=adinterest"   --data-urlencode "q=${SEED}"   --data-urlencode "locale=${LOCALE}"   --data-urlencode "limit=25"   --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound"   > "out_targetingsearch_interest_${SEED}.json"
```

Cargos:
```bash
SEED="social media"
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch"   --data-urlencode "access_token=${ACCESS_TOKEN}"   --data-urlencode "type=adworkposition"   --data-urlencode "q=${SEED}"   --data-urlencode "locale=${LOCALE}"   --data-urlencode "limit=25"   --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound"   > "out_targetingsearch_workposition_social_media.json"
```

Behaviors:
```bash
SEED="small business owners"
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch"   --data-urlencode "access_token=${ACCESS_TOKEN}"   --data-urlencode "type=adbehavior"   --data-urlencode "q=${SEED}"   --data-urlencode "locale=${LOCALE}"   --data-urlencode "limit=25"   --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound"   > "out_targetingsearch_behavior_smb.json"
```

**Saída:** arquivos `out_targetingsearch_*.json` (candidatos com IDs)

---

### Etapa 4 — IA Worker escolhe o “anchor seed” e IDs úteis
**Objetivo:** selecionar 1 ID principal (anchor) e um conjunto enxuto de IDs para compor públicos.

**Entrada:** `out_targetingsearch_*.json` + regras de desambiguação.

**Ação (IA Worker)**
- Regras de desambiguação recomendadas:
  - Preferir `path` coerente (ex.: **Interests > … > Canva (software)**)
  - Rejeitar padrões irrelevantes (ex.: “Canvas Gaming”, “Birthday”, “Friends of”, “Expats”)
  - Priorizar itens mais específicos (pelo `name`/`path`) e com sinal forte no ICP

**Saída:** `seed_selected.json`
```json
{
  "anchor_seed": { "type":"interests", "id":"6015636111201", "name":"Canva (software)" },
  "extra_ids": {
    "work_positions": [
      { "id":"120762141304604", "name":"Social Media Manager" }
    ],
    "behaviors": [
      { "id":"6002714898572", "name":"Small business owners" }
    ]
  }
}
```

---

### Etapa 5 — Ads Worker expande o anchor seed (Targeting Suggestions)
**Objetivo:** obter um “mapa do ecossistema” a partir do seed principal.

**Entrada:** `seed_selected.json` (`anchor_seed.id`)

**Ação (Ads Worker)**
```bash
ANCHOR_ID="6015636111201"

curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsuggestions"   --data-urlencode "access_token=${ACCESS_TOKEN}"   --data-urlencode "targeting_list=[{\"type\":\"interests\",\"id\":\"${ANCHOR_ID}\"}]"   --data-urlencode "limit=150"   > suggestions_raw.json
```

**Saída:** `suggestions_raw.json`

> Nota importante: este é o caminho principal do pipeline.  
> Evite basear o fluxo em `/search?type=adinterestsuggestion` (muito ruído e ambiguidade em vários casos).

---

### Etapa 6 — IA Worker faz curadoria das sugestões
**Objetivo:** transformar `suggestions_raw.json` em uma lista enxuta, classificada e utilizável.

**Entrada:** `suggestions_raw.json`, `icp.md`, `ia_worker_config.json`

**Ação (IA Worker)**
- Filtrar e ranquear por:
  - Relevância (semântica) ao ICP
  - Tipo (`interests`, `behaviors`, `work_positions`, `industries`, etc.)
  - Limites por público (ex.: 6–12 interesses, 2–8 cargos, 1–6 behaviors)
  - Regras de exclusão (reject terms)

**Saída:** `suggestions_curated.json`

---

### Etapa 7 — IA Worker define 3 hipóteses (arquétipos)
**Objetivo:** escolher 3 públicos **diferentes entre si**, para testar hipóteses.

**Entrada:** `icp.md`, `suggestions_curated.json`

**Ação (IA Worker)**
- Escolher 3 “baldes” coerentes com o produto e o funil.
- **Design / Marketing / SMB** foi um exemplo que funciona para o Marketing Hub, mas **não é obrigatório**.

**Saída:** `audience_plan.json`
```json
{
  "audiences": [
    {"key":"A", "name":"Creators", "include_types":["interests","work_positions"]},
    {"key":"B", "name":"Marketing", "include_types":["interests","work_positions"]},
    {"key":"C", "name":"SMB_Deciders", "include_types":["behaviors","interests"]}
  ]
}
```

---

### Etapa 8 — IA Worker monta 3 `targeting_spec` com `flexible_spec`
**Objetivo:** produzir os 3 JSONs finais prontos para reach/adset.

**Entrada:** `audience_plan.json`, `seed_selected.json`, `suggestions_curated.json`, regras de geo/idade.

**Saída:** `spec_1.json`, `spec_2.json`, `spec_3.json`

---

### Etapa 9 — (Opcional) Sanity check de IDs (`/targetingvalidation`)
**Objetivo:** checar rapidamente se **IDs** de Detailed Targeting são válidos (cheque auxiliar).

**Entrada:** lista de IDs (extraída dos specs)

**Ação (Ads Worker)**
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingvalidation"   --data-urlencode "access_token=${ACCESS_TOKEN}"   --data-urlencode "id_list=[\"6015636111201\",\"6003389760112\"]"   > out_targetingvalidation.json
```

**Saída:** `out_targetingvalidation.json`

---

### Etapa 10 — Reach Estimate (validação end-to-end)
**Objetivo:** validar se o spec funciona e medir tamanho real no Brasil.

**Entrada:** `spec_1.json`, `spec_2.json`, `spec_3.json`

**Ação (Ads Worker)**
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/reachestimate"   --data-urlencode "access_token=${ACCESS_TOKEN}"   --data-urlencode "targeting_spec@spec_1.json"   > reach_1.json
```

**Saída:** `reach_1.json`, `reach_2.json`, `reach_3.json`

---

### Etapa 11 — Recalibração automática (IA Worker) [recomendado]
**Objetivo:** ajustar specs para bater a faixa de alcance desejada.

**Entrada:** `reach_*.json` + thresholds do `ia_worker_config.json`

**Saída:** specs atualizados + novas medições + `decision_log.jsonl`

---

### Etapa 12 — (Opcional) Criar Ad Sets via API (fim operacional)
**Entrada:** `campaign_id` + `targeting_spec`  
**Saída:** 3 ad sets criados (recomendado iniciar com `PAUSED`)

---

## 5) Observações importantes (para evitar resultados “estranhos”)

- **Idioma dos nomes**: mesmo com `locale=pt_BR`, vários `name` vêm em inglês. Isso é normal — o que manda é o **ID**.
- **Tipo importa**: a seleção correta começa no `type=` certo no `targetingsearch`.
- **Desambiguação é obrigatória**: “Canva” traz “Canvas”; “agricultura” traz itens amplos e às vezes fora do país.
- **ICP (frase) ≠ targeting**: ICP vira seeds curtas; seeds viram IDs.
- **Reach é o juiz**: reachestimate é a validação “de verdade” do spec.

---

## 6) Checklist rápido de conclusão

- ✅ `icp.md`
- ✅ `seed_candidates.json` + `seed_selected.json`
- ✅ `suggestions_raw.json` + `suggestions_curated.json`
- ✅ `audience_plan.json`
- ✅ `spec_1.json`, `spec_2.json`, `spec_3.json`
- ✅ `reach_1.json`, `reach_2.json`, `reach_3.json`
- ✅ `decision_log.jsonl`

---

## 7) Referências oficiais (Meta)

- Targeting Search: https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-search/
- Ad Account Targeting Search: https://developers.facebook.com/docs/marketing-api/reference/ad-account/targetingsearch/
- Ad Account Targeting Suggestions: https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingsuggestions/
- Flexible Targeting (`flexible_spec`): https://developers.facebook.com/docs/marketing-api/audiences/reference/flexible-targeting/
- Reach Estimate: https://developers.facebook.com/docs/marketing-api/reference/ad-account/reachestimate/
- Targeting Validation: https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingvalidation/
- Targeting Description: https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-description/
- Create an Ad Set: https://developers.facebook.com/docs/marketing-api/get-started/basic-ad-creation/create-an-ad-set/
