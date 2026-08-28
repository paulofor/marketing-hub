# Homologação — ícones de processo e atividade v1

## Objetivo

Comprovar que o catálogo e o diagrama BPM distinguem visualmente processos, subprocessos e
atividades sem alterar os contratos oficiais, estados ou navegação vindos do backend.

## Decisão de implementação

| Alternativa                                             | Benefício                                        | Risco/custo                                      | Decisão    |
| ------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------ | ---------- |
| Adicionar ícones apenas ao processo 56                  | Mudança mínima                                   | A inconsistência reaparece em outros processos   | Descartada |
| Escolher ícones conforme o status                       | Destaca andamento                                | Confunde tipo da entidade com estado operacional | Descartada |
| Reutilizar um componente semântico por tipo de entidade | Consistência em todas as telas e novos processos | Pequena refatoração visual                       | Escolhida  |

O componente compartilhado usa `Workflow` para processo ou subprocesso e `ClipboardList` para
atividade. O tipo continua vindo de `processType` e dos nós `TASK` entregues pelo backend.

## Matriz ponta a ponta

| Área              | Cenário                                             | Critério de aprovação                                                                     |
| ----------------- | --------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| Caminho feliz     | Abrir `/business-processes?processId=56`            | Processo selecionado, catálogo, composição e diagrama permanecem legíveis                 |
| Processo          | Nome de processo ou subprocesso                     | Possui `Workflow`, texto visível e nenhum `ClipboardList`                                 |
| Atividade         | Cada nó `TASK`                                      | Possui `ClipboardList`, texto visível e nenhum `Workflow`                                 |
| Outros nós        | Início, decisão e fim                               | Não são apresentados como atividades                                                      |
| Navegação         | Abrir subprocesso e históricos                      | Links preservam os destinos oficiais                                                      |
| Validação e falha | API em carregamento ou erro                         | A tela mantém as mensagens existentes e não fabrica dados                                 |
| Integração        | Catálogo, composição, cadeia, recursos e documentos | Chamadas oficiais permanecem inalteradas                                                  |
| Observabilidade   | Erro de navegador e overflow                        | Zero erro JavaScript e zero overflow horizontal                                           |
| Métrica           | Cobertura visual                                    | 100% dos nomes de processo/atividade nas superfícies alteradas usam o componente canônico |
| Segregação        | Dados de homologação                                | Mocks locais não alteram processos, tarefas, campanhas, custos ou métricas reais          |
| Desktop           | Chromium 1440×1000                                  | Ícones, rótulos, links e conteúdo permanecem visíveis                                     |
| Mobile            | iPhone 15 Pro e Pixel 7                             | Conteúdo quebra linha sem sobreposição ou overflow                                        |

## Regra de repetição

Uma rodada local completa sem defeitos conclui a homologação. Se a rodada revelar defeito, após a
última correção serão executadas duas rodadas completas e consecutivas sem falhas.

## Resultado

Em 2026-08-28, a inspeção somente de leitura do ambiente operacional confirmou que o processo 56 e
seus quatro nós `TASK` eram entregues corretamente pelos endpoints oficiais, mas a tela não possuía
ícones de processo ou atividade. A causa-raiz era a implementação local dos ícones somente na visão
de atividades do produto, sem componente compartilhado com o catálogo BPM.

Após corrigir o harness de navegador antes da homologação completa, duas rodadas locais completas e
consecutivas terminaram sem falhas. Cada rodada aprovou:

- 422 testes do frontend em 129 arquivos, incluindo o componente compartilhado e as duas telas;
- tipagem TypeScript, build de produção e Prettier nos arquivos alterados;
- catálogo, processo selecionado, composição, subprocesso e quatro atividades do processo 56;
- a jornada do catálogo BPM e a jornada de atividades do produto em Chromium desktop, iPhone 15 Pro
  e Pixel 7, sem overflow horizontal ou erro JavaScript;
- distinção de 100% dos nomes avaliados: `Workflow` somente para processo/subprocesso e
  `ClipboardList` somente para atividade `TASK`; início, decisão e fim não foram reclassificados.

Os testes usaram interceptações HTTP e dados locais segregados. Nenhum processo, atividade, tarefa,
campanha, gasto, venda ou métrica produtiva foi criado ou alterado.
