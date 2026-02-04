# ANEXO A — Especificação da alteração (AI Worker “Seeds” + Resolver Meta)

> **Objetivo deste anexo**: orientar o time de desenvolvimento para alterar o fluxo atual (texto livre → validação tardia) para um fluxo **seed-first** (semente genérica → busca/sugestão/validação via Meta) reduzindo drasticamente `NO_MATCH` e falhas na criação de Ad Set.

---

## 1) Problema que a alteração resolve

Hoje, o AI Worker gera **texto livre** tentando “adivinhar” interesses/cargos/comportamentos. Esse texto é validado apenas no momento de montar o targeting, gerando:
- Falhas: “não encontrado na API da Meta”
- Retrabalho: o cliente vê sugestões que não existem como opções de targeting
- Baixa confiança no produto

A mudança proposta torna a Meta a **fonte de verdade** de opções de targeting.

---

## 2) Nova visão: Meta como “Ground Truth” e IA como geradora de sementes

### Antes (atual)
IA → lista final de opções (texto livre) → tentar validar → muitos `NO_MATCH`.

### Depois (novo)
IA → **sementes (keywords)** + sinônimos/variações → **Meta Search/Suggestions/Browse** → salvar somente opções válidas (IDs) → usar apenas IDs na criação do Ad Set.

**Principais endpoints Meta (conceitualmente)**
- Targeting Search (buscar opções válidas por query/tipo)
- Targeting Suggestions (expandir opções relacionadas a partir de um spec/semente)
- Targeting Validation (validar o targeting_spec final)
- (Opcional) Targeting Browse / Broad Targeting Categories (navegação/descoberta)

---

## 3) Alteração de contrato: saída do AI Worker (candidatos viram “seeds”)

### 3.1. Mudança de semântica
O campo `texto_sugerido` passa a significar **SEMENTE DE BUSCA** (não “nome oficial do Facebook”).

**Regras para gerar seeds (guardrails)**
- 1 a 4 palavras; evitar frases longas (“noivas minimalistas em SP” vira “casamento minimalista”)
- Remover localidade (cidade/estado/país) do texto (geo vai por filtros de campanha/ad set)
- Incluir variações: singular/plural, sem acento, inglês (quando fizer sentido)
- Evitar termos sensíveis / PII / proibidos (mantém as regras já previstas)

### 3.2. Payload recomendado (versão v2)
```json
{
  "candidates": [
    {
      "seed": "casamento minimalista",
      "tipo": "interest",
      "origem": "AI",
      "ai_score": 0.87,
      "rationale": "Tema central do nicho; provável existir como interesse consolidado",
      "idioma_hint": "pt_BR",
      "seed_variants": [
        "casamento minimalista",
        "casamentos minimalistas",
        "minimalist wedding"
      ],
      "intent_stage": "awareness",
      "constraints": {
        "country": "BR"
      }
    }
  ]
}
```

**Compatibilidade**: se o backend já grava `texto_sugerido`, mantenha-o por enquanto como alias de `seed` (migração incremental).

---

## 4) Alteração no Resolver: “Search → Suggestions → Validation”

### 4.1. Entradas do Resolver
Para cada candidato/seed:
- `seed` + `seed_variants`
- `tipo` ∈ {interest, behavior, work_position}
- `idioma_hint` e `country`
- (Opcional) “budget de chamadas” por request (para controle de custo e rate limit)

### 4.2. Estratégia de consulta (sequência)
Para **cada seed_variant**, em ordem:

1) **Targeting Search**
- Buscar por `type` correspondente:
  - interest → adinterest
  - behavior → adbehavior
  - work_position → adworkposition
- Tentar locais em sequência:
  1. `pt_BR`
  2. `en_US`
  3. sem locale (último recurso)

2) **Se retornou resultados**: ranquear e salvar top-N como `VALIDATED`
3) **Se retornou vazio** em todas as variantes/locais: marcar `NO_MATCH` com motivo:
   - `EMPTY_RESULTS` | `RATE_LIMIT` | `PERMISSION` | `INVALID_QUERY` | `UNKNOWN`

4) (Recomendado) **Targeting Suggestions**
- Quando houver pelo menos 1 interesse/behavior/cargo validado, criar um targeting_spec “semente” e pedir sugestões para expandir cluster.
- Salvar sugestões como novas `targeting_options` (com referência ao seed original), evitando duplicatas.

5) **Targeting Validation (pré-publicação)**
- Antes de criar/atualizar Ad Set, validar o targeting_spec final (incluindo geos, idades, genders, placements etc.).
- Se falhar: retornar erro claro, e registrar o motivo técnico + quais IDs causaram o problema.

### 4.3. Rankeamento (score final) — recomendação prática
Para cada opção retornada pela Meta, calcular `final_score`:

```
final_score = 0.55 * ai_score
            + 0.35 * match_score
            + 0.10 * size_score
```

Onde:
- `match_score`: exato > prefixo > fuzzy
- `size_score`: normalizado com base no audience_size (se disponível) ou estimado via Reach Estimate (opcional)

**Observação**: Em alguns casos, a Meta pode devolver menos (ou nenhum) indicador de tamanho no Search; então trate `size_score` como opcional.

---

## 5) Uso de “Browse” / “Broad categories” (opcional, mas poderoso)

Quando um seed falha muito (ex.: cargos específicos), em vez de insistir em texto livre:
- usar “browse” (taxonomia) para oferecer ao usuário seleção guiada
- ou listar “broad targeting categories” para iniciar um targeting mais amplo e deixar o algoritmo/creative fazer o trabalho

Isso reduz frustração e aumenta cobertura (principalmente para nichos muito específicos).

---

## 6) Cache e rate limit (requisito não-funcional)

### 6.1. Cache recomendado
- Chave: `type + country + locale + query`
- TTL sugerido: 7 dias (interesses mudam, mas não diariamente)
- Armazenar também “NO_MATCH” por TTL curto (ex.: 24h) para evitar martelar a API com o mesmo termo.

### 6.2. Proteções
- Limite de chamadas por request (ex.: 300 queries no total)
- Backoff exponencial em 429
- Fila e reprocessamento (DLQ) para falhas transitórias

---

## 7) Persistência (modelo mínimo)

Reaproveitar o modelo atual do documento e acrescentar campos:

### `targeting_candidates`
- `seed` (texto)
- `seed_variants` (jsonb)
- `idioma_hint`
- `status` ∈ PENDING_FACEBOOK_MATCH | VALIDATED | NO_MATCH
- `motivo_rejeicao` (enum + texto)

### `targeting_options`
- `facebook_id`
- `type` (adinterest/adbehavior/adworkposition)
- `name`
- `path/hierarchy` (se disponível)
- `audience_size` (se disponível)
- `final_score`
- `source` ∈ SEARCH | SUGGESTIONS | BROWSE

---

## 8) Critérios de aceite (Definition of Done)

1) Para qualquer request, o front só exibe opções `VALIDATED` (IDs válidos), mantendo “rascunhos” e motivos de rejeição acessíveis.
2) O Ad Set é criado **somente** com IDs retornados pela Meta (nunca texto livre).
3) Métricas:
   - taxa de match por tipo (interest/behavior/work_position)
   - tempo médio de resolução
   - taxa de erro por categoria (429, permissão, etc.)
4) Logs estruturados com:
   - requestId, candidateId, seed, variant, type, locale, country, status, motivo_rejeicao
5) Testes:
   - unit: normalização/geração de variantes
   - integração: fluxo Search/Suggestions/Validation com mocks
   - e2e: request → opções validadas → criação do Ad Set

---

## 9) Plano de rollout (seguro e incremental)

1) **Fase 1 (compatibilidade)**: manter AI Worker igual, mas interpretar `texto_sugerido` como seed e aplicar Search + fallback.
2) **Fase 2 (contrato v2)**: AI Worker passa a enviar `seed_variants` e `idioma_hint` explicitamente.
3) **Fase 3 (expansão)**: habilitar Suggestions e (opcional) Browse.
4) **Fase 4 (otimização)**: scoring final + reach estimate (se necessário) + painéis de governança.

---

## 10) Observações de Marketing (para orientar produto)

- Priorizar **clusters de interesses** e testes A/B de criativo em vez de microsegmentações.
- Em cenários onde a Meta restringe opções ou consolida interesses, oferecer caminho “público amplo” (Advantage+/broad) como alternativa nativa.
