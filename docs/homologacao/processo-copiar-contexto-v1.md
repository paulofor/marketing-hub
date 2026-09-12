# Contexto completo do processo em um clique

Solicitação de 12/09/2026. Escopo: card de execução no cabeçalho das atividades do produto.

## Decisão

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Copiar apenas o título e link | Mínimo esforço | Omite ciclo, tarefas e motivo do bloqueio | Descartada |
| Novo endpoint para exportar todos os dados brutos | Centraliza exportação | Duplica contratos existentes, aumenta manutenção e expõe dados desnecessários | Descartada |
| Formatar os contratos oficiais já lidos pela tela | Um clique com identidades, progresso e contexto completo | Esforço baixo; exige preservar escopo e tratar ausências e falhas | Escolhida |

As APIs existentes de `activity-executions`, `automation/v1`, `process-context` e
`value-chain-positions/{productId}` já fornecem os dados. A leitura de atividades possui
controller, service e records canônicos em `businessprocess.execution`; a automação usa
`businessprocess.automation.v1` e o ciclo usa `businessprocesschain.learningcycle.v1`.
Esta ação de apresentação não precisa de novo endpoint, comando, worker ou persistência.

## Matriz de homologação definida antes dos testes

| Controle | Critério |
| --- | --- |
| Card | Botão nomeado e prévia expansível no card do processo, acessíveis por teclado e toque |
| Identidade | Produto interno/comercial, IDs, cadeia/versão, processo/número/versão/definição, referência e plano |
| Ciclos | Ciclo efetivo do backend, experimento, versão, hipótese, mudança e aprendizado dos ciclos anteriores; ausência explícita |
| Execução | Situação, atividade atual, responsável, contagens oficiais, pendências, custos/cobertura e horários disponíveis |
| Atividades | Todas as retornadas, com versão/definição, objetivo, responsável, situação, critérios, tarefas, erros e dependências; histórico identificado |
| Escopo | Links do processo e atividades mantêm produto/cadeia/ciclo; não herdam âncora ou parâmetros estranhos; outra cadeia não fornece numeração/versão |
| Integração | Build real e contratos simulados localmente; carregamento, erro de consulta e mudança de produto/ciclo |
| Área de transferência | Cópia real e colagem com acentos/quebras de linha, HTTP e contexto seguro, permissão negada, seleção manual e nova tentativa |
| Observabilidade | Confirmação somente após sucesso, erro visível e foco preservado; dados desatualizados identificados |
| Isolamento / métricas | Nenhuma escrita, execução de agente ou requisição externa nos testes; nenhuma venda, custo ou evento comercial fictício |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação; sem overflow do card |
| Regressão | Testes pertinentes de tela/cópia/acompanhamento, TypeScript, build, formatação e revisão do diff |

Uma rodada completa sem defeitos conclui a homologação. Se houver defeito e correção,
exigir duas rodadas completas consecutivas sem falhas após a última correção.

## Evidências da investigação e ajustes

A página pública do Vega, processo 63, cadeia 14, ciclo 2 foi aberta somente para leitura.
Os quatro contratos responderam HTTP 200. O card exibiu acompanhamento e comandos da
execução, confirmando que as informações já estavam disponíveis para apresentação.
Captura inicial local: `artifacts/process-context-copy/public-before.png`.

O botão fica dentro de **Execução do processo** e oferece **Ver contexto completo**.
Processo e atividades compartilham o mesmo controle de área de transferência, incluindo
fallback HTTP, confirmação, seleção manual e restauração de foco. Os textos de situação
são compartilhados com a tela. A lista explícita de campos não exporta prompts brutos,
payloads de evidência ou tokens de confirmação. Custos desconhecidos permanecem desconhecidos.

A primeira verificação encontrou três falhas porque a prévia fechada já montava o texto
completo, duplicando conteúdo consultado nos testes da página. A prévia passou a montar
seu conteúdo somente quando aberta. Os testes originais da tela foram preservados.
O primeiro roteiro de navegador também revelou uma fixture incompleta: faltava a lista
obrigatória `helpLinks` no bloqueio de tarefa. A fixture foi alinhada ao contrato; não foi
necessário alterar o componente produtivo que lê esse campo.

## Resultado local

Runner: `bash infra/testing/process-context-copy/run-round.sh <rodada>`.

Cada rodada cobre os testes de contexto, tela, painel automático, painel de atividade,
acompanhamento e consulta; TypeScript; build; clipboard real do processo em seis combinações;
regressão do clipboard de atividade nas mesmas seis combinações; Prettier e `git diff --check`.

As APIs são simuladas conforme contratos do backend, com dados sintéticos, e o navegador usa
o build real. O roteiro recusa requisições externas e métodos de escrita, verifica zero
comandos e grava screenshots, o texto efetivamente colado, logs e `results.json` em
`artifacts/process-context-copy/<rodada>/`. Nenhum modelo, worker, e-mail, campanha ou evento
comercial é acionado. Os servidores e navegadores locais são encerrados pelo roteiro.
Não há alteração Java, Liquibase ou necessidade de topologia Docker para esta apresentação.

Os celulares são emulações de Chromium, sem validação em Safari físico. O build mantém
avisos anteriores sobre tamanho do bundle e API CJS do Vite. Dependências locais foram
instaladas pelo lockfile (`npm ci`), sem alteração de versões.

As duas rodadas completas e consecutivas passaram após os ajustes:

| Verificação | final1 | final2 |
| --- | --- | --- |
| Testes pertinentes do frontend | 61 aprovados | 61 aprovados |
| TypeScript e build | Aprovados | Aprovados |
| Clipboard real do processo: desktop, iPhone e Pixel; HTTP e contexto seguro | 6 combinações aprovadas | 6 combinações aprovadas |
| Regressão do clipboard das atividades | 6 combinações aprovadas | 6 combinações aprovadas |
| Formatação e revisão do diff | Aprovadas | Aprovadas |
| Escritas / comandos de produto nos navegadores | 0 | 0 |

Evidências: `artifacts/process-context-copy/final1/` e
`artifacts/process-context-copy/final2/`. A inspeção visual confirmou o botão no card,
prévia com quebra de linhas, confirmação e seleção manual, com alvo de toque no celular.
Os resultados dos dois roteiros de navegador foram conferidos nos respectivos
`results.json`, incluindo todos os perfis e zero comandos.

Alterações concluídas e validadas na sandbox. Sem commit, PR ou deploy.
