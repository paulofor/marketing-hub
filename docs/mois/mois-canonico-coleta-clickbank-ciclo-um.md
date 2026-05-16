# MOIS — Documento Canônico da Coleta ClickBank (Ciclo Um)

## 1. Objetivo do ciclo

Este documento define o **Ciclo Um** da coleta ClickBank no MOIS, com foco em:
- coletar ofertas públicas da página Top Offers;
- normalizar os dados coletados;
- persistir no backend MOIS com rastreabilidade de origem;
- manter mapeamento explícito **fonte → destino** dos dados.

Referência de origem da coleta pública:
- `https://www.clickbank.com/blog/clickbank-top-offers/`

## 2. Escopo do Ciclo Um

O Ciclo Um cobre exclusivamente:
1. leitura da página pública de Top Offers;
2. extração de bloco de produto (título, nickname, categoria e links);
3. montagem de snapshots internos (`ClickbankProductSnapshot`);
4. transformação para payload de persistência (`references` + `rawMetadata`);
5. envio para endpoint de persistência MOIS.

Não cobre neste ciclo:
- enriquecimento por outras fontes externas;
- classificação avançada por IA;
- reconciliação cross-source.

## 3. Mapeamento de dados (Fonte → Destino)

### 3.1 Fonte primária (HTML ClickBank Top Offers)

| Campo de origem (ClickBank Top Offers) | Regra de extração no ciclo um | Campo de destino interno (snapshot) | Campo de destino na persistência MOIS |
|---|---|---|---|
| Heading numerado com link do produto (`<h1-3>...<a href=...>Título</a>`) | Extrair texto do link e sanitizar HTML | `title` | `references[].title` |
| `href` do link do título no heading | Normalizar URL relativa para absoluta | `detailsUrl` | `references[].rawMetadata.productUrl` |
| Linha “Nickname: ...” no bloco do produto | Extrair texto após label | `rating` *(uso atual para nickname)* | `references[].rawMetadata.productNickname` |
| Linha “Category: ...” no bloco do produto | Extrair texto após label | `commission` *(uso atual para categoria)* | `references[].rawMetadata.productCategory` |
| Link “Check out their landing page here.” | Extrair `href` e normalizar | `salesPageUrl` | `references[].url` e `references[].rawMetadata.salesPageUrl` |
| Momento da coleta | `Instant.now()` | `collectedAt` | `references[].collectedAt` |

### 3.2 Fonte de contexto operacional (requisição do coletor)

| Campo de origem | Destino na persistência MOIS |
|---|---|
| `request.source` | `references[].sourceContext` |
| `request.maxProducts` | `job.limitPerSource` (com regra de limite aplicado no coletor) |
| `workspaceId` configurado | `job.workspaceId` |
| `defaultNiche` configurado | `job.niche` e `references[].niche` |
| `defaultMarketTheme` configurado | `job.marketTheme` |

## 4. Contrato de persistência no MOIS (Ciclo Um)

No Ciclo Um, cada produto coletado deve gerar:
- `references[].title` preenchido com nome do produto;
- `references[].url` apontando para landing page quando existir;
- `references[].rawMetadata` com campos mínimos:
  - `productName`
  - `productUrl`
  - `salesPageUrl`
  - `productNickname`
  - `productCategory`
  - `clickbankTemperature` *(opcional quando disponível)*

## 5. Regras canônicas de qualidade do ciclo

1. **Priorizar URL de landing page do produto** para `references[].url`.
2. **Normalizar URLs relativas** para absolutas antes de persistir.
3. **Evitar duplicidade** por chave composta (`title + nickname + detailsUrl`).
4. Se nenhum produto for encontrado, registrar aviso operacional no log.
5. Persistência deve manter rastreabilidade mínima no `rawMetadata` para auditoria.

## 6. Observações de compatibilidade

- No estado atual do DTO, o campo `rating` é utilizado para nickname e `commission` para categoria.
- Esta adaptação é aceita no Ciclo Um por compatibilidade, mas deve evoluir em ciclo posterior para campos semânticos próprios.

## 7. Evolução prevista (Ciclo Dois)

No Ciclo Dois, recomenda-se:
- DTO dedicado para produto ClickBank com campos semânticos explícitos (`nickname`, `category`);
- validações automatizadas de consistência de URL;
- testes de regressão com múltiplos formatos de HTML da página Top Offers.

## 8. Definição canônica do Ciclo Dois (ativada)

O Ciclo Dois do coletor ClickBank passa a operar com o seguinte fluxo obrigatório:

1. Ler do backend MOIS (`/api/v1/mois/clickbase/products`) a lista de produtos já persistidos no ciclo 1.
2. Para cada produto, acessar a `detailsUrl` do produto para resolver a URL final da página de vendas.
3. Persistir novamente no backend MOIS via endpoint de persistência (`/api/v1/mois/persistence/collection-jobs/{jobId}`), garantindo armazenamento em base de páginas de venda para análises futuras.

### 8.1 Regras de execução agendada

A execução passa a ocorrer **a cada hora**, com roteamento por `horaAtual % 3`:

- **Resto 0:** executar **Ciclo 1** (Top Offers público).
- **Resto 1:** executar **Ciclo 2** (resolução/persistência de páginas de venda).
- **Resto 2:** executar **Ciclo 3** (coleta via GraphQL ClickBank).

## 9. Definição canônica do Ciclo Três (ativada)

O Ciclo Três do coletor ClickBank passa a operar com o seguinte fluxo obrigatório:

1. Ler JWT ClickBank salvo no backend MOIS (`general-settings`).
2. Executar consulta no endpoint GraphQL da ClickBank (`/graphql`) para retornar produtos ranqueados.
3. Mapear os hits para o snapshot canônico do MOIS (`title`, `detailsUrl`, `category`, `temperature`, `salesPageUrl`).
4. Persistir no backend MOIS via endpoint de persistência (`/api/v1/mois/persistence/collection-jobs/{jobId}`).

### 9.1 Regras do Ciclo 3

- O Ciclo 3 é **independente** do Ciclo 1 e não deve ser tratado apenas como fallback interno.
- Sem JWT válido, o ciclo deve registrar status explícito de coleta sem dados (skipped), preservando rastreabilidade operacional.
- Logs de ingestão devem registrar payload bruto retornado do GraphQL antes de normalização.

### 8.2 Responsabilidades por módulo

- **Leitura e persistência:** backend pacote **MOIS**.
- **Coleta e tratamentos:** módulo **mois-clickbank-collector**.
