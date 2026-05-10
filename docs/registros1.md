# Registros de ajustes — gera-landing

## 2026-05-10

### Contexto
Foi identificado vazamento de orientações internas e termos técnicos na saída HTML provisória após a etapa de copy do fluxo `gera-landing`.

### Registro do trabalho executado
- Revisado o prompt `ai-worker/src/main/resources/prompts/geralanding/landing-page-copy.md` para reforçar bloqueio de metainstruções no texto final voltado ao usuário.
- Reforçado o critério de aceite da etapa para impedir termos técnicos/contratuais em `hero`, `bodySections`, `ctaBlocks` e `faq`.
- Endurecido o schema `ai-worker/src/main/resources/prompts/geralanding/landing-page-copy-schema.json` com validações textuais adicionais (`minLength`) e padrões de bloqueio para tokens técnicos recorrentes.
- Validada a sintaxe do schema com `jq`.

### Resultado esperado
Redução de saídas com “vazamento” de contrato interno (paths, ids e instruções operacionais) e melhora da qualidade da copy final para aderência comercial.

### Observações
- A execução de testes Maven no ambiente depende de acesso à dependência privada `com.marketinghub:ads-service:0.0.1-SNAPSHOT`.
