# Capella: parecer de Psique e retomada — 16/10/2026

O pedido é continuar Capella até o experimento em operação. O contexto conferido é produto 7,
cadeia 26, ciclo 12, experimento 105, ainda PLANNED. Preservar o teto audiovisual cumulativo de
US$ 10, a mídia de até R$ 100 por cinco dias após homologação e a meta de contribuição de 30%
da receita líquida. Revisão técnica, parecer funcional e seleção humana têm autoridades distintas.

## Evidência e causa

A tarefa 742 reutilizou a escuta 3 da demonstração 75, sem nova chamada, e recebeu uma escuta
válida do anúncio 74 no recibo 6 (US$ 0,021150 estimados). O parecer audiovisual custou
US$ 0,621922 estimados e pediu ADJUST somente no anúncio: explicitar kit/aplicação na abertura
e mostrar a arte de agenda aberta imediatamente antes de sua legenda. Imagem 596 e vídeo 75
foram considerados adequados. O sucesso atual não explica retrospectivamente as recusas 4 e 5.

O worker enviou o parecer válido a `/failure`, embora `/result` já aceite e valide as decisões
APPROVED, ADJUST e BLOCKED. A tarefa ficou BLOCKED e o recuperador de montagem recusou a
correção com HTTP 400: ele exige parecer concluído da mídia exata. O replay da tarefa 739 já
havia demonstrado a rota válida; a primeira entrega ainda usava a rota antiga. O teste novo
reproduziu três falhas antes da correção, inclusive com outra identidade; APPROVED já passava.

## Correção compartilhada e alternativas

Aceitar qualquer tarefa BLOCKED na montagem enfraqueceria a validação do backend. Repetir
automaticamente o replay manteria uma passagem desnecessária. Entregar o parecer criativo
validado diretamente em `/result` usa o contrato existente e elimina a causa. Essa terceira
opção foi aplicada no consumidor de Psique, sem alterar os gates ou os demais contextos.

COMPLETED significa que o parecer foi entregue, não que o artefato foi aprovado. O backend
continua decidindo a correção, conferindo identidade/hash, preservando bloqueios, custos e
seleção humana. Falha técnica, resposta inválida e callback recusado conservam a recuperação
existente. A correção não cria agente, fila ou nova inferência automática.

## Matriz de homologação local

| Área | Aceite |
| --- | --- |
| Primeira entrega | ADJUST e BLOCKED criativos chegam como resultados, com decisão bruta, auditoria e tokens |
| Isolamento | Caso original e outro experimento, sem regra por nome ou ID |
| Compatibilidade | APPROVED, demais contextos e falhas técnicas preservados |
| Recuperação | Outbox, reinício, replay e rejeição de callback continuam sem repetir inferência |
| Consumidor | Backend exige tarefa concluída e ajuste da mídia exata, sem conceder seleção humana |
| Montagem | Abertura legível em 393 px; arte de agenda e legenda correspondentes; sete vozes reaproveitadas |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7; MP4 com áudio, 35 segundos e sem erro de reprodução |
| Comercial | Dados de teste identificados; produção privada; nenhuma simulação vira venda |

A montagem foi exercitada pelo `PostProductionNarrationRecoveryIntegrationTest`, com FFmpeg
real, arquivos preservados e HTTP local que recusa síntese. Nenhuma chamada paga ocorreu no
teste. O novo arquivo publicado ainda exige revisão própria: os testes não concedem esse aceite.

Suíte local de Psique: 232 testes, zero falhas/erros e 11 cenários condicionais não aplicáveis.
Os testes do consumidor no backend (`CreativeFinalVideoEvidenceTest` e
`ExperimentVideoAssetApprovalCheckerTest`) também passaram, preservando mídia exata, status,
parecer independente e ausência de aprovação humana. Spotless e revisão do diff concluídos.

Métrica técnica: entregar decisões criativas na primeira tentativa, sem a sequência falha →
retomada → replay. Adoção depende dos testes e da passagem observada; reverter o roteamento se
houver perda de auditoria ou aceitação de resultado inválido. Não há medição nova de vendas,
lucro ou qualidade percebida por clientes reais.

## Recuperação após cancelamento durante captura

A retomada geral criou #743 em vez de recuperar #742. O operador cancelou #743 antes da
inferência; os logs confirmam interrupção no upload de frames, sem execução do modelo. O
retorno de falha passou a receber 409 repetidamente e a ocupar o outbox único. Retomar #742
pela mesa do agente adicionou o marcador canônico de replay, mas a fila ainda estava ocupada.

Foram comparadas três opções: apagar o outbox perderia evidência; reabrir a tarefa cancelada
violaria sua decisão terminal; arquivar seu retorno tardio no backend preserva ambos. A terceira
foi implementada no contrato `/failure` existente. Mantém CANCELLED, não chama hooks de
conclusão nem avança atividade, registra auditoria/tokens quando presentes e confirma reenvios
idênticos uma única vez. Outra autoria e retorno divergente continuam recusados.

O teste reproduziu três erros 409 antes da correção (caso original, outra identidade e retorno
após uso de modelo). A matriz inclui reenvio sem duplicar tokens, falha pré-modelo NOT_STARTED,
cancelamento preservado, saída não aceita como produto e compatibilidade dos callbacks ativos.
Passaram os 144 testes locais de tarefas, callbacks, conclusão assíncrona e seleção de vídeo,
além da geração do pacote de evidências comerciais. A recuperação em produção ainda precisa ser
conferida após o deploy; este registro não concede aceite dos vídeos nem autorização comercial.

## Correção revisada e seleção do ciclo

Depois do replay sem nova inferência, #742 ficou COMPLETED/ADJUST. O anúncio foi corrigido no
job #21294, reutilizando as sete locuções sem síntese nova. Psique #744 aprovou os arquivos
#76 (anúncio, 35 s) e #75 (demonstração, 55 s), com escuta #7 válida e escuta #3 reutilizada.
O consumo audiovisual conhecido acumulado chegou a US$ 3,7118458 estimados, dentro de US$ 10.
Isso não representa fatura conciliada nem aceite humano de uso. O experimento segue PLANNED.

A conferência final encontrou outra causa na passagem: os eventos 75/76 do ciclo ainda referiam
as peças #55/#56 rejeitadas em 10/10. A biblioteca oferecia #76/#75, mas a projeção do ciclo dizia
que não havia decisão humana e a integração continuaria presa às peças antigas. O histórico de
sucesso com seleções originais aprovadas explica por que os testes anteriores não detectavam
uma correção posterior a rejeição.

Alternativas consideradas: trocar somente o texto esconderia o bloqueio da integração; sobrescrever
os eventos destruiria a prova histórica; resolver a candidata revisada da mesma versão e congelar
sua identidade no recibo preserva a seleção humana e a auditoria. Foi adotada a terceira opção.
A leitura reutiliza o seletor de mídias de Psique (produto, experimento, versão, tenant e hash),
aceita somente candidata necessária e posterior com parecer válido, e não grava nem aprova peças.
A integração exige os dois aceites e registra seus IDs, hashes, responsáveis e horários. Depois
ela permanece vinculada ao recibo; uma candidata posterior não substitui a peça integrada. Uma
seleção original já aprovada também permanece estável. Falta de revisão, versão/tenant divergentes,
candidata opcional ou anterior não escondem a rejeição. O relógio da tela inclui a revisão criativa
recente, em vez de contar espera desde a produção antiga.

Matriz adicional local: rejeição → correção → parecer → espera humana → dois aceites simulados →
recibo privado; Capella e outra identidade; parecer incompleto/hash divergente; versão/tenant/data
incorretos; candidata opcional; aprovações anteriores; alteração de mídia após integração; projeção
do resumo e relógio. A regressão falhou em dois casos antes da correção. Todo aceite nesses testes
é uma fixture local, sem gravação ou aprovação em produção. Nenhuma nova chamada paga é necessária
para corrigir essa passagem. A adoção exige a tela refletir a seleção atual e a integração consumir
as mídias explicitamente aceitas; reverter se houver troca silenciosa de peça já aprovada.

Resultado local da correção da seleção: 199 testes Java aprovados, incluindo 92 regras de
arquitetura, mais 38 verificações do empacotador e do seletor de publicação. Spotless e diff
aprovados. A atestação Mira v26 mantém a v25 imutável e revalida as duas referências afetadas;
o seletor retornou `frontend-version=none` e `has-deployment=false` para os PDEs. O pacote
comercial real foi construído com 527 arquivos e 116 manifestos. Isso não substitui a instalação
do backend pelo pipeline nem o aceite humano das peças em produção.
