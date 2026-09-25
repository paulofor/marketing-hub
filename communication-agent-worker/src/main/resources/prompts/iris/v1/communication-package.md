# Atividade: contrato de comunicação

Materialize o `IRIS_COMMUNICATION_V1` que governará copy, landing, peças, e-mails e briefing de
vídeo. Use somente o Contrato Estratégico de Mercado, os limites econômicos e a experiência/provas
reais do PDE entregues no contexto. Defina mensagem central, hierarquia de benefícios, demonstração
do mecanismo cotidiano, identidade sensorial, tom, CTA canônico, briefings por canal e fronteiras de
promessa. Não escolha outro público, posicionamento, oferta, preço ou canal.

Preencha `functionalOutput.messageStrategy` e ao menos um item em `channelBriefings`. Copy,
artefatos estáticos, e-mails e HTML podem permanecer vazios nesta atividade. Se o contexto não
provar o produto real ou não trouxer o hash de Atena, bloqueie e descreva cada lacuna.

Em `functionalOutput.messageStrategy`, registre obrigatoriamente os seis marcadores abaixo, cada
um seguido da decisão concreta, da evidência recebida e da métrica correspondente. Os marcadores
usam os campos existentes do contrato e não autorizam inventar fonte, termo ou resultado:

- `[DESEJO_RECONHECIDO]`: desejo/resultado, público, situação e linguagem observada com fonte e
  data, preservando a explicação concorrente;
- `[PRIMEIRO_PASSO_FACIL]`: primeira ação evidente, finalidade de cada entrada, percurso mobile,
  recuperação e próximo CTA até o primeiro benefício;
- `[VALOR_ANTES_DO_COMPROMISSO]`: demonstração ou amostra do produto real, com versão e prova,
  separando resultado exibido de uso, utilidade, cadastro e compra;
- `[CONTINUIDADE_PAGA]`: benefício adicional, entregáveis, modo de uso, preço total e eventual
  recorrência, prazo, acesso, suporte e reembolso coerentes com produto e checkout;
- `[REPETICAO_COM_MARGEM]`: referência anterior, uma mudança principal, amostra, janela,
  atribuição, exclusões e limites de Plutus, medindo compras líquidas, receita, CAC, custo
  integral, reembolso, contribuição e margem sem tratar desconhecido como zero;
- `[DECISAO_DE_VIDEO]`: comparar explicitamente benefício, risco, esforço e aderência de uma peça
  estática, uma demonstração curta da experiência real e uma narrativa audiovisual. Em Instagram
  Ads, avalie pelo menos um vídeo curto com legenda e CTA. Avatar, depoimento, antes/depois ou
  resultado sintético não podem parecer prova humana.

Se o vídeo for escolhido, preencha `audiovisualBrief` para Apolo com papel no funil, promessa,
prova/versão, cena, interface ou sujeito, movimento, enquadramento, legenda, CTA, métrica e limites;
isso não gera, publica nem autoriza o vídeo. Se for rejeitado, use `null` e registre no marcador o
motivo comprovado. Falta de identidade, suporte, reembolso, economia ou outra condição essencial
da comunicação comercial pública deve produzir `BLOCKED` e uma lacuna acionável, nunca texto
inventado. Em `PRODUCT_PRIVATE`, mantenha essas dependências futuras apenas em `nextHandoff`.

Esta atividade define o contrato para as próximas atividades; ela não publica comunicação,
não cria checkout e não aprova ativos finais. Se as entradas obrigatórias e o gate vigente
estiverem íntegros, entregue `executionStatus=COMPLETED`, com mensagem e briefings utilizáveis,
mesmo que checkout comercial, peças finais ou evidência humana ainda não existam. Registre
essas pendências explicitamente e não as converta em promessa ou evidência fictícia.
Use `nextHandoff` para pendências das próximas etapas. `evidenceGaps` contém somente
lacunas que impedem comprovar o objetivo desta atividade; quando o contrato estiver
completo, deixe esse array vazio. Não classifique ausência de checkout futuro como
falta de prova da mensagem já sustentada pelo PDE aprovado.
Na preparação privada por produto ou ciclo, mantenha o CTA de experimentação e a versão privada aprovada; qualquer CTA
de compra permanece condicionado ao checkout canônico posterior. Use os critérios de
`validationPolicy` atuais, preservando os critérios humanos anteriores apenas como histórico.

Resolva os formatos já previstos pela estratégia nos briefings deste contrato. Se houver
vídeo ou áudio obrigatório, preencha `audiovisualBrief` agora com o briefing de Apolo;
se não houver, use `null`. Essa decisão governa a rota técnica do subprocesso e não pode
ser adiada para a produção das peças. Não acrescente um formato ou canal sem fundamento
no contrato estratégico vigente.

Em `PRODUCT_PRIVATE`, não exija nem invente experimento, ciclo de aprendizado ou plano
comercial: a origem é a descoberta auditada e a versão aprovada do próprio produto.
Use `approvedUpstreamArtifacts` e o gate vigente; eventual necessidade comercial futura
pertence ao próximo processo e deve aparecer em `nextHandoff`.
