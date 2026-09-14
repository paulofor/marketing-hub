# Plutus: rentabilidade durante todo o uso do produto

Data: 14/09/2026. Base recebida: `1e0aba8f8`. Solicitação: tornar Plutus mais ativo nos
processos para evitar que custos de IA consumam a receita e produzam vendas deficitárias.

## Diagnóstico e escopo

A leitura do código confirmou que `PdeEconomicsBpmTaskConsumer` já compõe o núcleo financeiro
com a atividade econômica v4/v5, valida contribuição e reporta ao backend por callbacks
oficiais. `FinancialCodexRunner` usa esse núcleo em premissas, projeções e conciliação; vídeo
possui prompt próprio. Esses caminhos já preservam autoridade somente leitura e auditoria.

As orientações existentes cobriam margem, envelope de produção, ledger e fontes, mas não
explicitavam de forma transversal todo o consumo posterior de um cliente, uso intenso,
franquias, custo por resultado aproveitável e distinção entre conciliação correta e produto
rentável. A revisão atua nesses prompts e no pedido compartilhado pelo botão AIHUB; preserva
schemas, decisões, rotas e fórmulas existentes. Não houve diagnóstico de prejuízo de um produto
específico, nem consulta/mutação de dados produtivos ou recuperação de Vega/Mira/Quartzo.

Fontes internas consultadas:

- `financial-agent-worker/src/main/java/com/marketinghub/financialagentworker/PdeEconomicsBpmTaskConsumer.java`:
  seleção de contrato por versão, núcleo + atividade, validação e callbacks.
- `financial-agent-worker/src/main/java/com/marketinghub/financialagentworker/FinancialCodexRunner.java`:
  composição efetivamente enviada ao modelo e auditoria de prompts.
- `frontend/src/pages/product/ProductProcessContextCopy.tsx`: template compartilhado e contexto oficial.
- `docs/canonical/financial-agent-canon.v1.md`, `matriz-responsabilidades-agentes-canon.v1.md`,
  `cadeia-produtos-pde-canon.v1.md` e `plutus-model-provider-pricing-canon.v1.md`.
- `docs/registros/loops.md`: `LOOP-STUDIO-COST-ATTRIBUTION`, `LOOP-COST-MODEL-AUDIT`,
  `LOOP-EXPERIMENT-COST-RECONCILIATION` e histórico de avaliações repetidas sem dados novos.
- `docs/homologacao/aquisicao-problema-compartilhado-v1.md` e
  `prompt-aihub-formatos-pde-vendas-v1.md`: handoffs existentes e homologação do prompt copiado.

## Alternativas e decisão

| Alternativa | Benefício | Risco/custo | Aderência |
| --- | --- | --- | --- |
| Aumentar frequência e chamar Plutus em toda atividade/uso | Mais verificações | Consome IA mesmo com entradas iguais; parecer não substitui limite determinístico | Baixa para reduzir custo sem desperdício |
| Reforçar Plutus e seus handoffs nos contratos existentes | Cobre oferta, construção, produção e operação, com reavaliação por mudança/desvio | Exige comprovar controles no produto; prompt não cria trava de venda | Escolhida: esforço limitado e orientação reutilizável |
| Criar novo subprocesso financeiro e motor universal de bloqueio | Permite centralizar novos controles | Exige contratos e semântica por produto, cobrança e entrega já contratadas; amplia muito o escopo | Evolução possível quando requisitos específicos estiverem comprovados |

A segunda alternativa amplia as instruções de Plutus, Atena, Dédalo, Íris e Hermes sem trocar
responsáveis, aumentar polling ou criar avaliações pagas repetidas. A política exige margem
e limites propostos/aprovados por produto, não um percentual arbitrário. Distingue cenário
privado e investimento de descoberta de venda com entrega deficitária. Não permite cancelar
entregas vendidas, mudar preço ou cobrar excedentes por decisão do modelo.

## Matriz definida antes dos testes

| Critério | Validação local | Aceite |
| --- | --- | --- |
| Caminho feliz | Suítes dos cinco workers, com runners/recursos reais e modelos simulados | Prompts atuais empacotados, contexto e callback preservados |
| Validações e falhas | Contratos existentes de economia privada/legada, contribuição, prazo, orçamento, falhas e retomadas | Rejeições e gates anteriores continuam vigentes |
| Integrações | Callbacks HTTP locais, composição/auditoria de prompt e pacote JAR | Sem nova rota, schema ou efeito externo; recursos do JAR iguais aos arquivos |
| Prompt do AIHUB | Runner `infra/testing/process-context-copy/run-round.sh` | Cópia, prévia e fallback entregam template completo seguido do contexto correto |
| Observabilidade | Logs e relatórios de testes, captura de prompt, auditoria simulada e screenshots | Sem custo inventado, sem afirmar execução real de IA |
| Métricas e economia | Revisão das instruções frente aos schemas e ao cânone | Uso total, retries, CAC, margem, falta de fonte, moeda, teste privado e entrega vendida sem contradição |
| Segregação | IDs sintéticos, HTTP local e test doubles | Nenhum cliente real, chamada paga, envio de mensagem ou dado comercial alterado |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, HTTP/contexto seguro | Sem corte do prompt; contexto não vaza entre produtos/ciclos |
| Regressão e entrega | TypeScript, build Vite, recursos, placeholders, revisão do diff | Mudanças locais prontas, sem commit, PR, deploy ou publicação |

Executar uma rodada completa. Somente defeito revelado na rodada e corrigido exige duas rodadas
completas consecutivas depois da última correção. Nenhuma topologia Docker é necessária para
esta revisão de instruções, sem migração ou contrato de persistência alterado.

## Limites e validação comercial futura

Esta entrega altera instruções efetivamente carregadas pelos workers e pelo frontend; não
implanta nova etapa BPM, cobrança, alerta ou bloqueio automático transversal. A operação ainda
depende da integração/publicação e das travas já implementadas em cada produto. A homologação
financeira do produto deve comprovar esses controles antes de sua ativação. Os testes locais
não medem obediência de um modelo real, lucro, vendas ou margem de clientes.

Hipótese de melhoria: pacote com franquia de imagens e regenerações claras pode preservar
valor e margem em Quartzo. Impacto potencial alto, esforço dependente do produto; medir
contribuição por cliente/pacote, custo por imagem aproveitável, uso intenso, conversão,
reembolsos e satisfação. Não há resultado comercial medido nesta solicitação.

## Resultados

Matriz concluída sem falhas. Foram executados **303 testes aprovados** (contagem única):

| Módulo | Aprovados | Falhas/erros | Ignorados |
| --- | ---: | ---: | ---: |
| Plutus | 38 | 0 | 0 |
| Atena | 33 | 0 | 0 |
| Dédalo | 62 | 0 | 0 |
| Íris | 29 | 0 | 2 |
| Hermes | 34 | 0 | 0 |
| Frontend | 107 | 0 | 0 |
| Total | 303 | 0 | 2 |

Executado `mvn -B -ntp -f <modulo>/pom.xml package` nos cinco workers. Os JARs foram produzidos;
os **11 prompts alterados dos workers** foram comparados byte a byte com os arquivos empacotados.
A conferência dos **12 prompts alterados**, incluindo AIHUB, preservou todos os placeholders.
Nenhum schema, classe Java, endpoint, cron ou changelog mudou. Não houve falha que exigisse
duas rodadas; a verificação de Plutus foi repetida após um ajuste de espaçamento no Markdown,
sem somar os mesmos testes novamente.

Os dois ignorados são integrações opcionais de Íris: entrada de ciclo real de Vega
(`VEGA_IRIS_INPUT_FILE`) e replay de imagem aprovada (`creative.replay.source/spec/output`).
Não foram fornecidos nesta revisão de orientações. Não são evidência de homologação de Vega,
geração de criativo ou teste financeiro de clientes. Os contratos ativos de contexto, composição,
callbacks, falhas e auditoria passaram com dependências simuladas.

O runner `infra/testing/process-context-copy/run-round.sh plutus-rentabilidade-1` passou em
testes, TypeScript, build Vite, formatação e diff. As três suítes de navegador passaram em
**18/18 combinações**: contexto, prompt AIHUB e cópia de atividade, cada uma em desktop,
iPhone 15 Pro e Pixel 7 emulados, HTTP e contexto seguro. Conferidos clipboard real, prévia,
falha de permissão, cópia manual, retentativa, carregamento e troca de produto/ciclo.

As seis cópias do AIHUB contêm integralmente o template de 16.568 caracteres, uma única seção
financeira e um único contexto oficial. Não houve mutação nem conexão externa inesperada.
Inspeção visual de `aihub-browser/desktop-http-card.png` e `iphone-http-preview.png`: comandos
legíveis, confirmação e quebra de linhas dentro do card, sem transbordamento horizontal.
Mobile foi emulado em Chromium, não validado em Safari/dispositivo físico.

Evidências locais da sessão:

- [Testes dos workers e hashes dos recursos](../../artifacts/plutus-rentabilidade/round1/worker-summary.json).
- [Placeholders e integridade dos contratos](../../artifacts/plutus-rentabilidade/round1/prompt-contracts.json).
- [Integridade das seis cópias integrais](../../artifacts/plutus-rentabilidade/round1/clipboard-integrity.json).
- [Rodada do frontend](../../artifacts/plutus-rentabilidade/round1/frontend-round.log).
- [Navegação do prompt](../../artifacts/process-context-copy/plutus-rentabilidade-1/aihub-browser/results.json).
- [Regressão da cópia de atividades](../../artifacts/process-context-copy/plutus-rentabilidade-1/activity-browser/results.json).

As instruções foram revisadas frente às fórmulas e campos existentes: custo de entrega completo,
moeda/período, dupla contagem, falta de receita, contribuição após CAC, política de margem,
uso intenso, limites, experimentação privada, autoridade e obrigações já vendidas. Essa revisão
não é uma avaliação paga do comportamento de um modelo nem comprovação de margem real.
Permanecem avisos preexistentes de dependências/API CJS do Vite e tamanho do bundle; não foram
alteradas versões de pacotes. Servidores e navegadores temporários foram encerrados pelo runner.
Alterações somente na sandbox, sem commit, push, PR, deploy ou publicação.
