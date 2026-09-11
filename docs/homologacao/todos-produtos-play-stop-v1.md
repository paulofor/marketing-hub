# Homologação — todos os produtos e controle PLAY/STOP v1

## Objetivo

Disponibilizar no menu administrativo uma visão compacta de todos os produtos, inclusive os que
estão em STOP, ordenada pelo nome interno e com acesso direto à edição e ao comando canônico de
PLAY/STOP. A tela deve preservar a visão operacional detalhada existente, que continua restrita aos
produtos em PLAY.

## Alternativas avaliadas antes da implementação

| Alternativa                                  | Benefício                                 | Risco, custo e aderência                                                                        |
| -------------------------------------------- | ----------------------------------------- | ----------------------------------------------------------------------------------------------- |
| Ampliar o catálogo detalhado atual           | Reaproveita a tela existente              | Mistura operação em PLAY com administração, mantém cards extensos e aumenta a confusão relatada |
| Substituir o catálogo atual pela lista total | Uma única tela                            | Retira a priorização comercial e os detalhes úteis da operação diária                           |
| Criar uma visão compacta separada            | Mantém cada tela com uma finalidade clara | Pequeno acréscimo de navegação; melhor equilíbrio entre clareza, esforço e segurança            |

A terceira alternativa foi escolhida. O endpoint canônico `GET /api/products` já fornece a lista
integral e o endpoint `PUT /api/products/{id}/automatic-execution` já realiza a troca atômica com
auditoria. Portanto, não será criado contrato paralelo.

## Matriz definida antes dos testes

| Dimensão              | Caminho feliz                                                                        | Validações e falhas obrigatórias                                                                          |
| --------------------- | ------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------- |
| Lista integral        | Exibir produtos em PLAY e STOP                                                       | Estado vazio e falha de carregamento são explícitos, sem inferir dados ausentes                           |
| Ordem                 | Ordenar alfabeticamente pelo nome interno, com nome comercial como fallback          | Acentos, caixa, números e itens sem nome não tornam a ordem instável                                      |
| Identidade            | Mostrar nome interno, nome comercial quando diferente, ID e slug                     | Ausências usam rótulo seguro e não impedem edição                                                         |
| Comando               | PLAY muda para STOP e STOP muda para PLAY pelo endpoint canônico                     | Botão fica desabilitado e com spinner durante a requisição; falha mantém o estado anterior e fica visível |
| Verdade do backend    | Após o comando, invalidar e consultar novamente a lista                              | Nenhum estado de negócio é concluído apenas por inferência local                                          |
| Navegação             | Novo item abre a lista total; edição abre o produto correto                          | A visão detalhada `/products` continua exibindo somente PLAY                                              |
| Busca                 | Nome interno, comercial, apelido ou slug usa o filtro do backend                     | Resultado vazio e falha preservam contexto e permitem nova tentativa                                      |
| Acessibilidade        | Status, ações, foco e mensagens possuem nomes compreensíveis                         | Teclado, leitor de tela e preferência de movimento reduzido permanecem funcionais                         |
| Responsividade        | Lista e comandos são legíveis em desktop, iPhone 15 Pro e Pixel 7                    | Sem overflow horizontal ou ação oculta                                                                    |
| Integração            | Contratos reais do backend são validados; navegador usa respostas locais controladas | Testes não alteram dados produtivos e não usam deploy como mecanismo de teste                             |
| Observabilidade       | Sucesso e erro do comando ficam visíveis no contexto da lista                        | Não registrar sucesso falso nem esconder falha HTTP                                                       |
| Métricas e segregação | Homologação usa produtos sintéticos locais                                           | Nenhum experimento, campanha, evento comercial, compra, contato ou gasto é criado                         |
| Regressão             | Testes da página, menu, TypeScript, build e contrato backend passam                  | Diff preserva a tela operacional e não altera schema                                                      |

## Critério de encerramento

Uma primeira rodada local completa sem defeitos encerra a homologação. Se a rodada revelar defeito,
a causa deve ser corrigida e duas rodadas completas e consecutivas precisam passar após a última
correção; qualquer novo defeito reinicia a contagem.

## Resultado — 11/09/2026

A revisão do primeiro candidato encontrou dois problemas no próprio ambiente de homologação e na
semântica da página. O smoke isolado da imagem não fornecia o hostname `backend` exigido pela
topologia real, então o teste foi corrigido para simular essa dependência sem alterar o Dockerfile.
A página também criava um segundo `<main>` dentro do marco principal global; o componente passou a
usar o marco já fornecido pela aplicação e recebeu teste contra regressão.

Depois da última correção, duas rodadas locais completas e consecutivas passaram, sem alteração de
código entre elas. Cada rodada aprovou:

- 590 testes do frontend, incluindo lista total, ordem alfabética, busca, carregamento, falhas e os
  dois sentidos do comando PLAY/STOP;
- 26 testes do controller canônico de produtos, incluindo consulta e troca atômica PLAY/STOP;
- TypeScript e build produtivo do frontend;
- Chromium desktop, iPhone 15 Pro e Pixel 7, com uma única região `<main>`, sem overflow horizontal,
  ordem `Antares → Mira → Vega`, busca por Vega e confirmação do comando de Mira;
- build da imagem pelo Dockerfile versionado, `healthz` saudável e fallback SPA da rota
  `/products/all` dentro do container.

A consulta somente leitura ao backend publicado confirmou dez produtos e estados PLAY/STOP
explícitos, incluindo Vega em PLAY e Mira em STOP. As mutações de navegador usaram dados locais
sintéticos. Nenhum produto real foi alterado e nenhum experimento, campanha, contato, evento de
conversão, compra ou gasto foi criado.

As capturas locais das duas rodadas estão em `artifacts/todos-produtos-play-stop-v1/` e permanecem
fora do versionamento.
