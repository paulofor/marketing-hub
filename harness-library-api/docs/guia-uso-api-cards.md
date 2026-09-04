# Guia de uso da API de cards da Biblioteca do Harness

## Objetivo

Os cards transformam pesquisas extensas em orientações curtas, rastreáveis e reutilizáveis pelo
harness dos agentes. O objetivo é ajudar os agentes a criar experiências digitais mais úteis,
sensoriais, personalizadas e comercialmente eficazes sem despejar artigos inteiros no contexto do
modelo.

Cada card preserva o achado, o mecanismo proposto, a aplicação comercial, a força da evidência, a
hipótese que ainda precisa ser testada, os riscos, os limites, a validade e o hash da fonte revisada.
Assim, o Marketing Hub consegue usar conhecimento externo sem confundir inspiração com prova.

Um card não comprova demanda ou venda, não aprova um criativo e não autoriza gasto ou publicação. O
efeito comercial deve ser verificado nos eventos reais do funil: retenção, CTA, checkout, pagamento,
entrega, satisfação e custo reconciliado.

## Como os agentes usam os cards

Os cards ativos e vigentes formam um catálogo global. Eles não ficam presos a um projeto: o backend
seleciona automaticamente até quatro cards aderentes ao contexto de cada execução audiovisual atual
ou futura e registra as fontes e os hashes efetivamente entregues.

| Agente | Chave retornada pela API | Coleções recebidas | Uso permitido |
| --- | --- | --- | --- |
| Íris | `communication-director` | `neuromarketing`, `momentos-de-compra-b2c` | Orientar mensagem, ângulo e briefing do canal. |
| Apolo | `videomaker` | `video`, `prazer-audio-visual` | Orientar roteiro, ritmo, áudio, continuidade e escolha técnica. |
| Psique | `customer-agent` | `neuromarketing`, `prazer-audio-visual` | Revisar percepção, fluidez, prazer, esforço e desejo. |
| Têmis | `meta-ad-approver` | Todas as quatro coleções | Verificar alegações, limites, coerência e integridade. |

Íris e Apolo usam os cards como orientação de comunicação e produção. Psique e Têmis os usam somente
como critérios de revisão independente. A coleção escolhida no cadastro determina os agentes
elegíveis; não é possível informar agentes arbitrariamente no JSON.

## Fluxo editorial obrigatório

```text
DRAFT -> IN_REVIEW -> ACTIVE -> ARCHIVED
```

1. O autor cadastra o card, que nasce como `DRAFT`.
2. Um revisor confere a fonte, o conteúdo e os limites e envia a versão para `IN_REVIEW`.
3. A versão revisada é ativada e passa a poder entrar nas seleções dos agentes.
4. Quando não deve mais orientar novas execuções, a versão é arquivada sem apagar a auditoria.

Uma nova submissão com o mesmo `cardKey` cria a próxima versão. A versão ativa anterior permanece em
uso até a nova versão ser revisada e ativada; nesse momento, a anterior é arquivada atomicamente.

## Endereço, autenticação e segurança

A URL pública canônica é `https://mkthub.api.br`, depois da ativação de DNS e HTTPS. Não use o IP do
host nem a porta interna por HTTP.

Os exemplos pressupõem Bash em Linux com `curl`, `jq`, `sha256sum`, `openssl` e GNU `date`.

Toda chamada exige:

- `X-API-Key`: chave de acesso fornecida pelo administrador;
- `X-Actor`: identificação de quem realiza a ação, por exemplo `paulo@digicomdigital.com.br`;
- `Idempotency-Key`: chave única em toda operação que altera estado;
- `Content-Type: application/json`: nas operações `POST`.

Nunca grave a API key em JSON, URL, script versionado, log ou histórico do terminal. Carregue-a em um
terminal confiável, sem `set -x`:

```bash
export HARNESS_LIBRARY_URL="https://mkthub.api.br"
read -rsp 'Harness Library API key: ' HARNESS_LIBRARY_API_KEY
export HARNESS_LIBRARY_API_KEY
printf '\n'
export HARNESS_LIBRARY_ACTOR="paulo@digicomdigital.com.br"
```

## Antes de cadastrar

Revise o material integral e calcule o SHA-256 dos mesmos bytes que serviram de base para o card. A
API não baixa a fonte automaticamente.

Exemplo para um Markdown versionado no repositório:

```bash
export SOURCE_FILE="pesquisas/video/minha-pesquisa.md"
export SOURCE_SHA256="$(sha256sum "${SOURCE_FILE}" | awk '{print $1}')"
```

Tipos e endereços aceitos:

| `sourceKind` | Formato de `sourceUri` |
| --- | --- |
| `URL` | `https://...` |
| `PDF` | `https://...` ou `s3://...` |
| `MARKDOWN` | `https://...` ou `repo:...` |
| `TEXT` | `urn:...` |

Use somente uma destas coleções: `video`, `prazer-audio-visual`, `neuromarketing` ou
`momentos-de-compra-b2c`. As datas usam o formato `AAAA-MM-DD`; `validUntil` não pode ser anterior a
`publishedOn`, e uma versão vencida não pode ser ativada.

## Exemplo completo com curl

### 1. Montar o JSON

O exemplo abaixo cria um card da coleção `video`. Ajuste o conteúdo, as datas e a fonte para o
material realmente revisado.

```bash
export CARD_KEY="ugc-demonstracao-produto-digital"
export PUBLISHED_ON="$(date -u +%F)"
export VALID_UNTIL="$(date -u -d '+45 days' +%F)"

jq -n \
  --arg cardKey "${CARD_KEY}" \
  --arg publishedOn "${PUBLISHED_ON}" \
  --arg validUntil "${VALID_UNTIL}" \
  --arg sourceSha256 "${SOURCE_SHA256}" \
  '{
    cardKey: $cardKey,
    collection: "video",
    title: "Demonstrar o produto digital no primeiro contato",
    finding: "A peça reduz ambiguidade quando mostra a experiência real no celular.",
    mechanism: "A demonstração torna o resultado e o esforço percebido mais concretos.",
    commercialApplication: "Comparar UGC com demonstração real contra uma peça apenas narrativa.",
    evidenceStrength: "Hipótese externa que exige teste no mesmo público e oferta.",
    publishedOn: $publishedOn,
    validUntil: $validUntil,
    experimentHypothesis: "A demonstração elevará CTA e checkout sem aumentar rejeição.",
    risks: "Não atribuir causalidade nem prometer resultado não observado.",
    limits: "Somente pagamentos reconciliados comprovam efeito em vendas.",
    sourceKind: "MARKDOWN",
    sourceUri: "repo:pesquisas/video/minha-pesquisa.md",
    sourceTitle: "Pesquisa revisada sobre demonstração de produto em UGC",
    sourceSha256: $sourceSha256
  }' > /tmp/harness-card.json

jq empty /tmp/harness-card.json
```

O JSON é estrito: campos desconhecidos são recusados e o corpo completo deve ter no máximo 32 KiB.
Os limites detalhados de cada campo estão no contrato OpenAPI citado ao final deste guia.

### 2. Cadastrar o rascunho

Crie uma chave idempotente e guarde-a até receber a resposta. Se houver timeout, repita o mesmo JSON
com a mesma chave; uma nova chave cria outra versão.

```bash
export REGISTER_IDEMPOTENCY_KEY="$(openssl rand -hex 16)"

export REGISTER_RESPONSE="$(
  curl --fail-with-body --silent --show-error \
    -X POST "${HARNESS_LIBRARY_URL}/v1/cards" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
    -H "X-Actor: ${HARNESS_LIBRARY_ACTOR}" \
    -H "Idempotency-Key: ${REGISTER_IDEMPOTENCY_KEY}" \
    --data-binary @/tmp/harness-card.json
)"

jq . <<<"${REGISTER_RESPONSE}"
export CARD_VERSION="$(jq -er '.version' <<<"${REGISTER_RESPONSE}")"
```

Resposta esperada: HTTP `201`, `status: "DRAFT"`, um `cardId` iniciado por `RI1-` e a lista
`routableAgents` calculada pelo backend. O primeiro cadastro da chave terá versão 1; os próximos terão
versão crescente, capturada em `CARD_VERSION`.

### 3. Consultar a versão

```bash
curl --fail-with-body --silent --show-error \
  "${HARNESS_LIBRARY_URL}/v1/cards/${CARD_KEY}/versions/${CARD_VERSION}" \
  -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
  -H "X-Actor: ${HARNESS_LIBRARY_ACTOR}" | jq
```

Antes da revisão, confirme:

- a fonte e o SHA-256 correspondem ao material revisado;
- o achado não apresenta hipótese como fato;
- o mecanismo é plausível e não inventa causalidade;
- a aplicação comercial descreve uma decisão executável;
- a hipótese possui um evento humano mensurável;
- riscos, limites e validade estão explícitos.

### 4. Enviar para revisão

```bash
export REVIEW_IDEMPOTENCY_KEY="$(openssl rand -hex 16)"

curl --fail-with-body --silent --show-error \
  -X POST "${HARNESS_LIBRARY_URL}/v1/cards/${CARD_KEY}/versions/${CARD_VERSION}/submit-review" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
  -H "X-Actor: revisor@digicomdigital.com.br" \
  -H "Idempotency-Key: ${REVIEW_IDEMPOTENCY_KEY}" \
  --data '{"reason":"Fonte, aplicação comercial, hipótese, riscos e limites conferidos."}' | jq
```

Resposta esperada: HTTP `200` e `status: "IN_REVIEW"`.

### 5. Ativar

```bash
export ACTIVATE_IDEMPOTENCY_KEY="$(openssl rand -hex 16)"

curl --fail-with-body --silent --show-error \
  -X POST "${HARNESS_LIBRARY_URL}/v1/cards/${CARD_KEY}/versions/${CARD_VERSION}/activate" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
  -H "X-Actor: aprovador@digicomdigital.com.br" \
  -H "Idempotency-Key: ${ACTIVATE_IDEMPOTENCY_KEY}" \
  --data '{"reason":"Versão aprovada para seleção contextual dos agentes."}' | jq
```

Resposta esperada: HTTP `200`, `status: "ACTIVE"` e `effectiveStatus: "ACTIVE"`. A ativação torna o
card elegível; ela não garante que ele será selecionado em toda execução, pois coleção, validade e
aderência ao contexto continuam sendo avaliadas.

### 6. Confirmar no catálogo administrativo

```bash
curl --fail-with-body --silent --show-error \
  "${HARNESS_LIBRARY_URL}/v1/cards?status=ACTIVE&collection=video&limit=100" \
  -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
  -H "X-Actor: ${HARNESS_LIBRARY_ACTOR}" \
  | jq --arg cardKey "${CARD_KEY}" '.items[] | select(.cardKey == $cardKey)'
```

Essa consulta confirma o estado editorial. A seleção final de cada job continua sendo feita pelo
backend de acordo com o contexto do projeto e o limite de quatro cards por agente.

### 7. Arquivar quando necessário

```bash
export ARCHIVE_IDEMPOTENCY_KEY="$(openssl rand -hex 16)"

curl --fail-with-body --silent --show-error \
  -X POST "${HARNESS_LIBRARY_URL}/v1/cards/${CARD_KEY}/versions/${CARD_VERSION}/archive" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
  -H "X-Actor: ${HARNESS_LIBRARY_ACTOR}" \
  -H "Idempotency-Key: ${ARCHIVE_IDEMPOTENCY_KEY}" \
  --data '{"reason":"Fonte superada por evidência mais atual."}' | jq
```

O arquivamento impede o uso em novas seleções, mas conserva conteúdo, autoria e histórico.

## Resumo dos endpoints

| Método e rota | Finalidade |
| --- | --- |
| `POST /v1/cards` | Criar a próxima versão em `DRAFT`. |
| `GET /v1/cards` | Listar versões, com filtros opcionais `status`, `collection` e `limit` de 1 a 200. |
| `GET /v1/cards/{cardKey}/versions/{version}` | Consultar conteúdo e auditoria de uma versão. |
| `POST .../submit-review` | Mover `DRAFT` para `IN_REVIEW`. |
| `POST .../activate` | Mover `IN_REVIEW` para `ACTIVE`. |
| `POST .../archive` | Mover `ACTIVE` para `ARCHIVED`. |

Não existe exclusão física pela API. Versionamento e arquivamento preservam as decisões tomadas por
execuções anteriores.

## Erros mais comuns

| HTTP | Interpretação | O que conferir |
| --- | --- | --- |
| `400` | JSON, campo, data, coleção ou cabeçalho inválido | Compare o payload com o OpenAPI e remova campos extras. |
| `401` | API key ausente ou incorreta | Recarregue a chave pelo cofre; não a envie em mensagem ou log. |
| `404` | Card ou versão inexistente | Confira `cardKey` e número da versão. |
| `409` | Transição inválida, versão vencida ou conflito idempotente | Consulte o estado atual e reutilize a chave somente com o mesmo payload. |
| `413` | JSON maior que 32 KiB | Resuma o card; não envie o artigo integral. |
| `502` ou `504` | Backend canônico indisponível | Preserve a chave idempotente e repita somente depois da recuperação. |

Toda resposta inclui `X-Request-ID`. Guarde esse identificador ao investigar uma falha, sem registrar a
API key.

## Critério de qualidade

O ciclo está concluído quando a versão aparece como `ACTIVE`, vigente, com fonte verificável, hash
correto e agentes roteáveis coerentes. Depois disso, acompanhe se os cards reduzem retrabalho e custo
por ativo aprovado e se melhoram retenção, CTA, checkout e pagamentos. Se apenas aumentarem custo ou
latência sem resultado humano mensurável, revise a coleção, o conteúdo ou a política de seleção.

Contrato técnico completo:
<a href="../../docs/swagger/harness-library-api-swagger.yaml" target="_blank" rel="noopener noreferrer">OpenAPI da Harness Library API</a>.
