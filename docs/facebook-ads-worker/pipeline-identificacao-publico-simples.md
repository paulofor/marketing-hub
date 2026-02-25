# Pipeline simples de identificação de público por cargo

## Objetivo

Simplificar a criação de público quando o nicho pode ser mapeado diretamente para um cargo da Meta Ads.

Exemplo: para o nicho **"Professores de Educação Física"**, o pipeline deve buscar em `work_positions` o cargo equivalente a "professor de educação física" e utilizar esse ID no `targeting_spec`.

---

## Acessos do Facebook necessários

Para montar esse público com segurança no ambiente de produção, a conta e o app precisam dos seguintes acessos:

1. **Permissões do token de usuário**
   - `ads_read`: consultar `targetingsearch` e `reachestimate`.
   - `ads_management`: criar ad set/campanha com o público validado.

2. **Nível de acesso do app**
   - App em modo **Live**.
   - Permissões acima aprovadas com **Advanced Access** no Meta for Developers.

3. **Permissões na conta de anúncios (`act_<AD_ACCOUNT_ID>`)**
   - Usuário com papel de **Anunciante** (ou superior) na conta de anúncios.
   - Conta ativa e sem bloqueio de política para criação de conjuntos de anúncios.

4. **Pré-requisitos operacionais**
   - `access_token` válido (long-lived recomendado).
   - `AD_ACCOUNT_ID` correto.
   - Versão da Marketing API definida (ex.: `v24.0`).

---

## Pipeline proposto (versão simples)

### 1) Normalizar o nicho para consulta

Entrada humana:
- Nicho: `Professores de Educação Física`

Termo de consulta canônico:
- `professor de educação física`

### 2) Buscar cargos na API (`targetingsearch`)

Endpoint:
- `GET /act_<AD_ACCOUNT_ID>/targetingsearch`

Parâmetros mínimos:
- `type=adworkposition`
- `q=professor de educação física`
- `locale=pt_BR`
- `limit=25`

Objetivo:
- Retornar uma lista de cargos com `id` e `name`.

### 3) Selecionar o melhor cargo

Critérios de seleção:
1. Correspondência exata de nome (ignorando maiúsculas/minúsculas).
2. Caso não exista exata, usar a mais próxima semanticamente.
3. Em empate, preferir resultado com maior cobertura de audiência (quando disponível).

Saída esperada:
- `selected_work_position = { id, name }`

### 4) Montar `targeting_spec` mínimo

Usar o cargo selecionado dentro de `flexible_spec`:

```json
{
  "geo_locations": { "countries": ["BR"] },
  "age_min": 18,
  "flexible_spec": [
    {
      "work_positions": [
        { "id": "<WORK_POSITION_ID>", "name": "Professor de Educação Física" }
      ]
    }
  ]
}
```

### 5) Validar alcance (`reachestimate`)

Endpoint:
- `GET /act_<AD_ACCOUNT_ID>/reachestimate`

Objetivo:
- Confirmar que o público é válido e possui volume útil para campanha.

Regra de fallback:
- Se o alcance ficar muito baixo, complementar com **interesse relacionado** (ex.: fitness, educação física), mantendo o cargo como filtro principal.

### 6) Publicar ad set (opcional)

Após validação de alcance:
- Criar ad set com status inicial `PAUSED` para revisão operacional.

---

## Exemplo de sequência de chamadas

1. `targetingsearch` com `type=adworkposition` e `q=professor de educação física`.
2. Seleção do `work_position.id` mais aderente.
3. `reachestimate` com `targeting_spec` contendo esse `work_position.id`.
4. (Opcional) `POST /adsets` com o mesmo `targeting_spec`.

---

## Resultado esperado

Com esse fluxo, o pipeline deixa de depender de expansão extensa de seeds e passa a operar de forma direta:
- **Nicho → Cargo (`work_positions`) → Alcance → Publicação**.

Isso reduz complexidade e acelera a criação de públicos quando o nicho é claramente representado por profissão/cargo.
