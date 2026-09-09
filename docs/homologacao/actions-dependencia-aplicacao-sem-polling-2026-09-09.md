# Homologação — continuação dos agentes sem polling da aplicação

Data: 2026-09-09.

## Objetivo

Impedir que uma fila central legítima transforme Argos, Psique e Íris em workflows vermelhos,
sem publicar agente antes da aplicação, trocar o SHA testado ou ocupar a fila compartilhada do
VPS durante a coordenação.

## Evidência e causa-raiz

- Argos `34356522474` e Íris `34356522469` aprovaram testes, arquitetura, imagens e contratos.
- O Argos atual `34361476862` e Psique `34356522612` repetiram o mesmo padrão: os jobs de
  teste e imagem ficaram verdes, enquanto somente a espera pela aplicação falhou.
- Os quatro runs falharam no passo de coordenação após 2.400 segundos.
- O deploy correspondente `34356522499` permaneceu `pending`, com zero jobs; não havia falha de
  build para consumir.
- O run central `34348227054`, criado às 11:56 UTC, iniciou detecção às 16:08 UTC e o deploy às
  16:19 UTC. A espera legítima superou em várias horas o gate de 45 minutos.
- O GitHub limita jobs hospedados a seis horas, enquanto `queue: max` preserva até cem itens.
  Apenas ampliar o timeout não fecha a incompatibilidade.

## Alternativas consideradas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Timeout de seis horas | Implementação mínima | Runner ocioso; ainda expira em rajadas maiores | Rejeitada |
| Backend sucessor libera agente antigo | Compacta a fila | Compatibilidade não comprovada entre revisões | Rejeitada |
| Continuação por `workflow_run` do mesmo SHA | Espera fora do runner e revisão imutável | Exige resolver run/artefato de origem | Adotada |

## Solução

- O `push` continua executando testes, build e empacotamento imutável.
- Em até dois minutos, ele confirma apenas que `deploy-containers.yml` foi registrado para o
  mesmo SHA; workflow ausente ou desativado continua produzindo falha visível.
- O término do deploy central emite a continuação por `workflow_run`.
- O coordenador procura uma execução `push` do agente no mesmo SHA e em `main`.
- Aplicação ou testes vermelhos bloqueiam; ausência do run do agente significa que aquele deploy
  central não tinha trabalho para esse agente.
- O deploy usa checkout do SHA resolvido e, em Psique/Íris, baixa o pacote pelo ID exato do run.
- A fila do VPS, a retenção de imagens, os smokes e a recuperação manual permanecem preservados.

## Matriz local

| Área | Critério |
| --- | --- |
| Registro | Push/manual do mesmo SHA; pendente aceito; ausência e autorização inválida bloqueadas |
| Continuação | Aplicação e agente verdes; espera do teste; falhas; trabalho não aplicável |
| Identidade | Somente `main`, eventos permitidos, SHA completo e checkout imutável |
| Artefatos | Run ID exato, nome por SHA, verificação criptográfica e retenção de sete dias |
| Locks | Resolução sem SSH; acesso remoto somente dentro de `deploy-vps-163-245-202-80` |
| Contratos | Imagem, disco, SSH, fila, deploy transacional, retomada e Actionlint |
| Higiene | Diff válido, nenhuma credencial, nenhum PR/deploy e recursos temporários removidos |

## Execução

A primeira validação direcionada aprovou os 16 novos cenários e revelou que o contrato antigo de
imagem ainda fixava retenção de um dia e `${GITHUB_SHA}` no job remoto. O teste foi alinhado ao
novo contrato de proveniência, exigindo sete dias e `DEPLOY_SOURCE_SHA` somente nos dois agentes
baseados em artefato. Por ter havido esse ajuste, a contagem das duas rodadas finais começa depois
dele.

Depois do último ajuste, duas rodadas completas e consecutivas foram aprovadas sem alteração entre
elas:

| Rodada | Resultado | Cobertura principal |
| --- | --- | --- |
| `queue-event-final-1` | 28/28 controles | 482 testes Java, navegadores, builds, Actionlint, imagens, SSH e runtime Docker |
| `queue-event-final-2` | 28/28 controles | Repetição integral da mesma matriz, sem falha |

Em cada rodada, os 16 cenários da nova coordenação e os 39 contratos de imagens passaram. A
consulta real à API também encontrou imediatamente o deploy central `34361476793` do SHA
`da9239dfe8dd7d98af0e24dcee7a13a1c5fb1d33` em estado `pending`; esse é o caso que o gate novo
aceita como registrado, sem ocupar um runner até a conclusão.

Também passaram isoladamente os contratos do CI completo do backend, suporte de `queue: max` no
Actionlint, política ShellCheck, fila e capacidade do VPS compartilhado, deploy transacional,
retomada cumulativa de módulos, resiliência, fallback SSH e escopo do controlador administrativo.

As evidências brutas estão em
`artifacts/actions-psique-pde-2026-09-08/queue-event-final-1` e
`artifacts/actions-psique-pde-2026-09-08/queue-event-final-2`. Não houve acesso a VPS, publicação,
PR ou deploy durante a homologação local, e nenhuma topologia Docker temporária permaneceu ativa.
