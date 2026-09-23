# Executar atividades do Backend e do Operador humano

Abra o produto, entre em **Histórico da cadeia de valor**, selecione o processo e abra **Atividades e
tarefas**. Cada atividade possui a seção **Como executar**, cuja situação vem diretamente do backend.

## Tipos de execução

- **Agente:** use **Executar atividade** ou **Reiniciar tarefa**.
- **Backend:** execute o comando exibido ou preencha o workspace incorporado.
- **Subprocesso:** use **Abrir subprocesso** e conclua o fluxo oficial indicado.
- **Aprovação humana:** confira todos os pré-requisitos, escolha aprovar ou reprovar, informe seu nome,
  justificativa e evidência, marque a confirmação e registre a decisão.
- **Automática:** aguarde ou conclua a origem indicada. Não existe botão quando uma execução manual
  poderia fabricar um resultado sem evidência.

## Preflight e ativação

Em **Executar preflight técnico**, crie o run pelo comando da atividade e registre no painel as
evidências funcionais solicitadas. A mesma tentativa é reutilizada; não crie runs paralelos.

Em **Autorizar ativação e orçamento**, a ação só aparece quando os gates estiverem aprovados e o teto
financeiro existir. Leia a confirmação completa: ela informa experimento, amostra e limite. A decisão
fica auditada no processo. Se a campanha já existir, abra antes o experimento e use **Retomar a
campanha com limite financeiro** para informar orçamento diário, teto acumulado, início, fim, parada
sem compra e meta de compras. A fila respeita a data inicial; registrar hoje não antecipa gasto.
Autorizar a atividade não duplica campanha e não substitui a confirmação da Meta.

Se uma ação estiver bloqueada, siga a **Próxima ação** mostrada no primeiro requisito pendente e volte
à mesma atividade. A tela atualizará o estado persistido após a correção.
