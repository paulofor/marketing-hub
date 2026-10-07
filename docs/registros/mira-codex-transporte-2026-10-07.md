# Mira — compatibilidade do transporte Codex e auditoria

## Caso, histórico e causa

Produto Mira #10, cadeia #26, ciclo #6, experimento #99; teto próprio autorizado
de US$ 10 para preparar e revisar produto e comunicação, sem mídia ou geração
paga de vídeos. A verificação técnica #608 foi aprovada. A correção do vínculo
de captura #609 foi publicada no PR #5523, main
`81ec29acac74025341c79fbda1772181a02d9c4f`. Uma única retomada pela tela preservou
essas provas e criou #610, que capturou e persistiu a evidência #519.

#610 terminou antes da inferência concluída, com HTTP 400 e
`Unsupported service_tier: flex`. Não há resposta funcional nem tokens/custo
informados; custo desconhecido não é zero. Os custos estimados persistidos de
#600–#607 somavam US$ 2,0917368 antes dessa correção. Os pareceres anteriores de
Atena #600 e Plutus #601 concluíram no transporte padrão com a exceção ao Flex
registrada. Não foi hipótese de indisponibilidade geral ou de modelo inadequado.

Por que esse erro aconteceu? Os dois consumidores antigos preservavam uma
justificativa que dizia usar o tier padrão e classificavam o custo `STANDARD`,
mas montavam o comando com `service_tier="flex"`. Os doubles anteriores aceitavam
qualquer argumento, de modo que a divergência atravessava os testes. O mesmo
defeito foi encontrado em `CommercialBpmTaskConsumer`, responsável pelo gate
comercial de Têmis; Íris tem outro executor e já solicita `default`.

## Alternativas consideradas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Usar o `default` já compatível, com exceção e custo auditáveis | Reutiliza o transporte autenticado e preserva modelo e contratos | Perde o desconto Flex; contabilização permanece Standard e dentro do teto autorizado | Escolhida: menor mudança sustentada pelo histórico e pela recusa observada |
| Migrar a chamada para Responses API com Flex | Poderia preservar desconto em modelos/rotas que o suportam | Exige contrato, credencial e autenticação distintos, com risco de nova integração | Fora desta correção; não há necessidade demonstrada de migrar |
| Negociar tier por catálogo antes de cada chamada | Pode acompanhar futuras capacidades do transporte | Introduz descoberta e tratamento adicionais sem corrigir melhor este caso conhecido | Reservada para mudança futura comprovada do catálogo |

O [contrato oficial do Codex](https://learn.chatgpt.com/docs/config-file/config-reference)
orienta usar tiers anunciados pelo modelo ativo. A recusa concreta comprova o
limite deste transporte; não se presume que todo transporte Codex rejeite Flex.
Não houve migração de modelo, habilitação de Fast/priority nem fallback silencioso.

## Mudança reutilizável e matriz local

Psique e o gate comercial solicitam `default` e auditam separadamente preferência
`FLEX`, request `DEFAULT`, classificação de custo `STANDARD` e justificativa.
Prompts, schemas, sandbox somente leitura, PLAY/STOP, evidências, outbox e
autoridade de avanço do backend continuam sob seus contratos atuais.

Antes da publicação, os testes dos dois módulos devem cobrir:

| Critério | Evidência local |
| --- | --- |
| Recusa de Flex antes de resposta/tokens | Double restritivo devolve HTTP 400 equivalente e não registra tentativa concluída |
| Mira com identidade original | Psique ADHERENT 10/99/6; Têmis 10/99/6, fila até callback |
| Outra identidade, sem exceções por nome/ID | Psique RECOVERY 8017/9017/7017; Têmis 8017/9017/7017 |
| Caminho antes válido | Psique SAFETY no contrato Vega; Têmis revisão legada de landing |
| Contrato de subprocesso e saída | Comando real, sandbox read-only, modelo preservado, schema materializado e resposta estruturada |
| Observabilidade e economia | Request/prompt, resposta, tokens reais do double, audit DEFAULT/STANDARD e uma chamada por tarefa |
| Captura e segregação | Regressões existentes com os três relatórios reais locais de Mira e suas capturas; AGENT_VALIDATION, sem prova humana/comercial |
| Retentativa e recuperação | Suíte existente protege callback preservado e repetição sem nova inferência |

Os pareceres dos doubles são fixtures técnicas, sem validade comercial ou
aprovação em nome dos especialistas. Nenhuma API paga é usada nos testes locais.
As provas reais de navegador previamente executadas permanecem preservadas;
esta correção não altera a experiência ou os manifestos imutáveis publicados.

Resultado local: Psique 164 testes, zero falhas/erros, dois testes opcionais de
outros harnesses não aplicáveis; Têmis 112 testes, zero falhas/erros, uma integração
Vega opcional não aplicável. Os quatro testes novos de transporte em cada módulo
executaram sem skips. Os três relatórios de cenário reais de Mira e o relatório
técnico de 18 percursos foram consumidos novamente pelas regressões de vínculo
visual. Spotless dos dois módulos, `bash -n` e ShellCheck do runner local e
validação de escopo de publicação passaram. O seletor confirmou que nenhum
frontend, backend ou contrato comercial imutável precisa ser republicado.

## Limites e retomada

A correção exige PR, checks do HEAD, consulta das revisões reais, merge e
publicação dos dois workers com coordenação e confirmação de saúde. Somente após
essa comprovação uma nova retomada do processo #49 é válida. A #610 permanece no
histórico; #608 não deve ser reexecutada. Um callback pago preservado deve ser
reaplicado, sem nova inferência. A preparação não autoriza mercado, anúncios,
pagamentos de clientes ou vídeos pagos e não comprova venda ou lucro.
