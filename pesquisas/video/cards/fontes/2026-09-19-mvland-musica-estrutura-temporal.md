# Fonte revisada — MVLAND 2.0: música como estrutura temporal do storyboard

Data da revisão: 2026-09-19

## Evidência observada

A Meitu anunciou o MVLAND 2.0 com o novo **Studio Mode** em 18/09/2026. O produto está ativo no web app e integra, em um único fluxo, compreensão da música, desenvolvimento criativo, storyboard, geração de tomadas e pós-edição.

O diferencial declarado é uma lógica **music-first**. O sistema analisa ritmo, clima, gênero e estrutura da faixa para planejar histórias e tomadas, em vez de usar a música apenas como camada adicionada no fim. A página oficial do MVLAND também descreve detecção de batida, BPM, drops, acentos e mudanças emocionais para sincronizar imagens e transições com a estrutura musical.

O Studio Mode adiciona controle no nível de tomada: o usuário pode modificar ou regenerar separadamente movimento de câmera, composição e ações de personagens. O editor multifaixa permite combinar material gerado por IA e assets originais no mesmo projeto, preservando revisão e montagem depois da geração inicial.

A plataforma é multimodelo. O Creative Canvas documenta modelos como Seedance 2.5, MiniMax H3 e Kling 3.0, enquanto o fluxo principal mantém análise musical, personagens, cenas, storyboard, geração, edição e exportação em um único ambiente. A página de preços indica planos a partir de **US$24/mês**, mas o custo efetivo depende de créditos e modelos escolhidos.

Há uma ressalva jurídica importante. Uma página comercial do MVLAND promete exportação 4K sem marca d'água e “full commercial rights” para o fluxo music-to-video, enquanto os Termos de Serviço publicados continuam dizendo que, salvo autorização expressa em contrário, o uso do serviço e dos conteúdos gerados é pessoal e não comercial. Antes de usar a plataforma em publicidade paga, essa permissão precisa ser confirmada para o plano/fluxo específico.

Não localizei documentação pública de API para o Studio Mode nem pesos abertos do sistema; portanto, ele deve ser tratado como produto web proprietário, não como backend self-hosted.

Fontes consultadas:
- https://www.businesswire.com/news/home/20260918263125/en/
- https://mvland.com/
- https://mvland.com/pricing
- https://mvland.com/infinite-canvas
- https://mvland.com/music-to-video
- https://mvland.com/agreements/terms-of-service

## Interpretação

O achado acrescenta um princípio diferente do card `video-trilha-condicionada-por-video`. Naquele caso, o rough cut orienta a criação da trilha; aqui, a **trilha existente vira a estrutura temporal que orienta roteiro, storyboard e montagem**.

Em vez de tratar música como acabamento, o harness pode extrair uma representação temporal — intro, verso, refrão, drop, silêncio, aceleração, clímax e final — e usá-la como restrição para duração das cenas, cortes, energia de câmera e distribuição de mensagens. Depois, somente as tomadas problemáticas precisam ser regeneradas.

## Aplicação possível no Marketing Hub

Para criativos em que música e emoção são relevantes, Apolo pode construir antes da geração um `audio_timeline` com batidas, seções, intensidade e pontos de transição. O storyboard mapeia elementos comerciais — hook, problema, demonstração, prova e CTA — para essa estrutura. Cada cena continua independente e editável, permitindo trocar renderer ou regenerar apenas um trecho.

O experimento deve comparar esse fluxo com um storyboard sem análise temporal de áudio, medindo aderência percebida entre som e imagem, quantidade de ajustes de timing, regenerações, tempo até aprovação e custo. Conversão e vendas devem permanecer métricas posteriores, não pressupostos.

## Limites

As afirmações sobre consistência, qualidade e sincronização vêm do fornecedor e ainda não encontrei benchmark independente específico do Studio Mode. O MVLAND é proprietário e não há API pública documentada do Studio Mode. A situação de licença comercial precisa de confirmação porque existe tensão entre a página de marketing e os Termos de Serviço atuais.
