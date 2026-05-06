# Diagnóstico — erro de integração OpenAI no Worker AI (2026-05-06)

## Evidência de log (MCP Server)

- Módulo: `ai-worker`
- Janela observada: por volta de `2026-05-06T02:16:05Z`
- O worker registra que conseguiu montar e enviar o payload para OpenAI.
- No payload enviado, `text.format` está com:
  - `type: "json_schema"`
  - `strict: true`
  - `schema: { "type": "object", "additionalProperties": true }`

## Causa provável

A configuração acima viola os requisitos de Structured Outputs em modo estrito (`strict: true`).

Pela documentação oficial da OpenAI, ao usar strict mode em structured outputs, objetos devem declarar `additionalProperties: false` e o schema precisa respeitar o subconjunto suportado em strict mode.

## Impacto

- A chamada chega na OpenAI (conectividade OK), mas a validação do formato estruturado pode ser rejeitada.
- Isso causa falha da etapa mesmo com autenticação e rede corretas.

## Correção recomendada

1. Ajustar o schema enviado em `text.format.schema` para cumprir strict mode:
   - `additionalProperties: false` para cada objeto do schema.
   - declarar `properties` + `required` de forma explícita conforme contrato canônico do artefato.
2. Se o contrato atual for muito aberto/dinâmico para strict mode, usar `strict: false` temporariamente até fechar schema compatível.
3. Padronizar este check no builder de payload para evitar regressão.

## Próximo passo técnico

- Revisar a classe que monta o payload `text.format` no `ai-worker` e alinhar ao schema canônico do artefato da etapa (`landing-page-wireframe`).
