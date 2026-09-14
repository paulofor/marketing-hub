# Prompt AIHUB: aperfeiçoamento de agentes com evidências

Data: 14/09/2026. Escopo: orientações compartilhadas pelo botão **Prompt para AIHUB**.

## Investigação e decisão

O prompt é um Markdown estático importado por `ProductProcessContextCopy.tsx`; a cópia
acrescenta o contexto oficial existente. Foram analisados o anexo de 21 páginas, seu
diagrama da página 20, os radares de 12 e 13/09, o cânone do catálogo e os registros
`processo-prompt-aihub-v1.md` e `prompt-aihub-correcao-sistemica-v1.md`.
O histórico confirma a composição compartilhada e a validação de cópia, prévia e falhas.
Não existe chamada de IA nem leitura automática das pesquisas no botão.

| Alternativa                                                         | Benefício                                                          | Risco / esforço                                                             | Decisão                                                                        |
| ------------------------------------------------------------------- | ------------------------------------------------------------------ | --------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Incluir todo o glossário do PDF em cada prompt                      | Conceitos disponíveis integralmente                                | Texto extenso, repetição e menor destaque ao impedimento; esforço baixo     | Útil como material de estudo, excessivo para o pedido operacional              |
| Instruções práticas no modelo comum e síntese canônica consultável  | Reutiliza o fluxo existente, mantém evidências e consulta seletiva | Exige conferir integridade da cópia e limites de autorização; esforço baixo | Escolhida: atende ao aperfeiçoamento orientado ao resultado                    |
| Criar recuperação automática das pesquisas e um serviço de evolução | Contexto selecionado e candidatas persistidas automaticamente      | Novos contratos, execução e avaliação de modelos; esforço alto              | Adequada a uma futura funcionalidade, além do pedido de orientação nos prompts |

As fontes OpenAI e a síntese dos conceitos ficam em
`docs/canonical/aihub-aperfeicoamento-agentes-canon.v1.md`.
O texto segue objetivo, instruções, critérios, fontes/contexto e entrega. A antiga
autorização de publicação manual foi substituída pela regra atual do usuário: validação
local completa e publicação via PR executado pelo usuário. O histórico anterior permanece.

A apresentação reutiliza as leituras de `businessprocess.execution`,
`businessprocess.automation.v1` e `businessprocesschain.learningcycle.v1`.
Não exige endpoint novo: não altera dados de negócio, estado, persistência ou Java.

## Matriz definida antes da validação

| Critério                   | Validação local                                                                                                                                            |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Caminho feliz e conteúdo   | Prompt completo na cópia, colagem e prévia; ciclo de melhoria e caminho de pesquisas presentes; um único contexto oficial                                  |
| Limites e fontes           | Publicação pelo PR, pesquisa como referência, falta de acesso explícita, causalidade e critérios de conclusão preservados; sem promessa de melhoria medida |
| Validações e falhas        | Carregamento, permissão negada, fallback HTTP, seleção manual, retentativa e falha de consulta                                                             |
| Integrações                | Build real com contratos HTTP sintéticos; nenhuma chamada de IA, início de processo ou conexão externa                                                     |
| Observabilidade e métricas | Confirmação somente após cópia; erro visível, logs, resultados, capturas e texto colado; nenhum custo ou evento comercial de teste                         |
| Segregação                 | IDs sintéticos; troca de produto/ciclo sem dados anteriores; ausência de prompts brutos e credenciais                                                      |
| Navegadores e dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, HTTP e contexto seguro; prévia sem transbordamento                                                     |
| Regressão                  | Suíte existente de contexto, prompt, painéis e cards; cópia de atividades; TypeScript, build, Prettier e revisão do diff                                   |

Rodada planejada: `bash infra/testing/process-context-copy/run-round.sh agents-20260914-1`.
Uma rodada completa sem defeitos conclui a homologação. Havendo defeito e correção,
executar duas rodadas completas consecutivas sem falhas após a última correção.

## Resultado

A primeira rodada completa passou sem defeitos. Não foi necessária outra rodada.

| Verificação                            | Resultado                                                                                                                         |
| -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Testes do frontend em 10 arquivos      | 107/107 aprovados                                                                                                                 |
| TypeScript e build                     | Aprovados                                                                                                                         |
| Cópia de contexto, prompt e atividades | 6/6 combinações em cada suíte; 18/18 no total                                                                                     |
| Integridade do prompt colado           | Texto do modelo integral, uma única vez, seguido de um único contexto oficial nas seis combinações                                |
| Fontes e autorizações no texto         | Caminho das pesquisas e síntese canônica presentes; nova regra de PR presente; antiga autorização excepcional ausente             |
| Falhas, segregação e observabilidade   | Fallback, seleção manual, retentativa, carregamento e troca de contexto aprovados; zero mutações ou conexões externas inesperadas |
| Formatação e revisão                   | Aprovadas; referências locais resolvidas                                                                                          |

Evidências em `artifacts/process-context-copy/agents-20260914-1/`: logs de frontend,
tipos e build; `results.json` em `browser/`, `aihub-browser/` e `activity-browser/`;
`prompt-integrity.json` com comprimento e hash dos seis textos efetivamente colados.
Os prompts completos têm 20.653 ou 20.717 caracteres, conforme a origem local da URL.
O modelo estático sozinho tem 11.925 caracteres após remover a quebra de linha final.

Foram inspecionadas visualmente `aihub-browser/desktop-http-card.png` e
`aihub-browser/iphone-http-preview.png`: confirmação legível, botões dentro do card e
prévia com quebra de linha, sem transbordamento horizontal. Os navegadores e servidores
temporários foram encerrados pelo runner; não foi necessária topologia Docker.

Limites: celulares emulados no Chromium, sem Safari físico. A validação comprova a entrega
do texto e a preservação do contexto; não mede cumprimento das instruções por um modelo,
melhoria real dos agentes ou aumento de vendas. Nenhuma chamada de IA, cobrança, comando
de processo ou alteração produtiva foi realizada. Permanecem apenas os avisos preexistentes
de API CJS do Vite e tamanho do bundle. Dependências instaladas pelo lockfile, sem alteração
de versões. Mudanças locais revisadas, sem commit, push, PR ou publicação.
