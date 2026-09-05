# Mira — leitura privada assistida v1

## Finalidade

Reduzir o esforço para validar utilidade real de Mira antes da venda. A atividade deve indicar como
abrir o protótipo aceito e aproveitar evidência própria, sem pedir que o operador invente código de
pessoa ou transcreva sinais técnicos.

## Contrato

- O backend fornece URL e versão do protótipo aceito; convites individuais permanecem privados.
  Não expor segredos do deploy em consultas administrativas, logs, relatório ou URLs HTTP.
- A pessoa aceita participar e usa o seu convite. QA permanece `QA_INTERNAL`; somente uma sessão
  `PRIVATE_READING` pode sustentar leitura humana. Duas leituras exigem pessoas independentes.
- A atividade assistida importa participante, consentimento, sinais, término e referência auditável
  pelo contrato autenticado do backend PDE. Não aceita booleanos digitados como prova de Mira.
- A confirmação do operador é explícita, nunca pré-marcada: leitura de pessoa real, aderente ao
  público, consentida e observada. Comentário é opcional; referência e justificativa são automáticas.
- Antes da decisão BPM, o backend principal reconsulta e valida produto, versão, participante,
  encerramento e evidência. Integração indisponível bloqueia; não equivale a ausência de eventos.
- Respostas negativas são permitidas e preservadas. Os cinco sinais continuam necessários para
  avançar o gate; encerrar uma leitura sem todos eles registra bloqueio, não sucesso comercial.
- Resultado e evidência encerrados não podem ser alterados para aproveitar sinais de entrada
  anterior. Uma nova rodada precisa de evidência independente, preservando a anterior.
- Checkout é `SIMULATED_NO_CHARGE`; leitura e QA não são compra, venda, receita ou autorização de
  mídia. O produto permanece `PLANNED` até os gates seguintes.
