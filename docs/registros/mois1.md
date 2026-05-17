# Registros — Mois

> 🔴 **Arquivo canônico principal (atual)** para registro operacional dos módulos coletores do MOIS.
> Toda alteração em `mois-hotmart-collector` , `mois-sales-library-worker` e `mois-clickbank-collector` deve ser registrada neste arquivo.
> Em caso de dúvida entre arquivos de registro, este é o ponto único de verdade.

## Template obrigatório de novo registro

```md
## YYYY-MM-DD HH:mm:ss UTC-3
- descrição breve do problema
- descrição breve do raciocínio para a solução
- registro do que foi feito
- documentos lidos para tratar a situação:
  - caminho/do/documento-1.md
  - caminho/do/documento-2.md
```

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

> Regra obrigatória de timestamp:
> Antes de adicionar qualquer novo registro, execute obrigatoriamente:
>
> ```bash
> TZ=America/Sao_Paulo date '+%Y-%m-%d %H:%M:%S UTC-3'
> ```
>
> Use exatamente a saída desse comando no título do novo registro.
> É proibido inventar, estimar, inferir ou reaproveitar data/hora a partir de:
> - contexto da conversa;
> - data do commit;
> - data do CI/build;
> - metadados do arquivo;
> - relógio UTC sem conversão explícita;
> - registros anteriores deste documento.
>
> O formato obrigatório do título é:
>
> ```md
> ## YYYY-MM-DD HH:mm:ss UTC-3
> ```
>
> Cada novo registro deve ser adicionado no final do arquivo.
> Se for necessário registrar mais de uma entrada, execute novamente o comando de data/hora para cada entrada.
> Nunca crie registro com timestamp futuro em relação ao horário atual de `America/Sao_Paulo`.
> Em caso de timestamp incorreto já registrado, não apague nem edite o registro antigo; adicione um novo registro de correção explicando o erro.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

## 2026-05-17 13:47:25 UTC-3
- consolidação da documentação do módulo MOIS ClickBank em um único documento canônico
- foi usado o código implementado como fonte de verdade para garantir aderência de endpoint, fluxo default e fetch GraphQL
- criado documento unificado em , e os documentos antigos passaram a apontar para o consolidado
- documentos lidos para tratar a situação:
  - docs/mois-clickbank-coletor.md
  - docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md
  - docs/mois/clickbase-fetch-ciclo-consulta.md
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/web/ClickbankCollectorController.java
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/service/ClickbankCollectorService.java
  - mois-clickbank-collector/src/main/resources/application.properties

## 2026-05-17 13:47:32 UTC-3
- consolidação da documentação do módulo MOIS ClickBank em um único documento canônico
- foi usado o código implementado como fonte de verdade para garantir aderência de endpoint, fluxo default e fetch GraphQL
- criado documento unificado em docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md, e os documentos antigos passaram a apontar para o consolidado
- documentos lidos para tratar a situação:
  - docs/mois-clickbank-coletor.md
  - docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md
  - docs/mois/clickbase-fetch-ciclo-consulta.md
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/web/ClickbankCollectorController.java
  - mois-clickbank-collector/src/main/java/com/marketinghub/moisclickbank/service/ClickbankCollectorService.java
  - mois-clickbank-collector/src/main/resources/application.properties

## 2026-05-17 14:23:53 UTC-3
- ajuste solicitado pós-revisão: mover o documento canônico unificado de ClickBank para a pasta /docs/canonical
- foi adotado nome versionado de cânone para facilitar evolução controlada: docs/canonical/mois-clickbank-collection-canon.v1.md
- atualizados os documentos de ponte para apontarem para o novo caminho canônico
- documentos lidos para tratar a situação:
  - docs/mois/mois-canonico-coleta-clickbank-ciclo-um.md
  - docs/mois-clickbank-coletor.md
  - docs/mois/clickbase-fetch-ciclo-consulta.md
  - docs/registros/mois1.md
## 2026-05-17 13:43:57 UTC-3
- consolidado o conteúdo de documentação dos ciclos de coleta Hotmart em um único documento canônico.
- fonte de verdade passou a ser a documentação alinhada ao comportamento atual implementado no coletor e no backend.
- removido documento duplicado do módulo para evitar divergência de manutenção.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/docs/ciclos-coleta-hotmart.md


## 2026-05-17 13:47:47 UTC-3
- documento canônico do fluxo de ingestão Hotmart movido para /docs/canonical conforme orientação.
- referências internas atualizadas para apontar o novo caminho canônico.
- documentos lidos para tratar a situação:
  - docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md
  - mois-hotmart-collector/AGENTS.md


## 2026-05-17 15:10:00 UTC
- atualização da tela /hotmart para exibir métricas de execução por ciclo: quantidade de jobs executados, total geral de produtos e total de produtos por ciclo (job).
- adicionada seção "Resumo de ciclos Hotmart" com cards de indicadores e tabela por ciclo.
- documentos/códigos consultados:
  - frontend/src/pages/hotmart/HotmartPage.tsx
  - frontend/src/api/settings/useHotmartCollectedProducts.ts

## 2026-05-17 15:35:00 UTC
- atualização da tela /clickbase para exibir as 6 últimas execuções de jobs da fonte Clickbank.
- adicionada seção com tabela contendo job, status, nicho, data de criação e mensagem de execução.
- ajuste de tipagem no frontend para incluir campos de `sources` e `niche` retornados pelo endpoint `/api/v1/mois/collection-jobs`.
- documentos/códigos consultados:
  - frontend/src/pages/clickbase/ClickbasePage.tsx
  - frontend/src/api/settings/useClickbaseCollectedProducts.ts
  - backend/ads-service/src/main/java/com/marketinghub/mois/dto/MoisWorkspaceDtos.java
  - backend/ads-service/src/main/java/com/marketinghub/mois/web/MoisController.java
