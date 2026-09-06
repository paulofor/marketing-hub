# Deploy do VPS de agentes — capacidade de armazenamento v1

Os nove publicadores de `163.245.202.80` usam a fila `deploy-vps-163-245-202-80`,
com `queue: max` e `cancel-in-progress: false`. A lista é protegida por
`scripts/test-shared-vps-deploy-queue.sh`.

Alteração isolada no YAML de um publicador não dispara build/deploy produtivo. O contrato central
valida os workflows no PR; alterações reais nas entradas do módulo mantêm os gatilhos existentes,
e `workflow_dispatch` permite um rollout operacional explicitamente solicitado.

Antes de sincronizar código, gravar credenciais de deploy ou executar build/pull/recriação,
o job executa `scripts/ensure-agent-vps-disk-space.sh` no host, pela própria revisão versionada.
Ao final da tentativa de publicação, inclusive após falha, o job executa novamente a sonda com
`if: always()`, ainda dentro da fila compartilhada, para restaurar a reserva consumida pelo build.
O bootstrap inicial do Docker de Argos precede a sonda quando a engine ainda não existe.

- Medir o filesystem raiz, `DockerRootDir` e `/var/lib/containerd` quando existir.
- Exigir ao menos 4 GiB disponíveis e 10.000 inodes livres em cada destino; insuficiência
  depois da coleta bloqueia a atualização. `AGENT_VPS_DISK_MIN_FREE_MB` permite dimensionar
  a reserva para builds maiores sem alterar o indicador de saúde da aplicação.
- Quando faltar espaço, coletar primeiro cache sem uso há pelo menos 24 horas,
  com `docker builder prune --force --filter until=24h --keep-storage 2GB`.
  Se a nova medição continuar insuficiente, permitir uma segunda e última faixa:
  `docker builder prune --force --filter until=1h --keep-storage 1GB`. A segunda faixa
  não roda quando a primeira já devolve a reserva. Cache em uso continua protegido pelo Docker.
- Se cache não bastar, coletar imagens dangling sem container, primeiro com 24 h e depois com 1 h,
  sempre por `docker image prune` sem `--all`.
- Como última faixa, considerar somente tags imutáveis de 40 caracteres hexadecimais dos
  repositórios explicitamente conhecidos dos agentes. Preservar toda imagem referenciada por
  container ativo ou parado e as duas versões sem container mais recentes de cada repositório como
  rollback. Remover apenas a referência exata, sem `--force`, da mais antiga para a mais recente e
  interromper assim que a reserva for recomposta. Tags `latest`, `local`, `buildcache`, imagens de
  outros repositórios e identidades inválidas nunca são elegíveis.
- Não usar `--all`, prune de sistema/volumes, remoção de containers, apagamento direto de diretórios
  Docker ou alteração do limite do health para obter um resultado verde.
- Proteger a coleta por lock local e limite de 120 segundos por faixa; falha de Docker, medição,
  exclusão mútua ou coleta impede o deploy antes da troca do serviço.
- Registrar capacidade antes/depois e motivo de bloqueio. O health HTTP 503 da Psique
  deve preservar seu corpo no diagnóstico; timeout de conexão/leitura permanece limitado.
- A coleta pode exigir recompilar camadas em um build futuro; imagens ativas, duas versões de
  rollback por repositório, containers e dados persistidos ficam preservados.

Essa proteção pertence ao fluxo de publicação do VPS. A limpeza de imagens da sandbox
continua separada, conforme `homologacao-local-docker-canon.v1.md`.

Alteração de código deve passar pelo PR solicitado pelo usuário. Diagnóstico ou recuperação
operacional de cache não autoriza instalar scripts novos nem publicar aplicações por SSH.
Imagem fora da lista explícita de agentes continua exigindo revisão operacional individual da
referência imutável, origem, idade, ausência de containers e versões de recuperação; não entra na
coleta automática e nunca usa remoção forçada.

Os gatilhos de evidências comerciais permanecem ativos: quando um agente empacota documentos de
homologação ou registros auditáveis em sua imagem, a alteração desses documentos deve reconstruir
o pacote para impedir divergência entre a revisão versionada e o runtime.

Contrato e evidências: `docs/homologacao/actions-agent-vps-disk-2026-09-06.md`.
