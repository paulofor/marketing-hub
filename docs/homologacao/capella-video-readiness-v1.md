# Capella — prontidão do roteiro antes do parecer de vídeo

## Escopo e aceite — 10/10/2026

Produto 7, cadeia 26, ciclo 12, experimento 105, versão `capella-private-v3`.
A solicitação #3335 autoriza **US$ 10 no conjunto dos dois vídeos e revisões** e
**R$ 100 no total de anúncios, por cinco dias após a liberação comercial**.
Não altera o teto anterior da preparação do produto e não antecipa a campanha.

Aceite desta passagem: autorização auditada uma única vez, roteiros e prova real
vinculados aos dois perfis, produção encaminhada pelo backend a Plutus e Apolo,
com revisão independente e decisão comercial preservadas. A prontidão local
deve recusar roteiro ausente antes de abrir parecer pago, manter consultas
isoladas sem consumo e explicar a causa na tela.

## Histórico e causa comprovada

- Evento 73, revisão 5: autorização audiovisual de US$ 10. A decisão sobre mídia
  está citada na justificativa; a campanha continua sem orçamento operacional
  até a passagem comercial canônica.
- Projetos 11/12 receberam os perfis novos 67/68, prova 685/584, mesma versão e
  roteiros do kit. Tetos individuais: US$ 4 por vídeo, mais US$ 2 reservados ao
  conjunto de revisões. Isso não autoriza gastar US$ 10 por vídeo.
- O ciclo de vídeo 31 abriu o parecer 718 sem roteiro aprovado no perfil 67.
  Em 10/10 às 21:22:42 UTC, o callback encontrou `SCRIPT_NOT_FOUND`. O modelo já
  tinha respondido, com custo conhecido estimado de US$ 0,057238; não houve render
  antes do parecer. O projeto do Estúdio tinha texto, mas o executor consumia o
  roteiro aprovado do perfil, um contrato distinto.
- A comparação com os ciclos 29/30 de Mira confirmou que os perfis 65/66 tinham
  roteiro aprovado e chegaram à revisão do vídeo. Não era falta de capacidade
  de Apolo nem prova de insuficiência do orçamento.
- Os roteiros 568/569 foram registrados pelo formulário dos perfis. O backend
  reaproveitou a resposta auditada de Plutus 718 sem nova inferência e criou o
  job de Apolo. Aprovar o roteiro operacional não aprova o vídeo comercial.

## Mudança compartilhada e alternativas

Manter orientação manual teria menor esforço, mas conservaria consumo antes da
checagem. Aprovar automaticamente o texto do Estúdio reduziria cliques, mas
mudaria a autoridade do roteiro e poderia renderizar conteúdo não selecionado.
A opção adotada valida o contrato canônico do perfil antes de criar o ciclo de
produção e o revalida antes de abrir Plutus após o preflight. Ela reutiliza a
mesma leitura de roteiro usada na renderização e preserva o isolamento por tenant.

A consulta isolada de preflight continua disponível sem roteiro: ela não cria
reserva, parecer pago nem job. A tela conserva a mensagem de recusa do backend e
oferece o link do perfil correspondente. Nenhum prompt novo, autoaprovação,
infraestrutura paralela ou exceção por ID foi introduzido.

## Matriz definida antes da validação local

| Caso | Evidência exigida | Validação |
|---|---|---|
| Capella sem roteiro | Recusa antes de persistência, preflight, Plutus e job | Unitário do ciclo com perfil 67 |
| Outro produto sem roteiro | Mesma recusa sem depender de nome ou ID | Mesmo contrato com perfil 9202 |
| Perfil antes válido | Roteiro aprovado aceito; render e gates anteriores preservados | Testes de perfil, ciclo, controller e arquitetura |
| Consulta isolada | Sem exigência de roteiro, reserva, parecer ou render | Preflight isolado e callback |
| Outro tenant | Recusa antes da leitura do roteiro | Teste do serviço canônico do perfil |
| Recusa na tela | Causa e link corretos; sem mensagem de produção iniciada | Componentes React com duas identidades |
| Aceite de vídeo acessível | Link para os dois vídeos do contexto mesmo sem integração automática | Capella, outra identidade e caminho automático antes válido; nenhuma gravação ao abrir |
| Falha de transporte | Mensagem de contingência preservada | Teste do extrator de erro |
| Desktop e celulares | Erro legível e link utilizável | Chromium, iPhone 15 Pro e Pixel 7 com API simulada local |
| Integração e observabilidade | Resposta de Plutus recuperada, jobs correlacionados e custo conhecido separado de desconhecido | Banco e logs MCP; sem nova inferência para o replay |
| Segregação comercial | Fixtures sem provider real; campanha e métricas reais separadas | Test doubles locais e conferência do experimento 105 |

## Aprendizado e limites

Hipótese reutilizável: detectar falta de roteiro antes do parecer reduz tarefas
pagas sem entrega utilizável e tempo parado entre Plutus e Apolo. Os testes
medem recusa sem efeitos e preservação de caminhos válidos, não aumento de vendas.
O custo anterior do parecer é preservado; recuperação não deve repetir IA.
Render visual editorial local não elimina custos de locução e de revisões.
## Resultado local

- Backend e arquitetura: **148 casos passaram**, sem falhas ou skips. A
  regressão adicional do callback de preflight passou junto dos 28 testes do
  ciclo; o conjunto final contém 149 casos distintos.
- Frontend: **93 testes passaram**, incluindo 27 casos do ciclo após a correção
  de navegação. TypeScript e build produtivo passaram.
- Chromium desktop, iPhone 15 Pro e Pixel 7: as duas identidades recusadas
  mostraram a causa e permitiram navegar ao perfil correto, com um único pedido
  segregado e sem provider real.
- Recuperação existente: oito testes de validação de segmentos passaram. O teste
  opcional da mídia histórica de Mira não foi executado por ausência daquela
  fixture; a recuperação essencial de Capella foi executada separadamente com
  os **16 áudios auditados reais**, FFmpeg e FFprobe, rede local segregada e duas
  montagens completas. Anúncio de 27 segundos e demonstração de 45 segundos,
  1080×1920, áudio presente e **zero novas chamadas de TTS**. Chromium reproduziu
  os arquivos até o fim sem erro. A leitura do conteúdo e o aceite comercial
  permanecem sujeitos a QA independente; reprodução técnica não os substitui.
- Compatibilidade da main: uma fonte de Atena alterada anteriormente pelo PR
  #5574 divergia da atestação de Mira v16. Os 40 testes de entrega/consumo de
  Atena passaram antes da atestação sucessora v17, sem alteração da experiência
  ou autorização. Registro: `mira-compatibilidade-auditoria-atena-2026-10-10.md`.

Os jobs 21263/21264 foram solicitados pelo formulário existente com o contrato
`PRESERVED_TTS_NARRATION_V1`, a mesma copy e `newTtsAuthorized=false`. Os custos
anteriores da voz continuam pendentes de conciliação e não são declarados zero.
Os dois pareceres financeiros conhecidos somam US$ 0,115712 estimados; esse
subtotal não representa o custo integral nem fatura conciliada.

## Entregas persistidas e próxima passagem

Os ciclos 31/32 chegaram a `VIDEO_READY_FOR_REVIEW`. O experimento 105 recebeu
os ativos 55 (AD, 27 segundos, job 21263) e 56 (LANDING_HERO, 45 segundos, job
21264), ambos `READY` com revisão `PENDING`. Os arquivos, áudio e HLS estão
vinculados ao mesmo contexto. Nenhum parecer de Psique ou Têmis foi fabricado.

O projeto 12 foi corrigido pelo Estúdio para o canal `PDE_HERO_DIAGNOSTIC`, pois
o rascunho conservava `PAYWALL_OFFER`, que produziria papel PRE_CHECKOUT. O
endpoint oficial de atualização reconciliou somente o papel do ativo 56,
preservando a resposta anterior e acrescentando `commercial_role_reconciliation`
com projeto, canal, job e motivo. Não houve render, síntese ou aprovação adicional.

A fila visual de revisão do produto/experimento mostra as duas peças necessárias
e oferece a decisão humana de uso. Produção técnica pronta não confere essa
decisão. As duas entregas foram registradas pelo formulário do ciclo, que chegou
a `VIDEO_APPROVAL`, revisão 8. A fonte da orientação já identificava o aceite
pendente, mas o link “Ver aprovações dos vídeos” era exibido somente dentro da
seção de continuação automática. Capella conserva percurso manual de integração,
então o botão não aparecia. Ele agora fica na área comum e abre a fila existente
com produto e experimento explícitos, sem aprovar, gravar ou iniciar execução.
Não foi alterado o contrato de integração nem declarada automação inexistente.
Seis navegações locais adicionais passaram em desktop, iPhone e Pixel para
Capella e outra identidade, com zero comandos ou aprovações.

O orçamento operacional de mídia continua zerado; os R$ 100 por cinco
dias permanecem consentimento condicionado à liberação comercial, não campanha
ativa nem janela iniciada antes da homologação.

A adoção fica restrita à abertura de produção audiovisual. Reverter se a regra
impedir perfil com roteiro aprovado ou consulta isolada, ou se quebrar a
recuperação auditada. Liberação comercial, seleção humana, mídia, checkout,
medição e margem continuam sujeitas aos contratos existentes. Teste privado
ou tarefa pronta não comprova demanda, venda ou lucro.
