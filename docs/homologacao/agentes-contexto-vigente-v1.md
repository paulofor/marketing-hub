# Passagem coerente de contexto entre agentes

## Evidência de 20/10/2026

Consulta à cadeia #26, painel de agentes e banco via MCP, pelo transporte SSH autenticado
(certificado HTTPS público expirado). O backend publicado estava no SHA
`5c80394224052c91343bc3bba8ad1529b2385b8f`.

O histórico de Alcyone contém Têmis #764 bloqueada e #766 concluída na mesma atividade,
seguida por outra falha técnica #778. Capella contém Psique #741 bloqueada e #744 concluída.
A consulta não altera essas tarefas. Os resultados e custos históricos permanecem auditáveis.

`AgentTaskService.processContext` selecionava o último COMPLETED e o último BLOCKED
**separadamente**. Assim, a mesma atividade podia aparecer como concluída e bloqueada no
mesmo contexto. Bloqueios eram agrupados só por atividade, embora conclusões também
considerassem o agente. Íris consome `blockedActivities` como instruções de correção;
Dédalo e revisores consomem essas listas como entrada operacional. A falha é no produtor
compartilhado de contexto, não ausência de mais um agente coordenador.

Isso comprova orientação contraditória. Não permite atribuir todas as falhas anteriores,
a duração total da cadeia ou perda comercial a essa única causa.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Pedir aos agentes para interpretar a cronologia no prompt | Mudança pequena | Mantém entradas contraditórias e exige interpretação repetida | Não escolhida |
| Criar outro agente para conciliar mensagens | Coordenação explícita | Mais inferências, custo e uma autoridade paralela ao backend | Não escolhida |
| Selecionar a conclusão vigente no backend existente | Contexto determinístico, sem IA adicional | Regressões de histórico, isolamento e recuperação | Escolhida |

O contrato mantém os campos consumidos pelos executores. A seleção considera a mesma
referência, atividade e agente. Conclusão posterior retira a reprovação superada da lista
operacional; bloqueio posterior impede que a conclusão antiga pareça vigente. Uma mera
retentativa pendente/em execução ou cancelada não prova resolução do bloqueio.
Cada entrada identifica também processo, versão e origem. Isso não homologa uma versão,
não muda gates nem autoriza gasto ou publicação. Tentativas de correção PDE continuam
separadas de sua reprovação de origem. Não reabrir tarefas para testar a publicação.

## Matriz de homologação local

Definida antes da implementação da correção; provedores e persistência de testes são
isolados da produção. Não há novos produtos nem alterações de interface nesta entrega.

| Caso | Critério de aceite |
| --- | --- |
| Reprovação → correção → aprovação → próximo agente | Pending entrega apenas o parecer vigente, com prova e identidade |
| Alcyone e Capella / outra identidade | Mesmo comportamento sem exceção por produto ou ID |
| Aprovação → nova reprovação | Aprovação anterior não aparece como vigente |
| Retentativa pendente, ativa ou cancelada | Não apaga bloqueio ainda não resolvido |
| Duas responsabilidades na mesma atividade histórica | Parecer de uma não substitui o da outra |
| Ordem do retorno e timestamps iguais | Seleção estável pela ordem das tarefas persistidas |
| Histórico PDE entre versões | Preserva origem e tentativas separadas; nenhuma mistura de produto |
| Caminho anteriormente válido | Evidência de predecessor, replay e gates continuam funcionando |
| Auditoria e economia | Tarefas, custos, saídas e recibos anteriores permanecem inalterados |
| Contrato HTTP e consumidor | Controller e fila reais entregam a seleção ao consumidor simulado por HTTP |
| Observabilidade | Campos de origem/versão permitem rastrear a decisão sem inferir pelo nome |
| Publicação | PR, revisão, checks, main, workflows aplicáveis e saúde/versão comprovados |

## Aprendizado e limites

Hipótese: contexto coerente reduz repetição de ajustes já aceitos, instruções conflitantes
e tempo parado entre entregas utilizáveis. Métrica determinística: zero atividade/agente
simultaneamente em `completedActivities` e `blockedActivities` no mesmo contexto.
Medir depois, por ciclo, retrabalho recorrente, tempo até aceite e custo estimado por entrega;
sem amostra comercial, não declarar aumento de conversão, vendas ou contribuição.

Adotar com regressões aprovadas e consumidores compatíveis; reverter pelo fluxo de PR se
uma reprovação ainda vigente desaparecer, evidência necessária for perdida ou contexto de
outro produto entrar no pacote. O histórico original permite diagnosticar e reverter.

## Resultado local

- Antes da alteração: três testes falharam, comprovando bloqueio superado e conclusão
  anterior ainda apresentados como vigentes. Depois: os três casos e os nove cenários
  adicionais de seleção passaram.
- Backend: 536 testes selecionados, 531 executados com sucesso, cinco condicionados a
  fixtures externas de capturas, vídeos, browser e ZIP. Esses cinco não exercitam esta
  alteração de contexto; nenhuma evidência visual ou interface foi alterada.
- A rodada inclui fila com service real e transporte MockMvc em duas identidades,
  recuperação de callbacks, correção PDE entre versões, criativos e ArquiteturaTest.
- Consumidor Íris existente: 18 testes executados com sucesso, incluindo rejeição de
  pixels repetidos e recuperação; uma captura externa opcional não foi fornecida.
- Spotless e revisão de diff conferem comentários em português e ausência de alterações
  em schema, dados, cron, limites financeiros, aprovação humana ou providers.

As chamadas de teste usam apenas portas/fixtures locais. Não houve inferência paga,
retomada de produto, autorização de campanha ou nova comprovação de vendas.
