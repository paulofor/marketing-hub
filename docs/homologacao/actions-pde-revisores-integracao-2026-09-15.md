# Actions — integração de contratos, revisores e proxy PDE

Data: 2026-09-15. Base local: `e393a2f39262e9ed191037a8116f930f49da4fe4`.

## Evidências e escopo

- Psique (`34920452331`) e Têmis (`34920452353`) falham ao empacotar a atestação v7
  do Rigel: o hash de `ProductCatalogService.java` é anterior à correção de resolução
  de versões. O diff `31958c1c...b6134124` confirma a mudança no catálogo.
- Backend CI (`34919979733`): `LearningCycleVideoEvidenceTest` falha ao publicar um
  contrato de teste sem slug, recusado pelo novo controle de identidade. Foram
  executados 3.030 testes, com um erro e nove ignorados.
- PDE (`34919979706`): o teste Docker de troca do backend retorna HTTP 500 ao
  autorizar materiais, quando espera 403. A causa do proxy será comprovada localmente.
- Histórico comparado: Psique `34900097413`, backend `34877695060` e PDE
  `34717183131` concluíram com sucesso antes dessas falhas.
- Referência de recorrência: `LOOP-PDE-REVISAO-MANIFESTO-GLOBAL-MUTAVEL`. A matriz
  anterior de versões testava apenas uma seleção do backend e não os revisores nem
  a autorização real de materiais no proxy.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
|---|---|---|---|
| Corrigir cada check isoladamente | Diagnóstico inicial rápido | Mantém lacunas entre módulos e exige novas correções | Insuficiente |
| Revalidar o catálogo, preservar atestações anteriores e executar uma matriz comum | Resolve as falhas relacionadas e verifica compatibilidade antes da entrega | Esforço moderado, testes locais de integração | Escolhida |
| Separar completamente os catálogos por produto | Reduz dependências compartilhadas | Refatoração ampla; ainda exige validar dependências comuns | Fora do ajuste necessário |

## Matriz definida antes da execução

| Área | Critério de aceite |
|---|---|
| Reprodução | Repetir localmente as três falhas e registrar seus diagnósticos |
| Backend principal | Suíte completa, incluindo ciclo de vídeos, identidade e publicação de contratos; empacotamento consistente |
| Catálogo PDE | Suíte completa; versões v5/v6/v7 independentes; Rigel e Mira preservados |
| Revisores | Suítes Java de Psique e Têmis; pacotes reais íntegros, segregados e com histórico preservado |
| Proxy | Nginx real, mudança de IP, materiais com e sem autorização antes/depois da troca, URI/query/POST e isolamento entre produtos |
| Jornadas | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; contratos versionados e materiais locais |
| Persistência | MySQL 5.7 descartável, recuperação existente e idempotência sem alterar migrações históricas |
| Observabilidade | Logs preservados inclusive na falha e resultado por controle; falhas de identidade continuam bloqueadas |
| Métricas e dados | Somente fixtures locais e QA_INTERNAL; nenhum dado de venda, pagamento, IA paga ou credencial produtiva |
| Prevenção | Matriz versionada passa a incluir os controles ausentes e testes de contrato protegem sua execução |
| Entrega | Duas rodadas locais completas e consecutivas sem falhas depois da última correção; diff revisado e topologia removida |

## Resultado

### Causas reproduzidas e correções

1. **Backend:** os 29 casos de `LearningCycleVideoEvidenceTest` reproduziram o único
   erro do Actions. A fixture de publicação agora declara seu slug antes da revisão.
   O teste confere slug, versão, layout e preservação dos vídeos após publicar. O
   controle de identidade do código de produção permanece obrigatório.
2. **Revisores:** o empacotador real reproduziu o hash divergente. Os 176 testes do
   catálogo PDE passaram; uma atestação v8 do Rigel registra a compatibilidade da
   alteração de resolução versionada, incluindo seu teste de recusa. V5/v6/v7
   permanecem imutáveis e a v8 não equivale a autorização comercial.
3. **Proxy:** o mesmo código passou no run posterior `34920452262` e em seis
   execuções locais. Isso descartou a hipótese de configuração permanentemente
   inválida. Uma conexão mantida no worker que consultou o backend antigo reproduziu
   **500 na autorização**, enquanto a API em outra conexão já respondia pelo backend
   novo. O teste anterior encerrava a espera apenas pela API, antes de comprovar a
   atualização do DNS da autorização. A fixture agora preserva essa conexão,
   comprova autorização antes e depois da troca e aguarda somente erros transitórios
   dentro do prazo; acesso anônimo/inválido aprovado ou falha persistente reprova.
   Os logs dos containers são emitidos antes da limpeza se houver falha.

Alternativas para o proxy: espera fixa (baixo esforço, continua dependente da velocidade
do runner); mudar IP/DNS de produção (esforço e risco desnecessários, dado o sucesso
posterior com o mesmo código); testar a recuperação das duas rotas com prazo e conexão
preservada (baixo esforço, reproduz a falha e mantém as exigências). Escolhida a terceira.
Referências de contrato: [resolução do proxy Nginx](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)
e [códigos da autorização por subrequest](https://nginx.org/en/docs/http/ngx_http_auth_request_module.html).

A matriz de versões passa a executar o backend completo, conferir o JAR e chamar a
matriz compartilhada de evidências, revisores, navegação e proxy. Node 22 é exigido,
como no CI dos revisores. O teste do contrato de CI impede o retorno à seleção parcial.

### Resultado final

**Duas rodadas completas consecutivas aprovadas**, `validated-1` e `validated-2`,
com os mesmos arquivos: **19/19 gates principais**, incluindo a chamada da matriz
compartilhada, e **15/15 controles dessa matriz** em cada rodada.

| Validação por rodada | Resultado |
|---|---|
| Backend principal completo | 3.030 casos: 3.021 aprovados, nove condicionais/desabilitados, zero erros/falhas |
| Backend PDE | 176 aprovados; a matriz compartilhada repete essa suíte |
| Têmis | 91 aprovados e um teste condicional ignorado |
| Psique | 106 aprovados e um teste condicional ignorado |
| MySQL 5.7 | Um teste físico aprovado: reparo histórico, HTTP, idempotência, preservação e rollback |
| Pacotes dos dois revisores | 122 arquivos e 13 manifestos em cada pacote; 20 execuções do carregador Java aprovadas |
| Contrato do empacotador | 11 testes aprovados, incluindo corrupção, evidência ausente e seleção de revisão |
| Navegação e APIs | 27 casos Playwright aprovados: nove smokes de versões em desktop/iPhone/Pixel, três diagnósticos HTTP, nove jornadas do Rigel e seis verificações de analytics |
| Imagem | Têmis construída pelo Dockerfile versionado; integridade do pacote dentro da imagem e runtime visual confinado aprovados |
| Proxy | Mesmo cache antigo reproduzido; autorização recuperada, tokens inválidos/ausentes recusados, legado recuperado e produtos isolados |
| Entrega técnica | JAR consistente com classes compiladas, Actionlint fixado, ShellCheck, contratos de workflow, formatação e diff aprovados |

Os testes condicionais pertencem a replays e matrizes específicas de outros fluxos;
nenhum foi desabilitado nesta correção. O teste MySQL de contrato versionado, ignorado
na execução unitária geral por depender de ambiente dedicado, foi executado depois
com banco descartável nas duas rodadas.

As falhas posteriores também foram conferidas: `34920452305` repete o mesmo erro
da fixture do backend; as continuações de Psique/Têmis `34921179455` e `34921179478`
preservam o bloqueio de seus runs de origem. Os reconciliadores `34921179511` e
`34921200538` informam corretamente que a publicação principal falhou. Não são
novas causas e seus controles não foram removidos.

Evidências locais: `artifacts/actions-pde-2026-09-15/summary.json`,
`validated-code-hashes.json`, logs de reprodução e diretórios das duas rodadas.
Os arquivos de código/configuração/teste mantiveram os mesmos hashes entre as
rodadas; depois delas este relatório recebeu apenas os resultados. As atestações
v5/v6/v7 e todos os changelogs antigos continuam byte a byte iguais ao `HEAD`.

Com Java 21, Node 22 no PATH, Python 3, ShellCheck e Docker/Compose disponíveis:

```bash
PDE_CONTRACT_COMPOSE_PROJECT=<projeto-exclusivo-da-sandbox> \
PDE_CONTRACT_MYSQL_HOST=sandbox-docker \
PDE_CONTRACT_EVIDENCE_DIR=artifacts/actions-pde/rodada-1 \
  bash scripts/run-docker-homologation.sh \
  bash infra/testing/pde-version-contract/run-round.sh
```

Usar `127.0.0.1` no lugar de `sandbox-docker` se a engine for local ao host. O runner
constrói as imagens pelos Dockerfiles versionados, usa serviços e credenciais
sintéticos e não executa publicação nem chamadas pagas. Os containers, redes e
volumes do projeto `aihub-c26bde72-016e-4a8a-b068-bdf1a3646751-faac5e8c04` foram
removidos; o wrapper também retirou as seis imagens temporárias de cada rodada.

**Mudanças concluídas na sandbox, sem commit, push, PR, reexecução de Actions ou
deploy.** As execuções históricas do GitHub permanecem com o resultado do código
anterior; esta entrega depende do fluxo de publicação do usuário para chegar ao
repositório remoto.
