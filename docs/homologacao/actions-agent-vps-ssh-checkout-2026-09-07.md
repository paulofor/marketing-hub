# GitHub Actions dos agentes — SSH e checkout limpo

## Evidência e escopo

- Revisão investigada: `6b427dce0c7f75e58b3622a7c4ebbce86578644e`.
- [Argos, run 34072568864](https://github.com/paulofor/marketing-hub/actions/runs/34072568864): testes e imagem aprovados; configuração SSH encerra com código 2, antes de conectar, porque exige `-x` de `ssh-keyscan-with-retry.sh`, versionado como `100644` e chamado com `bash`.
- [Psique, run 34072568894](https://github.com/paulofor/marketing-hub/actions/runs/34072568894) e [Atena, run 34072568888](https://github.com/paulofor/marketing-hub/actions/runs/34072568888): testes aprovados; SSH recusa a chave prioritária antes de medir disco. Os dois ainda usavam apenas `GROWTH_OPERATOR_VPS_SSH_KEY`.
- O [run anterior de Argos 34061360046](https://github.com/paulofor/marketing-hub/actions/runs/34061360046) concluiu também o deploy. No [run de Psique 34068288058](https://github.com/paulofor/marketing-hub/actions/runs/34068288058), a autenticação funcionou inicialmente e falhou na sonda final. Não há evidência suficiente para atribuir a mudança da autorização a disco, containers ou a um segredo específico.
- Reprodução local anterior à correção: checkout real retorna `Erro: helper de coleta da chave do host indisponível.`; o teste anterior ocultava a condição ao substituir o helper por arquivo com modo `0700`.
- Inspeção operacional: `vps_host_inventory` recebeu recusa de autenticação e erro ao gravar seu `known_hosts`; `sandbox-ssh root@163.245.202.80 true` foi bloqueado com código 77, destino não autorizado. Nenhuma proteção foi contornada.

## Decisão

| Alternativa | Benefício | Risco / custo | Decisão |
|---|---|---|---|
| Alterar apenas o bit executável | Mudança mínima | Mantém exigência incompatível com chamada via Bash e autenticação divergente entre agentes | Descartada |
| Exigir arquivo legível, compartilhar o preflight autenticado e testar SSH real local | Corrige o defeito reproduzido e cobre os nove publicadores do mesmo host | Alteração moderada nos workflows; autorização real do host ainda depende de acesso operacional | Escolhida |
| Redistribuir containers entre hosts | Pode aliviar capacidade quando houver saturação comprovada | Exige inventário, estado, volumes, portas e credenciais; indisponíveis no host de origem; maior risco operacional | Sem evidência que justifique migração |

O gargalo desta solicitação é a publicação interrompida antes do acesso ao host. A métrica local é a matriz completa sem falhas; a validação externa é autenticação seguida dos gates de capacidade e saúde. Continuar com evidência local, ajustar qualquer defeito causalmente relacionado e bloquear mutações remotas sem autenticação. Nenhum resultado técnico conta como venda.

## Matriz definida antes da implementação

| Critério | Validação local | Resultado esperado |
|---|---|---|
| Checkout | Helper Bash legível sem bit executável; arquivo ausente/diretório | Aceitar o arquivo regular legível; bloquear dependência inválida |
| Autenticação | Chave primária e cada fallback, chaves reais descartáveis e servidor OpenSSH local | Conectar somente com identidade autorizada, sem interação |
| Falhas | Ausência, chave inválida, chave cifrada, recusa total e coleta da chave do host sem resposta | Falhar antes de sinalizar autorização; remover arquivos temporários |
| Integrações | SSH, SCP e rsync com a configuração que o preflight produziu | Mesmo contrato autenticado em todas as operações |
| Identidade do servidor | Chave de host divergente depois do preflight | Conexão recusada; não desabilitar verificação |
| Nove publicadores | Contrato de preflight, parâmetros de transporte, ordem, fila e sonda final | Nenhuma escrita antes do preflight; limpeza final ignorada quando não autenticado |
| Capacidade e preservação | Contratos locais de disco/fila e proteção de imagens/containers/volume | Bloquear insuficiência e preservar dados e rollback |
| Sintaxe e regressão | Bash, Actionlint com ShellCheck e testes do Product Discovery Worker | Sem erros |
| Observabilidade e segregação | Diagnósticos, flags, logs e limpeza de chaves/topologia | Apenas credenciais sintéticas, sem tráfego para produção e sem eventos comerciais |
| Navegadores/dispositivos | Não aplicável: somente scripts de publicação e contratos SSH | Nenhuma mudança de interface ou jornada comercial |

Depois da última correção, executar duas rodadas locais completas e consecutivas sem falhas. A conexão ao VPS com os segredos reais do GitHub permanece fora da homologação local; não publicar para descobrir se existe uma credencial ainda autorizada.

Referência do contrato do cliente: [OpenSSH — ssh_config](https://man.openbsd.org/ssh_config), opções `BatchMode`, `IdentityFile`, `IdentitiesOnly` e `StrictHostKeyChecking`.

## Resultados

Depois de corrigir o contrato de permissão, os validadores existentes de sincronização foram
atualizados para exigir também a configuração SSH autenticada. O contrato novo passou a reconhecer
comandos divididos por continuação de linha; o aviso ShellCheck de variável não utilizada na
fixture foi corrigido antes de iniciar as rodadas finais.

| Verificação final | Rodada 1 | Rodada 2 |
|---|---:|---:|
| Matriz completa, incluindo verificações de sintaxe | 27/27 | 27/27 |
| Testes unitários de Argos | 124/124 | 124/124 |
| Identidade principal e três fallbacks, com SSH/SCP/rsync reais | Aprovado | Aprovado |
| Chave cifrada, recusa total, limpeza e identidade de host divergente | Aprovado | Aprovado |
| Contratos dos nove publicadores, fila, capacidade e resiliência | Aprovado | Aprovado |
| Identidades Codex, sincronização por módulo, gates de versão e saúde | Aprovado | Aprovado |
| Preservação de imagem, tag de rollback, containers ativo/parado e volume em Docker real | Aprovado | Aprovado |
| Bash, ShellCheck 0.9.0 e Actionlint fixado pelo repositório | Aprovado | Aprovado |
| Tempo total da matriz local | 41,79 s | 37,78 s |

As duas rodadas foram consecutivas, sem alterações de código entre elas. O erro histórico foi
reproduzido antes da correção. Nenhuma fonte Java ou changelog foi alterado.

Comandos de reprodução dos critérios principais (nos dois últimos, substituir o identificador pelo projeto exclusivo da sandbox atual):

```bash
bash scripts/test-configure-vps-ssh-fallback.sh
node scripts/test-agent-vps-ssh-workflows.mjs
bash scripts/validate-premium-agents.sh
bash scripts/run-actionlint.sh
bash scripts/test-shared-vps-deploy-queue.sh
bash scripts/test-shared-vps-deploy-resilience.sh
bash scripts/test-agent-vps-disk-space.sh
node scripts/test-agent-vps-disk-workflows.mjs
npm --prefix product-discovery-worker test
VPS_SSH_TEST_COMPOSE_PROJECT="projeto-exclusivo-da-sandbox" bash scripts/test-configure-vps-ssh-fallback-e2e.sh
AGENT_VPS_DISK_TEST_COMPOSE_PROJECT="projeto-exclusivo-da-sandbox" bash scripts/test-agent-vps-disk-space-e2e.sh
```

As fixtures usaram imagens criadas pelo Dockerfile e pelos wrappers versionados, sem rede externa
durante a execução, credenciais reais, bind mounts, acesso ao banco ou eventos comerciais. As
topologias foram encerradas com `down --volumes --remove-orphans` no projeto exclusivo
`aihub-8dad078b-63cb-482d-8a64-840d6186cad9-ae3a104d17`; suas referências temporárias de imagens foram
removidas pelos wrappers.

**Pendente externo:** o acesso ao VPS canônico ainda precisa ser restaurado para investigar a
mudança de autorização da chave e medir sua capacidade atual. Duas tentativas MCP confirmaram
recusa de autenticação; o helper da sandbox recusou o destino. HTTP 200/UP de Argos e Psique prova
resposta das aplicações, mas não comprova capacidade para novos builds nem aceitação dos fallbacks
reais do GitHub. Mover containers sem esse inventário não foi justificado pelas evidências.

Nenhum commit, push, PR, deploy ou reorganização produtiva realizado.
