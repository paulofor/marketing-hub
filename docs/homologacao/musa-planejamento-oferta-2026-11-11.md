# Reconciliação de oferta no planejamento — 11/11/2026

## Evidência e decisão

MUSA/Vega, produto 4, cadeia 27, ciclo 22, experimento 115, candidata
`musa-consultor-pwa-v1`: Atena #906 bloqueou corretamente uma entrada contraditória.
O cadastro e a Big Idea v3 definem aplicativo contínuo, base gratuita e premium
opcional com preço em aberto. O sucessor copiou `brief.offer`, `unitPrice=67`
e o vínculo da hipótese H003 do predecessor. O parecer custou US$ 1,754278 estimados;
isso não é fatura conciliada e deve permanecer no histórico.

Alternativas: reparo isolado no banco (não previne recorrência), ocultação da
contradição no prompt (perde autoridade) e reconciliação auditável na origem.
Adotada a terceira, com prevenção também na criação dos sucessores quando a
Big Idea fixada no predecessor difere da atual. Oferta estável continua preservada.

## Matriz de aceite definida antes da execução

| Cenário | Prova esperada |
| --- | --- |
| Caso original e outro produto com IDs diferentes | Oferta contínua, preço nulo, nova versão da hipótese ligada à origem, preço em aberto |
| Histórico anteriormente válido | Predecessor, tarefas, custos, recibos e oferta estável intactos |
| Replay e concorrência | Uma revisão e um recibo; chave divergente/revisão vencida recusadas |
| Identidade/Big Idea divergentes | Correção recusada sem escrita |
| Experimento exposto, ciclo encerrado, tarefa ativa ou estratégia aceita | Correção recusada; sucessor quando necessário |
| Contrato econômico ausente ou preço contraditório | Falha anterior à escrita e sem chamada paga |
| Integração HTTP e frontend | Prévia oficial → correção → ciclo atualizado; erro recuperável; nenhum orçamento enviado |
| Desktop, iPhone 15 Pro e Pixel 7 em Chromium | Oferta legível, botão acessível, bloqueio durante envio |
| Financeiro/observabilidade | Consumo e teto cumulativo inalterados; evento com antes/depois, autoria e origem |
| Segregação | Doubles locais sem modelos, mídia, vídeos, cobrança ou dados de teste em produção |

O alinhamento não reescreve `creation_json`, a hipótese H003 nem a tarefa #906;
conserva o briefing anterior no novo recibo e cria uma versão BACKLOG da hipótese pela linhagem canônica. A referência obrigatória do experimento continua preenchida; preço em aberto não se torna zero. Não produz aprovação estratégica.
Depois de publicado, o comando normal de retomada cria a nova análise somente
com saldo, identidade, STOP e revisão dos agentes conferidos.

## Melhoria reutilizável do harness

Capacidade: preparação de sucessores. Hipótese verificável: preservar a definição
registrada ao mudar direção evita análises pagas bloqueadas por oferta antiga.
Métrica local: sucessor coerente e ausência de efeitos financeiros/comerciais.
Métrica operacional posterior: recorrência desse bloqueio e custo por estratégia
aceita; sem evidência suficiente para atribuir impacto em vendas.
Adoção limitada ao planejamento privado, antes de trabalho aceito; fora desse
escopo, preservar história e exigir o fluxo canônico de sucessor. Reversão:
retirar a ação administrativa sem apagar recibos nem restaurar vínculo antigo.

## Resultados locais

- Regressão Java: 798 testes, zero falhas/erros, um caso de integração não aplicável
  sem seu banco específico; o contrato de alinhamento e as regressões financeiras executaram.
- Frontend: 30 testes aprovados, TypeScript e build aprovados.
- Navegação com o contrato Java real e repositories simulados: desktop, iPhone 15 Pro
  e Pixel 7 aprovados, da prévia até a revisão 2, sem erro de página ou chamada externa.
- Bash e ShellCheck do harness existente aprovados. Nenhum changelog foi alterado.

Reprodução da navegação: compilar testes e classpath do backend; iniciar
`LearningCyclePlanningAlignmentTest.main` (porta 18094); iniciar Vite com
`frontend/vite.planning-alignment.config.ts` (15174; pode configurar URL do browser);
rodar `frontend/e2e/planning-alignment-responsive.cjs`, com
`PLANNING_ALIGNMENT_UI_URL=http://127.0.0.1:15174`.
O endpoint `/__fixtures/reset` existe somente no runner de teste, fora do app produtivo.

A verificação ponta a ponta encontrou também o guard de repetição de Atena:
antes, ele só reconhecia tarefas predecessoras novas e barrava qualquer correção
da oferta. Agora reconhece somente um recibo de reconciliação compatível posterior
à tarefa recusada. Outra recusa criada depois desse recibo volta a bloquear a
mesma entrada. A chave do processo incorpora a definição reconciliada. Regressões
exercitam o guard real nos dois produtos e rejeitam recibo de outro experimento.

Rodada final após integrar o guard e a chave operacional: 485 testes relacionados,
zero falhas/erros e zero dispensas, incluindo arquitetura, reconciliação e continuação.

## Compatibilidade das atestações compartilhadas

O CI inicial recusou a atestação vigente Mira v82 porque oito fontes compartilhadas
mudaram. A solução conserva v82 e registra v83 como `EVIDENCE_COMPATIBILITY_ONLY`:
mesma experiência e provas visuais, sem renovar pareceres independentes ou aprovação
comercial. Os 105 testes locais de Mira, planejamento, reconciliação e passagem
passaram; os 17 testes de integridade do bundle também. O empacotador validou
808 arquivos e 209 manifestos. Esta compatibilidade não é nova homologação premium,
nem evidência de uso, satisfação ou venda.

A revisão final identificou que `experiment.hypothesis_id` é obrigatório: apenas
retirar a referência antiga falharia na gravação. O alinhamento e o sucessor agora
usam o versionamento canônico de hipóteses, com predecessor preservado, produto
vinculado e preço explicitamente em aberto somente no planejamento privado.
O contrato comercial de criação de versões continua exigindo preço positivo.
A regressão de persistência cobre o MUSA e outro produto, grava e recarrega o
experimento real, preserva o custo, verifica a linhagem e repete a mesma chave sem
criar outra versão. Isso corrige a lacuna dos doubles que aceitavam vínculo nulo.

Depois da correção do vínculo obrigatório: 187 testes de alinhamento, sucessor,
hipóteses com persistência H2 real, prontidão e arquitetura aprovados, sem dispensas.
A navegação desktop/iPhone/Pixel foi repetida com o contrato atualizado e passou.
