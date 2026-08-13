# Produção autônoma de vídeo por Apolo e Plutus v1

## Objetivo

Apolo é o executor criativo dos vídeos do Estúdio. Plutus é o gate financeiro independente de cada ciclo. O backend é a fonte de verdade da fila, da decisão, do custo e do avanço.

## Fluxo canônico

1. Um usuário solicita pelo Estúdio um ciclo associado a produto, planejamento, projeto e perfil de vídeo.
2. O backend persiste o ciclo como `PENDING_FINANCIAL_REVIEW` e abre na mesa de Plutus uma tarefa de gate `VIDEO_BUDGET_APPROVAL`, com Apolo como solicitante.
3. Plutus decide pelo contrato formal do gate; alterar apenas o status da tarefa não libera provider nem consumo.
4. Nenhum job de provider existe antes da decisão financeira.
5. Somente a identidade técnica `financial-agent` pode registrar `APPROVED` ou `REJECTED`, sempre com motivo auditável.
6. Uma aprovação cria em modo `TEST` o job do executor oficial de vídeo, vinculado ao ciclo e ao teto em USD. Uma rejeição termina em `FINANCIAL_BLOCKED`.
7. Apolo planeja, gera, inspeciona e devolve o candidato. O provider não decide próxima etapa.
8. O custo conhecido deve ser conciliado no ledger. Novo consumo é bloqueado ao atingir o teto ou quando o custo do ciclo atual estiver desconhecido. Cobertura histórica incompleta deve gerar pendência de conciliação, mas não bloqueia sozinha um ciclo de descoberta com teto explícito, ledger segregado e custo incremental rastreável.
9. QA independente decide qualidade. Apolo não aprova o próprio trabalho.
10. Quando o job vinculado terminar em falha, o backend deve preservar no ciclo o job, código, detalhe e horário da falha antes de reconciliar uma nova tentativa. O painel deve exibir a falha anterior e o novo job separadamente; o estado `QUEUED_FOR_APOLLO` nunca pode ocultar um job terminal falho.

## Contrato financeiro da fase de descoberta

Enquanto o produto ainda estiver produzindo materiais para descobrir qual combinação de mensagem, formato e provider funciona, Plutus deve avaliar o ciclo como investimento de aprendizado, e não exigir retorno ou venda prévia como condição de aprovação.

- Retorno igual a zero, ausência de vendas e ROI ainda não comprovado são esperados antes de existir material testável e não constituem, isoladamente, motivo para rejeição.
- Plutus deve aprovar gasto incremental controlado quando existir objetivo de aprendizado explícito, teto autorizado pelo usuário, ledger segregado do ciclo e capacidade de interromper novo consumo ao atingir o limite.
- Para o grupo inicial `musa-two-video-funnel-v1`, o teto autorizado é de US$ 20 no total, distribuído em até US$ 10 por vídeo; esse limite não autoriza excedente nem compra de créditos.
- Custos históricos conhecidos sem atribuição continuam como dívida obrigatória de conciliação, mas não devem ser somados ficticiamente ao novo ciclo nem bloquear sozinhos a descoberta quando o custo incremental novo puder ser medido desde a primeira tentativa. Por decisão comercial de 2026-08-12, custos anteriores a 2026-08-13 que permanecem irrecuperavelmente desconhecidos são assumidos em USD 0 com evidência auditável; esse fechamento histórico não autoriza estimar como zero qualquer consumo novo.
- Plutus deve bloquear quando faltar rastreabilidade do custo novo, houver risco de ultrapassar o teto, o objetivo não produzir aprendizado verificável ou o ciclo tentar publicar/consumir mídia sem autorização.
- A métrica operacional é material tecnicamente válido por dólar consumido. Deve-se continuar enquanto houver aprendizado dentro do teto, ajustar provider ou abordagem quando qualidade/custo falhar e parar ao atingir US$ 20, perder rastreabilidade ou concluir os dois candidatos válidos.

## Escopo operacional de Apolo v2

Apolo possui acesso operacional completo ao Estúdio de Áudio e Vídeo depois da aprovação financeira: roteiro, storyboard, imagens mestre aprovadas, geração de cenas, seleção de provider por cena, continuidade, narração, trilha, montagem, legendas, HLS, inspeção técnica e iteração causal. Esse acesso não remove os gates de Plutus, o ledger nem o QA independente.

A primeira missão v2 é concluir os dois projetos persistidos do grupo `musa-two-video-funnel-v1`: vídeo de qualificação da campanha e vídeo hero de conversão do PDE. Cada vídeo mantém objetivo, métrica e CTA próprios; a conclusão exige montagem narrativa, áudio pt-BR, legendas mobile, HLS, inspeção técnica e entrega ao QA.

Por decisão comercial de 2026-08-13, Apolo não pode selecionar Luma para o MUSA, inclusive quando um projeto legado ainda a mencionar. O padrão é `RUNWAY_SEEDANCE_2_5`, com Runway Gen-4 como alternativa explícita quando Seedance não atender ao contrato técnico ou ao QA. Vídeos acima da duração direta do modelo devem ser produzidos por cenas auditáveis e montagem; nunca por truncamento silencioso. A troca de provider preserva o teto já aprovado, mas não autoriza compra de créditos, publicação ou mídia.

## Autoridade e segurança

- Aprovar um ciclo não autoriza publicação, campanha, mudança de preço ou compra de créditos.
- O metadata do job deve declarar `publicationAllowed: false`.
- Um teto é limite, nunca meta de gasto.
- Estimativas, jobs e aprovações não são vendas.
- A primeira operação do MUSA v7 permanece assistida e em `TEST` até comprovar custo, qualidade audiovisual, legibilidade mobile e callback completo.

## Homologação

A matriz cobre: caminho feliz, projeto sem perfil, decisão por identidade indevida, rejeição, decisão duplicada, provider indisponível, custo desconhecido, teto excedido, callback idempotente, QA independente, desktop, iPhone e Android. Providers reais não devem ser chamados na homologação local.
