# Coordenação de deploys durante intervenções — v1

## Problema comprovado e escolha

Em 11/09/2026, o run `34592882916` publicou o SHA
`13b46548a455d3421b38719af224b8d3cf847bd1` durante a conferência privada do Vega e
restaurou o contrato anterior. Fonte primária local:
`docs/homologacao/vega-ciclo2-validacao-correta-v1.md`, seção de interferência concorrente.
A API do GitHub confirmou run bem-sucedido e publicação entre 11:47 e 11:52 UTC.
Os workflows possuem fila entre Actions, mas os publicadores manuais não participam dela.

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Ampliar a fila dos Actions | Mudança pequena | Não inclui a intervenção externa | Não resolve a causa |
| Trava consultada por cada novo workflow no host | Controle fino por etapa | Exige adaptar todos os publicadores e não alcança os runs antigos | Evolução possível |
| Pausar os publicadores envolvidos e drenar suas execuções | Protege inclusive workflows antigos, sem consumir runner em espera | Suspende temporariamente o CI combinado desses módulos; exige retomada registrada | Adotada |

## Matriz definida antes dos testes

Todos os testes de mutação usam API GitHub simulada, estado temporário e processos locais.
Nenhuma campanha, tarefa de produto, métrica comercial, IA paga ou publicação participa deles.
A interface deste fluxo é uma CLI; navegadores e dispositivos não se aplicam.

| Controle | Critério |
| --- | --- |
| Caminho feliz | Pausar escopo, drenar, proteger, comprovar integração e restaurar estado anterior |
| Escopo | Backend inclui Argos, Psique e Íris dependentes; demais publicadores ficam intactos |
| Concorrência | Dois operadores não adquirem a mesma intervenção; perda de sessão mantém estado persistido |
| Fila | Run iniciado ou pendente impede ACTIVE; nenhuma transação remota é cancelada |
| Fila obsoleta | Descarte explícito alcança só APP pending/queued sem jobs; reconfirma estado e pode preservar run existente da main atual |
| Falha parcial | Erro de pausa, consulta ou retomada deixa estado recuperável; jamais libera silenciosamente |
| Retomada | ID incorreto, commit ausente de main ou evidência ausente são recusados |
| Estado anterior | Workflow previamente desativado permanece desativado; nenhum run antigo é reexecutado |
| Observabilidade | Histórico inclui ator, motivo, versão, SHA, workflows e runs; sem tokens ou payloads de secrets |
| Transporte | Controle remoto usa helper SSH autorizado, lock real e estado fora do rsync |
| Contratos | Inventário cobre os publicadores selecionáveis e dependências reais; CI executa a regressão |

Se a rodada encontrar defeito, corrigir e executar duas rodadas completas consecutivas sem
falha após a última correção. Limitações e operação real serão registradas ao final.

## Procedimento operacional

1. Confirmar a autorização já existente e finalizar investigação, implementação e testes locais.
2. Abrir a proteção com os componentes que serão alterados. `app` inclui o publicador central
   (backend/frontend e vídeo no mesmo workflow), Argos, Psique e Íris. `pde` inclui o publicador
   PDE e a recuperação de proxy. Os demais agentes são selecionáveis individualmente.
3. Guardar o `id` retornado. `DRAINING`/exit 75 significa que ainda não é seguro intervir.
   Consultar `status` e repetir `protect --id ...` após as execuções existentes terminarem.
4. Se necessário, executar `discard-unstarted --id ... --keep-run ...` para retirar revisões
   antigas ainda sem jobs da fila APP e conservar o run já existente da main atual. Não força
   cancelamento de transações em execução, não cancela filas de agentes e não dispara builds.
5. Em `ACTIVE`, executar os comandos já autorizados sob `execute`, mantendo a exclusão entre
   operadores durante toda a alteração e a conferência. O registro permanece aberto entre comandos.
6. Ao encerrar a homologação e identificar o commit exato validado, registrar `prepare-resume`
   com o commit e a evidência correspondente, mesmo que ainda aguarde integração pelo PR.
   O reconciliador chama `resume` após comprovar a integração e recupera eventos perdidos.
   Workflows previamente desativados permanecem desativados. A preparação encerra comandos
   operacionais naquela intervenção; não registrar homologação ainda incompleta.
   O comando manual `resume` permanece compatível para recuperações explícitas do histórico.

Exemplo para uma intervenção autorizada no Vega (preencher os campos com dados reais):

```bash
python3 scripts/coordinate-deploy-intervention.py begin \
  --scope app --scope pde --scope dedalo --scope psique \
  --owner 'responsável pela intervenção' \
  --reason 'preservar a versão privada durante a homologação' \
  --authorization 'referência à autorização do usuário' \
  --protected-version 'Vega / ciclo 2 / versão homologada'

python3 scripts/coordinate-deploy-intervention.py protect --id <id>
python3 scripts/coordinate-deploy-intervention.py execute --id <id> --scope app \
  -- sandbox-ssh root@191.252.181.168 docker ps

python3 scripts/coordinate-deploy-intervention.py prepare-resume --id <id> \
  --validated-commit <sha-completo-validado> \
  --evidence 'referência ao relatório de validação aprovado'
```

O controle usa Python padrão, `gh` já autenticado e `sandbox-ssh` com host key fixada.
Nenhum token é lido ou salvo pelo coordenador. Registros ficam em
`/var/lib/marketinghub/deploy-coordination` no host administrativo, separados do rsync:
`current.json` e um snapshot histórico por intervenção. Motivos e evidências não devem conter secrets.
O código de controle é executado em memória via SSH; não instala scripts, imagens ou aplicações no host.

Os workflows combinados de CI/deploy selecionados ficam temporariamente desativados, inclusive
seus gatilhos de teste. `Backend CI`, `Frontend CI`, contratos e demais workflows independentes
continuam ativos. A disciplina se aplica a todos os operadores deste repositório por `AGENTS.md`;
ela não é uma barreira de segurança contra um administrador que reative workflows por fora.

Documentação oficial consultada: [pausa de workflows](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/disable-and-enable-workflows),
[API de workflows](https://docs.github.com/en/rest/actions/workflows) e
[estados e paginação de runs](https://docs.github.com/en/rest/actions/workflow-runs#list-workflow-runs-for-a-workflow).

## Defeito encontrado durante a homologação local

A primeira versão preservava a pausa após queda da sessão, mas liberava o lock de transporte
enquanto um subprocesso iniciado podia continuar. O teste de interrupção reproduziu uma retomada
indevida. A correção persiste `OPERATING` antes do comando e exige resultado ou reconciliação
com evidência do término no host. Esse teste passa a integrar todas as rodadas completas.

A consulta operacional também revelou o run `31127637345`, de 06/08/2026, em `queued`,
sem nenhum job. O descarte tratava apenas `pending`. Esse cenário foi reproduzido localmente
e passou a aceitar também `queued`, mantendo a exigência de zero jobs e reconfirmação de estado.
As duas rodadas completas foram reiniciadas depois desse ajuste.

## Resultado local

Duas rodadas completas consecutivas (`artifacts/deploy-intervention/final1` e `final2`),
com o mesmo SHA-256 de todos os arquivos de implementação, testes e contrato de CI:

| Controle executado em cada rodada | Resultado |
| --- | --- |
| Coordenação, API, estado persistido, interrupção e concorrência de processos | 33/33 testes Python |
| Continuação dos agentes vinculada ao mesmo SHA da aplicação | 17/17 testes Node |
| Actionlint na revisão fixada pelo repositório | Todos os workflows válidos |
| Filas compartilhadas dos hosts | Contrato aprovado |
| Deploy transacional e leitura de revisões | Contratos aprovados, incluindo saúde e rollback |

Total: **5/5 controles por rodada**, incluindo **50 testes por rodada**. Relatórios, durações
e hashes estão em `report.json` de cada diretório. Não houve alteração em Java, banco, produto
ou interface. Os testes não precisaram de imagens Docker nem de serviços externos reais.

## Aplicação operacional em 11/09/2026

Após homologação e revisão local, iniciou-se a intervenção
`fc8e02cab70a49ea96bfc88839a2caa7`, com `app`, `pde`, `dedalo` e `psique`.
Os sete publicadores estavam ativos e foram pausados; seus estados anteriores estão preservados
no registro remoto. Workflows de CI independentes permaneceram ativos.

A fila revelou quinze runs APP antigos sem jobs, entre eles `31127637345`, de agosto.
Foram retirados pelo comando validado e tiveram os estados reconsultados. Preservados:

- `34595792467`: já em execução, sem interrupção da transação de publicação.
- `34606219815`: run existente da `main` atual, SHA
  `f3ce903637dd1de06d9ee1cdea6d3a217314a74e`, preservado explicitamente.

A comparação de `b5e401afa` com esse SHA confirmou ancestralidade e somente documentos de
pesquisa adicionais: a correção anterior do Vega já está incorporada à `main` preservada.
O coordenador não disparou novo workflow, dispatch, PR, envio de imagem ou deploy manual.
As publicações que já estavam em curso/na fila seguiram o pipeline existente antes da
confirmação de ACTIVE.

Evidências operacionais em `artifacts/deploy-intervention/production`: `begin.json`,
`queue-cleanup.json`, `queue-cleanup-final.json`, `workflows-paused.txt` e
`integrated-code-comparison.json`. O encerramento e a conferência final são registrados a seguir.

O cancelamento efetivo dos quinze runs foi confirmado pela API (`cancelled-runs-confirmed.json`).
O run anterior `34595792467` concluiu normalmente. O run preservado `34606219815` concluiu
com **success**, publicando a revisão integrada `f3ce903637dd1de06d9ee1cdea6d3a217314a74e`.
A proteção chegou a **ACTIVE**, com sete publicadores pausados e zero runs pendentes, em
`protected-active.json`.

A conferência real foi executada pelo próprio `execute`, sob lock, sem criar tarefas ou sessões:

- MCP `runtime_build_info`: backend em `f3ce903637dd1de06d9ee1cdea6d3a217314a74e`, branch main.
- Backend `/api/pde/vega/private/v1/contract`: `musa-pde-entry-v11-primeiro-ajuste-aplicavel`.
- Saúde: `UP`; contrato privado sem cobrança e com mídia em zero.
- Resultado: **APPROVED**, em `runtime-validation.json`.

A v11 ficou confirmada pelo pipeline já existente. A imagem anterior de recuperação permaneceu
disponível; não foi necessário restaurá-la manualmente. Nenhuma imagem nova foi construída
ou publicada manualmente pela sandbox.

Após essa conferência, `resume` confirmou a ancestralidade do commit e restaurou os sete
publicadores que estavam ativos antes da intervenção. O estado final é **RELEASED**,
registrado em `released.json`; uma consulta independente confirmou os sete como **active**
em `workflows-final.json` e `final-summary.json`. Nenhum workflow permanece pausado por esta
intervenção. O registro remoto e o histórico de cada decisão foram preservados.

O coordenador, a regra em `AGENTS.md` e sua regressão no CI permanecem na worktree, revisados
e validados, para integração pelo PR do usuário. Nenhum commit, push ou PR foi criado por esta
execução. Não houve topologia Docker temporária nem processo de monitoramento deixado ativo.
