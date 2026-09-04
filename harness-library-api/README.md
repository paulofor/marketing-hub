# Harness Library API

API JSON externa para cadastrar e curar cartões da Biblioteca do Harness. Ela não possui banco: toda
escrita e leitura passa pelo backend principal do Marketing Hub, que continua responsável por
versionamento, estados, validade, roteamento e auditoria.

## Fluxo editorial

1. `POST /v1/cards` cria uma nova versão em `DRAFT`.
2. `POST .../submit-review` move a versão para `IN_REVIEW`.
3. `POST .../activate` publica a versão para os agentes e arquiva atomicamente a versão ativa anterior.
4. `POST .../archive` retira um cartão ativo das novas seleções, preservando o histórico.

Toda rota exige `X-API-Key` e `X-Actor`. Toda mutação também exige `Idempotency-Key`. Não envie chaves
em arquivos JSON, URLs, logs ou no repositório. O JSON possui limite físico de 32 KiB e o payload
bruto de mutações autenticadas é auditado antes da desserialização.

## Exemplo com curl

Calcule o hash SHA-256 do material que foi realmente revisado e crie `card.json`:

```json
{
  "cardKey": "ugc-demonstracao-produto-digital",
  "collection": "video",
  "title": "Demonstrar o produto digital no primeiro contato",
  "finding": "A peça reduz ambiguidade quando mostra a experiência real no celular.",
  "mechanism": "A demonstração torna o resultado e o esforço percebido mais concretos.",
  "commercialApplication": "Comparar UGC com demonstração real contra uma peça apenas narrativa.",
  "evidenceStrength": "Hipótese externa que exige teste no mesmo público e oferta.",
  "publishedOn": "2026-09-04",
  "validUntil": "2026-10-19",
  "experimentHypothesis": "A demonstração elevará CTA e checkout sem aumentar rejeição.",
  "risks": "Não atribuir causalidade nem prometer um resultado não observado.",
  "limits": "Somente pagamentos reconciliados comprovam efeito em vendas.",
  "sourceKind": "TEXT",
  "sourceUri": "urn:marketing-hub:research:ugc-demonstracao-produto-digital",
  "sourceTitle": "Síntese revisada sobre demonstração UGC",
  "sourceSha256": "<sha256-em-minusculas-com-64-caracteres>"
}
```

Com a chave carregada de um cofre local, sem imprimi-la:

```bash
curl --fail-with-body --silent --show-error \
  -X POST "${HARNESS_LIBRARY_URL}/v1/cards" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
  -H "X-Actor: operador@digicomdigital.com.br" \
  -H "Idempotency-Key: $(uuidgen)" \
  --data-binary @card.json
```

As transições recebem sempre JSON:

```bash
curl --fail-with-body --silent --show-error \
  -X POST "${HARNESS_LIBRARY_URL}/v1/cards/ugc-demonstracao-produto-digital/versions/1/submit-review" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
  -H "X-Actor: revisor@digicomdigital.com.br" \
  -H "Idempotency-Key: $(uuidgen)" \
  --data '{"reason":"Fonte e limites conferidos."}'
```

O contrato completo está em `docs/swagger/harness-library-api-swagger.yaml`.

## Deploy planejado

- host: `163.245.200.7`;
- origem: `127.0.0.1:8103`;
- imagem: `ghcr.io/paulofor/marketing-hub/harness-library-api:sha-<commit>`;
- compose: `docker-compose.deploy.yml`;
- workflow: `.github/workflows/harness-library-api-ci.yml`.

O workflow testa e publica a imagem imutável após entrada em `main`. O deploy é manual pelo
`workflow_dispatch` com `deploy=true`, provisiona os secrets como arquivos protegidos, valida health,
faz uma consulta assinada ao backend e confirma que a porta continua em loopback. O domínio, DNS e TLS
devem ser configurados antes de encaminhar tráfego público; até lá não existe URL pública segura.
Health e métricas usam a porta de gerenciamento `9103`, vinculada somente ao loopback do container e
não publicada pela imagem.

O container executa com a identidade não privilegiada fixa `10001:10001`. No host, os dois arquivos de
secret pertencem à mesma identidade e usam modo `0400`; o CI inicia a imagem com essa topologia antes
de permitir a publicação. Não altere os arquivos para leitura global e não execute o gateway como root.

## Secrets obrigatórios

- `HARNESS_LIBRARY_API_KEY`: autentica clientes de curl no gateway;
- `HARNESS_LIBRARY_INTERNAL_SIGNING_KEY`: assina gateway → backend e deve ter o mesmo valor nos dois
  deploys;
- chave SSH já usada pelo ambiente `production` para `163.245.200.7`;
- token GHCR para o host baixar a imagem.

Nenhum secret possui fallback versionado. Ambos devem ter pelo menos 32 caracteres e podem ser
rotacionados de forma independente; durante a rotação da assinatura interna, backend e gateway devem
ser atualizados na mesma janela.

## Homologação local

O arquivo `docker-compose.homologation.yml` sobe MySQL 5.7, backend e gateway com dados e chaves
exclusivamente sintéticos. Antes de subir, gere o JAR do backend. Use obrigatoriamente o projeto Compose
isolado definido para a sandbox e remova volumes ao terminar. Os cenários e a regra de repetição após
defeitos estão em `docs/homologacao/harness-library-api-v1.md`.

O ciclo HTTP completo é automatizado por:

```bash
bash harness-library-api/scripts/homologate-local.sh
```

Como o changelog mestre legado depende de uma base já evoluída, esse E2E usa schema efêmero gerado
pelas entidades. A migração desta funcionalidade é comprovada separadamente, no mesmo MySQL 5.7, por:

```bash
bash backend/ads-service/scripts/validate-harness-library-api-mysql57.sh
```
