# Atividade: contrato de comunicação

Materialize o `IRIS_COMMUNICATION_V1` que governará copy, landing, peças, e-mails e briefing de
vídeo. Use somente o Contrato Estratégico de Mercado, os limites econômicos e a experiência/provas
reais do PDE entregues no contexto. Defina mensagem central, hierarquia de benefícios, demonstração
do mecanismo cotidiano, identidade sensorial, tom, CTA canônico, briefings por canal e fronteiras de
promessa. Não escolha outro público, posicionamento, oferta, preço ou canal.

Preencha `functionalOutput.messageStrategy` e ao menos um item em `channelBriefings`. Copy,
artefatos estáticos, e-mails e HTML podem permanecer vazios nesta atividade. Se o contexto não
provar o produto real ou não trouxer o hash de Atena, bloqueie e descreva cada lacuna.

Quando `communicationMaterializationContext.privateCreativePreparation` declarar
`PDE_PRIVATE_CREATIVE_PREPARATION_V1`, registre no briefing a prova interna `PROOF_CARD_V1`
obrigatória para Psique e Têmis. Essa imagem documenta a mensagem e os pixels aprovados na
avaliação privada; ela não seleciona anúncio estático nem muda o formato comercial escolhido.
Mesmo que a alternativa comercial seja exclusivamente vídeo, preserve a prova interna prevista
pela próxima atividade. Não descreva essa obrigação técnica como formato não autorizado.
Com `audiovisualProductionIntent=BRIEF_ONLY`, entregue o briefing completo, identificando vídeo
planejado e ainda não produzido. `GOVERNED_PRODUCTION_REQUESTED` identifica um pedido próprio
da mesma versão: sua produção continua sujeita aos gates independentes de Apolo e do Estúdio.
Essa declaração não concede orçamento, publicação ou geração paga. Peças comerciais futuras
continuam dependendo de suas condições e autorizações.

Em `functionalOutput.messageStrategy`, registre obrigatoriamente os seis marcadores abaixo, cada
um seguido da decisão concreta, da evidência recebida e da métrica correspondente. Os marcadores
usam os campos existentes do contrato e não autorizam inventar fonte, termo ou resultado.
Use cada marcador exatamente uma vez, com conteúdo próprio; uma lista de rótulos vazios
não comprova cobertura nem conclusão:

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

Em `INITIAL_EXPERIMENT_PRIVATE`, `prototypeVersion`, `approvedDestination`,
`visualProofAuthorization` e `approvedVisualArtifacts` identificam a experiência real já
homologada que este primeiro experimento deve comunicar. Cite a versão exata na decisão de valor
e no briefing da prova. Não proponha, renomeie ou fabrique uma versão inferior ou futura; uma
mudança de versão exige novo gate e novas capturas. Use somente os pixels autorizados e mantenha
o CTA na experiência privada enquanto `paymentEnabled=false`.


## Linguagem da cliente e briefing audiovisual — v2

Separe o destino privado da linguagem da peça: `paymentEnabled=false` e os identificadores de
versão ficam no contexto de produção, nunca na narração, legenda, headline ou CTA. Não escreva
“acesso privado”, “demonstração interna”, “homologação”, nomes de agentes ou etapas na comunicação
à cliente. Um CTA de experimentar deve explicar a ação real disponível, sem prometer compra ou
resultado inexistente. “Exemplo ilustrativo” e limites de uso claros são permitidos e necessários.

No briefing de Apolo, separe explicitamente NARRAÇÃO, TEXTO NA TELA e INSTRUÇÕES DE PRODUÇÃO.
Abra pela situação e pelo desejo reconhecido; mostre a primeira ação simples, um resultado útil
legível e o valor adicional real da continuidade. Não use urgência, depoimentos, eficácia clínica
ou escassez inventados. Não transforme “privado” em benefício. Uma pessoa sem conhecer o Hub deve
entender o produto e a próxima ação.

O anúncio deve funcionar no feed: contexto visual de identificação/uso, mudanças significativas
de plano e recompensa visual. Uma página inteira encolhida, ampliada lentamente ou repetida não
comprova demonstração nem variedade. A demonstração deve mostrar ação → resultado, em recortes
legíveis no celular. Preserve a identidade da prova; pessoas ou cenas sintéticas são ilustrações,
nunca clientes, depoimentos ou resultados clínicos reais. Apolo deve entregar o vídeo final para
Psique inspecionar a sequência temporal, não reutilizar um parecer de PNG como aceite do vídeo.

## Roteiro que conversa com a cliente — VIDEO_COPY_REVIEW_V1

Você também escreve a copy falada do vídeo. Não entregue só uma descrição para Apolo narrar.
Transforme atributos em benefício na situação da pessoa: por que ela se importa, o que fica mais
fácil, o que verá agora e qual ação faz sentido depois. Reconhecimento e conexão vêm de um momento
concreto e de uma fala respeitosa, não de adjetivos, culpa ou da obrigação de começar com pergunta.

No `audiovisualBrief`, quando houver vídeo, use uma vez cada seção textual abaixo, sem serializar
JSON. Mantenha `null` quando não houver vídeo. Os marcadores ficam no briefing interno:

[ABERTURAS]
Escreva três falas alternativas curtas para a mesma promessa: situação cotidiana, desejo concreto
e contraste demonstrável. Compare adequação ao público, clareza e risco de promessa; escolha uma
e justifique. Não são três vídeos nem autorização para três gerações.

[FALA_FINAL]
Entregue o texto literal pronto para narração, separado por papel solicitado (anúncio e/ou
demonstração). Fale com uma pessoa, com frases curtas e palavras dela. A abertura deve produzir
reconhecimento; conecte a dificuldade ao ganho desejado, explique o mecanismo sem aula, mostre
uma prova disponível e termine com uma única ação concreta. Leia o texto como conversa e corte
frases frias ou intercambiáveis entre produtos. Não liste todos os arquivos, menus e condições
seguidos como catálogo. Na demonstração, explique o ganho ao mostrar a ação; na oferta, traduza
entregáveis em uso e explique o incremento pago sustentado. Ajuste o volume de fala ao tempo
solicitado, reservando pausas para a prova. Não altere preço, oferta, público ou disponibilidade.

[PROVA_E_LIMITES]
Vincule promessas e trechos à versão/fonte recebida, diga o que a imagem demonstra e o que não
comprova. Separe limites relevantes à cliente de auditoria interna. Sem depoimentos, urgência,
resultado clínico ou demanda inventados; sem promessa de agenda cheia. Se não há evidência de
conversão, registre a hipótese, nunca “copy validada para vender”.

[DIRECAO_VISUAL]
Separe TEXTO NA TELA, AÇÃO VISUAL e INSTRUÇÕES DE PRODUÇÃO da fala. Indique como cada cena faz
avançar situação → benefício → prova → ação. Não transformar contrato, hash, gate ou status em
narração. Apolo recebe uma fala utilizável e suas provas, não precisa improvisar a mensagem.

Contrastes para aprender a transformação, nunca copiar fatos de outro produto:
- Descritivo: “Arte e texto combinam. Copie a legenda. Baixe seus arquivos.”
  Conversa possível para um kit: “O atendimento ficou lindo. Mas, na hora de postar, faltou o que
  escrever? Veja este post e a legenda feitos para combinar. Você informa seu serviço e recebe
  um kit com a identidade do seu negócio. Veja a amostra e confira o que vem no kit.”
- Descritivo: “Organiza informações, com fontes e limites.”
  Conversa possível para organização de informações: “Tem produto de pele em casa e acaba
  procurando de novo as instruções? Reúna os nomes e as orientações que você já tem para consultar
  depois. Olhe este exemplo. Confira como funciona antes de escolher a versão completa.”
Só use cada alegação se as entradas comprovarem entrega, acesso e ação. Esses exemplos são
hipóteses de redação, não resultados de clientes nem textos já aprovados. Persuasão não depende
obrigatoriamente de “você”, pergunta, medo ou urgência; depende da relação concreta com o desejo.
