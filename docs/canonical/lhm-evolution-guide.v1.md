# LHM Evolution Guide v1

## 1. Propósito

Este guia define o procedimento obrigatório para evoluir o **LHM (Landing HTML Module)** sem quebrar o contrato canônico de `landingPageHtml`.

Ele existe para garantir que mudanças futuras mantenham:
- aderência ao pipeline oficial de experimentos;
- consistência entre cânone, backend e testes;
- diagnósticos objetivos quando houver `422 Unprocessable Entity`.

## 2. Quando este guia deve ser lido

Leitura obrigatória antes de qualquer alteração em:
- `backend/ads-service/.../lhm/*`
- validações de runtime de submit da etapa `landingPageHtml`
- prompts/contratos que impactem a composição HTML da landing

## 3. Checklist obrigatório de evolução do LHM

1. **Revalidar contrato canônico vigente**
   - Ler `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md` (seção `landingPageHtml`).
   - Confirmar requisitos obrigatórios do runtime (`submit` assíncrono, validação, loading, feedback inline).

2. **Aplicar evolução orientada a contrato (não por heurística local)**
   - Mudar o LHM apenas quando o comportamento novo estiver explícito no cânone.
   - Se surgir requisito novo, atualizar o cânone antes (ou na mesma mudança) e registrar versão/motivação.

3. **Manter validação backend explícita e diagnóstica**
   - Em caso de falha 422, retornar mensagem com diferença literal entre:
     - o que foi recebido;
     - o que era esperado;
     - trecho/campo exato rejeitado;
     - ação corretiva sugerida.

4. **Evoluir testes junto com a regra**
   - Toda mudança de regra canônica deve atualizar testes unitários correspondentes.
   - Cobrir cenário positivo (runtime completo) e cenário de rejeição (itens obrigatórios ausentes).

5. **Preservar compatibilidade e previsibilidade**
   - Quando houver mudança estrutural relevante de runtime, introduzir `runtimeVersion` ou estratégia equivalente de versionamento.
   - Evitar mudanças “silenciosas” que alterem comportamento sem atualização de contrato/teste.

## 4. Requisitos mínimos atuais do runtime de submit

O HTML final da landing deve implementar, no mínimo:
- listener de `submit` no formulário alvo;
- `event.preventDefault()`;
- gate de validação com `checkValidity()` e `reportValidity()`;
- envio assíncrono com `fetch(form.action, ...)`;
- payload usando `new FormData(form)`;
- controle de loading no botão de submit (desabilitar durante request e restaurar depois);
- feedback inline de sucesso/erro ao usuário.

## 5. SOP obrigatório para incidentes 422 no LHM

Sempre seguir esta ordem:
1. obter logs do backend via MCP;
2. extrair payload literal rejeitado;
3. comparar com o contrato canônico de `landingPageHtml`;
4. comparar com DTOs/validators/regras ativas no backend;
5. apontar explicitamente o trecho rejeitado e a validação correspondente;
6. reportar causa raiz e ação corretiva (prompt, mapeamento, contrato ou validação).

## 6. Critério de pronto para merge

Uma evolução do LHM só está pronta quando há alinhamento simultâneo entre:
- código do LHM/validações backend;
- documentos canônicos aplicáveis;
- testes unitários cobrindo a regra alterada.
