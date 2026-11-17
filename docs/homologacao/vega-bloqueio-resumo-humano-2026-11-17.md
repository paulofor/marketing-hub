# Vega — motivo da pausa e pareceres compreensíveis

## Estado confirmado e aceite

Consulta da tela, API e MySQL via MCP em 17/11/2026: produto 4, cadeia 27,
ciclo 22, experimento 115, processo 117, execução 102 PAUSED, Têmis #921 BLOCKED.
O suplemento do PR #5840 já está publicado. A pausa genérica perdeu a justificativa
da solicitação #3958: conferir dez custos ausentes antes de nova revisão paga.
A atividade exibia prontidão técnica, confundida com execução em andamento.

## Alternativas

1. Trocar somente o texto da tela: barato, mas o frontend inventaria o motivo.
2. Bloquear toda cobertura financeira parcial: simples, mas contradiz contratos
   anteriores que aceitam envelopes e não registra a decisão específica do operador.
3. Registrar a razão da pausa no diário existente e apresentar orientação do backend:
   preserva decisões, identidade e contratos; escolhida. Somente uma pausa explicitamente
   financeira exige conciliação para retomar. Consultar não registra decisão nem gasta.

Para pareceres, foram comparados nova inferência de tradução (custo e risco de alterar
história), resumo arbitrário no frontend (perde autoridade) e síntese curta no contrato
do agente com projeção determinística para legados. Escolhida a terceira: mantém o
parecer integral e não repete chamadas pagas para traduzir resultados antigos.

## Matriz definida antes dos testes

| Área | Critério |
| --- | --- |
| Caso original | Atividade mostra pausa, motivo financeiro, responsável e link do mesmo produto/ciclo; parecer histórico permanece bloqueado e suplemento aguarda revisão |
| Decisão | Leitura não autoriza nem retoma; pausa financeira explícita impede resume enquanto houver custos ausentes; pausa comum preserva comportamento anterior |
| Isolamento | Outra identidade de produto/execução e caminho antes válido; nenhum custo ou decisão transferido |
| Resumos | Síntese curta em português, causa e próximo passo; detalhes preservados; legado identificado; JSON inválido não fabrica parecer |
| Falhas | Erro de consulta mostra erro recuperável; request inválida recusada; gravação assíncrona impede duplo envio |
| Integração | Controller → serviço → diário persistido → consulta → UI; resume protegido também no backend |
| Navegador | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; Safari físico não disponível |
| Observabilidade | Evento auditável com razão, contexto e data; testes isolados sem provedores, gastos ou métricas comerciais |
| Entrega | Diff revisado, testes locais, PR/revisão/main, workflows, saúde e tela publicada |

## Aprendizado reutilizável

Lacunas: pausa sem motivo persistido e parecer técnico usado como resumo humano.
Mudança: razão estruturada no diário existente, orientação oficial junto à atividade,
síntese curta antes da auditoria. Medir recorrência de dúvidas e intervenções; impacto
comercial desconhecido. Não cria atividade, agente, aprovação nem infraestrutura nova.
Critério de adoção: regressões acima aprovadas, sem alteração de custos ou decisões.

## Ambiente

O helper de capacidade retornou acesso não autorizado (HTTP 401). Conferência local
independente: 43 GiB livres, 50% de uso. Não foram removidos dados nem alterados runtimes.

## Resultados

1.298 casos Java conferidos nos sete módulos: 366 no backend, 143 em Têmis,
261 em Psique, 90 em Plutus, 168 em Atena, 113 em Dédalo e 157 em Íris.
Sem falhas ou erros após os ajustes. Os 22 casos opcionais condicionados a flags de integração
e arquivos externos permanecem fora do aceite; os transportes, o diário, a leitura curta e a
proteção da pausa foram executados localmente. As últimas alterações foram repetidas
somente nas suítes afetadas.

Frontend: suíte integral com 1.055 testes em 212 arquivos aprovada, além das
validações anteriores das páginas afetadas;
TypeScript conferido. Bundle gerado com a configuração produtiva do repositório,
separando a verificação TypeScript do empacotamento para evitar dois processos grandes
simultâneos na sandbox de 2,75 GiB. O checker foi retirado apenas do wrapper local,
após o mesmo `tsc --noEmit` passar. O último bundle local desativa somente sourcemaps
no wrapper após falta real de memória; código, minificação e configuração versionada
permanecem os do projeto. O CI executa o build integral com sourcemaps.

26 testes de prompts/schemas, 17 do pacote de provas e 14 do workflow passaram.
O trecho shell alterado no workflow passou em `bash -n` e ShellCheck. A atestação
sucessora Mira v86 preserva v85 e somente atualiza compatibilidade; o seletor PDE
retorna `frontend-version=none`, sem nova publicação comercial de produto.

A matriz Chromium cobre duas identidades, desktop/iPhone/Pixel, consulta curta fora
da lista leve, gravação da pausa sem retomada, parecer completo, erro e recuperação
da síntese, indisponibilidade do estado e ausência de escrita adicional ou chamada paga.
Os dados e chamadas são doubles locais, identificados como testes. O backend usa JPA/H2
real para provar diário, segregação e consulta sem hidratar prompts; não há mudança de schema.

O verificador de capacidade respondeu 401 e o recibo automático de dependências não
pôde ser registrado; dependências e fontes foram preservadas. Node/npm/npx continuam
na instalação persistente, com saúde conferida. As correções adicionais do CI usam Docker local em topologias descartáveis e segregadas;
não houve limpeza global nem intervenção nos serviços produtivos.

Nenhuma nova revisão paga foi autorizada ou iniciada. A execução produtiva continua
pausada; a entrega técnica ainda depende do PR, integração, workflows e conferência da tela.

## Falhas reais do primeiro CI e correções locais

O primeiro HEAD passou Backend CI, Frontend CI e as suítes dos oito executores.
O run MySQL 44177983235 revelou recriação do banco entre migrações nas fixtures
de tipos de produto, auditoria acionável e fronteiras dos agentes. A execução
anterior 43050699671 passara; comparar os logs confirmou perda do estado aplicado
no segundo rebuild, e não defeito no SQL de produção. Os três runners passam a
esperar o banco saudável e executar migradores sem reiniciar suas dependências.
O contrato existente do CI verifica essa preservação. A fixture Mira falhou por
HTTP 429 do Maven Central; isso é falha externa transitória identificada, não SQL inválido.

O job de TLS do run 44177983313 usou Ubuntu 26.04, depois de sucesso em Ubuntu 24.04
no run 43370693143. A criação da fixture vencida com duração negativa deixou de
funcionar. A fixture agora emite datas explícitas e extensões completas de CA/servidor,
com handshake real e conferência de que a recusa decorre especificamente de expiração.
Fontes: [OpenSSL CA](https://docs.openssl.org/3.0/man1/openssl-ca/),
[Docker Compose run](https://docs.docker.com/reference/cli/docker/compose/run/).

Foram comparadas repetição sem mudança, fixação do runner antigo e correção das
fixtures. Escolhida a correção: fecha a dependência implícita sem esconder falha nem
exigir infraestrutura adicional. Não troca o ambiente nem os serviços de produção.

A conferência do Catálogo Vivo via MCP mostrou dois schemas de parecer fixados por
hash. Uma extensão obrigatória nesses arquivos quebraria o uso do catálogo. Foram
comparadas migração dos vínculos ativos, criação de outro catálogo e preservação dos
contratos com síntese nos campos existentes. A terceira preserva os contratos: Psique
usa `customerPerspective` e Têmis `commercialRationale`; os outros 16 schemas
recebem `humanSummary`. O preflight de 26 testes protege também os dois hashes
fixados. A consulta de resumo reconhece ambos os campos existentes.
O job Opala passa a preservar o log completo para não perder a causa em um corte
do fim da saída do Maven. Não há migração Liquibase nesta entrega.

Resultados complementares locais: as três suítes físicas corrigidas passaram no
MySQL 5.7; 21 casos do catálogo passaram dentro da rede isolada da engine, reutilizando
as classes compiladas localmente (a porta da engine não é o localhost da sandbox).
Os 16 testes TLS passaram tanto no OpenSSL 3.0.22 local como no OpenSSL 3.5.7 com
Python 3.14 em container. As duas regressões de síntese do catálogo passaram; o
contrato do CI tem 15 casos aprovados. Bash/ShellCheck, YAML e pacote de provas
foram reconferidos. As topologias temporárias foram removidas pelo próprio job.

Uma tentativa inicial de fixture usou o nome padrão por variável de ambiente incorreta.
Ela foi interrompida e sua topologia removida; as rodadas finais usaram o projeto
exclusivo autorizado. A conferência final preservou somente os serviços auxiliares
preexistentes da sandbox; nenhuma publicação ou recurso produtivo foi alterado.

O contrato Java do runner de Íris também foi atualizado para exigir preservação
das dependências. Os dez testes de Íris/tipos de produto passaram localmente; os
demais testes que leem os três scripts foram pesquisados antes de atualizar o PR.

A suíte integral também revelou uma expectativa antiga do rótulo de parecer de Argos.
O teste foi alinhado à apresentação nova, mantendo as asserções sobre prompts, custo
e acesso ao JSON completo. Todos os 1.055 testes do frontend passaram localmente.
Para encurtar o feedback, a etapa existente do Frontend CI agora inclui relatórios
de processos e o aviso de pausa, além do painel de execução. Foram comparadas
correção isolada do rótulo, ampliação dessa etapa e duplicação da suíte integral
no CI principal; escolhida a ampliação da etapa, suficiente para esta classe de
regressão sem repetir toda a matriz. YAML, Bash/ShellCheck e contrato de eficiência
do workflow passaram. Não houve mudança adicional no comportamento do produto.
