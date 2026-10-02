# Capella — exemplos personalizados e vídeo de demonstração v1

Pedido de 02/10/2026: vários exemplos de posts de alta qualidade, vídeo dinâmico,
argumentos de venda e possibilidade de pedir amostra gratuita. Produto #7,
Quartzo, plano comercial #2. Perfil de Apolo #64 criado pela tela oficial.
Não autoriza mídia, compra, novo experimento nem substituição de peças aprovadas.

## Entrega e escolha

Seis posts, três identidades demonstrativas (Aurora Nails, Ateliê Jade, Studio Lis),
legendas e mensagens de WhatsApp. Nomes e localidades são fictícios e os avisos
integram as imagens. O vídeo demonstra a persona passando de não saber o que
publicar a ter arte, texto e sequência utilizáveis; não representa depoimento,
agendamentos ou receita reais.

Comparação: nova geração por IA oferece diversidade, mas adiciona custo e revisão;
repetir as peças anteriores preserva custo, mas demonstra pouca personalização;
compor layouts com fontes aprovadas permite mostrar identidade e utilidade com
custo de APIs controlado. Escolhida a composição determinística. O consumo pago
incremental é zero; processamento, operação e custo histórico das fontes não são
convertidos em custo integral zero.

Produção versionada em `scripts/marketing/create-capella-demo-assets-v1.mjs`,
briefing e hashes em `scripts/marketing/capella-demo-v1/`. Fontes #133–138 da
biblioteca aprovada e criativo #523 (usado na abertura). Hash divergente impede
produção. Saída inclui manifesto, arquivos individuais, legendas, roteiro por
tempo e locução. Novas peças são candidatas; aprovação da fonte não aprova a saída.

Locução local com Piper 1.3.0, modelo pt_BR-faber-medium, dados CC0, voz sintética
identificada. [CLI oficial](https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/CLI.md)
e [ficha da voz](https://huggingface.co/rhasspy/piper-voices/blob/main/pt/pt_BR/faber/medium/MODEL_CARD).
Sem clonagem, trilha sem licença ou geração audiovisual paga.

A oferta permanece R$ 67, pagamento único, 10 posts, 10 stories, 10 legendas,
cinco mensagens e calendário de sete dias. Entrega em até três dias úteis após
pagamento aprovado e briefing completo. Sem prometer fotos próprias, exclusividade,
arquivos editáveis, geração ilimitada, clientes ou agenda cheia.

Amostra limitada: uma peça demonstrativa e sua legenda, sem compra obrigatória
nem geração individual paga por solicitante. O canal é o e-mail de atendimento
já contratado, com assunto “Quero minha amostra Capella”; as peças prontas ficam
vinculadas ao plano para atendimento. Não pressupõe formulário, coleta automática,
disparo automático, prazo de resposta imediato ou opt-in de marketing.

## Matriz local de aceite, definida antes da validação

| Caso | Critério |
|---|---|
| Fontes e direitos | Hash exato, aviso de demonstração, negócios fictícios e fotos ilustrativas; fontes identificadas. |
| Seis exemplos | Nome, cidade, cores e serviço coerentes; texto legível em 1080×1080 e celular; sem sobreposição e telefone inventado. |
| Transformação e oferta | Arte, legenda, mensagem e sequência demonstradas; quantidades, R$ 67 e prazo iguais ao contrato. |
| Vídeo | MP4 H.264/AAC, 9:16, 6–60s, áudio não truncado, legendas até duas linhas, aviso de voz sintética e CTA de amostra. |
| Recuperação | Fonte alterada bloqueia antes da locução; erro de upload explicável e retentável, sem chamada de geração. |
| Roteiro e identidades | CTA persistida do próprio produto; ausência de roteiro não injeta MUSA; troca de perfil não mistura rascunhos. |
| Candidata opcional | Upload permite `requiredForRelease=false`; mantém revisão pendente e não altera aprovação de peças existentes. |
| Integração local | Tela com APIs simuladas: upload oficial → URL → vínculo DRAFT no plano; roteiro correto no perfil resolvido. |
| Desktop e mobile | Chromium desktop, iPhone 15 Pro e Pixel 7: reprodução, dimensões, controles e textos acessíveis. |
| Métricas e isolamento | Nenhum teste vira compra, evento comercial ou lead; consumo pago incremental zero; mídia não ativada. |

## Lacunas concretas do harness

O editor enviava CTA e legenda fixas do MUSA ao salvar qualquer produto. A correção
usa roteiro persistido do perfil e campos próprios, com fallback da CTA cadastrada
do produto. Intenção de comunicação é apresentada como intenção, sem deduzir
produto ou objetivo comercial por número no título. Regressões incluem perfis
independentes com nomes e IDs diferentes.

O formulário de upload de vídeo forçava `requiredForRelease=true`, tornando uma
candidata adicional requisito do piloto já homologado. A opção explícita preserva
o padrão anterior e permite anexar material opcional sem aprovar ou publicar.

A biblioteca aceitava URLs e geração paga, mas não expunha o upload já existente
para composição local. O importador de imagens usa `POST /api/assets`, sem fila
de IA, e conserva a criação DRAFT pelo contrato existente do plano. Metadados de
origem/direitos e manifesto continuam necessários; upload não equivale a revisão.

Resultados finais de testes, PR, SHA, runs e vínculos produtivos devem acompanhar
a resposta de entrega. Este registro define critérios e não antecipa aprovação.

## Resultado local em 02/10/2026

- Seis PNGs 1080×1080 inspecionados, com texto e CTA sem sobreposição.
- MP4 de 50,048s, 1080×1920, H.264/AAC 48kHz e menos de 6 MB; SHA-256
  `5df37fd3d1d1f5eda30279ec1569a0d6172d89cc92afcd72c74f79a50fc9fa8c`.
- Legendas de até duas linhas; reprodução e busca nos tempos 7s, 28s e 48s
  confirmadas no Chromium desktop, iPhone 15 Pro e Pixel 7 emulados.
- 819 testes frontend aprovados; typecheck e build aprovados. Integração pela
  tela local em três dispositivos preservou perfil, CTA e status DRAFT, com
  somente três escritas simuladas e nenhuma requisição externa/geração paga.
- Fonte adulterada bloqueou antes da leitura do modelo de voz e de produzir
  arquivo. A primeira composição teve texto encoberto pela CTA; após correção,
  o renderer passou a recusar esse caso por medição do layout. Codificação
  limitada a dois threads também evita uso irrestrito de recursos locais.
- O player comprovou reprodução e controles. A avaliação perceptual auditiva
  humana não foi executada; não é declarada aprovação de naturalidade da voz.
  A síntese usa a voz e o texto registrados; não há apresentador nem sincronismo
  labial a validar. A candidata permanece separada de autorização comercial.

[Manifesto dos arquivos](evidencias/capella-posts-amostra-video-v1-manifest.json).
Evidências locais em `/tmp/capella-creation/`: `generated/acceptance.json`,
`generated/mobile-playback.json`, `generated/mobile-seek.json`,
`authoring-evidence/results.json` e `frontend-tests.log`.
