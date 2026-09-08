# Homologação — dependência da aplicação fora da fila do VPS de agentes

Data: 2026-09-08.

## Objetivo

Eliminar o bloqueio em cadeia no qual Argos, Psique ou Íris ocupavam a fila exclusiva do VPS
enquanto aguardavam outro workflow, sem remover a exigência de aplicação publicada com sucesso no
mesmo SHA.

## Evidência e causa-raiz

O run `34221987913` de Argos entrou no job remoto às 15:17 UTC e permaneceu 2.400 segundos em
`Wait for matching application deployment`. Não existia run de `deploy-containers.yml` para seu
SHA, porque o publicador central esteve desativado naquele período. O timeout ocorreu às 15:57 UTC.

Durante a espera, a API oficial de grupos de concorrência mostrou 14 membros em
`deploy-vps-163-245-202-80`: o Argos antigo detinha a posição ativa e treze deploys estavam
pendentes. Psique e Íris ainda possuíam concorrência no nível do workflow inteiro, então suas
revisões atuais aguardavam também a execução anterior antes de iniciar os próprios jobs. O deploy
da aplicação do HEAD já estava verde; não havia falta de runner nem falha dessa revisão.

## Alternativas consideradas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Cancelar somente a fila antiga | Recuperação imediata | Recorrência no próximo deploy ausente | Contenção apenas |
| Remover a espera pela aplicação | Fluxo mais rápido | Worker pode operar contra backend incompatível | Rejeitada |
| Separar dependência e seção crítica | Preserva o mesmo SHA e libera o host para trabalhos prontos | Ajuste coordenado em três workflows e contratos | Adotada |

## Solução

- Argos, Psique e Íris aguardam o deploy da aplicação no job independente
  `application-deployment`.
- Esse job não possui SSH e não participa da fila do VPS.
- O job remoto depende explicitamente do gate e só depois adquire
  `deploy-vps-163-245-202-80`.
- Um gate novo cancela somente a espera anterior do mesmo workflow e branch; não cancela um deploy
  que já entrou na seção crítica.
- Psique e Íris não serializam mais o workflow inteiro. Testes e gates de uma revisão atual podem
  avançar sem aguardar um run antigo bloqueado.
- A fila remota continua com `queue: max` e `cancel-in-progress: false`, preservando exclusão mútua,
  imagens aprovadas, retenção e rollback.

## Matriz local

| Área | Critério |
| --- | --- |
| Coordenação | Mesmo SHA, push e retomada manual; sucesso, falha, timeout e entradas inválidas |
| Locks | Gate fora da fila do VPS; ausência de concorrência global; cancelamento limitado ao gate |
| Fronteira remota | Nenhum SSH/SCP/rsync no gate; deploy depende do gate |
| Imagens e disco | Pacote aprovado, carga sem rebuild, retenção e reserva antes/depois |
| Sintaxe e transporte | Actionlint, ShellCheck, SSH real isolado, Docker e Compose reais |
| Regressão funcional | Psique, Dédalo, Apolo, PDE/Mira, catálogo, builds e navegador |
| Higiene | Diff limpo, topologia temporária removida e nenhum dado comercial de teste |

A primeira execução ampla revelou que o homologador local chamava o navegador de Psique sem
executar `npm ci`. O script passou a instalar dependências de forma determinística, como já faz o
workflow real. Depois dessa última correção, as rodadas `deadlock-clean-1` e `deadlock-clean-2`
concluíram consecutivamente com **28/28 controles**, zero falha em cada rodada.

## Recuperação no GitHub

- O run antigo que segurava o lock terminou com o timeout comprovado.
- Doze runs obsoletos foram cancelados; os três runs do HEAD foram preservados.
- Para o SHA `dac55971e6fe1cebd335f1a27d70b34ba585e536`, contratos, aplicação, Argos, Íris e
  Psique terminaram `success`.
- Depois da recuperação, não restou workflow ativo em `main` e a consulta do grupo compartilhado
  retornou HTTP 404, estado esperado para um grupo sem membros ativos.

Essa recuperação operacional comprova que a fila atual foi liberada. A prevenção estrutural deste
documento permanece local e precisa entrar pelo fluxo de Pull Request antes de valer nos próximos
pushes.

