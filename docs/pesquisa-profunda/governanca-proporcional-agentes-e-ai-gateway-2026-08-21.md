# Governança proporcional de agentes e AI gateway — 2026-08-21

## Decisão

Aplicar agora governança proporcional e reconstrução de falhas sobre o runtime existente. Não criar
um AI gateway neste ciclo.

## Evidência externa

- A Gartner recomenda governança proporcional ao nível de autonomia e prevê que controles iguais
  para agentes com riscos diferentes criam tanto excesso de restrição quanto exposição indevida:
  <https://www.gartner.com/en/newsroom/press-releases/2026-05-26-gartner-says-applying-uniform-governance-across-ai-agents-will-lead-to-enterprise-ai-agent-failure>.
- O panorama oficial de AI gateways descreve roteamento, balanceamento, proteção de dados,
  visibilidade de custo, segurança e identidade como responsabilidades dessa camada:
  <https://www.gartner.com/en/documents/7855181>.
- A análise de cenários de IA apresenta `AI Fall` como avanço econômico acompanhado de concentração
  e perda de confiança, reforçando portabilidade e capacidade interna:
  <https://www.gartner.com/en/documents/8035133>.
- O material de 20 de agosto fornecido para esta análise reforça sandbox, ações limitadas e
  reversíveis, revisão humana e um failure log que reconstrua cada execução. A página específica não
  ficou disponível no índice público consultado; a decisão foi apoiada também pelos documentos
  oficiais acessíveis acima e pela evidência interna.

## Evidência interna

Consulta somente leitura no MySQL de produção em 2026-08-21:

- 64 atividades BPM concluídas possuem resultado e evidência persistidos;
- 30 tarefas legadas concluídas não possuem `result_json` nem `evidence_json`;
- seis tarefas antigas em `BLOCKED` não possuem `execution_error`, resultado ou evidência;
- o runtime já possui sandbox somente leitura, MCP por domínio, endpoints `pending`, callbacks,
  telemetria, tokens, custo e políticas de autoridade.

A lacuna não era falta de outro orquestrador. Era tornar explícito o risco de cada agente e levar a
auditoria já persistida até a interface administrativa.

## Alternativas comparadas

### 1. Implantar um AI gateway completo agora

- Benefício: roteamento e portabilidade entre provedores.
- Risco: nova dependência crítica entre agentes e modelos.
- Esforço: alto; exige contratos, benchmark, fallback, segurança e observabilidade cross-module.
- Aderência comercial: baixa no momento, porque não há dois provedores substituíveis disputando a
  mesma etapa nem economia mensurada que pague a camada.

### 2. Criar um subsistema separado de governança

- Benefício: modelo conceitualmente isolado.
- Risco: duplicação de cadastro, tarefas, telemetria e custos; nova fonte de verdade concorrente.
- Esforço: médio/alto.
- Aderência comercial: baixa; aumenta operação antes de reduzir o tempo até venda.

### 3. Evoluir o contrato e a tela existentes

- Benefício: evidencia autonomia, limites e falhas sem alterar a decisão dos agentes.
- Risco: histórico antigo continua incompleto, mas passa a ser identificado como parcial.
- Esforço: baixo.
- Aderência comercial: melhor; reduz tempo de diagnóstico e recorrência de tarefas travadas.

Escolha: alternativa 3.

## Implementação

- manifesto premium classifica todos os agentes por autonomia;
- gate arquitetural impede agente consultivo com efeito externo e exige revisão proporcional;
- tarefas bloqueadas recebem projeção de auditoria com intenção, contexto, autoridade, evidência,
  saída, erro e lacunas;
- a Mesa do Agente, a fila ativa e a instância BPM exibem o log governado;
- Hermes preserva ferramentas MCP já usadas mesmo quando a execução falha;
- Psique e Têmis registram contexto, modo somente leitura e ausência de efeitos externos também em
  falhas técnicas.

## Métricas e gatilhos

- objetivo imediato: 100% das novas falhas BPM como `COMPLETE`;
- ajustar: qualquer nova falha `PARTIAL` exige corrigir o callback do executor responsável;
- parar a autonomia: ação externa incompatível com o nível declarado ou falha sem causa auditável;
- reavaliar AI gateway: segundo provedor substituível, custo/indisponibilidade comprovados ou
  necessidade real de política central que não seja atendida pelo catálogo atual.

Simulação, prontidão de governança e economia estimada não contam como vendas. O efeito esperado é
indireto: menos tempo perdido em reconstruir falhas e menor risco de um agente travado interromper o
funil comercial.

## Matriz de homologação ponta a ponta

| Dimensão | Critério local |
| --- | --- |
| Caminho feliz | tarefa BPM bloqueada exibe intenção, contexto, autoridade, evidência, saída e erro com selo `Reconstruível` |
| Validações | histórico sem dados ou evidência inválida aparece `Parcial` e lista exatamente o que falta |
| Falhas | timeout, erro do Codex e parecer funcional preservam tokens e evidências já observadas |
| Integrações | manifesto → gate arquitetural e backend → contrato TypeScript → três telas permanecem compatíveis |
| Observabilidade | origem, atividade, modo de acesso, efeitos externos, ferramentas, tokens e custo continuam correlacionados |
| Métricas | prontidão mede completude da auditoria; não é contabilizada como venda ou resultado comercial |
| Segregação | cada auditoria usa somente `sourceReference`, processo, atividade e agente da própria tarefa |
| Dados de teste | ids e referências sintéticos; nenhuma tarefa, campanha, custo ou estado produtivo é alterado |
| Navegadores/dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 sem overflow e com detalhes expansíveis |

Como a primeira rodada encontrou falha de compilação no construtor de compatibilidade, após a
correção são obrigatórias duas rodadas integrais e consecutivas sem falha.
