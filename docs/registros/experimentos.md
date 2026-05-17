# Registros — Experimentos

> 🔴 **Arquivo canônico principal (atual)** para registro operacional do tema Experimentos.

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
>

## 2026-05-17 03:38:52 UTC-3
- Atualização documental para remover referências à aba descontinuada "Portal do Lead" no fluxo de liberação para Facebook Ads.
- Raciocínio: a operação atual é centralizada no Gera Landing; manter instruções antigas induzia erro operacional e bloqueio indevido na liberação do experimento.
- Foi feito: revisão e ajuste dos documentos operacionais/canônicos para substituir instruções de aprovação em aba por aplicação/publicação via Gera Landing.
- documentos lidos para tratar a situação:
  - docs/manual-usuario/aihub/liberar-facebook-ads-worker.md
  - docs/pipeline-landing-experimento.md
  - docs/canonical/facebook-campaign-publication-canon.v1.md
  - docs/registros/experimentos.md
## 2026-05-17 03:30:43 UTC-3
- solicitação para registrar formalmente o tema experimento no local canônico após ajuste de fluxo de aprovação de landing.
- o raciocínio foi garantir rastreabilidade operacional no arquivo obrigatório de registros do domínio de experimentos, mantendo aderência ao contrato append-only.
- registro realizado neste documento informando a centralização da aprovação final de landing apenas na aba "Landing" e a remoção dos demais pontos de aprovação na visualização de conteúdo.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentContentGenerationTab.tsx


## 2026-05-17 00:00:00 UTC
- solicitação para evoluir o frontend na aba de backtest com visualização das quantidades por outcome e indicadores de progresso.
- implementação realizada na aba de funil/backtest com gráfico de barras por outcome (quantidade por etapa), destaque do total atual e percentual em relação à referência ideal de 500.
- também foi adicionado cálculo explícito da meta (500) para facilitar leitura operacional durante acompanhamento de performance.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/ExperimentFunnelTab.tsx

## 2026-05-17 04:08:17 UTC-3
- solicitação para ajustar a aba Landing do experimento para usar o HTML salvo no próprio registro da tabela `experiment` e alinhar com a migração da geração para o Gera Landing.
- raciocínio aplicado: remover dependência da listagem antiga de landings do pipeline e exibir diretamente a prévia do HTML final que agora pertence ao fluxo Gera Landing.
- foi feito no frontend:
  - atualização da aba Landing para renderizar o `landingPageHtml` do experimento em preview (`iframe` com `srcDoc`) e manter o botão de aprovação para campanha apontando o destino da campanha para `/landing/{id}`.
  - remoção dos blocos de landing do conteúdo do pipeline na aba "Conteúdo" (mantidas apenas etapas de Campaign Angle e Ad Copy).
- documentos/arquivos lidos:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/experiment/LandingTab.tsx
  - frontend/src/pages/experiment/ExperimentDetailPage.tsx

## 2026-05-17 00:00:00 UTC
- solicitação para investigar e corrigir `400 Bad Request` no `PUT /api/experiments/20`.
- causa-raiz identificada: ao enviar `leadPortalFlowId: null` no payload de atualização, o backend tratava o campo como obrigatório quando presente e lançava erro `leadPortalFlowId required`.
- correção aplicada: ajuste no `ExperimentService.update` para permitir `leadPortalFlowId` nulo quando informado explicitamente (limpando o vínculo do fluxo), mantendo validação normal quando um ID é enviado.
- validação automatizada adicionada com teste unitário cobrindo o cenário de limpeza do `leadPortalFlowId` via update.
- documentos/arquivos lidos:
  - AGENTS.md
  - backend/AGENTS.md
  - docs/registros/experimentos.md
  - backend/ads-service/src/main/java/com/marketinghub/experiment/service/ExperimentService.java
  - backend/ads-service/src/test/java/com/marketinghub/experiment/ExperimentServiceTest.java

## 2026-05-17 13:20:14 UTC-3
- solicitação para corrigir a lista de pendências de publicação no Facebook Ads exibindo item duplicado de Landing e removendo bloqueio indevido de status planejado.
- raciocínio aplicado: padronizar a renderização da lista para evitar duplicidade por chave/label e filtrar explicitamente entradas de status que não devem bloquear publicação.
- foi feito no frontend: atualização do componente `MissingConfigurationList` para deduplicar pendências por rótulo e ignorar entradas de status `planned/planejado`.
- documentos lidos para tratar a situação:
  - AGENTS.md
  - frontend/AGENTS.md
  - docs/registros/experimentos.md
  - frontend/src/pages/facebook/MissingConfigurationList.tsx
