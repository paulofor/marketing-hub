# Programa de treinamento para jornadas no Marketing Hub

Este guia ensina, passo a passo, como dominar a configuração de jornadas que evoluem os estímulos do mais genérico ao mais específico, reforçando catexias positivas ao longo do funil.

## Visão geral do treinamento

- **Carga estimada:** 6 horas distribuídas em três módulos sequenciais.
- **Formato sugerido:** combinação de leitura guiada, exercícios práticos na plataforma e sessões de feedback.
- **Pré-requisitos:** acesso ao Marketing Hub, template de jornada publicado e segmento de teste com leads fictícios.

## Módulo 1 — Fundamentos (2h)

1. **Revisão conceitual (30 min)**
   - Leia o resumo de orquestração para entender como templates, jornadas e atribuições se relacionam.<br>
     Consulte o material de apoio: <a href="../docs/journeys/orquestracao-jornadas.md" target="_blank">Orquestração de Jornadas</a>.
2. **Exploração guiada (45 min)**
   - Navegue até **Jornadas → Templates** e identifique os campos obrigatórios (nome, fases, estímulos).
   - Observe como cada passo do template permite definir mensagem, canal e condicionais para disparo.
   - Anote exemplos de estímulos amplos (topo de funil) e específicos (fundo de funil).
3. **Checklist de compreensão (45 min)**
   - Diferencie blueprint (`JourneyTemplate`) de instância operacional (`Journey`).
   - Explique como o `contextPayload` guarda dados necessários para personalização.
   - Identifique no blueprint onde registrar mensagens que reforcem as catexias positivas esperadas.

## Módulo 2 — Construção orientada (2h)

1. **Configuração de template (45 min)**
   - Crie ou duplique um template existente.
   - Para cada etapa, defina estímulos seguindo a progressão genérico → específico, adicionando gatilhos emocionais.
   - Documente limites de frequência e atrasos entre passos conforme a estratégia.
2. **Instanciação da jornada (45 min)**
   - Publique uma nova jornada conectada ao template criado.
   - Configure janela de execução, segmentação (nicho/experimento) e metas.
   - Salve como rascunho, valide alertas/erros e então ative.
3. **Exercício prático com atribuições (30 min)**
   - Crie um lote de atribuições com leads fictícios e `contextPayload` contendo e-mail, telefone e estágio atual.
   - Simule o avanço manual marcando estímulos como concluídos para entender a timeline.

## Módulo 3 — Monitoramento e otimização (2h)

1. **Análise de métricas (45 min)**
   - Acesse a jornada ativa e leia indicadores de volume por etapa, conversões e eventos recentes.
   - Compare resultados com o plano teórico e identifique gargalos.
2. **Telemetria e feedback emocional (30 min)**
   - Verifique eventos `journey.stimulus.*` e responda: os estímulos específicos estão mantendo a catexia positiva?
   - Registre insights sobre ajustes necessários em mensagens ou timing.
3. **Ciclo de melhoria (45 min)**
   - Atualize o template com otimizações (ex.: novo criativo para a fase de consideração).
   - Replique alterações para jornadas já em execução quando fizer sentido.
   - Planeje nova rodada de testes A/B para validar hipóteses.

## Materiais complementares

- <a href="../docs/openapi.yaml" target="_blank">Referência de APIs REST</a> — consulta endpoints de templates, jornadas, atribuições e eventos.
- <a href="../docs/frontend-navigation.md" target="_blank">Mapa da navegação do frontend</a> — confirma onde cada recurso aparece na interface.
- <a href="../docs/frontend-screens-entities.md" target="_blank">Relação telas × entidades</a> — auxilia a conectar métricas e painéis às entidades persistidas.

## Checklist final de proficiência

Antes de concluir o treinamento, verifique se você consegue:

- Descrever o fluxo completo (template → jornada → atribuições → eventos).
- Construir uma jornada com estímulos progressivos e ativá-la sem erros de validação.
- Monitorar métricas e eventos para ajustar catexias positivas continuamente.

Ao seguir este plano, qualquer membro do time se torna capaz de conduzir campanhas estruturadas no Marketing Hub, mantendo a evolução gradual de estímulos e a experiência emocional desejada.
