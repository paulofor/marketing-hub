# Pausa do processo no resumo do produto — 08/10/2026

Capella, produto 7, ciclo 5, experimento 98, versão v1, processo 117:
a entrega privada foi registrada na revisão 4 e a tarefa técnica 648 foi aprovada
com nove combinações de cenário/dispositivo e nove evidências visuais. A execução
47 passou para PAUSED antes dos pareceres pagos seguintes, preservando o limite
de zero novas chamadas de IA do contrato atual. O PR 5545 e sua entrega permanecem
aceitos; não é necessário repetir a composição, o registro ou a homologação.

A projeção da atividade seguinte mostrava NOT_STARTED e disponibilidade técnica,
mas o resumo do ciclo indicava que nenhuma decisão fora identificada. A causa era
a ausência do estado persistido do processo na projeção. A mesma classe de erro
poderia ocultar pausas de outros produtos após uma entrega aceita.

Foram comparados inferir a pausa pela ausência de tarefas, reutilizar o estado
persistido do processo e criar um novo mecanismo de autorização. A consulta do
estado existente foi escolhida: é auditável, proporcional e conserva os controles
vigentes; ausência de tarefas não comprova pausa e outro mecanismo duplicaria
responsabilidades. O ajuste consulta produto, processo, cadeia, ciclo e referência
do experimento no backend. PAUSED expõe a retomada necessária, seu motivo e a
atividade que aguarda. PAUSING preserva a tarefa em andamento; ciclos encerrados
não recebem nova pendência a partir de seu histórico.

Regressões: Capella com implementação aceita e processo pausado; outro produto
com identificadores diferentes; tarefa técnica em conclusão de pausa; caminho
antes válido sem pausa; histórico encerrado; persistência e isolamento das cinco
dimensões da consulta. Não muda orçamento, não libera gasto nem retoma tarefas.

A validação local também encontrou uma conversão indevida do valor desconhecido
da decisão para booleano primitivo. A atribuição agora preserva os três estados
sem lançar erro na leitura de uma atividade bloqueada ou em conclusão de pausa.

Validação local: 132 testes relacionados aprovados, incluindo sete casos da
projeção, dois de persistência, cinco de registro da prova, 26 do gate e 92 de
arquitetura; nenhuma falha, erro ou skip nessa seleção. Spotless e o construtor
de evidências comerciais passaram. A matriz já aceita de nove percursos não foi
repetida por uma alteração de projeção. A conferência publicada exige a pausa
visível na tela e a preservação da tarefa 648, da revisão 4 e dos dois pacotes.
