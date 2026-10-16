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
