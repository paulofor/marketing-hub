## Contexto

O Processo 5 de Mira não reconhecia o plano financeiro LIVE aprovado porque o contexto Safira comparava a versão pública do runtime `mira-commercial-v1` com a versão canônica do contrato econômico `MIRA_COMMERCIAL_V1`.

## Correção

- seleciona o plano financeiro pela `validationDefinitionVersion` do produto;
- preserva a validação do slot pela versão pública do runtime;
- adiciona regressão com as duas identidades deliberadamente distintas;
- registra a recorrência e amplia a matriz de homologação Safira.

## Validação local

- `./mvnw -Dtest=SafiraCommercialContextTest,SafiraCommercialChecksTest test` — 10 testes verdes;
- `./mvnw -Dtest='SafiraCommercial*Test' test` — 16 testes verdes;
- `./mvnw spotless:check` — verde;
- `./mvnw test` — 3.680 testes, 0 falhas, 0 erros.
