# Desativação temporária de executores — 18/10/2026

Autorização: desativar MDS (`177.153.62.107`), somente a FEO antiga
(`191.252.181.168`) e Scientific Research (`191.252.120.96`) para priorizar
recursos dos módulos usados. Nenhuma autorização para excluir dados, limpar logs,
publicar novos serviços ou alterar produtos.

## Resultado operacional

| Host / container | Antes | Depois |
| --- | --- | --- |
| `177.153.62.107` / `mds` | Reinício contínuo, contador 69122; imagem preservada | `exited`, `running=false`, `restarting=false`, `restart=no` |
| `191.252.181.168` / `feo` antiga | Ativa, iniciada em julho; aproximadamente 22.0 MiB RSS e 286.2 MiB em swap | `exited`, `running=false`, `restarting=false`, `restart=no` |
| `191.252.120.96` / `scientific-research-worker` | Ativa; fila científica sem execução | **Parada pendente**: `sandbox-ssh` retornou exit 77, `destino não autorizado: root@191.252.120.96` |

O terceiro host permanece consultável pelo inventário MCP; esse acesso é somente
leitura e não inclui parada de containers. A liberação operacional foi solicitada,
sem contornar a allowlist, consultar chaves privadas ou desabilitar a verificação SSH.
A FEO **canônica** nesse host continua ativa e fora da autorização de parada.

Após a parada da FEO antiga, uma comparação imediata no host administrativo mostrou
memória disponível de 182.5 para 297.1 MiB,
e swap utilizada de 521.6 para
292.7 MiB. São fotografias de todo o host,
sujeitas às atividades concorrentes, sem atribuir todo o delta exclusivamente à FEO.
MDS foi parado entre tentativas; seu ganho principal é encerrar os picos recorrentes
de CPU/memória e o crescimento do log de aproximadamente 1,6 GB. Nenhum arquivo,
volume, imagem ou log foi removido; não há recuperação de disco atribuída a limpeza.

## Coordenação e recuperação

Intervenção `e14673eb314248fcbdf158469cc73cab`, iniciada em `ACTIVE`, filas GitHub vazias.
Os comandos passaram por `coordinate-deploy-intervention.py execute`, com validação
do nome, host e identificador completo antes de cada parada. A auditoria sanitizada
foi preservada localmente e no host respectivo, fora do diretório de deploy:

- MDS: `/var/lib/marketinghub/runtime-suspensions/20261018T010850-mds-61da669cb15d.json`.
- FEO antiga: `/var/lib/marketinghub/runtime-suspensions/20261018T010710-feo-9e0ad265556b.json`.

`mds-ci.yml` e `scientific-research-worker-ci.yml` estavam ativos e foram
intencionalmente desabilitados conforme a suspensão autorizada. Esse estado foi
registrado **antes** de `begin`; a liberação da intervenção deve preservá-lo.
Não desabilitar o deploy administrativo, o reconciliador, Apolo, Psique ou FEO canônica.

A reativação futura exige restaurar conscientemente a política de restart anterior
(`unless-stopped`), corrigir a configuração do MDS e confirmar fila ou homologação
programada. Habilitar os publicadores requer autorização de retomada; não há
reativação por timeout ou encerramento desta solicitação.

## Melhoria reutilizável e validação

A lacuna observada foi a ausência de escopos MDS e Scientific Research no coordenador.
Foram incluídos `mds`, `scientific-research` e `feo-legado`; o último tem lista vazia
de publicadores porque nenhum workflow atual publica a cópia antiga. Não representa
o runtime canônico. Recuperação manual/automática dos dois workers exige o SHA
congelado e sucesso do job real de deploy, preservando workflows já desabilitados.
O build de imagem do MDS também deixa de publicar a partir de Pull Request.

Alternativas operacionais comparadas:

| Alternativa | Benefício | Limite / decisão |
| --- | --- | --- |
| Somente parar o processo | Mudança pequena | Deploy ou política de reinício pode restaurar o consumo |
| Remover containers e arquivos | Pode reduzir disco | Desnecessário para liberar CPU/RAM e prejudica recuperação/auditoria |
| Parar, desabilitar restart e preservar suspensão dos publicadores | Reversível e mantém histórico | Adotada, com escopos, lock e evidência de saúde |

Validação local antes de commit/push: 51 testes de coordenação, 60 de recuperação,
9 testes MDS, 13 FEO e 8 Scientific Research, todos aprovados; compilação Python,
`bash -n`, ShellCheck dos helpers analisados e Actionlint dos workflows alterados
aprovados. Dependências de GitHub/SSH dos testes de contrato são simuladas; nenhuma
chamada de IA, cobrança, envio de e-mail ou campanha foi usada para testar.

Homologação operacional: duas leituras confirmaram `exited` e `restart=no`;
backend e frontend administrativos, serviço de vídeo, pagamentos, PDE e páginas
públicas responderam com sucesso em duas verificações consecutivas. A sonda inicial
`/actuator/health` no backend principal retornou 404 porque a rota de observabilidade
versionada é `/ops-mh-observability-v2/health`; a rota correta respondeu `UP` duas vezes.
Os identificadores dos demais containers permaneceram preservados e suas sondas
saudáveis. Inventário MCP confirmou FEO canônica e portal ativos; o worker científico
continua ativo até a liberação operacional.

O ganho é operacional. Não há novas evidências comerciais nem atribuição de vendas
ou lucro a esta intervenção. O pedido permanece **parcial** até a terceira parada.
