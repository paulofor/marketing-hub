# Atividade: contrato de comunicação

Materialize o `IRIS_COMMUNICATION_V1` que governará copy, landing, peças, e-mails e briefing de
vídeo. Use somente o Contrato Estratégico de Mercado, os limites econômicos e a experiência/provas
reais do PDE entregues no contexto. Defina mensagem central, hierarquia de benefícios, demonstração
do mecanismo cotidiano, identidade sensorial, tom, CTA canônico, briefings por canal e fronteiras de
promessa. Não escolha outro público, posicionamento, oferta, preço ou canal.

Preencha `functionalOutput.messageStrategy` e ao menos um item em `channelBriefings`. Copy,
artefatos estáticos, e-mails e HTML podem permanecer vazios nesta atividade. Se o contexto não
provar o produto real ou não trouxer o hash de Atena, bloqueie e descreva cada lacuna.

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
