# Scientific Research Worker

Executor Spring Boot responsável por transformar ideias de produto do Marketing Hub em base científica auditável e entregáveis simples de entender.

Fluxo v1:

1. `source-discovery`: busca artigos e revisões em PubMed/Crossref a partir da hipótese do produto.
2. `evidence-synthesis`: usa IA com prompt/schema versionados para separar evidência real, limites e risco de alegação falsa.
3. `deliverable-composer`: gera entregáveis explicativos para o produto, mantendo fontes, cautelas e linguagem permitida.

O worker não acessa banco de dados. A entrada operacional deve vir do backend pelo endpoint canônico:

```text
/api/internal/scientific-research/product-evidence/v1/<stage>/stage-executions/pending
```

Resultados e artefatos são enviados de volta para o backend pelo callback interno configurado para cada etapa.
