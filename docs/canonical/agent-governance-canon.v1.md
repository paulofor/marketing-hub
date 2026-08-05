# Governança de agentes v1

## Objetivo

O cadastro de agentes do Marketing Hub é a fonte operacional para identidade, versão, estado,
objetivo, métricas, gatilhos, ferramentas e limites de autoridade. Prompt e schema permanecem
versionados no módulo executor responsável.

## Contrato mínimo

Todo agente deve possuir `agentKey` estável, versão incremental, status operacional, responsável,
objetivo de negócio, métricas de sucesso, modelo, política de gatilhos, política de autoridade e
caminhos versionados do prompt e schema. Cada gravação cria uma fotografia imutável em
`agent_version`.

Status permitidos: `DRAFT`, `TEST`, `ACTIVE`, `PAUSED` e `BLOCKED`.

## Autoridade

O cadastro nunca amplia silenciosamente a autoridade do executor. Ações de gasto, preço,
publicação, comunicação em massa, início ou retomada de campanhas e abertura de PR exigem regra
explícita e aprovação humana. Gates determinísticos podem autorizar apenas ações preventivas já
previstas no contrato canônico do agente.

## Migração do Operador de Crescimento

O Operador usa a chave `growth-operator`, versão inicial `1`, modelo `gpt-5.6-sol` e execução
orientada a eventos. Toda nova execução deve apontar para a `agent_version` ativa no momento em
que foi criada, preservando qual contrato fundamentou a decisão.

O prompt e o schema canônicos são:

- `growth-operator-worker/src/main/resources/prompts/growth-operator/v1/diagnosis.md`
- `growth-operator-worker/src/main/resources/prompts/growth-operator/v1/diagnosis-schema.json`

## Métrica de maturidade

A qualidade de um agente é medida por pendências resolvidas e resultados posteriores comprovados,
não por quantidade de ciclos, relatórios, estimativas ou recomendações.
