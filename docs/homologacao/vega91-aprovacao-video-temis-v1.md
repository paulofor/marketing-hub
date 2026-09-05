# Homologação — aprovação do vídeo de Vega #91 por Têmis v1

## Objetivo e estado inicial

Eliminar o bloqueio técnico que impede Têmis de inspecionar o anúncio #524 e tornar a fila humana
autoexplicativa, sem remover o gate independente, publicar campanha ou consumir mídia. Em produção,
o vídeo #38 e sua governança estão aprovados, mas o parecer de Têmis permanece `ADJUST`: o Chrome
completo encerra pelo `chrome_crashpad_handler` no container somente leitura e os Chromium
empacotados não decodificam o H.264/AAC do MP4.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validações e falhas | Evidência esperada |
|---|---|---|---|
| Landing de Têmis | Playwright escolhe o headless shell empacotado e produz screenshot | Chrome completo e caminho `/usr/bin/chromium` não podem retornar | execução como UID 10001, filesystem read-only e screenshot não vazio |
| Vídeo H.264/AAC | FFmpeg/FFprobe 7.1.1 pinados extraem quadros em 10%, 50% e 90% | arquivo vazio, inválido ou maior que 64 MiB bloqueia | duração válida e três JPEGs não vazios |
| Inspeção | mídia e landing abrem pelo MCP do anúncio reservado | falha visual mantém Têmis fora de `APPROVED` | request, response, URLs e decisão ligados ao creativeId |
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
3. Separar os runtimes: Playwright headless shell para a landing e FFmpeg/FFprobe pinados para o
   vídeo: esforço médio, isolamento preservado e suporte determinístico ao H.264/AAC.

A terceira alternativa foi escolhida porque corrige a causa-raiz sem enfraquecer o gate independente
nem aumentar o privilégio do container.

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

A produção permanece deliberadamente em `DRAFT/ADJUST` até que esta correção seja publicada e
Têmis execute uma nova revisão. Não houve aprovação forçada, campanha, gasto de mídia ou nova
geração de vídeo durante a homologação.
