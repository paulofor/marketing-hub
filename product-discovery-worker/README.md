# Product Discovery Worker

Worker operacional da Descoberta de Produtos PDE v1.

## Responsabilidade

- consumir pendências do backend em `/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending`;
- pesquisar sinais públicos sem coletar dados pessoais;
- operar em `DISCOVER_MARKETS` para organizar até três candidatas factuais ou em
  `VALIDATE_MARKET` para aprofundar um mercado informado;
- solicitar ao backend a cobertura persistida da categoria no Instagram, sem receber a credencial Meta;
- cruzar busca pública, fontes editoriais fornecidas, artigos versionados de `/pesquisas`,
  ofertas e cobertura Meta;
- sintetizar candidatas vinculadas aos identificadores das evidências, sem assumir a estratégia da Atena;
- reportar sucesso ou falha ao backend.

Cada pendência chega com lease exclusivo. O worker processa uma execução por vez,
renova o lease ao registrar o plano e o repete nos callbacks. Se o processo cair, o
backend recupera o ciclo após vinte minutos; uma tentativa antiga não pode sobrescrever
a retomada.

O worker não cria produto, hipótese, landing, campanha ou gasto de mídia.

Argos usa duas chamadas separadas e auditáveis quando o Codex está habilitado: uma planeja as
consultas e outra organiza somente os fatos coletados. Prompt, resposta bruta, modelo, tokens
disponíveis e URLs ficam ligados ao ciclo. Se o modelo estiver desligado, o modo determinístico
preserva a coleta, mas retorna zero candidatas em vez de repetir sugestões genéricas.

A biblioteca interna é gerada deterministicamente por `npm run build:research-library`. O índice
versionado preserva caminho, hash e trechos dos Markdown de `/pesquisas`; esses artigos inspiram
lentes de investigação, mas nunca substituem confirmação pública de demanda.

Em ciclos B2C para Instagram, Argos registra no plano uma consulta Meta com país,
plataforma e termos específicos. O backend cria ou reutiliza o acompanhamento canônico e
retorna status, modo de coleta, anúncios aderentes, anúncios ativos, anunciantes e
atualidade. A evidência Meta fica separada das ofertas comparáveis: anúncio ativo indica
presença e investimento aparente, nunca venda comprovada.

O mesmo plano pesquisa afeto e pertencimento, reconhecimento e alívio de esforço como territórios
de valor que precisam de evidência. A candidata deve entregar um resultado pronto com entrada
mínima, sem transferir prompting, configuração, montagem ou conhecimento de IA ao consumidor. O
gate privado observa `READY_RESULT_USED` antes de permitir priorização final.

## Variáveis

- `BACKEND_BASE_URL`: URL do backend principal. Padrão: `http://191.252.181.168`.
- `PRODUCT_DISCOVERY_POLL_INTERVAL_MS`: intervalo de polling. Padrão: `60000`.
- `PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS`: máximo total de resultados públicos usados no ciclo. Padrão: `30`.
- `PRODUCT_DISCOVERY_MIN_SEARCH_QUERIES`: mínimo de consultas diferentes por ciclo antes de encerrar a busca. Padrão: `10`.
- `PRODUCT_DISCOVERY_MAX_SEARCH_QUERIES`: teto de consultas diferentes por ciclo. Padrão: `24`.
- `PRODUCT_DISCOVERY_MAX_RESULTS_PER_QUERY`: máximo de resultados aproveitados por consulta, para evitar que uma única frase domine a evidência. Padrão: `5`.
- `PRODUCT_DISCOVERY_HEALTH_HOST`: host do servidor HTTP de health. Padrão: `0.0.0.0`.
- `PRODUCT_DISCOVERY_HEALTH_PORT`: porta interna do servidor HTTP de health. Padrão: `8080`.
- `PRODUCT_DISCOVERY_HEALTH_PUBLISHED_PORT`: porta publicada no host pelo Compose. Padrão: `18081`.
- `PRODUCT_DISCOVERY_HEALTH_BIND_ADDRESS`: endereço publicado para health e observabilidade. Padrão: `0.0.0.0`.
- `PRODUCT_DISCOVERY_SEARCH_PROVIDER`: provedor dedicado de busca. Aceita `brave`,
  `tavily`, `serpapi` ou `duckduckgo`. Quando vazio, o worker escolhe pela primeira
  chave disponível nesta ordem: Brave, Tavily, SerpAPI e DuckDuckGo.
- `BRAVE_SEARCH_API_KEY`: chave da Brave Search API.
- `BRAVE_SEARCH_API_KEY_FILE`: arquivo com a chave da Brave Search API. Use em
  produção para não expor segredo em variável direta.
- `TAVILY_API_KEY`: chave da Tavily Search API.
- `SERPAPI_API_KEY`: chave da SerpAPI.
- `PRODUCT_DISCOVERY_SEARCH_COUNTRY`: país usado na busca. Padrão: `br`.
- `PRODUCT_DISCOVERY_SEARCH_LANGUAGE`: idioma usado na busca. Padrão: `pt-br`.
- `ARGOS_CODEX_ENABLED`: habilita planejamento e síntese factual pelo Codex. Padrão operacional: `true`.
- `ARGOS_CODEX_MODEL`: modelo das duas fases. Padrão: `gpt-5.6-sol`.
- `ARGOS_CODEX_REASONING_EFFORT`: esforço registrado para auditoria. Padrão: `high`.
- `ARGOS_CODEX_TIMEOUT_MS`: timeout individual de cada fase. Padrão: `600000`.

## Deploy

O workflow `Product Discovery Worker CI` publica o container no host operacional de
workers `191.252.120.96`.

No deploy de produção, o provider padrão é Brave, com busca direcionada ao Brasil
(`PRODUCT_DISCOVERY_SEARCH_COUNTRY=br`, `PRODUCT_DISCOVERY_SEARCH_LANGUAGE=pt-br`).
A chave deve existir no servidor em:

```bash
/root/infra/brave-token/brave_api_key
```

O compose de produção monta esse arquivo como Docker secret em
`/run/secrets/brave_search_api_key` e o worker lê pelo
`BRAVE_SEARCH_API_KEY_FILE`.

## Health operacional

O worker expõe `GET /healthz` e `GET /health` na porta interna `8080`. Em
produção, o Compose publica o endpoint em `0.0.0.0:18081` para leitura pelo MCP.

O endpoint `GET /ops-product-discovery-observability-v1/logfile` expõe as linhas
operacionais recentes sem incluir chaves de API. Quando todas as consultas externas
falham, o ciclo falha e bloqueia a tarefa; resposta vazia não mascara o provider.

O payload informa o provider ativo, status da chave Brave sem revelar o segredo,
último polling e último ciclo processado:

```json
{
  "service": "product-discovery-worker",
  "status": "UP",
  "activeSearchProvider": "brave",
  "braveSearch": {
    "keyStatus": "CONFIGURED",
    "keySource": "file"
  },
  "lastCycleProcessed": null
}
```

## Provedor recomendado

Use Brave como primeiro provedor dedicado (`PRODUCT_DISCOVERY_SEARCH_PROVIDER=brave`)
porque entrega resultados web estruturados a partir de índice próprio e preserva melhor
o sinal bruto de dor, lacuna e concorrência. Tavily é útil quando a pesquisa precisar
de conteúdo mais pronto para agente. SerpAPI é útil quando a validação depender
especificamente do que aparece no Google.

DuckDuckGo fica apenas como fallback sem chave e não deve ser considerado evidência de
escala suficiente para decisões comerciais fortes.

## Execução local

```bash
PRODUCT_DISCOVERY_SEARCH_PROVIDER=brave \
BRAVE_SEARCH_API_KEY=... \
ARGOS_CODEX_ENABLED=true \
npm test
npm start
```
