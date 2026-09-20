# Preparação comercial Quartzo v1

## Escopo e evidência de partida

Em 20/09/2026, tela, API e MCP confirmaram Capella #7 como
`LOW_TICKET_DIGITAL_PRODUCT`, com preço de R$ 67. O Processo 5 v7 não oferece rota
para esse tipo. O experimento #88 pertence a Capella, está `USER_STOPPED` e não possui
ciclo de aprendizado. Sua tarefa histórica #422 foi bloqueada por ausência de URL:
o resolvedor de homologação procurava um slot PDE, apesar da página GeraSalesPage
auditada #27 existir. A hipótese de referência a outro produto foi descartada.

## Matriz definida antes da implementação e dos testes

| Área | Critério de aceite |
| --- | --- |
| Caminho feliz | Rota Quartzo → preparação com fontes reais → revisões independentes → consolidação → retorno ao pai |
| Tipo e identidade | Quartzo sem slot Opala; outro tipo, produto ou experimento rejeitado; referência explícita preservada com e sem ciclo |
| Oferta e entrega | Preço coerente; página e checkout auditados; provas e kit aprovados; personalização, prazo, acesso e reembolso avaliados |
| Falhas | Fonte ausente, URL divergente, criativo reprovado, economia vencida, retorno inválido e ativo alterado bloqueiam com causa e próxima ação |
| Integrações | Backend e callbacks reais sob teste; modelos e serviços externos simulados; filas dos agentes reconhecem o novo contrato |
| Auditoria | Entradas, saídas, versão, evidência e responsável persistidos; callback atrasado recusado; repetição idempotente |
| Métricas e dinheiro | QA segregado; nenhuma venda inventada; não alterar orçamento, janela ou estado Meta na preparação |
| Interface | Navegação, contexto e bloqueios em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Persistência | MySQL 5.7, migração incremental e reaplicação; definições e execuções históricas preservadas |
| Regressão | Opala mantém sua rota e seus gates; suítes unitárias dos módulos Java alterados e arquitetura aprovadas |

## Resultado local — 20/09/2026

- Backend: suíte completa executada; a inclusão inicial dos prompts revelou uma
  pendência no catálogo de harness, corrigida antes de publicar. As regressões
  subsequentes e os novos testes deixaram 3.293 casos reportados sem falhas/erros.
  Casos físicos opcionais são conferidos separadamente na fixture MySQL.
- Psique: 128 testes, sem falhas/erros; Têmis: 103, sem falhas/erros. Em cada worker
  um teste opcional de ambiente externo permanece não aplicável à execução local.
- MySQL 5.7: 50 testes do runner Opala/Quartzo passaram, incluindo SQL real,
  idempotência, aposentadoria por rollback e reaplicação. Topologia temporária removida.
- Frontend: 59 testes relevantes, typecheck e build passaram; navegação real em
  Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, com integrações simuladas,
  cobriu pai → filho preservando a referência sem ciclo, oito etapas, pendência,
  conclusão e retorno. Firefox/Safari nativos não foram executados.
- Estático: Spotless nos arquivos Java alterados, Prettier no frontend/fixture,
  `bash -n`, ShellCheck e validador Liquibase aprovados.

Os testes comprovaram a recusa de outro produto/experimento, revisão atrasada,
prova ausente, checkout divergente, margem vencida, critérios omitidos e aprovação
supersedida. Uma reprovação funcional fica no relatório sem ser convertida em falha
técnica ou aprovação. Consultas de vigência selecionam uma única revisão no SQL e
não hidratam os prompts históricos; o teste JPA comprova zero entidades carregadas.

Evidências locais ficam em `artifacts/capella-quartzo/` (ignorado pelo Git): logs,
relatórios de navegador e capturas. Não houve chamadas pagas a modelos, compras,
e-mails reais ou eventos de teste no funil de produção. Execuções e callbacks reais
dos serviços foram testados com repositórios/dependências externas simulados;
a migração e a consulta JPA receberam validações de persistência separadas.

## Publicação e operação

O PR #5272 foi integrado em `6e3c45e1259f8f3645c6bc659d92dde0a702f84c`; os checks
e o deploy da aplicação passaram. A inspeção funcional posterior encontrou MySQL 1792
na leitura do processo com os dados reais de Capella. A correção complementar separa
as consultas de prontidão das reservas de escrita, conforme o loop já conhecido.
Cinco testes com JPA e transações reais cobrem as quatro leituras e o comando de escrita;
a execução física usa o schema descartável `quartzo_persistence_test` no runner do PR.
Antes da correção, os quatro testes de leitura reproduziram MySQL 1792; a reserva de
escrita passou. Após a correção, os 70 testes do runner Opala/Quartzo passaram sem
falhas, erros ou skips. A regressão com arquitetura, reaproveitamento de pareceres,
continuidade de processos e persistência H2 passou em 122 casos. Spotless, `bash -n`,
ShellCheck e revisão do diff aprovados; a topologia MySQL temporária foi removida.

Pela interface foi criado o plano financeiro preliminar LIVE de Capella, mantendo custos
não comprovados como pendências. Página móvel e checkout responderam com a oferta de
R$ 67; as 16 imagens carregaram. Não foi realizado pagamento. A execução histórica #7,
bloqueada pela tarefa #422 sem URL, foi pausada pela tela antes da nova preparação.
Preparação concluída não representa campanha autorizada: custos e limites precisam
estar explicitados na fonte oficial antes de liberar divulgação paga.

## Reconciliação após gravação — 20/09/2026

O PR #5273 resolveu as leituras MySQL e permitiu iniciar o run #12 pela tela. A
instância #318 da entrada foi gravada como concluída, mas o leitor descartou sua
identidade: o contexto usa IDs Long e Jackson relê IDs pequenos como inteiros.
As fixtures anteriores também liam os IDs de texto, ocultando a diferença. Usar
os tipos reais reproduziu dez falhas em doze testes antes da correção.

O comparador agora confere inteiros positivos pelo valor, sem coerção de texto ou
fração, e preserva a validação de versão e fingerprint. A regressão local passou
em 215 casos; dois testes opcionais de migração foram executados em seguida no
runner físico. Os 72 testes Opala/Quartzo no MySQL 5.7 passaram sem falhas, erros
ou skips, incluindo comandos e reconciliação em transações separadas, retorno ao
pai e repetição idempotente. A topologia temporária foi removida.
