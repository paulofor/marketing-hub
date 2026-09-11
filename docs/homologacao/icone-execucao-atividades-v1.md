# Ícone de execução nas atividades

Data: 11/09/2026. Escopo: apresentação do estado das atividades no frontend administrativo.

## Diagnóstico e escolha

A captura do usuário aponta o indicador de execução da atividade 3.7 do Vega. O componente
`ActivityStateIcon` já escolhe `Loader2` para `IN_PROGRESS`, porém não aplicava animação.
O mesmo componente atende os cards e o resumo de atividades. O endpoint existente
`GET /api/business-processes/{processDefinitionId}/products/{productId}/activity-executions`
expõe `operationalState`; não é necessário alterar contrato, persistência ou executor.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Animar o ícone existente com CSS | Preserva o desenho e o texto, sem dependência adicional | Baixo; limitar ao estado ativo e respeitar movimento reduzido | Escolhida |
| Substituir pelo spinner do Bootstrap | Reutiliza componente visual já instalado | Baixo; muda o desenho do indicador | Viável, sem vantagem para este pedido |
| Controlar rotação por JavaScript | Permite controles de animação adicionais | Maior manutenção e ciclo de vida desnecessário | Dispensada |

Os links diretos da próxima atividade já estão na revisão base `8be37934`; sua navegação será
verificada como regressão. Esta alteração não cria tarefas, muda status ou executa atividades.

## Matriz definida antes da validação

| Critério | Verificação local |
| --- | --- |
| Execução | Ícone gira continuamente com `IN_PROGRESS` no card e no resumo; texto estático |
| Outros estados | Pendente, aguardando, não iniciada, concluída, bloqueada, cancelada e histórica sem rotação |
| Atualização | Ao receber conclusão ou bloqueio pelo acompanhamento existente, remove a rotação sem recarregar |
| Acessibilidade | Movimento reduzido desativa rotação e mantém o rótulo; ícone decorativo continua oculto ao leitor de tela |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, sem overflow |
| Integração e observabilidade | Frontend real com respostas HTTP locais, capturas e erros de navegador; nenhum acesso externo nos cenários locais |
| Isolamento e métricas | Dados sintéticos; nenhuma tarefa, escrita, evento de conversão ou cobrança em produção |
| Regressão | Testes existentes da tela, acompanhamento e links de próxima atividade, TypeScript, build e revisão do diff |

Uma rodada completa sem defeitos conclui a validação. Se houver correção após falha, executar
duas rodadas completas consecutivas após a última correção.

## Resultado

Na primeira validação, os 43 testes chegaram a passar, mas surgiu um erro assíncrono após
encerramento da suíte da página: a limpeza acontecia somente antes do teste seguinte, deixando
a última tela montada durante a restauração dos mocks. A limpeza foi movida para `afterEach`,
incluindo o último teste. A correção é do isolamento da suíte, sem mascarar falhas da aplicação.
As expectativas de quatro rótulos no roteiro local também foram alinhadas aos textos canônicos
existentes, sem mudar o produto.

**Matriz concluída em duas rodadas locais, `final1` e `final2`, após a correção.**

| Controle por rodada | Resultado |
| --- | --- |
| Testes existentes de tela, painel, acompanhamento e link | 43/43 |
| Animação, atualização de estados e movimento reduzido em três dispositivos | 36/36 |
| Navegação dos cards até a próxima atividade, contextos e recuperação | 15/15 |
| TypeScript, build, Prettier e revisão do diff | Aprovados |
| Erros JavaScript ou requisições inesperadas nos cenários de navegador | Zero |

Na segunda rodada, as sessões de duas ferramentas retornaram código 143. Esses retornos não
foram aceitos como aprovação: os comandos de testes e TypeScript foram repetidos sem mudança
de código e encerraram com código zero; os logs definitivos são `tests-confirmed.log` e
`typecheck-confirmed.log`. Os bundles JavaScript e CSS das duas rodadas têm hashes idênticos.

A rotação foi confirmada pela mudança de transformação entre frames no navegador. Conclusão,
bloqueio e demais estados foram recebidos pelo acompanhamento HTTP existente, sem recarregar.
Os rótulos permaneceram estáticos, e a preferência de movimento reduzido parou a animação.
Os percursos dos links preservaram produto, cadeia, ciclo e a âncora da atividade.

Evidências locais: `artifacts/activity-running-icon/final1/` e `final2/`; capturas de referência
em `final2/browser/desktop-running.png`, `iphone-running.png` e `pixel-running.png`. A consulta
pública somente de leitura encontrou o card já bloqueado, com ícone estático; essa consulta
não foi usada como prova de execução ativa. O cenário em execução foi validado na sandbox.

Limites: APIs simuladas localmente e celulares emulados no Chromium, sem Safari ou aparelho
físico. O build preserva o aviso preexistente de tamanho do bundle. Navegadores e servidores
temporários foram encerrados; nenhuma topologia Docker foi necessária.

Entrega local em frontend e documentação, sem alteração de backend, banco ou contrato de API.
Nenhum commit, PR, deploy, tarefa produtiva ou publicação foi executado.
