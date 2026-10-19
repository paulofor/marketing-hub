# Alcyone — prova funcional para a comunicação

## Evidência e causa — 19/10/2026

Produto 11, cadeia 26 v26, experimento 97 PLANNED, processo 113 v11 e subprocesso
121 v10. A tarefa 711 produziu a primeira peça; Psique 712 pediu um passo de entrada
ou escolha de looks. Íris 713 e 753 bloquearam a correção porque receberam somente
488/489, da homologação técnica 583: as duas imagens mostram o encerramento técnico.
O MCP confirmou fontes, custos e ausência de outra prova funcional naquela homologação.
Não é falta de roteiro nem uma falha do modelo: o harness capturava depois de concluir
todo o percurso, quando entrada e resultado já tinham desaparecido.

## Alternativas

| Caminho | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Nova inferência com todas as capturas históricas | Sem nova integração | Repete custo; todas mostram o encerramento | Descartado |
| Reabrir toda a homologação e seus pareceres | Produz novas provas | Invalida aceites sem mudança da experiência e pode repetir inferências | Descartado |
| Ampliar a captura complementar existente e selecionar momentos funcionais | Preserva versão, gate, custos e avaliações | Exige identidade exata e validação dos novos pixels | Escolhido |

A referência histórica `@video-proof-v1` é conservada por compatibilidade. A captura
complementar serve a comunicação estática e audiovisual; não produz vídeo nem chama IA.
O gate original permanece obrigatório. A comunicação não incorpora a captura complementar
ao fingerprint da estratégia: os arquivos são resolvidos separadamente e validados novamente
na leitura e na conclusão da peça.

## Matriz definida antes dos testes

| Área | Aceite |
| --- | --- |
| Percurso real | Entrada, três looks e encerramento nos três dispositivos; nove cenários e quinze capturas |
| Seleção | Íris recebe CONTENT antes de encerramento; fallback histórico preservado |
| Isolamento | Outro produto/versão/destino/referência, hash divergente ou gate inválido recusados |
| Segurança | SAFETY sem resultado; recuperação, credenciais e eventos preservados |
| Custos | Captura determinística sem provedor, cobrança, campanha ou mídia |
| Auditoria | Momento, dispositivo, tarefa, versão e hash associados ao mesmo percurso |
| Recuperação | Homologação 583–587 e comunicação 710 preservadas; nenhum parecer criado pelo operador |
| Regressão | Duas identidades sintéticas e caminho antigo aceito nos testes compartilhados |
| Dispositivos | Chromium desktop 1440, iPhone 15 Pro e Pixel 7 |

Testes locais comprovam a passagem técnica e os guardrails. Não comprovam demanda,
preferência humana, venda, lucro ou melhora de conversão. A peça corrigida exige novos
pareceres independentes e a seleção humana prevista no processo.

## Resultados

- Backend: uma rodada integral de 4.869 testes, sem falhas/erros; 44 dispensas opcionais do projeto.
- Íris: 92 casos no conjunto da suíte e da regressão com pixels reais, sem falhas/erros;
  Psique: 235 casos, sem falhas/erros. As validações opcionais dependentes de outros
  ambientes mantêm suas condições anteriores. A matriz real foi passada aos validadores Java.
- O leitor de prova e a arquitetura foram executados novamente após o vínculo explícito
  à referência privada original; sete casos do leitor e as regras de arquitetura passaram.
- Duas identidades sintéticas, versões, referências, destino, hash, gasto e gate inválidos
  foram exercitados sem S3, IA paga ou dados comerciais.
- O harness real executou nove cenários em desktop/iPhone/Pixel e produziu quinze PNGs.
  A captura complementar com outra referência de experimento também passou, conservando
  a origem privada e zero chamadas de provedor. A prova real foi renderizada e persistida
  pelo teste HTTP de Íris com backend simulado, com fonte e hash preservados.
- Bash -n e ShellCheck passaram no runner; o construtor comercial e seus 16 testes passaram.
  O pacote contém 534 arquivos e 119 manifestos. A atestação sucessora de Mira v29 altera
  somente a compatibilidade do capturador compartilhado; publicação automática continua desligada.
- A primeira execução local apontou que o daemon Docker usa o host sandbox-docker;
  o runner passou a resolver esse endereço a partir de DOCKER_HOST. A falta de espaço
  foi contornada preservando arquivos e encerrando a topologia exclusiva com volumes/orphans.

As dispensas não substituem a matriz local executada. Nenhum aceite humano, revisão
independente ou resultado comercial é inferido destes testes.

## Abertura da captura no painel

A conferência operacional mostrou que a mesa carregava a caixa completa de tarefas
(incluindo prompts e respostas) e a coleção completa de experimentos antes da criação.
A seleção não se completou; houve conexão encerrada e um reinício do backend.
O detalhe do experimento 97 e sua consulta resumida voltaram a responder. Isso confirma
a dependência desnecessária de coleções completas; não comprova a causa do reinício.

Foram comparados refatorar os contratos de todas as listas, criar um formulário paralelo
e abrir o formulário existente sem carregar histórico. A terceira opção reduz o escopo:
`/agents/<id>?newTask=1&experimentId=<id>` consulta a identidade escolhida no endpoint
de detalhe existente. A caixa, o monitor e as listas não utilizadas não participam da
criação; a mesa anterior continua disponível. A lista de agentes oferece esse acesso.
O backend continua validando o pedido oficial, sem gravação direta ou aprovação simulada.

Matriz adicional: dois experimentos sintéticos nos testes de componente; consulta ausente
impede envio; criação preserva a referência exata; falha de gravação conserva o formulário;
nenhuma coleção completa é chamada; sucesso usa o número retornado pelo backend.
Navegação e envio devem passar em Chromium desktop, iPhone 15 Pro e Pixel 7,
com dependências HTTP locais e sem eventos comerciais.

Runner: `node infra/testing/alcyone-creative-proof/admin-form.cjs`.
As fixtures de API interceptam somente caminhos iniciados por `/api/`; arquivos de
módulos JavaScript como `/src/api/http.ts` continuam sendo entregues pelo Vite.

Resultado da extensão: 19 testes frontend passaram, typecheck e build concluíram.
O navegador confirmou três dispositivos, duas identidades adicionais, nenhum carregamento
integral de tarefas/experimentos e recuperação da falha de envio com o mesmo pedido.
As respostas e gravações da matriz são simuladas; nenhuma tarefa produtiva foi criada.

## Retomada real e ajuste do template — 19/10/2026

Após o PR 5625, a captura complementar 754 passou nos nove cenários e persistiu quinze
imagens, com zero chamadas de provedor. Íris 755 recebeu CONTENT 1028/1025, preservando
583–587 e 710 com os mesmos hashes. A peça 1039 foi renderizada; Psique 756 e Têmis 757
pediram enquadrar a combinação visual e qualificar a demonstração antes do botão.

A nova evidência confirmou outra causa compartilhada: `ProofCardRenderer` fixava o aviso
de teste somente após o CTA. O recorte de 755 mostrava apenas texto; o refinamento 758
reposicionou os mesmos pixels para mostrar a roupa, sem gerar imagem. A execução 68 foi
pausada pela tela, preservando a tarefa que já havia começado e impedindo novas revisões
durante a correção local. Os pareceres são decisões independentes, não falhas de callback.

| Alternativa | Benefício | Limite | Decisão |
| --- | --- | --- | --- |
| Acrescentar instrução ao prompt | Mudança curta | Não altera a posição fixa do aviso | Insuficiente |
| Repetir o aviso na copy | Pode qualificá-lo antes da ação | Duplica texto, depende da inferência e ocupa o benefício | Descartada |
| Corrigir o template e orientar seleção da prova visual | Ordem garantida para todos os produtos | Exige regressão de pixels e leitura em celular | Adotada |

A composição privada mantém fonte 36, CTA e prova 952 × 667. Reorganiza espaços acima
da prova e coloca o aviso imediatamente antes do botão, com o rodapé factual depois.
O ramo comercial conserva suas coordenadas. O prompt passa a escolher a fonte pela entrega
visível e legibilidade, evitando favorecer mobile quando isso exclui o objeto demonstrado.

Matriz adicional definida antes da implementação: duas identidades sintéticas; posição do
aviso antes da ação detectada pelos pixels, sem depender de copy gerada; ressalvas, recorte
e ramo comercial preservados; replay local da especificação real 758 com a fonte 1028;
leitura em Chromium desktop, iPhone e Pixel; nenhum upload ou inferência durante os testes.
O teste novo falhou antes da correção (zero pixels da qualificação antes do botão em ambos
os casos) e passou depois. A suíte de Íris executou 94 testes, sem falhas ou erros; quatro
casos opcionais mantiveram suas condições. O replay com a fonte real foi executado separadamente.
O PNG desse replay foi conferido em desktop, iPhone 15 Pro e Pixel 7: roupa inteira,
orientação legível, aviso antes da ação e ressalva preservada, sem transbordamento.
O novo arquivo produtivo ainda exige produção pelo executor e novos pareceres independentes.

## Tipografia declarada e pixels — 19/10/2026

A candidata 759/1041 reproduziu exatamente o SHA do replay local. Psique 760 confirmou a
combinação completa, dica e aviso antes do CTA, mas apontou identificadores menores que os
36 pixels prometidos em `visualComposition`. O renderer mantinha 25 pixels na identificação
fixa e no rótulo solicitado, enquanto o teste anterior protegia somente aviso e rodapé.

Foram comparados encurtar o rótulo, flexibilizar a revisão e garantir a tipografia no renderer.
Encurtar não corrige tamanho; flexibilizar retiraria uma condição legítima de legibilidade.
A terceira opção foi adotada: marca, identificação e rótulo usam 36 pixels no ramo privado;
o topo é reorganizado mantendo prova 952 × 667, copy, aviso, CTA e ressalvas.
O ramo comercial continua com suas métricas anteriores.

Matriz adicional: medir os três blocos de identificadores pela tinta dos glifos e compará-los
à mesma palavra no CTA de fonte 36, em duas identidades. O teste falhou no renderer anterior
e passou na correção; a suíte completa executou 96 casos sem falhas/erros (quatro opcionais).
O replay real mantém a mesma fonte, crop e especificação de Íris. A leitura em 393 pixels
foi conferida localmente em desktop, iPhone 15 Pro e Pixel 7 antes de publicar, junto à
ausência de transbordamento e à ordem visual.
Pareceres anteriores e custos não são reescritos; a produção e a revisão da candidata corrigida
continuam obrigatórias. Qualidade local não comprova conversão ou venda.

## Reaplicação sem nova inferência — 19/10/2026

A tarefa 762 preservou um plano `READY_FOR_RENDER`, mas foi bloqueada por repetir
os pixels da candidata rejeitada. A tipografia era fixa no renderer. Repetir a
inferência não altera essa causa. O comando manual de retomada já existente
reconhecia callbacks completos, mas não preparava o plano pronto para o executor
compatível; a tela tampouco expunha essa recuperação no detalhe do produto.

Foram comparados criar outra tarefa, automatizar qualquer bloqueio e ampliar a
recuperação existente. Outra tarefa repete custo; automatizar rejeições funcionais
criaria um loop. A terceira opção reaplica a mesma resposta após a correção técnica,
por ação explícita no detalhe auditado, mantendo o handshake `IRIS_RENDER_PLAN_V1`.
Não há novo endpoint, fila ou mecanismo paralelo. A resposta bruta e o resultado
anterior são preservados antes da substituição pela saída materializada.

Matriz adicional definida antes dos testes: caso original e outra identidade; worker
antigo não recebe plano pronto; executor compatível reaplica sem chamar o modelo ou
renovar o custo; resposta bruta exata preservada em dois modos privados; callbacks
anteriores continuam válidos; tela recusa outro agente, atividade, escopo, tipo,
estado, evidência inválida e ausência de erro. Envio incerto conserva a resposta e
reconsulta o estado. Desktop, iPhone e Pixel exercitam o componente real com HTTP
local simulado, incluindo falha 503 e nova tentativa manual, sem POST de criação.

Resultado: 4.871 testes na suíte integral do backend, sem falhas/erros e 44 dispensas opcionais;
98 testes na suíte de Íris, sem falhas/erros e quatro dispensas opcionais;
28 testes de frontend, typecheck, build e navegação local nos três dispositivos
passaram. As duas regressões de retomada no backend falharam antes da mudança e
passaram depois, preservando custo e identidade. Esses resultados comprovam o
comportamento local; o aceite produtivo depende dos pareceres da candidata final.

## Origem e continuidade entregues à integridade — 19/10/2026

O PR 5627 materializou 762/1042 com o mesmo SHA do replay local e preservou a resposta
bruta e o custo de US$ 0,442742. Psique 763 aprovou a candidata. Têmis 764 confirmou
a correção visual, mas bloqueou a falta de fonte verificável e de continuidade atual
em desktop/mobile. O loader de Têmis anexava apenas `CREATIVE_RENDER`; não entregava
os pixels da origem nem as capturas funcionais. Repetir copy ou render não resolve isso.

| Alternativa | Benefício | Risco ou esforço | Decisão |
| --- | --- | --- | --- |
| Browser dedicado ao modelo com novas ferramentas | Navegação atual independente | Exige governar também as sessões e os arquivos privados; maior esforço | Reservada |
| Fonte mais captura simples da página atual | Poucos arquivos e baixo custo | A primeira tela não comprova três combinações e escolha após o formulário | Insuficiente neste caso |
| Renovar a captura determinística existente e entregar fonte, recorte e relatório | Reutiliza nove cenários e preserva a mesma candidata | Exige conservar linhagem ao atualizar a captura | Adotada |

Não há endpoint ou fila nova. `visual-inputs` entrega `creativeProof` somente à Têmis,
com acesso reservado aos arquivos listados. O executor recalcula os hashes, anexa a
fonte original, a prévia do recorte e capturas CONTENT datadas, evitando duplicar pixels
idênticos. `creativeEvidenceChecks` fica no prompt auditado e distingue a conferência
dos arquivos da navegação que originou o relatório. Têmis mantém a decisão independente.

A origem histórica de uma peça permanece válida após uma captura aprovada mais recente
da mesma versão; nova captura bloqueada ou identidade/hash incompatível continua
impedindo a passagem. Não alterar fonte, candidata, Psique ou custos para renovar o destino.

Matriz definida para esta extensão: dois produtos/referências e duas tarefas sintéticas;
fonte original e captura renovada pelo serviço real; acesso cruzado recusado; gate atual
bloqueado e produto divergente recusados; loader HTTP real com hash adulterado, crop fora
dos pixels e versão divergente; caminho anterior sem metadados adicionais preservado;
arquivos temporários removidos; capturas desktop/mobile sem modelos ou tráfego comercial.

A suíte integral do backend concluiu 4.874 testes, sem falhas/erros e 44 dispensas opcionais;
a regressão focada também passou após acrescentar a segregação entre revisores.
As duas regressões do backend falharam antes da correção por perder a fonte antiga ao
renovar a captura e passaram depois. As duas do loader esperavam quatro anexos e recebiam
apenas um antes da correção. A suíte completa de Têmis passou com 115 testes, zero falhas
ou erros e uma dispensa opcional. O replay HTTP local com os PNGs reais 1042, 1028 e 1025
passou: quatro anexos, duas capturas de continuidade, hashes conferidos e zero chamadas
ao modelo. Os IDs da captura renovada desse replay são test doubles, sem alegação de
navegação produtiva nova. O teste produtivo deve renovar a captura pelo painel após o deploy.

Métrica de adoção: reavaliar o mesmo SHA com a origem e a continuidade disponíveis, sem
nova materialização ou inferência de Íris. Reverter/bloquear se o gate atual ou qualquer
identidade divergir. Ganho comercial permanece uma hipótese até compras e contribuição.

O mesmo contrato também entrega a homologação principal quando não há captura
complementar. Uma captura atual pendente ou bloqueada impede recorrer à origem anterior;
a segregação conserva fontes adicionais somente para Têmis. A regressão do caminho
principal passou junto à suíte integral. Falhas de hash/crop/versão anteriores ao modelo
registram tarefa e causa no erro persistido, além do stack trace nos logs.
