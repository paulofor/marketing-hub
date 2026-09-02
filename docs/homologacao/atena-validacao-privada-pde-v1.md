# Matriz de homologação — handoff Atena para validação privada PDE v1

## Objetivo e decisão operacional

Eliminar a circularidade que exigia prova de uso antes da criação do protótipo. O fluxo deve
transformar uma candidata factual `DOSSIER_READY` em um produto `PLANNED`, em `STOP`, com estratégia,
economia e protótipo privado definidos, preservando duas leituras humanas como gate posterior à
construção e anterior a qualquer publicação, contato, gasto ou venda.

- **Gargalo real:** tarefas #309, #313 e #317 bloqueadas antes de Plutus e Dédalo por ausência de
  sinais que somente um protótipo poderia produzir.
- **Métrica esperada:** no máximo uma candidata selecionada por execução, exatamente duas leituras
  privadas predeclaradas, produto posicionado na construção e zero efeitos comerciais externos.
- **Continuar:** contratos v3 e de protótipo válidos, produto `PLANNED` em `STOP` e próxima etapa
  visível como Validação do Momento de Compra.
- **Ajustar:** fonte comercial vencida ou desenho de leitura/protótipo incompleto, mantendo o produto
  sem publicação.
- **Parar:** nenhuma candidata factual apta, risco não controlável, contrato inválido ou tentativa de
  contato, cobrança, campanha ou gasto antes dos gates finais.

## Alternativas consideradas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Repetir Argos | pode adicionar fontes | repete custo e não cria o protótipo ausente | rejeitada quando já há `DOSSIER_READY` |
| Liberar operação comercial | acelera exposição ao mercado | confunde hipótese com produto validado e pode gerar dano ou gasto | rejeitada |
| Separar seleção, protótipo e validação final | remove a circularidade e preserva segurança comercial | exige contratos e relatório explícitos | escolhida |

## Matriz ponta a ponta

| Área | Cenário | Evidência esperada | Critério |
| --- | --- | --- | --- |
| Entrada | execução possui duas candidatas `DOSSIER_READY` | dossiês, fontes e lacunas persistidos | Atena compara três alternativas e escolhe no máximo uma |
| Retomada legada | ciclo concluído nas execuções #22–#24 e Atena antiga bloqueada | botão `Retomar com Atena` chama o comando backend do mesmo ciclo | reutilizar evidências e abrir v6 sem nova pesquisa de Argos |
| Idempotência da retomada | gate atual já está pendente ou em execução | botão oculto e tarefa vigente preservada | nenhuma duplicata, novo ciclo ou custo de Argos |
| Retomada inválida | ciclo incompleto ou sem `DOSSIER_READY` | HTTP 409/422 com causa de negócio | nenhuma tarefa comercial criada |
| Atena | resposta `MARKET_STRATEGY_V3` | estratégia e plano com exatamente duas leituras | somente `READY_FOR_PRIVATE_VALIDATION` pode avançar |
| Validação Atena | resposta antiga ou plano incompleto | erro auditável e resposta bruta preservada | rejeitar `READY_FOR_OPERATION`, quantidade diferente de duas ou sinais ausentes |
| Plutus | candidata selecionada chega ao gate econômico | preço como hipótese, custo, teto e riscos | aprovação não vira receita, orçamento ou gasto |
| Dédalo | arquitetura inclui protótipo privado | entrada simples, resultado pronto, harness, até dez minutos e cinco sinais | checkout permanece simulado e sem cobrança |
| Validação Dédalo | protótipo excede escopo | erro e artefato preservados | rejeitar tempo maior que dez minutos ou instrumentação incompleta |
| Materialização | três gates aprovados | um produto ligado ao dossiê e ao plano | estado `PLANNED`, execução `STOP` e definição `PDE_PRIVATE_VALIDATION_V1` |
| Segregação | várias candidatas ou execuções coexistem | IDs e fingerprint próprios | nenhuma herança de campanha, venda, custo ou evidência de outro produto |
| Cadeia | produto planejado abre histórico | macroprocesso atual e próximo gate | `PLANNED` resolve para `pde-construction-approval` |
| Liberação | produto planejado permanece em `STOP` | comando explícito na tela da cadeia | liberar somente construção privada, sem contato, publicação, campanha, cobrança ou gasto |
| Contexto pré-experimento | construção é iniciada antes de existir experimento | tarefa com referência `product:<id>` e contexto PDE versionado | nenhum experimento artificial e nenhum acesso a contexto de outro produto |
| Aceitação do protótipo | operador informa URL, versão, instrumentação e fonte | snapshot, data, desktop/mobile e sete confirmações persistidos | fonte vigente, acesso privado, pagamento/publicação desligados e mídia zero |
| Privacidade | pessoa consentida executa uma leitura | código `PV-` com 12 caracteres hexadecimais e nenhum dado pessoal | uma pessoa por leitura, evidência própria e vínculo à mesma versão |
| Leitura feliz | os cinco sinais são observados | contagens 1/1 e taxas 1 em todos os sinais | atividade `COMPLETED` e próxima leitura/revisão liberada |
| Leitura negativa | ao menos um sinal não é observado | tentativa persistida com causa e contagens reais | atividade `BLOCKED`, repetível; nunca completar por confirmação textual |
| Pessoa repetida | segunda leitura reutiliza o código da primeira | rejeição auditável | exigir pessoa distinta sem apagar a primeira leitura |
| Psique | duas leituras completas chegam à revisão humana | prompt recebe alvo e contexto privado da mesma referência | decisão explícita `APPROVED`; nenhum artefato global ou de outro PDE |
| Têmis | Psique aprovou e a integridade é revisada | versão, fontes, privacidade e ausência de efeitos externos | decisão explícita `APPROVED`; checkout simulado não é venda |
| Priorização final | protótipo, leituras e pareceres estão aprovados | backend recalcula sinais, taxas, tempo, fonte e versão | produto em `COMUNICACAO_E_JORNADA`, ainda em `STOP` |
| Gate adulterado | booleano `criteriaPassed` contradiz contagens ou sinais | recálculo do backend | bloquear sem confiar no frontend, operador ou modelo |
| Fonte vencida/futura | data fora do prazo de 1–90 dias | requisito insatisfeito e ação corretiva | atualizar o snapshot antes de nova leitura |
| Relatório | execução independente é consultada | seleção, economia, arquitetura e leituras privadas separadas | Validação do Momento de Compra fica `WAITING` sem inferência |
| Legado | Atena antiga bloqueou por ausência de uso | próxima ação e comando em linguagem de negócio | retomar Atena v3 no mesmo ciclo, sem repetição automática de Argos |
| Fontes | qualidade comercial está vencida | causa e ação persistidas | atualizar fontes antes das leituras, sem apagar o protótipo |
| Observabilidade | cada agente executa | prompt/schema, request/response bruto, modelo, tokens, custo e correlação | logs não substituem o relatório persistido |
| Métricas | fluxo termina em produto planejado | selecionados, leituras requeridas e posição na cadeia | visitas, contatos, checkouts pagos, pagamentos, vendas, receita e gasto permanecem zero |
| Interface desktop | dossiê e execução são abertos | explicação do fluxo e link do produto em nova aba | sem overflow, erro JavaScript ou indicação de venda |
| Interface mobile | iPhone 15 Pro e Pixel 7 | conteúdo legível, link acionável e estágios íntegros | mesma verdade funcional do desktop |
| Dados de teste | fixtures locais exercitam o fluxo | IDs e payloads `LOCAL_QA` | nenhuma escrita ou callback produtivo |
| Falha externa | modelo, banco ou integração falha | status técnico, causa e possibilidade segura de retentativa | nenhuma materialização parcial ou efeito externo |

## Rodadas

A primeira rodada local completa cobre workers, backend, MySQL 5.7, frontend, imagens e jornadas nos
três dispositivos. Se ela revelar qualquer defeito, a causa-raiz será corrigida e a contagem será
reiniciada; após a última correção, duas rodadas completas e consecutivas devem terminar sem falha.

Cada rodada deve comprovar explicitamente zero publicação, zero campanha, zero contato, zero gasto,
zero pagamento e zero venda. A homologação não usa tarefas ou dados produtivos e não transforma
aprovação técnica em resultado comercial.

## Resultado da homologação local — 2026-09-02

Duas rodadas locais completas e consecutivas terminaram sem falhas após a última correção. Em cada
rodada foram executados:

- 2.945 testes: 2.245 do backend, 237 dos cinco executores e 463 do frontend, com zero falhas e zero
  erros; três testes preexistentes do backend permaneceram explicitamente ignorados;
- tipagem TypeScript e bundle de produção do frontend;
- aplicação, reaplicação idempotente, rollback e nova aplicação no MySQL 5.7 físico, dentro do
  projeto Compose exclusivo da sandbox;
- construção e inspeção das sete imagens Docker de backend, frontend, Atena, Plutus, Dédalo, Psique
  e Têmis;
- quatro jornadas Playwright sequenciais em desktop, iPhone 15 Pro e Pixel 7: execução independente,
  tarefas da atividade, controles humanos/backend e histórico da cadeia;
- Spotless nos seis módulos Java, Prettier nos arquivos frontend alterados, validação dos schemas
  JSON, includes relativos do Liquibase e integridade do diff.

As fixtures permaneceram segregadas como dados locais de QA. As duas rodadas terminaram com zero
publicação, campanha, contato, gasto, pagamento e venda. As tags Docker temporárias das sete imagens
foram removidas após a inspeção e continuam integralmente reproduzíveis pelos Dockerfiles do
repositório.
