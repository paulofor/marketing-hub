# Retomada sonora e editorial de Mira — 24/10/2026

## Evidência e causa

Após confirmação do operador sobre o limite da organização, a retomada pela tela da
execução 55 criou Psique 803. Os recibos sonoros 11/12 concluíram PASS para os arquivos
64/63: US$ 0,035350 estimados; a inferência textual registrou US$ 0,6185492 estimados.
O resultado completo foi ADJUST/FINAL_VIDEO: abertura fragmentada, pouca identificação,
prova pequena e chamada divergente. Não equivale a aprovação nem a venda.

Dois defeitos de passagem foram reproduzidos: o motivo da pausa recuperava a falha antiga
802 em vez do ajuste 803; a remontagem aceitava somente criticalDeficit=true, excluindo
ajustes editoriais explícitos com identidade correta. O compositor também impunha prova
pequena sobre a cena na primeira/última legenda, impedindo atender o parecer de legibilidade.

## Alternativas e decisão

| Caminho | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Repetir a revisão sem alterar a mídia | Menor trabalho inicial | Novo custo e mesmo defeito | Descartado |
| Tratar escuta PASS como aprovação completa | Avanço rápido | Ignora parecer visual e autoridade independente | Descartado |
| Encaminhar o ADJUST vigente e permitir enquadramento explícito | Resolve a passagem e a causa visual | Correção pequena em contrato, editor e compositor | Adotado |

## Matriz de homologação local

| Dimensão | Critério |
| --- | --- |
| Retomada real | H2/MockMvc reproduz falha antiga → retomada → ajuste concluído; razão atual e nenhuma tarefa duplicada |
| Identidade/autoridade | Mesmo vídeo/hash, parecer FINAL_VIDEO; rejeitar contexto alheio e preservar bloqueio de aprovação |
| Composição | FFmpeg real em duas identidades: prova ampliada no início/final e montagem legada preservada |
| Validação | Recusar enquadramento desconhecido, ordem, leitura e referência inválidas antes de consumo externo |
| Tela | Escolha persistida no contrato em desktop, iPhone e Pixel emulados |
| Auditoria/recuperação | Manter hashes, momentos de revisão, recibos e estimativas; não repetir inferência concluída |
| Dados/métricas | Fixtures locais sem API paga; nenhuma compra ou resultado comercial fabricado |

O contrato POST `/api/sales-videos/jobs/{id}/request-post-production` mantém seus campos e
aceita `proofShots[].layout` opcional: FULL_PRODUCT_PROOF_V1 ou MOTION_WITH_PRODUCT_PROOF_V1.
Ausência preserva montagem antiga. A escolha é auditada no resultado. O frontend usa o
endpoint existente do próprio módulo. Nenhuma campanha ativa é alterada por esse controle.

A hipótese reutilizável é reduzir retrabalho e tempo parado por ajustes editoriais, sem
confundir criticidade com necessidade de correção. Adoção depende dos testes e do parecer
independente da mídia nova; reverter se houver divergência de identidade ou regressão legada.
O impacto em conversão e contribuição exige medição de mercado posterior autorizada.

## Resultado local

- Backend: 89 testes relevantes aprovados, incluindo H2/MockMvc, autorização de
  remontagem, aprovação independente e contrato completo de pós-produção.
- Compositor: 24 testes aprovados; quatro renderizações FFmpeg reais cobrem as duas
  identidades com enquadramento explícito e legado.
- Psique: oito testes de contrato/recuperação sonora aprovados antes da retomada.
- Frontend: dois testes de edição aprovados, build de produção concluído e seleção
  conferida em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados.
- Diff e comentários de responsabilidade revisados. Sem SQL ou migração de schema.

A montagem das peças reais e seus novos pareceres serão registrados após a publicação;
esta validação local não representa aprovação dos vídeos nem resultado comercial.
