# Matriz de homologação — autorização simples do Rigel v1

## Objetivo

Comprovar que a atividade **Autorizar ativação e orçamento** apresenta a verdade persistida e pode
ser concluída por uma pessoa com um único aceite, sem criar campanha, gasto, contato ou venda.

## Matriz ponta a ponta

| Dimensão | Cenário | Resultado esperado | Evidência |
|---|---|---|---|
| Caminho feliz | run produtivo `READY_TO_PUBLISH`, gates aprovados e teto positivo | resumo mostra amostra e teto; um clique conclui a atividade | resposta HTTP, instância BPM e estados persistidos |
| Validação | token de outra atividade | rejeição antes de qualquer gravação | HTTP 400 e ausência de nova ocorrência |
| Validação | gate, run, plano ou teto ausente | comando oculto e causa objetiva exibida | contrato de prontidão do backend |
| Validação | autorização repetida | segunda decisão bloqueada de forma idempotente | HTTP 409 e uma única ocorrência concluída |
| Recusa | operador escolhe não autorizar | solicita apenas o motivo e preserva estados comerciais | ocorrência `BLOCKED` com evidência |
| Integração | autorização aprovada | experimento, run, janela e período do produto mudam na mesma transação | consultas MySQL 5.7 |
| Falha | erro em qualquer persistência da ativação | rollback integral | teste de integração transacional |
| Observabilidade | decisão aprovada | evidência contém modo, origem humana, run, experimento, plano e horário | `objective_evidence_json` |
| Métricas | QA local e autorização | não cria visita, checkout, compra, receita ou gasto | contagens antes e depois |
| Segregação | dados de teste | usa referências locais próprias e não altera o experimento produtivo | fixtures e banco efêmero |
| Desktop | Chromium 1440 × 1000 | resumo e botão principal visíveis sem formulário técnico | captura Playwright |
| Mobile | iPhone 15 Pro | botão ocupa largura útil e recusa permanece secundária | captura Playwright |
| Mobile | Pixel 7 | resumo, evidências expansíveis e aceite sem rolagem lateral | captura Playwright |
| Publicação | backend atualizado e bundle anterior no host | detector compara a revisão real de `/healthz`, recompila o frontend defasado e só marca o deploy após a revisão esperada responder | contrato de deploy e healthz |

## Critério de conclusão

- Uma rodada integral sem defeito conclui a homologação.
- Se uma rodada revelar defeito, corrigir a causa e executar duas rodadas integrais consecutivas sem
  falha após a última correção.
- Produção só pode ser declarada concluída quando a atividade estiver `COMPLETED`, o experimento e o
  run estiverem `RUNNING`, a janela comercial estiver aberta, o produto estiver na etapa correta e
  não houver campanha, gasto ou venda criados pela autorização.
