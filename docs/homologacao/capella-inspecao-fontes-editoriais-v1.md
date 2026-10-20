# Capella — inspeção independente das fontes editoriais

Em 20/10/2026, a segunda revisão do anúncio #540 reconheceu V5 VERIFIED, identidade do
vídeo #76, voz nativa e destino/checkout de R$ 67. Manteve ADJUST porque o MCP mostrava
três quadros finais e identificadores das fontes, sem entregar os pixels de origem nem
os textos/recibos completos da locução. O parecer não alegou falsidade ou violação.

O histórico confirma a diferença: montagens V3/V4 já entregavam fontes visuais pelo MCP;
a receita editorial nova só tinha a projeção do backend. Testar apenas essa projeção
e a presença de instruções no prompt não comprovava a capacidade de inspeção do agente.

Foram comparadas três alternativas:

1. Embutir imagens em base64 no prompt: duplica dados, aumenta contexto e não oferece
   visualização adequada nem leitura vinculada ao anúncio.
2. Dar ao MCP um leitor de URL arbitrária: simplifica o código, mas amplia acesso e
   não comprova que os pixels pertencem ao criativo reservado.
3. Entregar fontes pelo controller canônico de criativos e pelo contrato tipado de
   auditoria: preserva escopo e hashes, permite comparação visual e reaproveita tudo.
   Escolhida, sem nova geração, síntese, seleção humana ou consulta de faturamento.

## Contrato e matriz definidos para validação local

- `GET /api/internal/creatives/{id}/agent-review/product-proofs/{taskId}/{evidenceId}?experimentId=...`
  fornece PNG sem cache, somente com governança V5 válida, mesmo anúncio/experimento e
  fonte declarada. Revalida pixels por SHA-256 e o gate/tenant do projeto de origem.
- `originalReceipt` expõe os campos permitidos dos requests/responses originais de Speech;
  inclui texto, modelo, voz, parâmetros, endpoint, requestId, bytes e hash. Nenhum cabeçalho
  de autenticação ou payload binário é exposto. `narrationText` conserva a sequência incorporada.
- `inspecionar_midia` usa `EDITORIAL_MEDIA_INSPECTION_V1`: três quadros básicos e momentos
  efetivos das cenas, capturas recortadas, artes e recibos. Confere hashes antes de mostrar
  pixels; deduplica downloads e registra a relação com trecho, intervalo e quadro.
- Imagens possuem limite físico de 12 MiB, dimensões e recortes verificados pelo decoder.
  Só números entram no filtro; expressões de auditoria não são executadas.
- Hash, contexto, recorte, momento ou recibo inválido fecha a ferramenta e o gate. Sem
  fontes, o agente não pode aprovar por cadastro. A inspeção não afirma escuta de áudio.
- Caso original, outra identidade e caminho V4 antes válido são exercitados pelo MCP stdio
  real com backend local e mídias de teste. Testes não publicam nem representam demanda.

## Resultados locais

- Backend: 188 testes aprovados, incluindo 92 regras de arquitetura, inicialização Spring,
  escopo, hash, PNG inválido, ausência de parâmetro e caminhos legados. A ausência de parâmetro
  revelou uma lacuna do handler global: agora retorna 400, preservando erros técnicos como 500.
  O teste usa controller, leitor e porta real de vídeo juntos: a rota interna não depende do
  header administrativo de vídeo, mas continua recusando tenant, produto ou fonte divergentes.
  A rota administrativa conserva a exigência do header; não se força contexto de outro tenant.
- Worker: 115 testes aprovados; uma integração antiga de Vega permanece condicionada à
  fixture externa. A asserção estática do decoder foi atualizada para os momentos adicionais.
- Container local: não root, filesystem somente leitura, rede externa desativada, dois
  CPUs, 1 GiB e FFmpeg 7.1.1 com o mesmo digest da produção. O MCP real passou em duas
  identidades, hash errado, recibo ausente, outro experimento, recorte fora da imagem e V4.
- Replay dos arquivos reais: seis imagens de origem, dez quadros finais nos momentos
  registrados e sete recibos de voz, com o mesmo SHA-256 final
  `ade2733d07c435ed65e20403f6ef111609e8450691036824856a9b3f08f87119`.
  O recorte foi conferido visualmente. Nenhuma mídia real foi alterada ou enviada ao mercado.
- O bundle atual passou com 123 manifestos e 540 arquivos, sem reescrever atestações.

O daemon Docker isolado não enxerga o caminho do bind mount da sandbox. A homologação
usou um build temporário de contexto restrito aos módulos e fixtures, sem privilégios ou
socket do host. Os arquivos reais foram coletados por leitura e conferidos por hash;
o acesso direto ao CDN retornou 403, e a leitura operacional autorizada recuperou os mesmos bytes.

## Estimativa e limites

O catálogo persistido de `gpt-6.1-sol` foi conferido em 20/10: US$ 2/10 por milhão de
tokens de entrada/saída Standard. É referência interna, não cobrança ChatGPT. Preparação
conhecida anterior: US$ 9,884244; sete etapas da página: US$ 0,738528 estimados pelo uso.

Para planejar os pareceres sem extrato nem tokens reportados pelo revisor, adota-se um
envelope **estimado** de 250 mil tokens de entrada e 35 mil de saída por revisão, ou US$ 0,85.
Esses números são premissas conservadoras, não uso medido. O envelope cobre contexto,
ferramentas, imagens e resposta, acima do tamanho dos pedidos observados e da rodada
histórica de três revisões de aproximadamente US$ 1,09. São até cinco revisões consideradas
nesta passagem: três do anúncio (incluindo as duas já encerradas) e duas comerciais.
Projeção: US$ 10,622772 + US$ 4,25 = **US$ 14,872772**, dentro do teto cumulativo US$ 15.
Não preenche `providerCostUsd`, não inventa tokens e não converte valor ausente em zero.
Novo trabalho além desse envelope exige reavaliar o saldo; não autoriza elevar o teto.

Audiovisual permanece US$ 10; anúncios R$ 100 totais/cinco dias após homologação;
contribuição de 30% continua como critério de continuidade/escala, com descoberta limitada
já autorizada. O token Meta expirado continua bloqueio externo da ativação.

Hipótese reutilizável: entregar fontes efetivamente inspecionáveis reduz devoluções
documentais e novas gerações desnecessárias. O teste comprova a capacidade executável;
o novo parecer independente comprovará o aceite dessa passagem. Vendas e contribuição
exigem medição real posterior. Manter falha fechada quando a inspeção não se completa.

A rotina agendada de integração de pesquisas do Harness #39419750815, no mesmo HEAD,
sinalizou 39 branches pendentes, zero erros de limpeza. Não foi CI/deploy desta alteração;
nenhuma branch de pesquisa foi apagada ou integrada por esta solicitação.
