# Pipeline de construção de 3 públicos-alvo (Meta Ads API)

Este documento descreve um **roteiro prático, curto e eficiente (começo → fim)** para construir **3 públicos-alvo** no **Brasil** usando a **Meta Ads API**, com **entrada e saída** em cada etapa e comandos prontos para **Git Bash** (sem `jq`).

> Fluxo principal (endpoints): **Targeting Search** → **Targeting Suggestions** → **Flexible Targeting (`flexible_spec`)** → **Reach Estimate** → (opcional) **Create Ad Set**.  
> Endpoints opcionais: **Targeting Validation** (valida *IDs*, não o spec inteiro) e **Targeting Description** (descrição humana do spec).

---

## Objetivo final (fim definido)

Você termina com:

1) **3 arquivos JSON** contendo `targeting_spec` (um por público/ad set):  
   - `spec_1_creators.json`, `spec_2_marketing.json`, `spec_3_deciders.json`

2) **3 respostas de alcance** (faixa lower/upper) no Brasil via `reachestimate`:  
   - `reach_1_creators.json`, `reach_2_marketing.json`, `reach_3_deciders.json`

3) (Opcional) **criação dos 3 Ad Sets** via API (`POST /adsets`) ou uso manual no Ads Manager.

---

## 0) Preparação do ambiente

### Objetivo
Garantir variáveis e padrão de requests robusto (evita “pipe vazio” e erros silenciosos).

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

> Dica prática: use `-sS --fail` no `curl` para não receber saída vazia sem perceber.

---

## 1) Definir o ICP e transformar em “seeds” (sementes) de targeting

### Objetivo
Sair do abstrato (“meu mercado”) para uma lista pequena de termos que viram **IDs** via API.

### Entrada
- 1 frase do seu mercado/ICP (exemplo):
  - “Pequenos negócios no Brasil que precisam criar conteúdo visual rápido para Instagram e anúncios.”
- Oferta (o que você vende): ex.: “criativos e imagens por IA a partir de foto do cliente (para posts/anúncios)”.

### Ação
Converta o ICP em **3 a 10 seeds** (palavras curtas e específicas), separados por tipo:

**Seeds (interesses / ferramentas)**
- `canva`
- `photoshop`
- `graphic design`
- `image editing`
- `photography`
- `social media marketing`
- `digital marketing`

**Seeds (cargos)**
- `social media manager`
- `designer`
- `marketing manager`

**Seeds (comportamentos)**
- `small business owners`
- `business page admins`
- `facebook page admins`
- `engaged shoppers` (opcional, se seu funil for compra direta)

### Saída
- Lista final de seeds (texto) para buscar IDs no passo 2.

> Importante: na API, o “seed” útil é o **ID** (não a frase). Frases longas ajudam a escolher seeds, mas não são seed “direto” para segmentação.

---

## 2) Converter seeds (texto) em IDs (Targeting Search)

### Objetivo
Encontrar os **IDs oficiais** dos descritores de segmentação.

### Entrada
- 1 seed por vez (`q=...`)
- Tipo (`type=...`):
  - `adinterest` para interesses
  - `adworkposition` para cargos
  - `adbehavior` para comportamentos

### Ação 2A — Buscar **interesses** (ex.: Canva)
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adinterest" \
  --data-urlencode "q=canva" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > out_interest_canva.json
```

### Ação 2B — Buscar **cargos** (ex.: Social Media)
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adworkposition" \
  --data-urlencode "q=social media" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > out_workpositions_socialmedia.json
```

### Ação 2C — Buscar **comportamentos** (ex.: Small business owners)
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsearch" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "type=adbehavior" \
  --data-urlencode "q=small business owners" \
  --data-urlencode "locale=pt_BR" \
  --data-urlencode "limit=25" \
  --data-urlencode "fields=id,name,type,path,audience_size_lower_bound,audience_size_upper_bound" \
  > out_behavior_smb.json
```

### Saída
- Arquivos `out_*.json` com listas contendo IDs.
- Você seleciona os IDs “bons” (relevantes ao ICP) para:
  - escolher o seed principal (passo 3)
  - compor os 3 públicos (passos 5–7)

> Observação: nomes podem vir em inglês mesmo com `locale=pt_BR`. O que importa é **geo_locations BR** no spec e os **IDs**.

---

## 3) Escolher o seed principal (ID + nome)

### Objetivo
Definir 1 seed “âncora” para obter sugestões coerentes.

### Entrada
- Lista de interesses do passo 2A (`out_interest_canva.json`)

### Ação
Escolha um item **muito específico** e com intenção clara (ex.: **Canva (software)**).

Exemplo (do seu caso real):
- `SEED_INTEREST_ID=6015636111201`
- `SEED_INTEREST_NAME="Canva (software)"`

### Saída
- Seed principal confirmado (ID + nome).

> Regra prática: seed específico > seed genérico. Quanto mais específico, melhores sugestões.

---

## 4) Expandir o seed com sugestões relacionadas (Targeting Suggestions)

### Objetivo
Obter um “mapa do ecossistema” (interesses, behaviors, cargos) ligado ao seed.

### Entrada
- `SEED_INTEREST_ID`

### Ação
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingsuggestions" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode 'targeting_list=[{"type":"interests","id":"6015636111201"}]' \
  --data-urlencode "limit=100" \
  > out_suggestions_seed.json
```

### Saída
- `out_suggestions_seed.json` com itens sugeridos (interesses/behaviors/cargos).

---

## 5) Curadoria (selecionar os melhores IDs para os 3 públicos)

### Objetivo
Transformar a lista de sugestões em um conjunto enxuto (evita “público Frankenstein”).

### Entrada
- `out_suggestions_seed.json`
- Seu ICP (quem compra / quem executa / sinal de intenção)

### Ação (curadoria manual guiada)
Selecione, tipicamente:

- **Interesses (5 a 15)**: ferramentas e temas diretamente ligados à criação e marketing
- **Behaviors (1 a 5)**: sinais de decisão/gestão (admins/owners)
- **Work positions (2 a 10)**: cargos que executam/compram (social media, designer etc.)

### Saída
- Um conjunto final de IDs por “balde” (exemplo):
  - Público 1 (Criadores): interesses de criação + cargos criativos
  - Público 2 (Marketing): interesses de marketing + cargos de social media
  - Público 3 (Decisores): behaviors (owners/admins) + interesses de ferramentas

---

## 6) Definir os 3 públicos (os “baldes”) — template replicável

### Objetivo
Definir 3 hipóteses que não sejam “a mesma coisa com nomes diferentes”.

### Entrada
- IDs curados
- Seu ICP

### Ação (modelo mental)
Escolha 3 baldes que respondem a:

1) **Quem cria** (criador/operacional)
2) **Quem distribui** (marketing/social)
3) **Quem decide** (dono/admin/sinal B2B)

> Se você mudar de nicho, os baldes mudam. O pipeline permanece igual.

### Saída
- Descrição do que entra em cada público (e por quê).

---

## 7) Montar os 3 `targeting_spec` (Brasil) com `flexible_spec`

### Objetivo
Produzir 3 JSONs finais prontos (um por público).

### Entrada
- IDs curados
- Idade/limites iniciais (ajustáveis depois)
- País: Brasil

### Ação 7A — Público 1: Criadores (Design / Edição)
```bash
cat > spec_1_creators.json <<'JSON'
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

### Ação 7B — Público 2: Marketing / Social Media
```bash
cat > spec_2_marketing.json <<'JSON'
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

### Ação 7C — Público 3: Decisores (SMB / Admins / Owners)
```bash
cat > spec_3_deciders.json <<'JSON'
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

### Saída
- `spec_1_creators.json`
- `spec_2_marketing.json`
- `spec_3_deciders.json`

---

## 8) (Opcional) Sanity check de Detailed Targeting (`/targetingvalidation`)

### Objetivo
Validar rapidamente IDs/nomes de **Detailed Targeting** (principalmente interesses/categorias).  
**Não** é validação do `targeting_spec` completo.

### Entrada
- Lista de IDs de interesses/categorias (extraídos dos specs)

### Ação
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingvalidation" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "id_list=[\"6015636111201\",\"6003096002658\",\"6003341931023\"]" \
  > out_targetingvalidation.json
```

### Saída
- `out_targetingvalidation.json` (validação dos IDs informados).

---

## 9) Validar “end-to-end” e medir o alcance no Brasil (Reach Estimate)

### Objetivo
Validar de ponta a ponta cada `targeting_spec` e medir se cada público está com tamanho suficiente, já com **BR**.

### Entrada
- `spec_*.json`

### Ação 9A — Reach do Público 1
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/reachestimate" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "targeting_spec@spec_1_creators.json" \
  > reach_1_creators.json
```

### Ação 9B — Reach do Público 2
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/reachestimate" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "targeting_spec@spec_2_marketing.json" \
  > reach_2_marketing.json
```

### Ação 9C — Reach do Público 3
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/reachestimate" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "targeting_spec@spec_3_deciders.json" \
  > reach_3_deciders.json
```

### Saída
- `reach_1_creators.json`, `reach_2_marketing.json`, `reach_3_deciders.json` contendo faixas de alcance.

### Como interpretar (regra simples)
- **Muito pequeno** → tende a não entregar: amplie idade, remova filtros, reduza ANDs, use menos cargos.
- **Muito grande** → tende a ser genérico: adicione um bloco relevante, refine seed/curadoria.

---

## 10) (Opcional) Gerar descrição humana do targeting (Targeting Description)

### Objetivo
Confirmar em texto o que o targeting está “dizendo” (ótimo para auditoria e documentação).

### Entrada
- `spec_*.json`

### Ação (exemplo com público 2)
```bash
curl -sS --failG "https://graph.facebook.com/${API_VERSION}/act_${AD_ACCOUNT_ID}/targetingdescription" \
  --data-urlencode "access_token=${ACCESS_TOKEN}" \
  --data-urlencode "targeting_spec@spec_2_marketing.json" \
  > desc_2_marketing.json
```

### Saída
- `desc_2_marketing.json` com descrição humana do público.

---

## 11) Checklist de conclusão

Fluxo concluído quando existir:

- ✅ Seed escolhido (ID + nome)
- ✅ Sugestões salvas (`out_suggestions_seed.json`)
- ✅ 3 specs (`spec_1_creators.json`, `spec_2_marketing.json`, `spec_3_deciders.json`)
- ✅ 3 reachs (`reach_1_creators.json`, `reach_2_marketing.json`, `reach_3_deciders.json`)

---

## 12) (Opcional) Criar Ad Sets via API (fim operacional)

### Objetivo
Criar os 3 Ad Sets no ad account via API.

### Entrada
- `campaign_id`
- orçamento, otimização, billing_event etc.
- `targeting_spec` (um dos specs)
- recomendação: iniciar `status=PAUSED`

### Ação
Consulte a referência “Create an Ad Set” e envie um `POST` para:
- `POST /act_<AD_ACCOUNT_ID>/adsets`

### Saída
- 3 Ad Sets criados (normalmente iniciar pausado para revisão).

---

## Erros comuns (e como evitar)

- **Usar texto como seed final**: sempre converta para **ID** via `targetingsearch`.
- **Seed genérico demais**: escolha seed específico (ex.: “Canva (software)”), não “marketing”.
- **Listas gigantes de interesses**: faça curadoria (5–15 itens) por público.
- **Não restringir BR**: sempre inclua `geo_locations: countries:["BR"]` antes de medir alcance.
- **Confiar no nome do interesse**: o que manda é o **ID**.
- **Não salvar outputs**: salve JSONs (`> arquivo.json`) para debug e repetição do experimento.

---

## Referências oficiais (Meta)

- Ad Account Targeting Search  
  https://developers.facebook.com/docs/marketing-api/reference/ad-account/targetingsearch/
- Ad Account Targeting Suggestions  
  https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingsuggestions/
- Flexible Targeting (`flexible_spec`)  
  https://developers.facebook.com/docs/marketing-api/audiences/reference/flexible-targeting/
- Ad Account Reach Estimate  
  https://developers.facebook.com/docs/marketing-api/reference/ad-account/reachestimate/
- (Opcional) Ad Account Targeting Validation (valida IDs)  
  https://developers.secure.facebook.com/docs/marketing-api/reference/ad-account/targetingvalidation/
- (Opcional) Targeting Description  
  https://developers.facebook.com/docs/marketing-api/audiences/reference/targeting-description/
- Create an Ad Set  
  https://developers.facebook.com/docs/marketing-api/get-started/basic-ad-creation/create-an-ad-set/
