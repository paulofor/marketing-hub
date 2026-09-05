# Matriz de homologação — integridade operacional do Vega v1

## Objetivo comercial

Permitir que Hermes valide o ciclo realmente ativo do Vega sem transformar tráfego antigo, QA,
tarefa ou sucessor planejado em visita, venda ou autorização de mídia.

## Gargalo e decisão

- Gargalo real: a atividade operacional escolhia o experimento #91 `PLANNED`; o #90 está `RUNNING`
  e possui os gates produtivos do canal direto em `PASS`.
- Evidência: as tarefas #339 e #340 apontam para `experiment:91`; a amostra do #90 está em 0/100 e
  os analytics atribuídos estão zerados. As 17 sessões globais da v7 são anteriores aos dois ciclos.
- Métrica esperada: uma nova tarefa de `Verificar integridade dos eventos` apontando para
  `experiment:90`, funil comercial zerado e decisão de Hermes baseada no run produtivo #7.
- Continuar: referência #90, contrato de Atena v2 íntegro, três gates diretos aprovados e nenhum dado
  sintético promovido a humano.
- Ajustar: divergência entre funil atribuído, amostra direta, run e cockpit.
- Parar: qualquer gasto, contato, publicação, geração paga ou falsa venda durante a homologação.

## Alternativas avaliadas

1. Repetir a tarefa atual: menor esforço, mas reapresenta #91 e consome modelo para o mesmo bloqueio.
2. Forçar conclusão ou converter as 17 sessões em evidência: rápido, porém falsifica o estado
   comercial e pode liberar otimização sobre um funil inexistente.
3. Corrigir seleção do ciclo, atribuição das métricas e filas que antecedem a operação: maior
   cobertura, mas remove as três causas-raiz sem alterar preço, canal ou campanha.

A terceira alternativa foi escolhida.

## Casos de homologação

| Área | Caso | Resultado obrigatório |
|---|---|---|
| Caminho feliz | #90 `RUNNING`, #91 `PLANNED` e contrato Atena v2 pronto | Tela e novo request usam `experiment:90` |
| Histórico | #340 permanece bloqueada em #91 | Tentativa continua auditável, sem comandar o ciclo atual |
| Prontidão | Parecer Atena ausente, legado, hash inválido ou fronteira divergente | UI bloqueia antes de criar tarefa ou consumir modelo |
| Prontidão | `MARKET_STRATEGY_V2`, `READY_FOR_OPERATION`, SHA-256 e fronteira íntegros | Comando de Hermes é disponibilizado |
| Prontidão composta | `task-2` possui contrato estratégico e gate de amostra consentida | Todos os gates são avaliados; qualquer pendência bloqueia com seus motivos |
| Métricas | Versão PDE possui 17 sessões globais, mas nenhuma UTM do experimento | Visita, checkout, compra, acesso e primeiro uso ficam em zero |
| Métricas | UTM de campanha ou criativo corresponde exatamente ao experimento | Apenas a origem correspondente entra no funil |
| Integração | UI solicita execução | Controller, service, `agent_task`, fila `pending`, Hermes e callback preservam `experiment:90` |
| Observabilidade | Hermes conclui ou bloqueia funcionalmente | Prompt, evidências, URLs, tokens, custo, resultado e motivo ficam auditáveis |
| Fila financeira | Reserva Product UGC expirou sem consumo | Reserva liberada, ciclo/gate bloqueados, sem job e sem starvation |
| Fila financeira | Reserva ainda está vigente | Reconciliação não a altera |
| Segregação | Homologação local, QA e sessões históricas | Zero contato humano, campanha, gasto, checkout, pagamento e venda |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | Histórico, estado atual e comando permanecem legíveis e acionáveis sem overflow |

## Critério de encerramento local

Como a investigação encontrou defeitos, depois da última correção devem passar duas rodadas locais
completas e consecutivas. Cada rodada cobre backend, Financial Agent, Hermes, frontend, contratos,
imagens Docker e navegação nos três perfis. Integrações externas são substituídas por test doubles;
nenhuma chamada à Meta, Runway, checkout ou pessoa real faz parte da homologação.

## Resultado executado em 05/09/2026

As duas rodadas completas e consecutivas terminaram sem falhas depois da última correção. Em
cada rodada foram aprovados:

- 2.310 testes do backend, com zero falhas e zero erros;
- 28 testes do Financial Agent e 31 testes Java de Hermes, além dos cinco contratos MCP;
- 474 testes em 139 arquivos do frontend, typecheck, build e formatação do smoke;
- navegação com o request para `experiment:90` em Chromium desktop, iPhone 15 Pro e Pixel 7;
- imagens Docker do backend, frontend, Financial Agent e Hermes;
- contrato OpenAPI da reconciliação financeira, diff e validações estáticas Liquibase/MySQL 5.7.

Os testes preservaram #339 e #340 como histórico do experimento #91, projetaram a futura tarefa
#341 sobre o ciclo #90 e mantiveram em zero contatos humanos, campanha, gasto, checkout, pagamento,
venda, job de vídeo e consumo Runway.
