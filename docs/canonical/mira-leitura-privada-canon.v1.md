# Mira — leitura privada assistida v1

## Finalidade

Reduzir o esforço para validar utilidade real de Mira antes da venda. A atividade deve indicar como
abrir o protótipo aceito e aproveitar evidência própria, sem pedir que o operador invente código de
pessoa ou transcreva sinais técnicos.

## Contrato

- `Mira` é exclusivamente o codinome interno do produto `10`, conforme decisão reafirmada em
  2026-09-05. A experiência da participante, inclusive título da aba, instruções e mensagens de
  erro, deve apresentar o benefício ou o nome comercial do catálogo; nunca usar o codinome como
  marca. IDs, rotas, eventos e referências internas permanecem estáveis para preservar o histórico.
  Enquanto o catálogo contiver o nome descritivo com sufixo operacional `PDE planejado #36`, usar
  o benefício já aprovado, “Sua rotina, organizada com calma”, sem criar uma marca comercial nova.
- O backend fornece URL e versão do protótipo aceito; convites individuais permanecem privados.
  Não expor segredos do deploy em consultas administrativas, logs, relatório ou URLs HTTP.
- A atividade deve distinguir visualmente a tela pública do convite: o link simples apenas comprova
  disponibilidade e não concede acesso. O operador entrega à participante o arquivo de convite
  individual; esse arquivo abre o protótipo com fragmento transitório, preenche o código localmente e
  remove o fragmento antes da primeira requisição. A interface não pede transcrição do segredo.
- A pessoa aceita participar e usa o seu convite. QA permanece `QA_INTERNAL`; somente uma sessão
  `PRIVATE_READING` pode sustentar leitura humana. Duas leituras exigem pessoas independentes.
- A atividade assistida importa participante, consentimento, sinais, término e referência auditável
  pelo contrato autenticado do backend PDE. Não aceita booleanos digitados como prova de Mira.
- A confirmação do operador é explícita, nunca pré-marcada: leitura de pessoa real, aderente ao
  público, consentida e observada. Comentário é opcional; referência e justificativa são automáticas.
- Antes da decisão BPM, o backend principal reconsulta e valida produto, versão, participante,
  encerramento e evidência. Integração indisponível bloqueia; não equivale a ausência de eventos.
  Na consulta administrativa, preservar a URL previamente aceita com estado `EVIDENCE_UNAVAILABLE`,
  sinais vazios e registro bloqueado se o PDE não responder. Uma prova incompatível ou aceitação
  inválida continua sendo erro; nunca substituir a verificação de conclusão por essa projeção.
- Respostas negativas são permitidas e preservadas. Os cinco sinais continuam necessários para
  avançar o gate; encerrar uma leitura sem todos eles registra bloqueio, não sucesso comercial.
- Resultado e evidência encerrados não podem ser alterados para aproveitar sinais de entrada
  anterior. Uma nova rodada precisa de evidência independente, preservando a anterior.
- Checkout é `SIMULATED_NO_CHARGE`; leitura e QA não são compra, venda, receita ou autorização de
  mídia. O produto permanece `PLANNED` até os gates seguintes.
- As leituras privadas são um gate anterior à aquisição, não o canal comercial de Mira. Depois de
  duas leituras independentes aprovarem utilidade e segurança, o canal principal planejado é mídia
  paga no Instagram via Meta Ads, em experimento próprio e com a identidade pública do produto.
  Criação ou ativação dessa campanha continua condicionada à comunicação, jornada, checkout,
  entrega, instrumentação, teto econômico e autorização humana explícita.
