# Capella: direção de beleza e passagem ao sucessor — 26/10/2026

## Evidência e decisão

A Big Idea v2 (#6) preservava a ideia central do usuário sobre profissionais autônomos
de beleza, mas os outros campos e o ciclo #14/#107 mantinham manicures/nails-v1.
A inspeção do produtor de sucessores mostrou cópia incondicional de `brief.audience`
e de `Experiment.hypothesis`. Assim, escrever uma hipótese nova não bastava para
encaminhar o público escolhido aos próximos agentes.

Alternativas: manter unhas (contraria a decisão), abrir para todos os pequenos
negócios (amplia o escopo sem evidência), ampliar dentro da beleza com personalização
por especialidade (adotada). Para a passagem: edição direta de banco rejeitada;
reescrever ciclo antigo rejeitado; campo explícito no preparo oficial adotado.

## Matriz de aceite local (definida antes da rodada)

| Critério | Validação |
| --- | --- |
| Mudança de público chega ao ciclo, hipótese chega ao experimento | Serviço com Capella e outra identidade |
| Público não informado conserva comportamento anterior | Regressões de preparação já existentes |
| Histórico e limites preservados | Oferta/preço iguais; mídia zero; sem janela ou tarefas; predecessor intacto |
| Recuperação não muda direção já preparada | Replay idêntico e replay com outro público |
| Validações e falhas | HTTP invalida excesso de caracteres; STOP, revisão antiga, exposição e mesmo versionamento |
| Interface e integração | Formulário real, contrato HTTP, envio único, erro visível, estado de espera |
| Dispositivos | Chromium desktop e emulação iPhone/Pixel no formulário local |
| Observabilidade | `successorAudience`, fonte da decisão e hipótese no aprendizado persistível |
| Segregação e métricas | Fixtures locais sem IA/mídia; nenhuma compra simulada conta como comercial |

Critério de adoção: direção explícita preservada no novo contexto e nenhuma regressão
nas proteções. Reversão: nova revisão/PR, sem apagar histórico. Medir posteriormente
retrabalho por público contraditório e tempo até entrega aceita; vendas e margem só
podem ser medidas no experimento autorizado.

## Limites operacionais

Os custos de Codex são estimados, não fatura. As sete tarefas #811–#817 do ciclo #14
somam US$ 6,254654; seis tiveram inferência e a audiovisual registrou NOT_APPLICABLE.
A continuação exige conferir o acumulado e os limites antes de nova tarefa. A mudança
da Big Idea e o cadastro preparatório não autorizam ampliar mídia ou audiovisual.


## Resultado local

- Backend: 50 testes sem falhas, incluindo duas identidades, preservação do histórico,
  validação HTTP, replay divergente e hipótese longa limitada apenas na coluna legada
  de 255 caracteres (o ciclo conserva o texto integral).
- Frontend: 13 testes sem falhas, tipagem e build aprovados; formulário real exercitado
  em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, com API local simulada e um
  único POST por preparação. Sem chamadas para serviços produtivos nesse teste.
- Contrato do CI: 14 testes e verificação do workflow frontend aprovados. O CI passa a
  executar as regressões da mudança de público. Revisão do diff sem erros de whitespace.
- Memória da sandbox: primeira compilação/build simultâneos excederam a cota de 3 GiB;
  ambos passaram sequencialmente com heap limitado. Sem publicação usada como teste.
- Big Idea: revisão #7/v3 confirmada por MCP, todos os campos iguais ao rascunho e
  ideia central idêntica à revisão #6/v2. A gravação ocorreu pela tela; uma espera de
  resposta expirou depois do commit, e a leitura confirmou o recibo sem repetir gravação.
- Financeiro: notas do produto registravam base US$12,9597216 antes da tarefa #811.
  Somando US$6,254654 das tarefas #811–817, o conhecido estimado fica US$19,2143756,
  acima do teto US$15. Não é fatura nem autorização retroativa; nenhuma nova tarefa
  deve ser iniciada nesta continuação sem decisão financeira explícita.
