# Seleção audiovisual na liberação de Capella — v1

Conferência de 16/10/2026. Produto #7, cadeia #26, ciclo #12, experimento #105,
capella-private-v3. Aceites humanos de AD #76 e LANDING_HERO #75 confirmados no
MySQL pelo MCP; integração automática registrada no evento #77. A homologação
determinística #745 aprovou nove cenários em desktop, iPhone 15 Pro e Pixel 7,
com os mesmos arquivos, reprodução, recuperação e 36 itens do pacote.

## Causa e alternativas

O endpoint de prontidão ainda retornava `VIDEO_ASSET`, pois consultava os 14
registros obrigatórios do experimento. Doze eram versões anteriores rejeitadas
ou pendentes, embora o ciclo já tivesse congelado a seleção substituta aprovada.

| Caminho | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Limpar flags históricas manualmente | Destrava o caso | Altera história e exige repetir a operação a cada nova versão | Descartado |
| Considerar qualquer vídeo aprovado suficiente | Simples | Pode ignorar peça atual reprovada e outros slots obrigatórios | Descartado |
| Reutilizar o recibo vigente de integração | Mantém uma identidade entre ciclo e gate | Exige revalidar os mesmos arquivos; baixo esforço e bloqueio conservador | Adotado |

`ExperimentVideoReleaseSelection` reutiliza `LearningCycleVideoBinding`, sem
novo seletor por data ou exceção por produto. `ExperimentVideoAssetService`
mantém a política anterior quando não existe integração. Erro de revalidação
continua bloqueante e registra a exceção completa com o experimento.

## Matriz local de aceite

| Área | Caso | Aceite |
| --- | --- | --- |
| Caminho original | #105, #76/#75 aprovados e versões anteriores rejeitadas/pendentes | Requisito audiovisual satisfeito; estados e flags históricos preservados |
| Isolamento | Outro experimento com IDs diferentes | Mesmo comportamento, sem regra por nome/ID |
| Validação | Mídia ausente, papel, arquivo ou experimento divergentes | Bloqueio |
| Revisão atual | Aprovação revogada, mesmo com versão antiga aprovada | Bloqueio |
| Integração | Recibo de outra identidade ou falha de revalidação | Bloqueio com diagnóstico |
| Outros requisitos | PRE_CHECKOUT obrigatório pendente | Continua bloqueante |
| Compatibilidade | Experimento sem recibo | Mantém consulta legada de todos os vídeos obrigatórios |
| Efeitos externos | Testes com repositórios e integrações simulados | Zero inferência, publicação, compra ou mídia |
| Navegadores | Não há alteração de frontend | Navegação operacional conferida; nove cenários publicados de #745 não substituem testes locais do código |
| Comercial | Página, checkout, orçamento e gates independentes | Não são concedidos pelo requisito audiovisual |

Primeira rodada local: 75 testes aprovados (seleção, serviço, vínculo e substituição).
A rodada ampliada aprovou 231 testes Java, incluindo prontidão comercial e 92
regras de arquitetura. Spotless, diff, os 16 testes do empacotador e a construção
do pacote comercial real (527 arquivos, 116 manifestos) passaram.
A mudança aprimora o harness compartilhado, prevenindo a recorrência sem apagar
resultados. Adoção condicionada ao recibo canônico; sem ele, aplica-se o caminho
anterior. Reverter a mudança se uma mídia atual reprovada deixar de bloquear ou
um contexto legado perder a política anterior. Não há alegação de aumento de
conversão, demanda ou lucro.

## Limites financeiros preservados

US$10 para os dois vídeos e suas revisões; mídia de até R$100 totais por cinco dias
após homologação. Margem mínima escolhida: 30% da receita líquida por venda.
O plano financeiro #17 preserva cenários desfavoráveis: com hipóteses conservadoras,
cinco compras e consumo integral da mídia implicam CAC R$20 e contribuição de
15,81%; oito compras implicam CAC R$12,50 e 30,16%. São sensibilidades, não previsão
nem resultados. Não aumentar volumes ficticiamente nem reduzir a meta para aprovar.

A seleção de vídeos não completa a preparação financeira e comercial. Evidências
de publicação desta correção e estado final da execução devem acompanhar o PR.
