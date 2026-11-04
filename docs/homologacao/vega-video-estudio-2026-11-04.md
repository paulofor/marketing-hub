# Vega — pedido de vídeo e preparação no Estúdio — 04/11/2026

## Evidência e causa

Consulta de tela, APIs oficiais e MCP às 19h UTC: produto 4, cadeia 27, ciclo 18,
experimento 111, v17. Íris #873 escolheu demonstração curta e preservou briefing
45s; #874 produziu prova PNG interna. A rota #816 e dispensa #817 declaram
`audiovisualPlanned=true`, `audiovisualProductionDeferred=true`, `BRIEF_ONLY` e
`videoProductionRequestId=0`. Não foi falta de briefing nem diagnóstico de saldo.
O último ciclo audiovisual de Vega encontrado foi #22, experimento 92; ele não
autoriza o experimento 111. Financeiro do #18 não registra teto audiovisual.

Lacunas compartilhadas: a projeção substituía o motivo persistido da dispensa por
texto genérico; o resumo do ciclo não seguia WAITING_SUBPROCESS até WAITING_HUMAN;
o aceite privado não explicitava que cobria somente a imagem revisada.

A solicitação muda a direção para pessoa adulta, movimento, trilha e demonstração
real. CHANGE_PER_CYCLE_V1 exige sucessor mesmo sem exposição. Pela UI foram
registrados ADJUST do #18 e sucessor #21/experimento #114, versão de planejamento
`musa-pde-entry-v18-video-cinematico`. Não representa implementação ou homologação
já realizada da v18. Históricos, provas, custos e decisões anteriores preservados.
Nenhuma peça aprovada pelo Codex. Nenhum gasto, publicação, campanha ou cobrança.

## Alternativas e decisão

- Reutilizar a prova estática: menor esforço, mas não atende ao pedido de vídeo.
- Filmar só a interface: prova clara e produção menor, mas não atende à direção
  de desejo cotidiano, pessoa e roupas em movimento.
- Narrativa de 30–35s com encenação sintética identificada e demonstração real:
  atende à solicitação, exige produção própria e revisão temporal. Escolhida
  para pré-produção; teto não inferido, execução paga ainda depende de aprovação.

## Matriz de homologação definida antes dos testes

| Dimensão | Critério |
| --- | --- |
| Dispensa | Motivo persistido visível, sem fabricar sucesso, aprovação ou saldo |
| Decisão | Espera humana no filho aparece no resumo do ciclo correto |
| Isolamento | Outro produto/ciclo/cadeia/referência ou pai inválido não contamina o resumo |
| Recuperação | Ciclo de referências não trava a consulta; pausa e execução antes válidas preservadas |
| Aceite | Decisão estática identifica STATIC_IMAGE e audiovisualApproved=false |
| Integração | Backend fornece texto à interface existente; nenhum GET inicia tarefa ou gasto |
| Experiência | Conferir textos no navegador desktop, iPhone e Pixel; vídeo final exige reprodução real |
| Observabilidade | IDs de origem, auditoria, motivo e limite desconhecido preservados |
| Comercial | Testes sintéticos separados; sem atribuir venda ou melhoria de conversão |
| Produção real | Pedido → Íris → Apolo → projeto/candidata → Psique/Têmis → Paulo; pendente de orçamento próprio |

## Aprendizado reutilizável

Hipótese: distinguir prova estática, planejamento audiovisual e entrega de vídeo
reduz decisões equivocadas e retrabalho. Regressões verificam o comportamento,
não impacto comercial. Adotar se as esperas forem exibidas no contexto correto;
reverter se aparecerem decisões de outro ciclo ou leitura causar efeitos externos.

## Pré-produção persistida e limite da entrega

Pelo frontend foram criados o projeto de Estúdio #14 e o perfil #69, ambos DRAFT,
produto 4, experimento 114, duração 32s. O editor foi conferido em desktop,
iPhone 15 Pro e Pixel 7, aguardando o carregamento efetivo de produto, experimento
e perfil. O MCP confirmou a mesma identidade em `video_project`.

O projeto contém proposta editorial do Codex para Íris/Apolo, sem atribuir autoria
ou parecer a esses agentes: oito cenas, direção de personagem adulta sintética,
figurino/objetos/ambiente, continuidade, narração, trilha, legendas, montagem,
CTA e critérios de revisão. Gancho: “E se a próxima combinação já estiver no seu
armário?”. Roupa e lenço em movimento alternam com entrada, orientação, salvar e
reabrir do produto real. Referências descritas ainda não são imagens aprovadas;
o roteiro ainda não foi aprovado por Paulo nem validado por Íris.

O plano de render deve ser reconciliado com o contrato real, não apenas com os
segundos aproveitados: 32s geram três clipes de 15+15+2s. A tarifa pública
Seedance 2.5/Runway a 720p é US$0,30/s, com mínimo US$0,80 por geração, resultando
em US$9,80 de estimativa parcial, sem referência de vídeo, impostos ou outras
etapas. Repetir 10s adiciona US$3. Fonte consultada em 04/11/2026:
https://docs.dev.runwayml.com/guides/pricing/ . Imagem, voz, trilha, planejamento
e revisões precisam de cotação própria, sem dupla contagem entre grupos.

Foi solicitada decisão audiovisual de até US$20 como limite proposto, não preço
garantido ou autorização presumida. O financeiro do ciclo também não possui teto
de agentes: subtotal histórico estimado US$91,4600736 e dez tarefas sem custo
informado. Foi solicitada decisão separada do teto total acumulado, sem herdar
valores de Capella ou de ciclos anteriores. A consulta confirmou listas vazias de
ciclos de produção do projeto #14 e jobs do perfil #69.

Não há MP4/HLS, storyboard ilustrado, áudio, parecer temporal ou aceite desta
candidata. Não foi executado teste perceptual de vídeo inexistente. A passagem
real até Psique/Têmis/Paulo permanece pendente dessas autorizações e da produção;
testes com doubles comprovam contratos, não a qualidade da futura candidata.

## Validações locais

- 129 testes específicos do backend aprovados, incluindo repetição após a revisão final de texto.
- 32 testes da tela do Estúdio aprovados.
- Dez testes de Apolo (fila/callback, materialização governada e plano do Router)
  aprovados, com backend/provedor simulados e sem chamadas pagas.
- Harness responsivo de aceite: duas identidades em desktop, iPhone e Pixel,
  incluindo prévia, arquivo original, falha/recuperação e aceite local explícito.
- A suíte ampliada inicialmente terminou em 137, com OOM registrado no cgroup.
  Com cache de contextos Spring limitado a dois, 4.307 casos passaram antes de
  novo encerramento por memória. A execução continua somente com suítes restantes
  e testes afetados pelo ajuste final, em JVM nova. A continuação alcançou 5.808
  casos sem falhas; as quatro suítes finais são executadas isoladamente após novo
  encerramento por memória no fim da rodada. Houve também falta
  de disco durante a gravação de evidência do navegador; a gravação do projeto
  foi reconferida após liberar caches e excluir cópias de anexos do worktree
  por sparse checkout, preservando os originais e os arquivos versionados.

Resultado consolidado: 5.812 casos do backend, zero falhas/erros e 66 skips
condicionais de integração. As últimas suítes terminaram com código zero; os
relatórios anteriores aprovados foram preservados, sem repetir toda a matriz.
Nenhum changelog foi alterado. As integrações MySQL seguem no CI do PR.

A atestação Mira v74 registra somente compatibilidade das fontes compartilhadas,
preserva v73 e não renova pareceres nem autoriza publicação comercial.
