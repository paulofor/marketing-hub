# Prompt AIHUB: compatibilidade com o tipo de produto

Data: 15/09/2026. Escopo: orientação do botão **Prompt para AIHUB** em todos os processos de produto.

## Evidência e decisão

`ProductProcessContextCopy.tsx` importa `prompts/process-aihub-help.v1.md` e acrescenta a
consulta oficial do processo. Cópia, prévia e seleção manual compartilham esse conteúdo.
A versão anterior já consultava o tipo para escolher o formato do PDE; faltava explicitar
a compatibilidade obrigatória durante todo o trabalho com o produto.

| Alternativa                                 | Benefício                              | Risco                                       | Esforço e aderência                        |
| ------------------------------------------- | -------------------------------------- | ------------------------------------------- | ------------------------------------------ |
| Registrar somente no cânone                 | Centraliza a regra                     | A orientação copiada continua implícita     | Baixo; atende parcialmente                 |
| Atualizar o prompt compartilhado e o cânone | Todos os pedidos recebem a mesma regra | Exige preservar particularidades e contexto | Baixo; escolhida                           |
| Manter um prompt por tipo                   | Permite detalhamento por categoria     | Duplica regras e facilita divergências      | Médio; desnecessário para esta regra comum |

A regra distingue diretrizes macro de tipo e especialização pela ficha versionada do produto,
conforme `product-types-canon.v1.md` e `product-execution-profiles-canon.v1.md`. Inclui
processos, atividades, agentes, oferta, funil, entrega, métricas e análise financeira com Plutus.
Divergências exigem correção com preservação dos contratos e decisões explícitas quando houver
mudança de produto. A avaliação econômica do tipo não aprova todos os seus produtos.

Os contratos existentes de `businessprocess.execution`, `businessprocess.automation.v1` e
`businessprocesschain.learningcycle.v1` continuam fornecendo o contexto. Trata-se de uma mudança
de texto estático: não exige novo endpoint, persistência de negócio nem chamada de modelo.

## Matriz definida antes da validação

| Critério                   | Validação local                                                                                        |
| -------------------------- | ------------------------------------------------------------------------------------------------------ |
| Caminho feliz              | Cópia, prévia e alternativa manual entregam o prompt integral com a nova seção                         |
| Validações e falhas        | Carregamento, clique duplicado, permissão negada, falha da consulta e retentativa                      |
| Integração e isolamento    | Build real, APIs simuladas e troca de produto/ciclo sem contexto residual                              |
| Observabilidade e métricas | Confirmação de cópia e erro visível; logs e texto colado, sem evento comercial ou chamada paga         |
| Navegação                  | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, HTTP e contexto seguro                             |
| Regressão                  | Suíte existente de contexto, prompt, painéis e cards, TypeScript, build e revisão do diff              |
| Conteúdo                   | Regra completa antes do contexto oficial, preservação do restante do modelo e coerência com os cânones |

Rodada: `bash infra/testing/process-context-copy/run-round.sh compatibilidade-tipo-20260915-1`.
Uma rodada sem defeitos conclui a validação; se houver defeito e correção, duas rodadas
completas e consecutivas sem falhas após a última correção.

## Resultado

A primeira rodada completa passou sem defeitos. Não foi necessário repetir a matriz.

- **109/109 testes** aprovados em 10 arquivos do frontend; suítes existentes revisadas e
  executadas, incluindo preservação do contexto, isolamento, prévia e seleção manual.
- **TypeScript e build Vite aprovados**, assim como formatação dos arquivos verificados pelo
  runner e revisão do diff. Permanecem avisos de dependências e tamanho do bundle.
- **18/18 combinações de navegação aprovadas:** seis para contexto, seis para prompt AIHUB e
  seis para cópia de atividades, em desktop, iPhone e Pixel emulados, HTTP e contexto seguro.
- As seis cópias do prompt contêm o modelo completo, a seção nova uma única vez antes do
  contexto oficial e a identificação da execução. Cópia, prévia e alternativa manual mantêm
  o mesmo texto; falhas e troca de produto/ciclo foram validadas pelos cenários existentes.
- Comparação com `HEAD`: remover somente a seção acrescentada recupera integralmente o
  template anterior. A diferença é a nova orientação transversal, sem alteração das demais
  instruções ou inclusão de exceções por produto, ciclo ou experimento.

Evidências locais em `artifacts/process-context-copy/compatibilidade-tipo-20260915-1/`:
`frontend.log`, `typecheck.log`, `build.log`, `format.log`, `diff.log`, os três arquivos
`results.json`, capturas e textos colados nas pastas de navegador. `template-integrity.json`
registra a preservação da versão anterior; `prompt-integrity.json` registra a integridade
das seis cópias. Inspeção visual dos cards `desktop-http-card.png` e `iphone-http-card.png`
da pasta `aihub-browser`: comando e confirmação de cópia legíveis.

Os testes usam contratos sintéticos locais; nenhuma mutação comercial ou chamada paga foi
realizada. Servidores e navegadores temporários foram encerrados pelo runner. Os celulares
foram emulados no Chromium; não houve teste em Safari físico. A validação comprova a entrega
da orientação, não o cumprimento futuro por um modelo, a compatibilidade já auditada de
todos os produtos ou aumento de vendas. Nenhum agente/worker foi alterado.

Mudança concluída na worktree, sem commit, push, PR ou publicação. O prompt publicado
receberá a nova regra quando o frontend passar pelo fluxo de publicação do repositório.
