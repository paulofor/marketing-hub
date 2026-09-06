# Mira — recrutamento segregado no Instagram v1

## Decisão comercial

- Gargalo real: nenhuma participante humana qualificada para iniciar as duas leituras privadas.
- Evidência de 2026-09-05: plano comercial `7` sem experimento ou campanha de Mira, zero gasto de
  mídia e somente a conta `@produtividade360_` disponível na Meta, com tema incompatível.
- Autorização: R$ 20 por dia, teto acumulado de R$ 100 e meta de duas participantes qualificadas.
- Métrica primária: participantes distintas, qualificadas e consentidas atribuídas ao recrutamento.
- Continuar: cada qualificada recebe acesso privado segregado e consegue iniciar a experiência.
- Ajustar: há clique sem qualificação, qualificação sem início ou abandono antes da leitura.
- Parar: duas qualificadas, R$ 100 de gasto, falha de atribuição/consentimento/privacidade/acesso,
  duplicidade ou identidade pública incompatível.
- Este orçamento não autoriza o experimento posterior de vendas e nenhum lead ou leitura representa
  venda, receita ou validação comercial.

## Alternativas avaliadas

| Alternativa | Benefício | Risco e esforço | Decisão |
|---|---|---|---|
| Usar `@produtividade360_` imediatamente | Menor prazo operacional | Mensagem e audiência incompatíveis com cuidados pessoais; reduz confiança e qualidade da amostra | Rejeitada |
| Usar Instant Form genérico da conta atual | Captação rápida dentro da Meta | Identidade continua incompatível; handoff manual e política atual pausa no primeiro formulário, não em duas qualificadas | Rejeitada |
| Recrutamento próprio, segregado e ligado a uma identidade pública aderente | Consentimento, qualificação, acesso e métricas auditáveis; preserva o experimento futuro de vendas | Exige preparar landing, criativo, integração de parada e identidade | Escolhida |

## Matriz de homologação antes da publicação

| Área | Caminho feliz | Validação e falha | Evidência esperada |
|---|---|---|---|
| Orçamento | R$ 20/dia e teto R$ 100 persistidos | Recusar valor maior ou orçamento parcial | Plano versionado e contrato da campanha |
| Qualificação | Mulher de 35–60 anos, aderente, consentida e distinta | Rejeitar QA, duplicidade, ausência de consentimento e pedido clínico | Resultado estruturado sem dado clínico |
| Convite | Cada uma das duas qualificadas recebe um acesso diferente | Terceira qualificada não recebe convite | Dois acessos, duas referências e nenhum segredo em log/URL HTTP |
| Parada | Segunda qualificada solicita pausa da campanha | Retry não duplica solicitação; teto também pausa | `stop_requested_at`, motivo e confirmação do worker |
| Segregação | UTM e eventos pertencem somente ao recrutamento | QA, leitura e venda não contaminam contagens | Relatório por experimento/campanha/classe de tráfego |
| Observabilidade | Request, resposta, IDs e decisão ficam auditáveis | Falha informa causa e ação sem depender somente de log | Tela e backend com estado persistido |
| Privacidade | `no-store`, `no-referrer`, `noindex` e consentimento | Não coletar diagnóstico nem expor codinome/acesso | Cabeçalhos e payload sanitizado |
| Desktop | Qualificar, receber acesso e iniciar | Mensagens de bloqueio compreensíveis | Chromium desktop aprovado |
| iPhone | Mesmo fluxo com toque e viewport móvel | Sem overflow ou CTA inacessível | Emulação iPhone 15 Pro aprovada |
| Android | Mesmo fluxo com toque e viewport móvel | Sem overflow ou CTA inacessível | Emulação Pixel 7 aprovada |

## Estado desta preparação

- O plano comercial `7` foi atualizado pela tela e congelado na versão `6` com teto de R$ 100,
  limite diário descrito, meta de duas qualificadas e critérios de parada.
- Banco confirmado com zero experimentos, zero campanhas e R$ 0 de mídia para o produto `10`.
- A publicação permanece bloqueada até existir identidade pública aderente, landing, criativo e
  trava automática homologada. Registrar a autorização não cria campanha nem gasto.
- O formulário do planejamento revelou uma recorrência de limite textual; frontend e backend foram
  corrigidos localmente para impedir novo HTTP 500 sem truncar conteúdo.

## Homologação executada em 2026-09-05

Depois da correção do defeito, duas rodadas locais completas e consecutivas terminaram sem falha.
Cada rodada confirmou:

- 9 testes do backend para plano, limites e snapshot versionado;
- 21 testes da tela de planejamento, typecheck e build de produção;
- leitura do plano real em Chromium desktop, iPhone 15 Pro e Pixel 7, com R$ 100, meta 2, limites
  `191/191/512` e sem overflow horizontal;
- plano `7` congelado na versão `6`, zero experimento, zero campanha e R$ 0 de mídia para o produto
  `10`.

A matriz de campanha ainda não foi executada porque os quatro pré-requisitos persistidos continuam
ausentes: identidade pública aderente, experimento de recrutamento, landing e criativo. A publicação
ou o gasto para descobrir esses itens violaria os gates acima.
