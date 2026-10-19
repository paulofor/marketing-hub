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
