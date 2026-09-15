# Actions PDE — contrato independente da v5

Data: 2026-09-15. Base: `31958c1c7def36ca48ef9f55db3471c76b2109db`.

## Diagnóstico confirmado antes da correção

- O [run 34916516103](https://github.com/paulofor/marketing-hub/actions/runs/34916516103)
  publicou as imagens e passou em disponibilidade/renderização, mas o diagnóstico público
  de v5 recebeu `musa-pde-entry-v7-espelho-antes-de-sair`.
- GETs somente leitura reproduziram a divergência tanto no backend principal quanto no
  proxy PDE, por `slotCode=v5` e por `experienceVersion`. V6 e v7 retornaram suas identidades.
- MCP confirmou `pde_production_slot` v5 (id 3) ACTIVE, vinculada ao experimento 74,
  sem `published_experience_json`, rascunho ou data de publicação. V6 e v7 têm snapshots.
  O contrato global de `product` id 4 está na v7.
- A migração de 30/07 criou as colunas de snapshot sem preencher a v5. Os controllers
  públicos interpretam ausência de snapshot como autorização para usar o contrato global.
  O catálogo PDE aceita esse HTTP 200. Não é erro de download, build ou dependência.
- O [run verde 34717183131](https://github.com/paulofor/marketing-hub/actions/runs/34717183131)
  verificou somente v7. A recuperação atual selecionou `all`, revelando a v5 sem snapshot.
- `runtime_build_info` confirmou o backend PDE na revisão investigada. A consulta de logs
  por slug retornou zero linhas; HTTP, banco e código fornecem a evidência positiva.
- A revisão de impacto consultou as quatro versões ACTIVE/READY: Kit WhatsApp v1,
  MUSA v5, v6 e v7. Somente v5 estava sem snapshot; as demais tinham identidade compatível.
- Relacionado a `LOOP-ACTIONS-PDE-DIAGNOSTICO-LEGADO` e
  `LOOP-PDE-TARGETED-DEPLOY-CROSS-SURFACE-SMOKE`: um double de seleção não comprova
  resolução de contratos versionados com os dados legados.

## Alternativas e decisão

| Alternativa | Benefício | Risco/custo | Aderência |
|---|---|---|---|
| Restaurar somente o snapshot histórico da v5 | Repara o caso concreto com pouco esforço | Outra versão sem snapshot repetiria a mistura | Parcial |
| Bloquear somente contratos ausentes | Impede conteúdo e métricas de outra versão | V5 permanece indisponível | Parcial |
| Restaurar a v5 e bloquear substituições, com prova local | Recupera a versão e previne recorrência | Esforço moderado em migração, contrato e testes | Escolhida |

A restauração deve partir dos changelogs históricos da v5, jamais renomear o conteúdo v7.
Preservar snapshots/rascunhos existentes, identidade, status, experimento e métricas.
Não remover o smoke, diminuir `all` ou usar uma publicação para descobrir o próximo erro.

## Matriz local definida antes dos testes

| Área | Critério |
|---|---|
| Reprodução | Slot v5 sem snapshot e produto global v7 reproduzem a resposta incorreta |
| Recuperação | MySQL 5.7 executa Liquibase e restaura somente o snapshot ausente de v5 a partir do histórico |
| Preservação | Contratos v6/v7, outros produtos, rascunhos, vínculos e dados comerciais não mudam |
| Resolução | Slot e versão retornam somente o contrato correspondente; rota sem seletor preserva o contrato global |
| Falhas | Slot inexistente, snapshot ausente/inválido, produto/versão divergentes e seletores contraditórios são recusados |
| Integração | Controllers reais, service, contrato persistido e catálogo PDE; rejeição canônica não vira fallback local |
| Jornada | Smoke de diagnóstico real cria/consulta solicitação local para v5/v6/v7; entrada em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Migração | Reaplicação, preservação de publicação personalizada e rollback restrito ao reparo |
| CI | Testes de contrato executam antes de build/publicação; validar YAML e scripts |
| Observabilidade | Erros identificam produto/versão sem expor contrato privado; logs e resultados locais preservados |
| Segregação | Banco e serviços descartáveis, tráfego QA sem analytics, nenhuma cobrança, IA paga ou venda real |
| Conclusão | Após a última correção, duas rodadas completas consecutivas sem falhas e revisão do diff |

## Reprodução e prevenção

A suíte nova, antes da correção, executou 14 casos e encontrou 12 falhas esperadas.
O caso HTTP reproduziu a v5 sem snapshot retornando o JSON global da v7 com status 200.

A migração incremental reconstrói o valor usando as mesmas expressões SQL de seis
changelogs aplicados no histórico, de 21 a 28/07, e o layout registrado em 30/07. O teste
físico reaplica esses arquivos antigos em um produto descartável e exige igualdade
integral com o snapshot reparado. Não lê o contrato global atual para reconstruir v5.
É a recuperação do histórico versionado; não presume recuperar edições manuais que
nunca tenham sido versionadas ou preservadas em snapshot.

O reparo só preenche a v5 sem snapshot e sem metadados de publicação; não altera rascunho,
status ou experimento. O rollback exige o marcador da migração e o SHA-256 exato do
conteúdo restaurado, preservando alterações posteriores inclusive com marcador inalterado.
Nenhum changeset antigo foi editado. O master usa include relativo explícito.

O resolvedor recusa consultas versionadas sem snapshot íntegro. Os dois controllers
herdam a proteção; o publicador também rejeita identidade de outro produto antes de gravar.
O backend PDE mantém as recusas HTTP nas consultas versionadas. Consultas sem seletor
preservam a compatibilidade anterior, incluindo catálogo local quando aplicável.

O workflow Liquibase ganhou um job físico com MySQL 5.7 e a mesma fixture local.
Os testes unitários novos são descobertos pelos jobs existentes dos dois backends.
O alvo `all`, os smokes e suas verificações de identidade permanecem exigentes.

## Ambiente e escopo da prova

- Java 21, Node 20.20.2, Chromium instalado e MySQL 5.7 em engine Docker dedicada.
- Projeto Compose exclusivo: `aihub-1bb18823-cfee-4ba5-823a-10f324a1d0e9-5d0d9afb20`.
  O runner sempre encerra com `down --volumes --remove-orphans`.
- O teste físico usa MySQL real, Liquibase real e os dois controllers com o service real
  via MockMvc; a leitura do repository é simulada a partir do snapshot lido por JDBC.
- A jornada usa o backend PDE Java empacotado e o frontend construído localmente.
  O Hub é simulado: v5 recebe o JSON exportado pelo teste MySQL; v6/v7 usam fixtures
  versionadas existentes. O proxy local encaminha o Host de cada versão e o entrypoint
  real gera os diagnósticos/runtime. Não é uma publicação nem prova de produção corrigida.
- Os smokes criam/consultam três diagnósticos em armazenamento local: v5/v6 podem ficar
  PENDING, conforme contrato do teste; v7 conclui pelas regras locais. Nenhum worker pago
  foi iniciado. As navegações desabilitam analytics e não criam acesso ou compra comercial.
- Uma tentativa inicial do harness excedeu o limite de threads da sandbox com navegadores
  simultâneos. O runner passou a limitar JVM a dois processadores e Playwright a um worker,
  e encerra grupos de processos na limpeza. A matriz completa passou depois desse ajuste.
- A revisão final restringiu a nova proteção a consultas versionadas no catálogo PDE,
  preservando fallback sem seletor. As duas rodadas finais foram reiniciadas após esse ajuste.

## Execução reproduzível

```bash
PDE_CONTRACT_COMPOSE_PROJECT=aihub-1bb18823-cfee-4ba5-823a-10f324a1d0e9-5d0d9afb20 \
PDE_CONTRACT_MYSQL_HOST=sandbox-docker \
PDE_CONTRACT_EVIDENCE_DIR="$PWD/artifacts/actions-pde-v5/final-1" \
bash infra/testing/pde-version-contract/run-round.sh
```

Usar um diretório novo por rodada. Em runner com Docker local, omitir
`PDE_CONTRACT_MYSQL_HOST` (padrão 127.0.0.1). São necessárias as dependências npm,
Chromium/Playwright, Java/Maven, Docker/Compose e Actionlint; o runner instala as
dependências npm e prepara MySQL com retry restrito ao download da imagem base.

## Resultados finais

As duas rodadas completas após o último ajuste terminaram consecutivamente sem falhas,
com código idêntico entre elas:

| Validação | Final 1 | Final 2 |
|---|---:|---:|
| Backend principal: versões PDE, controllers públicos e produtos | 81 aprovados | 81 aprovados |
| Suíte completa do backend PDE e empacotamento | 176 aprovados | 176 aprovados |
| Fixture MySQL 5.7 / Liquibase / controllers | 1 aprovado | 1 aprovado |
| Consistência HTTP e runtime | 17 aprovados | 17 aprovados |
| Contratos de isolamento com Node | 5 aprovados | 5 aprovados |
| Playwright: v5/v6/v7 em desktop, iPhone e Pixel, mais diagnóstico API | 12 aprovados | 12 aprovados |
| Build, fronteira API, Shell, Liquibase estático, Actionlint, Spotless e diff | Aprovados | Aprovados |
| Limpeza de containers, rede e volumes da fixture | Concluída | Concluída |

São **292 execuções de testes por rodada, 584 no total**, além das assertivas dos scripts
de seleção, consistência, recusas HTTP e persistência. Nenhum teste foi ignorado nessas suítes.
Cada rodada contém nove navegações e três diagnósticos API, três recusas/verificações finais
de integração e três verificações de consistência contra o servidor local.

Logs: `artifacts/actions-pde-v5/final-1/` e `final-2/`; `gates.txt` registra os 13 gates,
e `journeys/results.json` registra os resultados da jornada. A integridade dos 13 arquivos
de código/configuração/testes é registrada em `validated-final-code-hashes.json`.
SHA-256 do snapshot restaurado: `c24391e13ee25e395cfa6983c055761aabf174a2481813c658d3b27e9e701439`.

A revisão final confirmou comentários de responsabilidade nos métodos/classes Java alterados,
include relativo único, ausência de edição de changesets antigos, nenhuma subconsulta na
tabela-alvo do UPDATE, preservação de rascunhos/contratos publicados e nenhuma alteração no
produto global. A proteção também atua na leitura e na publicação de identidade divergente,
fechando a causa da substituição silenciosa.

Não houve commit, push, PR, reexecução remota, alteração de dados produtivos ou deploy.
O run `34916516103` continua falho na revisão anterior. A correção só chegará ao ambiente
publicado pelo fluxo de PR executado pelo usuário; o job físico novo rodará nesse PR.
