# Homologação — aprovação do vídeo de Vega #91 por Têmis v1

## Objetivo e estado inicial

Eliminar o bloqueio que impedia Têmis de inspecionar e vincular o arquivo final ao anúncio de Vega,
tornando a fila humana autoexplicativa sem remover o gate independente, publicar campanha ou consumir
mídia. O histórico começou no anúncio #524, cujo runtime não inspecionava H.264/AAC, e revelou depois
lacunas de hash, disclosure sintético e inspeção do checkout.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validações e falhas | Evidência esperada |
|---|---|---|---|
| Landing de Têmis | Playwright escolhe o headless shell empacotado e produz screenshot | Chrome completo e caminho `/usr/bin/chromium` não podem retornar | execução como UID 10001, filesystem read-only e screenshot não vazio |
| Vídeo H.264/AAC | FFmpeg/FFprobe 7.1.1 pinados extraem quadros em 10%, 50% e 90% | arquivo vazio, inválido, maior que 64 MiB ou com hash divergente bloqueia | duração, bytes, SHA-256 coincidente e três JPEGs não vazios |
| Disclosure sintético | pessoa e voz geradas por IA são declaradas no próprio vídeo | declarar somente a voz não atende a peça com apresentadora sintética | texto visível durante toda a peça e metadata correspondente |
| Inspeção | mídia, landing e checkout abrem pelo MCP do anúncio reservado | falha visual mantém Têmis fora de `APPROVED` | request, response, URLs, preço e decisão ligados ao creativeId, sem interação no checkout |
| Gate backend | `APPROVED` de Têmis libera a decisão humana | `PENDING`, `PROCESSING`, `ADJUST`, `REJECTED` e `FAILED` retornam bloqueio funcional | status e motivo canônicos na fila e no banco |
| Tela | operador entende o parecer, reenvia e acompanha a atualização | botão de aprovação fica indisponível enquanto houver bloqueio | mensagem de Têmis, resumo, ação de reavaliar e polling controlado |
| Erros | resposta de sucesso atualiza a fila | HTTP 409 mostra a mensagem do backend, não o texto genérico do cliente HTTP | teste de interface e toast funcional |
| Observabilidade | health e deploy comprovam lançamento real do browser | serviço saudável sem browser utilizável deve reprovar o deploy | gate do workflow e check dentro do Compose publicado |
| Segregação | fixtures locais usam anúncio e experimento isolados | QA não cria campanha, impressão, checkout, venda ou gasto | zero chamadas Meta e zero alteração de métricas comerciais |
| Desktop | fila, motivo e ações ficam legíveis | nenhum comando fica oculto ou ambíguo | Chromium desktop |
| iPhone 15 Pro | alerta e ações permanecem tocáveis | nenhum overflow horizontal | emulação móvel Chromium |
| Pixel 7 | mesma verdade do backend é exibida | nenhuma inferência local libera aprovação | emulação móvel Chromium |

## Critérios

- **Continuar:** browser confinado abre, Têmis observa mídia e destino e retorna parecer auditável.
- **Ajustar:** qualquer falha de inspeção, contrato, legibilidade ou atualização persistida.
- **Parar:** tentativa de ignorar Têmis, publicar campanha, gastar mídia ou registrar aprovação sem
  parecer `APPROVED`.
- Como o diagnóstico revelou defeito, após a última correção devem passar duas rodadas locais
  completas e consecutivas; qualquer nova falha reinicia a contagem.

## Decisão de solução

Foram comparadas três alternativas:

1. Ignorar o parecer de Têmis e liberar a aprovação humana: esforço baixo, porém risco comercial e
   de conformidade inaceitável.
2. Tornar gravável o container para executar o Chrome completo: esforço médio, maior superfície de
   segurança e manutenção, sem garantir os codecs necessários.
3. Separar os runtimes, completar a governança do arquivo e refazer apenas a pós-produção
   determinística: esforço médio, isolamento preservado, suporte a H.264/AAC e nenhum novo consumo
   de 648 créditos na Runway.

A terceira alternativa foi escolhida porque corrige a causa-raiz sem enfraquecer o gate independente,
aumentar o privilégio do container ou pagar por uma segunda geração.

## Resultado de 05/09/2026

Depois da última correção, passaram duas rodadas locais completas e consecutivas. Cada rodada
aprovou:

- 2.353 testes do backend, com quatro testes ignorados pelo próprio projeto;
- 87 testes do Meta Ad Approver Worker;
- 487 testes, typecheck e build do frontend;
- Spotless, Prettier, Actionlint e validação do diff;
- build da imagem e execução como UID 10001, filesystem somente leitura e `no-new-privileges`;
- inspeção do vídeo premium real de 15,08 segundos, com três quadros distintos;
- inspeção da landing em 390 x 844 e 1440 x 1000;
- bloqueio auditável de vídeo inválido e de payload superior a 64 MiB;
- jornada da fila em desktop, iPhone 15 Pro e Pixel 7, com apenas um pedido de reavaliação e
  aprovação humana desabilitada durante o bloqueio.

A primeira rodada produtiva após esse ajuste revelou que o uploader do vídeo derivado ainda não
persistia `size_bytes` e `sha256`. A causa foi corrigida no executor e protegida por teste unitário;
por isso, a contagem das duas rodadas finais foi reiniciada.

## Resultado operacional de 06/09/2026

- job de pós-produção #21236 e ativo final #2788, sem uma segunda geração Runway;
- vídeo produzido #40 e criativo de anúncio #526 aprovados pela tela;
- 15,083 segundos, 3.731.448 bytes e SHA-256
  `8a3959a36b3d043a9b34c79a5c15e65ebb01402a08cd229845cd721a00da4a96`, idêntico no upload,
  download e governança;
- disclosure “Apresentadora e voz geradas por IA” visível na peça;
- landing inspecionada em desktop e celular e checkout Pepper de R$ 67 observado sem clique,
  formulário ou pagamento;
- Têmis `APPROVED`, decisão humana `READY` e `approvalBlockedReason` ausente no anúncio #526;
- anúncios #524 e #525 reprovados pela tela como versões substituídas, preservando o histórico;
- experimento #91 ainda `PLANNED`, sem campanha Meta e com gasto de mídia igual a zero.

As duas rodadas locais completas e consecutivas posteriores à última correção estão registradas na
seção seguinte deste documento.

## Rodadas finais de 06/09/2026

Depois de corrigir também a chave não suportada `concurrency.queue` do workflow de Têmis, a contagem
foi reiniciada. Duas rodadas locais completas e consecutivas terminaram sem falhas. Cada rodada
aprovou:

- 2.353 testes do backend, com quatro testes ignorados pelo próprio projeto, e Spotless;
- 148 testes do Video Management Service;
- 87 testes, Spotless e handshake MCP do Meta Ad Approver Worker;
- 487 testes, typecheck e build do frontend administrativo;
- Actionlint, ShellCheck, sintaxe dos quatro módulos JavaScript do revisor e integridade do diff;
- imagens Docker reconstruídas do worktree versionado e runtime saudável do serviço de vídeo;
- navegador e FFmpeg confinados em filesystem somente leitura, sem capabilities e com
  `no-new-privileges`;
- captura local segregada de landing mobile/desktop, checkout somente leitura, SHA-256 e bloqueio de
  arquivo inválido;
- leitura produtiva da fila em desktop, iPhone 15 Pro e Pixel 7, sempre exibindo o anúncio #526 como
  `No portfólio` e `Revisão de Têmis: aprovado`.

Após as rodadas, endpoint e banco produtivos continuaram concordantes: #526 em `READY/APPROVED`,
#524 e #525 em `REJECTED`, `creative_approved=true` no experimento #91 e zero campanhas Meta. Os
workers de vídeo e Têmis permaneceram saudáveis. Nenhuma validação clicou no checkout, ativou
campanha ou gerou gasto de mídia.
