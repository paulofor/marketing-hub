# Matriz de homologação — sucessor Facebook em vídeo do Capella v1

## Escopo

Criar o experimento sucessor do #88 mantendo produto, preço de R$ 67, página, checkout e público, e isolando a comparação entre o criativo estático vertical aprovado e um vídeo vertical curto com peças reais do kit.

## Critério financeiro

- Teto absoluto de mídia: R$ 100.
- Interrupção sem compra: R$ 50.
- Contribuição unitária antes da mídia: R$ 53,50 (R$ 67,00 menos R$ 13,50 de custo variável).
- Evidência mínima para repetição: duas compras líquidas conciliadas dentro do teto, contribuição positiva e ausência de falha em checkout, entrega ou rastreamento.

## Cenários

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Vincular o #94 ao #88 pela edição do experimento | #94 recebe a mesma página e checkout e registra `sourceExperimentId=88` |
| Segregação | Consultar campanha, métricas, público e criativos após o vínculo | Nenhuma execução do #88 é copiada; os ativos do #94 são selecionados separadamente |
| Validação | Tentar origem com produto, hipótese, preço, território, tipo ou identidade Meta divergente | Backend responde conflito sem alterar o #94 |
| Validação financeira | Informar parada menor que R$ 25, acima de R$ 100 ou sem meta de compras | Backend e interface bloqueiam o planejamento inconsistente |
| Concorrência | Repetir o mesmo comando de vínculo | Operação é idempotente e não cria outro experimento |
| Criativo controle | Reutilizar a peça vertical #523 | Nova cópia pertence ao #94 e volta aos gates de revisão |
| Criativo variante | Produzir vídeo vertical de 15–20 segundos com os posts e stories aprovados do produto | Vídeo explicita desejo, prova, pacote pago, preço e prazo, sem sugerir trabalhos de clientes, depoimento ou antes/depois inventado |
| Upload governado | Anexar o MP4 pela aba Vídeos do #94 | Backend valida assinatura MP4, 9:16, duração, áudio, origem e referência versionada; grava custo zero e mantém revisão `PENDING` |
| Fontes do vídeo | Informar #522 e #523 no upload | Backend confirma que ambos pertencem ao #88, estão aprovados técnica e humanamente e preserva URLs e instantes de aprovação em formato ISO no snapshot; snapshots numéricos legados continuam verificáveis |
| Integridade do arquivo | Revisar o criativo em vídeo | SHA-256 calculado pelo backend coincide com o arquivo decodificado pelo MCP; fontes visuais também são inspecionadas |
| Linhagem do sucessor | Revisar o controle #531 e a variante em vídeo | Têmis recebe #88 como origem verificada e não trata URLs herdadas como divergência isolada |
| Checkout observável | Inspecionar a landing herdada | O coletor encontra o checkout canônico nos links reais do DOM, abre-o sem interação e comprova `checkoutLinkedFromLanding=true` |
| Arquivo inválido | Renomear outro arquivo para `.mp4`, usar horizontal, exceder 50 MB ou omitir áudio/proveniência | Interface ou backend bloqueia antes de disponibilizar a peça |
| Observabilidade | Abrir detalhe e cockpit do #94 | Orçamento, funil, compras, receita e contribuição ficam separados do #88 |
| Falha comercial | Atingir R$ 50 sem compra líquida | Campanha deve ser interrompida e a hipótese não pode ser escalada |
| Margem | Atingir duas compras líquidas dentro de R$ 100 | Resultado pode avançar para repetição somente após conciliação de receita, custos e entrega |
| QA de interface | Abrir edição/detalhe em desktop, iPhone 15 Pro e Pixel 7 | Vínculo, criativos, vídeo e indicadores permanecem legíveis e operáveis |

## Copy do vídeo

1. Desejo: “Seu trabalho é caprichado. Seu Instagram mostra isso?”
2. Primeiro passo: “Veja amostras do kit em poucos segundos.”
3. Prova: mostrar dois posts e dois stories aprovados do Capella.
4. Continuidade paga: “Por R$ 67: 10 posts, 10 stories, 10 legendas, 5 mensagens e calendário personalizado, em até 3 dias úteis.”
5. Margem: “Repetir somente com duas compras líquidas, entrega confirmada e contribuição positiva dentro do teto de R$ 100.”

O quinto ponto é um gate interno do experimento, não uma promessa exibida à cliente. A primeira montagem permanece reproduzível em `scripts/marketing/create-capella-successor-video-v1.sh`; a candidata comercial revisada usa `scripts/marketing/create-capella-successor-video-v2.sh`, que mantém os mesmos hashes dos criativos aprovados #522 e #523 e chama as peças de amostras do kit, sem sugerir trabalhos de clientes.
