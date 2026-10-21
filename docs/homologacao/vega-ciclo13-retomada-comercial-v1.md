# Vega — retomada da preparação comercial

## Aceite e alternativas

Resposta à pendência #3709: teto de IA de US$ 20 cumulativos para os experimentos
100/103/106. Mantidos R$ 100 de mídia por cinco dias após homologação e ausência
de autorização de vídeos pagos. Registro pela tela, sem reiniciar custos.

A tarefa 795 foi criada no subprocesso 80, chamado pelo processo 79. A consulta
da fila retorna HTTP 500 em `OpalaCommercialVersionContract.resolve`: a candidata
não pertence à versão do ciclo. O histórico comercial publicado é v12; o ciclo
13 exige v14. Construção e comunicação privadas não comprovam entrega comercial.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Ajustar apenas o diagnóstico HTTP | Torna a falha legível | Mantém uma tarefa inexequível sendo reservada | Insuficiente |
| Validar somente na abertura da atividade | Evita novas tarefas inválidas | Não recupera tarefas pendentes anteriores | Insuficiente |
| Usar o mesmo validador antes da abertura e da reserva | Evita custo e reserva inválida, recupera a fila existente | Mantém os requisitos comerciais próprios | Adotada |

## Matriz local antes dos testes

| Caso | Aceite |
| --- | --- |
| Vega 4/106/13, candidata predecessora | Espera persistível antes da reserva; nenhum modelo, checkout ou publicação |
| Outro produto, experimento e versão | Mesma proteção sem exceção por identidade |
| Candidata exata já válida | Reserva preservada com versão, preço, checkout e acesso próprios |
| Contrato sem checkout ou com acesso divergente | Causa explícita; não tratar como pronto |
| Contexto corrigido | A mesma tarefa pode voltar a ser elegível; não duplicar resultado ou custo |
| Economia e revisões posteriores | Mantêm seus validadores e revisões independentes |
| Observabilidade | Causa e ação disponíveis na atividade e na tarefa pendente |
| Interface | Orientação e limites legíveis no Chromium desktop, iPhone e Pixel emulados |
| Dados de teste | Apenas fixtures locais; nenhum evento sintético conta como venda |

O teto foi registrado uma vez no produto, e a revisão financeira 27 preserva
preço, fontes e lacunas, atualizando o limite anterior. A meta de contribuição
da v14 ainda não foi escolhida pelo usuário. Estimativas anteriores somam
US$ 11,5302408; a reserva total de US$ 3 para 709 permanece separada de cobrança.
O saldo conservador inicial é US$ 5,4697592, antes de novas tarefas.

Testes e prontidão técnica não comprovam demanda, margem realizada ou lucro.

## Resultado da validação local

- 383 casos Java distintos, sem falhas ou erros; os testes condicionais de MySQL
  foram executados com MySQL 5.7 no projeto exclusivo da sandbox. A topologia e
  seus volumes foram removidos ao terminar.
- O teste MockMvc percorreu o endpoint `pending` e o serviço real de reserva com
  o validador real. A candidata predecessora não foi reservada; outra ocorrência
  válida recebeu HTTP 200 e entrou em execução. Nenhum provider foi chamado.
- A abertura da atividade e o claim devolvem a mesma causa. Corrigir a candidata
  reabilita a tarefa existente; checkout ausente, acesso divergente, destino
  ausente, ambiguidade e erro de infraestrutura permanecem separados.
- Spotless nos arquivos alterados, `git diff --check`, `bash -n` e ShellCheck do
  runner MySQL aprovados. O check global Spotless encontrou formatação anterior
  em arquivos sem relação com esta mudança; eles foram preservados.
- Nenhuma alteração de frontend, changelog, prompt, modelo, orçamento de mídia,
  aprovação privada, preço ou status comercial. A navegação publicada será
  reconferida após a entrega do backend.

Esta correção transforma a falha da fila em uma pendência auditável; não cria a
candidata comercial v14. A autorização financeira foi atendida. O próximo aceite
comercial conserva contrato utilizável, meta de contribuição escolhida pelo
usuário, custos projetados e pareceres próprios. Nenhuma tarefa paga começou na
retomada antes dessa conferência; a inferência da tarefa 795 permanece ausente.
