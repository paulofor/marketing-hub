# Matriz de homologação — preflight Runway e Plutus v1

Data: 2026-09-03

## Objetivo

Comprovar localmente que nenhum job pago de Apolo é criado antes de o Marketing Hub conhecer a conta agregadora, consultar saldo e limites oficiais, executar o `dryRun` do mesmo payload, reservar preventivamente o teto e obter o parecer de Plutus.

## Dados de teste e segregação

- Backend, MySQL 5.7, workers e frontend executados somente na sandbox.
- Runway substituída por servidor HTTP local; nenhuma chave, crédito ou geração real será usada.
- Ciclos, contas, snapshots, reservas e jobs usam identificadores exclusivos da homologação.
- Nenhuma campanha, publicação, compra de créditos ou chamada paga faz parte desta matriz.

## Cenários obrigatórios

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Criar ciclo final 9:16, receber organização com saldo/quota, executar `dryRun`, reservar preventivamente e Plutus aprovar | Um único job é criado depois do parecer; payload real é idêntico ao preflight, exceto por `dryRun` |
| Rejeição financeira | Plutus rejeitar um preflight apto | Reserva preventiva é liberada sem consumo e nenhum job é criado |
| Prova do dry run | Resposta do router omitir `dryRun: true` | Callback recusado; nenhuma evidência de simulação é aceita como autorização financeira |
| Roteamento | Duas cenas retornarem modelos/fabricantes distintos via Runway | Agregador e conta permanecem Runway; fabricante/modelo ficam registrados por cena |
| Segregação | Adapter Runway receber conta pertencente a outro agregador | Bloqueio antes de ler credencial ou chamar API externa |
| Custo-benefício | Rotas elegíveis apresentarem custo e qualidade distintos | Plutus recebe custo total, perfil de otimização, limites e custo esperado por material aprovado |
| Saldo desconhecido | Organização omitir saldo ou snapshot vencer | Ciclo bloqueado; nenhum parecer aprovável, reserva ou job |
| Saldo insuficiente | Saldo disponível, descontadas reservas, ser menor que o `dryRun` | Reserva recusada atomicamente e nenhum job |
| Quota | Uso diário atingir o limite ou concorrência ser zero | Preflight bloqueado com causa e ação visível |
| Elegibilidade | Router responder `no_eligible_model` | Ciclo bloqueado com filtros responsáveis e sem consumo |
| Configuração | Configuração de router não existir ou credencial faltar | Ciclo bloqueado com instrução operacional, sem fallback pago |
| Homologação | Router selecionar modelo sem status, licença, preço ou QA vigentes no catálogo | Plutus recebe o bloqueio e nenhum job é criado |
| Teto duro | Estimativa caber no ciclo, mas soma de `priceCeiling` não caber | Plutus rejeita; nenhuma reserva ou geração é criada |
| Concorrência | Dois ciclos tentarem reservar o mesmo saldo | Lock da conta permite apenas reservas cobertas pelo saldo oficial |
| Expiração | Job não iniciar antes da validade da reserva | Nenhuma chamada paga; reserva sem consumo é liberada sob lock para ciclos futuros |
| Idempotência | Worker ou Plutus repetirem callback | Um snapshot, uma reserva e um job por ciclo |
| Retomada de Plutus | Auditoria do modelo persistir e callback funcional falhar | Próximo polling reutiliza a resposta bruta; nenhuma segunda chamada de IA |
| Reconciliação | Job falhar por crédito, custo ou provider | Reserva não autoriza substituições pagas automáticas; causa fica persistida |
| Preços | Plutus consultar catálogo interno sem tenant | Pesquisa recebe candidato; rota administrativa continua protegida por tenant |
| Observabilidade | Consultas oficiais, `dryRun`, parecer, reserva e job | URLs, status, hashes, custos, correlação e resposta sanitizada persistidos/logados; segredo ausente |
| Drift faturável | Resposta paga mudar modelo, configuração, preferência, teto ou custo | Task aceita fica auditada e as cenas restantes são bloqueadas |
| Métricas | Consultar relatório do ciclo | Exibe saldo oficial, reservado/disponível, quota, custo previsto/realizado e decisão |
| Compatibilidade | Conta futura de outro agregador | Contrato aceita nova conta/rota sem alterar a governança do ciclo |
| Desktop | Chromium em viewport desktop | Preflight legível, botão com spinner e nenhum overflow horizontal |
| Mobile | Chromium em iPhone 15 Pro e Pixel 7 | Saldo, quota, recomendação e bloqueio legíveis; ações utilizáveis por toque |

## Resultado executado

Durante a primeira tentativa, a homologação revelou dois defeitos sistêmicos: o serviço de preflight ocupava o pacote reservado à fachada canônica do ciclo e a navegação móvel podia gerar overflow. As responsabilidades foram separadas, a consulta financeira foi encapsulada e o layout responsivo foi corrigido. A contagem foi então reiniciada.

Após a última correção, duas rodadas locais completas e consecutivas terminaram sem falhas:

| Validação por rodada | Rodada 1 | Rodada 2 |
|---|---:|---:|
| Backend — Maven, Spotless, testes unitários, integração e ArchUnit | 2.287 testes; 0 falhas | 2.287 testes; 0 falhas |
| Financial Agent Worker | 26 testes; 0 falhas | 26 testes; 0 falhas |
| Video Management Service | 100 testes; 0 falhas | 100 testes; 0 falhas |
| Frontend — Vitest, TypeScript e build | 468 testes; 0 falhas | 468 testes; 0 falhas |
| Liquibase físico em MySQL 5.7 | 8 changesets, interrupção e reaplicação aprovadas | 8 changesets, interrupção e reaplicação aprovadas |
| GitHub Actions | 38 workflows aprovados no Actionlint | 38 workflows aprovados no Actionlint |
| Imagens versionadas | backend, frontend e dois workers construídos | backend, frontend e dois workers construídos |
| Jornadas visuais | desktop, iPhone 15 Pro e Pixel 7 aprovados | desktop, iPhone 15 Pro e Pixel 7 aprovados |

O servidor Runway foi substituído por um test double que comprovou saldo conhecido, saldo insuficiente, quota, concorrência, `dryRun`, idempotência, drift faturável e sanitização da resposta. Não houve chamada à conta produtiva, compra de créditos, geração paga ou publicação.

## Critérios de conclusão

- Zero chamadas pagas recusadas por saldo ou quota nos cenários controlados.
- 100% dos jobs pagos possuem snapshot oficial recente, `dryRun`, parecer de Plutus e reserva ativa.
- O custo previsto e o custo realizado permanecem correlacionados ao ciclo, à conta, à rota e às tasks do provedor.
- A primeira rodada completa sem defeitos conclui a homologação. Se surgir defeito e houver correção, executar duas rodadas completas e consecutivas sem falhas após a última correção.
