# Mira: transferência entre especificação e renderização

## Estado e causa comprovada

Autorização: US$10 totais para preparar/revisar produto e comunicação de Mira, sem mídia ou
geração paga de vídeo. Consulta MCP de 07/10/2026 às 21:28 UTC: ciclo 9, experimento 102;
nenhuma tarefa paga ativa; soma estimada conhecida US$9,0629936. Valores são estimativas,
não despesas comerciais conciliadas. A tarefa 636 custa estimados US$0,5309912 e contém a
composição completa, estratégia preservada e uma única lacuna técnica de PNG.

A tarefa 636 foi bloqueada após o PR 5535 já integrado e publicado, por uma causa distinta:
o prompt exigia PNG persistido para concluir a especificação, mas o worker só acionava o
renderizador após `COMPLETED`. O shell aninhado recusou namespaces; a composição existente
foi renderizada localmente com os mesmos pixels aprovados pelo renderizador versionado.
Fonte: tarefa 627, prova 523, SHA256
`45ec50db5899063f59b2d788ee24d9f7f326e18bb3c9194dd32fe63184132fa7`.
O resultado antigo e o custo devem ser preservados. Não há prova nova de demanda, venda ou lucro.

## Alternativas e escolha

| Alternativa | Benefício | Risco e esforço | Aderência |
|---|---|---|---|
| Liberar shell/namespaces para o modelo | Permitir geração dentro da sessão | Mais privilégios e dependência do ambiente; duplica função já existente | Baixa |
| Repetir a inferência pedindo conclusão diferente | Pode obter outro JSON | Novo custo e risco de repetir a mesma dependência; descarta plano aproveitável | Baixa |
| Delegar PNG ao renderizador existente e reaplicar o plano | Resolve a classe de falha e reutiliza resultado pago | Exige validação de entrada e rollout compatível, sem infraestrutura adicional | Alta; escolhida |

O modelo entrega `READY_FOR_RENDER` exclusivamente em `NON_AUDIOVISUAL_PACKAGE`.
O worker valida a especificação e as fontes atuais, gera e grava cada PNG e somente depois
reporta `COMPLETED`. Backend continua decidindo o avanço. O handshake existente protege a
transição entre imagens antigas e novas dos serviços. Prompt e schema são versionados no worker.

O replay anterior é reaproveitado para o caso legado estrito: preparação privada pronta,
origem/versão/estratégia iguais, prova interna obrigatória, uma lacuna de namespace,
guardrails preservados e nenhum novo gasto ou autorização comercial. A entrada bruta continua
no histórico; auditoria identifica recuperação técnica e custo incremental de modelo zero.
Isso não aprova a mensagem nem autoriza publicação. Psique e Têmis precisam revisar os pixels
finais; a decisão de uso continua independente.

## Matriz local definida antes da execução

| Critério | Evidência de validação |
|---|---|
| Caso arquivado gera PNG e persiste antes do sucesso | Fixture compartilhada, Java Graphics2D, fonte/hash, upload HTTP, callback |
| Outras origens funcionam sem exceção por nome/ID | Experimento distinto e referência independente de produto nos testes |
| Callback antes válido permanece válido | Regressões de materialização `COMPLETED` e callback HTTP 500 |
| Worker anterior não reserva o plano novo | Claim sem handshake permanece vazio; claim versionado recupera a mesma tarefa |
| Atualização de entrada ou estratégia bloqueia | Testes antes de upload, sem chamada de IA |
| Lacuna funcional e prova inválida bloqueiam | Guardrails, contratos, hash, crop e fonte são obrigatórios |
| Falha de storage preserva a saída | Callback de falha conserva resultado e impede conclusão |
| Replay é limitado e não cobra IA novamente | Mesmo taskId/custo, rawModelResponse preservado, zero interação com runner |
| Revisão independente e decisão de uso continuam necessárias | Testes existentes do gate da jornada privada |
| Histórico e segregação são preservados | Fixture AGENT_VALIDATION; demonstração sintética, sem compra/cobrança |
| Navegação e imagem legíveis | Chromium desktop, iPhone 15 Pro e Pixel 7; sem alegação de Safari nativo |
| Persistência e observabilidade | Repositório JPA real, suíte de backend e auditoria de transferência/renderização |

Resultados, PR e SHA de publicação são registrados somente após a validação correspondente.
Não se repete a matriz completa por contagem de rodadas: falhas exigem correção local e
revalidação dos cenários afetados.

## Resultados locais

Validação de 07/10/2026: 4.302 testes de backend cobertos, sem falha ou erro pendente;
35 condições opcionais. A primeira execução foi encerrada pelo limite de memória da sandbox.
A execução com cache Spring limitado completou a suíte e encontrou apenas Node ausente do
PATH do subprocesso de um teste MCP; os 18 testes dessa integração passaram após corrigir
o ambiente para Node 22. Nenhuma mudança de aplicação foi feita por essas limitações locais.

Íris: 69 testes, sem falha/erro; o replay da fonte real 523 também foi executado.
A única condição não executada exige entrada exportada de Vega e não se aplica ao caso atual.
A imagem 1080 × 1350 foi renderizada da especificação arquivada e conferida a 393 pixels.
Desktop, iPhone 15 Pro e Pixel 7 passaram em correção, retorno contextual, gate explícito,
conclusão exposta pelo backend, imagem, ausência de overflow e erros JavaScript.

Contratos: 21 regressões canônicas, 13 de CI e nove de recursos; 16 testes do pacote comercial
mais a construção de 414 arquivos/77 manifestos. Spotless, comentários Java e diff conferidos;
bash -n/ShellCheck dos scripts usados aprovados. Nenhum teste usou modelo pago ou escrita
produtiva. PNG local é evidência da sandbox; o artefato persistido será produzido pelo worker
versionado após publicação verificada, mantendo o resultado da tarefa 636 e suas revisões.

A seleção real de candidatos pelo repositório JPA também passou no MySQL **5.7.44**
da engine isolada: falha de namespace e callback legado incluídos, rejeição funcional
e outro agente excluídos. Um teste físico, sem falha/erro. O JAR contém as mesmas
4.258 classes compiladas, 761 recursos íntegros e inicializa o catálogo de 523 cartões.
