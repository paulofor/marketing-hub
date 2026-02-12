# Criação de públicos para experimento (Meta Ads API)

Este documento descreve um **roteiro prático, curto e eficiente** (começo → fim) para chegar em **3 Ad Sets no Brasil** usando a **Meta Ads API**, com entrada e saída esperadas em cada etapa e comandos prontos para **Git Bash**.

> Endpoints-chave deste fluxo: **Targeting Search** → **Interest Suggestions (`/search` + `adinterestsuggestion`)** → **Flexible Targeting (`flexible_spec`)** → **Targeting Validation** → **Reach Estimate**.

## 0) Preparação do ambiente

**Objetivo:** garantir variáveis e padrão de requests.

**Entrada:**

- `ACCESS_TOKEN`
- `AD_ACCOUNT_ID` (sem `act_`)
- `API_VERSION` (ex.: `v24.0`)

**Ação (Git Bash):**

```bash
export API_VERSION="v24.0"
export AD_ACCOUNT_ID="1234567890"
export ACCESS_TOKEN="EAAB..."
```

**Saída:** variáveis prontas para os próximos comandos.

---

## 1) Definir o seed principal do público

**Objetivo:** achar 1 interesse central e obter o **ID correto** (ex.: Canva software).

**Entrada:**

- Palavra-chave do seed (ex.: `canva`)
- `locale` (ex.: `pt_BR`)

**Ação (Targeting Search):**

```bash
curl -sG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adinterest" \
  --data-urlencode "q=canva" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound"
```

**Saída (guardar):**

- `SEED_INTEREST_ID=6015636111201`
- `SEED_INTEREST_NAME="Canva (software)"`

---

## 2) Expandir o seed com sugestões relevantes

**Objetivo:** puxar interesses/cargos/comportamentos relacionados ao seed (mapa do ecossistema).

**Entrada:**

- `SEED_INTEREST_ID` (ex.: `6015636111201`)

**Ação (Targeting Suggestions):**

```bash
curl -sG "https://graph.facebook.com/${API_VERSION}/search" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adinterestsuggestion" \
  --data-urlencode 'interest_list=["6015636111201"]' \
  --data-urlencode "limit=100" \
  --data-urlencode "locale=pt_BR"
```

**Saída (extrair e listar):**

- **Interesses** (ex.: Graphic design, Photoshop, Image editing, Social media marketing)
- **Behaviors** (ex.: Facebook Page admins, Small business owners)
- **Work positions** (ex.: Freelance Designer)

---

## 3) (Opcional, recomendado) Completar com cargos de Social Media via search

**Objetivo:** pegar IDs de cargos como Social Media Manager/Strategist/Coordinator.

**Entrada:** palavra-chave (ex.: `social media`)

**Ação (Targeting Search para cargos):**

```bash
curl -sG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adworkposition" \
  --data-urlencode "q=social media" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound"
```

**Saída (guardar):** IDs de cargos úteis para B2B.

---

## 4) Montar os 3 `targeting_spec` (Brasil)

**Objetivo:** produzir **3 JSONs finais** (um por ad set) usando `flexible_spec` para combinar blocos.

### 4A) Ad Set 1 — Designers / Edição (criadores)

**Entrada:** IDs de interesses de design + cargos relacionados.

**Ação (`spec_design.json`):**

```bash
cat > spec_design.json <<'JSON'
{
  "geo_locations": {"countries":["BR"]},
  "age_min": 18,
  "age_max": 45,
  "flexible_spec": [
    { "interests": [
      {"id":"6003096002658","name":"Graphic design (visual art)"},
      {"id":"6003332838432","name":"Image editing (graphic design)"},
      {"id":"6003341931023","name":"Adobe Photoshop (software)"},
      {"id":"6003322091141","name":"Adobe Illustrator (software)"}
    ]},
    { "work_positions": [
      {"id":"119151501433716","name":"Freelance Graphic Designer"},
      {"id":"128589787185844","name":"Freelance Designer"}
    ]}
  ]
}
JSON
```

**Saída:** `spec_design.json`

### 4B) Ad Set 2 — Social Media / Marketing (gestores e agências)

**Entrada:** IDs de interesses de marketing + cargos de social media.

**Ação (`spec_marketing.json`):**

```bash
cat > spec_marketing.json <<'JSON'
{
  "geo_locations": {"countries":["BR"]},
  "age_min": 20,
  "age_max": 55,
  "flexible_spec": [
    { "interests": [
      {"id":"6003389760112","name":"Social media marketing (marketing)"},
      {"id":"6003127206524","name":"Digital marketing (marketing)"},
      {"id":"6003526234370","name":"Online advertising (marketing)"},
      {"id":"6003031657055","name":"Content marketing (marketing)"}
    ]},
    { "work_positions": [
      {"id":"120762141304604","name":"Social Media Manager"},
      {"id":"347150238819359","name":"Social Media Strategist"},
      {"id":"147394665273861","name":"Social Media Coordinator"}
    ]}
  ]
}
JSON
```

**Saída:** `spec_marketing.json`

### 4C) Ad Set 3 — Dono de negócio / Admin de página (alta intenção B2B)

**Entrada:** behaviors + seed Canva + ferramentas correlatas.

**Ação (`spec_smb.json`):**

```bash
cat > spec_smb.json <<'JSON'
{
  "geo_locations": {"countries":["BR"]},
  "age_min": 22,
  "age_max": 60,
  "flexible_spec": [
    { "behaviors": [
      {"id":"6002714898572","name":"Small business owners"},
      {"id":"6020530281783","name":"Business page admins"},
      {"id":"6015683810783","name":"Facebook Page admins"}
    ]},
    { "interests": [
      {"id":"6015636111201","name":"Canva (software)"},
      {"id":"6003670602220","name":"Instagram (social media)"},
      {"id":"6003241014410","name":"WordPress (software)"},
      {"id":"6003199702082","name":"Wix.com (software)"},
      {"id":"6003230166788","name":"Shopify (software)"}
    ]}
  ]
}
JSON
```

**Saída:** `spec_smb.json`

---

## 5) (Opcional) Sanity check de Detailed Targeting (`/targetingvalidation`)

**Objetivo:** validar IDs/nomes de **Detailed Targeting** (principalmente interesses/categorias).

> Importante: esse endpoint **não** valida o `targeting_spec` completo. Use-o apenas como checagem rápida de IDs.

**Entrada:** lista de IDs de interesses/categorias coletados dos `spec_*.json`.

**Ação (Targeting Validation):**

```bash
curl -sG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingvalidation" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode 'id_list=["6003389760112","6004030160948"]'
```

Se necessário, valide por nomes com `name_list` ou por estruturas com `targeting_list`.

**Saída:** resposta de validação para os IDs informados.

---

## 6) Estimar alcance no Brasil

**Objetivo:** validar de ponta a ponta cada `targeting_spec` (BR + idade + `flexible_spec`) e medir o tamanho do público.

**Entrada:** `spec_*.json`

**Ação (Reach Estimate):**

```bash
curl -sG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/reachestimate" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "targeting_spec@spec_design.json"
```

Repita para os outros specs (`spec_marketing.json` e `spec_smb.json`).

**Saída:** alcance estimado por ad set (faixa lower/upper).

---

## 7) Resultado final: 3 Ad Sets prontos para uso

### Opção A — Ads Manager (manual)

**Entrada:** `spec_design.json`, `spec_marketing.json`, `spec_smb.json`.

**Saída:** 3 ad sets criados com públicos consistentes.

### Opção B — API (automação)

**Entrada:** `campaign_id`, orçamento, objetivo e `targeting_spec`.

**Ação:** `POST /act_<AD_ACCOUNT_ID>/adsets`.

**Saída:** ad sets criados (recomendado iniciar com `status=PAUSED`).

---

## Checklist de conclusão

Fluxo concluído quando existir:

- ✅ 1 seed confirmado (ID + nome)
- ✅ sugestões relevantes via `/search?type=adinterestsuggestion`
- ✅ 3 arquivos `spec_*.json`
- ✅ validação OK em `/targetingvalidation`
- ✅ alcance estimado BR em `/reachestimate`

Se necessário, avalie os retornos de `reachestimate` para calibrar idade, blocos e prioridades de cada público (evitando públicos amplos ou estreitos demais).

## Referências

- [Graph API Reference v24.0: Ad Account Targetingsearch](https://developers.facebook.com/docs/marketing-api/reference/ad-account/targetingsearch/?utm_source=chatgpt.com)
- [Graph API Reference v24.0: Ad Account Targetingsuggestions](https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingsuggestions/?utm_source=chatgpt.com)
- [Flexible Targeting - Marketing API](https://developers.facebook.com/docs/marketing-api/audiences/reference/flexible-targeting/?utm_source=chatgpt.com)
- [Graph API Reference v24.0: Ad Account Targetingvalidation](https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingvalidation/?utm_source=chatgpt.com)
- [Ad Account Reach Estimate](https://developers.facebook.com/docs/marketing-api/reference/ad-account/reachestimate/?utm_source=chatgpt.com)
- [Create an Ad Set](https://developers.facebook.com/docs/marketing-api/get-started/basic-ad-creation/create-an-ad-set/?utm_source=chatgpt.com)
