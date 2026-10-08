# Fluidez da cadeia e implementação privada de kits — 08/10/2026

## Evidência e decisão

Capella, produto 7, ciclo 5, experimento 98, cadeia 26: o processo 116 concluiu
planejamento; a execução 47 do processo 117 aguarda `technicalHomologation`.
As tarefas 596, 597 e 599 entregaram contratos. O executor de especificação é
somente leitura e não materializa o software. Psique não pode substituir essa entrega.
O compositor de kits já produz candidatas sem venda ou envio de e-mail.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Mudar somente a orientação | Pequeno esforço | Preserva a ausência de implementação | Recusada |
| Refazer toda a cadeia | Organização uniforme | Migração histórica e esforço elevado | Recusada |
| Completar o percurso com capacidades existentes | Entrega concreta e prevenção compartilhada | Integrar persistência, compositor e harness | Adotada |

A candidata privada utiliza fixtures explícitas, sem provedor pago, SMTP real,
pagamento, publicação comercial ou mídia. Não altera a oferta nem comprova demanda.
O backend conserva contexto e avanço; o executor de pagamentos reutiliza o compositor
somente para produzir o kit de QA. Psique e Têmis mantêm avaliações independentes.

## Matriz definida antes da implementação

| Dimensão | Casos e aceite |
| --- | --- |
| Caminho completo | Contratos → sessão → entrada → pending → claim → compositor → callback → pacote utilizável → prova → homologação disponível |
| Identidade | Capella e contexto independente; produto, ciclo, experimento e versão cruzados recusados |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7, com cenários ADHERENT, RECOVERY e SAFETY |
| Uso | Primeira aplicação associa post, story, legenda e mensagem; ZIP íntegro e retomada preservam briefing e hash |
| Validações | Sete campos obrigatórios, consentimento simulado, correção sem perda de entrada válida, limite e ausência de contato externo |
| Falhas | Callback duplicado, claim concorrente, falha preservada, quota final, sessão expirada/revogada e acesso cruzado |
| Integrações | MySQL 5.7 real; compositor real com imagens criadas pelo código; dependências comerciais recusadas |
| Observabilidade | Status, entrada/saída estruturadas, artefato/hash, horários, erros e eventos persistidos; custos simulados separados dos reais |
| Decisões | Cenários com fontes e lacunas; meta humana registrada uma vez, sem orçamento ou parecer inventado |
| Métricas | Tempo parado com fonte, versão executável, autorização, compras e contribuição somente do ciclo correto; desconhecido permanece desconhecido |
| Regressão | Outro produto/contexto e passagem anteriormente válida; nenhuma inferência adicional por repetição de callback |

Executar uma matriz técnica. Os três pareceres independentes não repetem a matriz.
Retestar somente critérios que mudarem ou falharem. A conclusão operacional exige
PR, merge, workflows aplicáveis, identidade publicada e conferência pela tela.

## Validação local observada

- MySQL 5.7 aplicou o changelog novo; Hibernate validou o schema canônico.
- Matriz real: 9 cenários/dispositivos aprovados; 2 arquivos completos reutilizados entre dispositivos.
- Contexto independente: outro produto, ciclo, experimento, versão e perfil barber-v1 aprovados.
- Oito grupos de controles de API aprovados: identidade, campos, consentimento, contatos, concorrência, callbacks, transferências, revogação e segregação.
- Falha de imagem foi recuperada pelo mesmo pacote; reinício após callback indisponível reaplicou o ZIP sem outra composição.
- Interface administrativa: desktop/iPhone/Pixel sem overflow; meta inicialmente vazia; rascunho sem escrita; escolha explícita persistida no contexto local.
- Backend: 4.354 testes, nenhuma falha/erro; pagamentos: 62 testes; Psique: 167 testes. Testes dependentes de runners/relatórios específicos mantêm suas condições documentadas; a nova matriz e os controles MySQL foram executados pelo runner local.
- Registro da prova: perfil, URL e pacote aceito da mesma identidade são obrigatórios; identidade original e outro perfil passaram. O repository final iniciou com MySQL 5.7.
- Frontend: 77 testes relacionados e typecheck/build passaram. Scripts: bash -n e ShellCheck passaram.
- O pacote canônico preserva Mira com atestação sucessora de compatibilidade v10; sem renovar pareceres, ciclos, despesas ou publicação comercial.

As ilustrações privadas são criadas pelo código e explicitamente sintéticas. Comprovam a estrutura, personalização e uso do kit, sem substituir a biblioteca fotográfica homologada ou uma revisão independente de qualidade comercial. Não houve compra, contato externo, inferência paga ou gasto de mídia.
