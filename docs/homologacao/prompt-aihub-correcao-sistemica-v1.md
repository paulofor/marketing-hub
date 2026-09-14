# Prompt AIHUB: correção atual e prevenção de recorrência

Data: 14/09/2026. Escopo: reforçar a orientação copiada pelo botão **Prompt para AIHUB**.

## Evidência e decisão

`ProductProcessContextCopy.tsx` carrega o modelo único
`frontend/src/pages/product/prompts/process-aihub-help.v1.md` e acrescenta o contexto oficial
do processo. O texto anterior já exigia investigação da causa-raiz e melhoria dos agentes,
mas não explicitava a aplicação da solução a todas as próximas execuções do mesmo fluxo.
O histórico está em `docs/homologacao/processo-prompt-aihub-v1.md`; os loops conhecidos
reforçam a necessidade de prevenção sistêmica e preservação das autorizações.

| Alternativa                                       | Benefício                                                    | Risco e esforço                                                | Aderência                                                                    |
| ------------------------------------------------- | ------------------------------------------------------------ | -------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Acrescentar uma frase ao pedido atual             | Reforço imediato; esforço mínimo                             | Depende de repetir a orientação a cada pedido                  | Atende apenas esta conversa                                                  |
| Reforçar o modelo compartilhado e o cânone        | Todos os processos passam a gerar a instrução; baixo esforço | Exige validar cópia, prévia e contexto preservado              | Escolhida: atende ao pedido para execuções atuais e futuras                  |
| Criar configuração editável de prompts no backend | Permite governança dinâmica; esforço maior                   | Novo contrato e persistência para manter uma regra obrigatória | Útil se houver demanda por edição administrativa; desnecessária neste ajuste |

A regra exige correção nas fontes compartilhadas, regressão com novas identidades/entradas,
retomadas e falhas, além da distinção entre recuperação e prevenção comprovada. Mantém gates,
histórico, custos e limites de evidência. Não afirma que um prompt garanta o comportamento
de todo modelo ou elimine qualquer falha futura. Nenhum novo endpoint é necessário: apenas
o texto local estático mudou, usando os contratos de contexto já existentes.

## Matriz definida antes da validação

| Critério                   | Validação local                                                                                               |
| -------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Conteúdo e caminho feliz   | Copiar e visualizar o prompt completo com a nova regra, sem truncar o contexto oficial                        |
| Validações e falhas        | Carregamento, clipboard indisponível, seleção manual, retentativa e erro de consulta                          |
| Integrações                | Frontend real com contratos HTTP simulados; cópia sem comando de processo ou chamada de IA                    |
| Observabilidade e métricas | Confirmação após cópia, falhas visíveis, resultados e texto copiado; zero evento comercial ou custo de modelo |
| Segregação                 | IDs sintéticos e troca de produto/ciclo sem carregar contexto anterior; cópia original preservada             |
| Navegadores e dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, HTTP e contexto seguro                                    |
| Regressão                  | Suíte existente de contexto/prompt/painéis, TypeScript, build, formatação e revisão do diff                   |

Executar `bash infra/testing/process-context-copy/run-round.sh systemic-20260914-1`.
Uma rodada completa sem defeitos conclui a validação; se houver defeito e correção, duas
rodadas completas e consecutivas devem passar após a última correção.

## Resultado

A primeira rodada completa passou sem defeitos; não foi necessária uma segunda rodada.

| Verificação                                   | Resultado                                                                                        |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Testes existentes do frontend, em 10 arquivos | 107/107 aprovados                                                                                |
| TypeScript e build                            | Aprovados; nova instrução presente no bundle                                                     |
| Cópia de contexto, prompt e atividades        | 6/6 combinações por suíte: desktop, iPhone e Pixel, em HTTP e contexto seguro                    |
| Nova regra no texto efetivamente colado       | Presente integralmente nas seis combinações, uma única vez, seguida de um único contexto oficial |
| Cópia, prévia, falhas e mudança de contexto   | Aprovadas; nenhuma mutação ou conexão externa inesperada                                         |
| Formatação e revisão do diff                  | Aprovadas                                                                                        |

Resultados conferidos em `artifacts/process-context-copy/systemic-20260914-1/`, incluindo
`frontend.log`, `typecheck.log`, `build.log` e `results.json` nas pastas `browser`,
`aihub-browser` e `activity-browser`. Os arquivos `aihub-browser/*-copied.txt` preservam
o prompt efetivamente colado, com 15.266 ou 15.330 caracteres conforme a origem local da URL.
Conferência visual de `aihub-browser/desktop-http-card.png` e
`aihub-browser/iphone-http-preview.png`: confirmação legível, botões dentro do card e
prévia sem transbordamento horizontal. Servidores e navegadores temporários encerrados.

Limites: os celulares foram emulados no Chromium, sem Safari físico; não houve chamada
de modelo para avaliar seu cumprimento futuro da instrução. O escopo desta alteração é
o prompt de ajuda; não executa ou conclui o processo comercial de Vega. Alterações locais,
sem commit, PR ou publicação, prontas para o fluxo normal de integração do usuário.
