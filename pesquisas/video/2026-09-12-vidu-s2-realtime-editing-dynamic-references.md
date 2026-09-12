# Radar IA para Vídeo — 2026-09-12

## Mudança material da rodada: Vidu S2

A principal novidade desta rodada é o **Vidu S2**, apresentado em paper submetido em 10 de setembro de 2026 por pesquisadores da ShengShu Technology e Tsinghua University. O sistema combina três linhas que vinham aparecendo separadamente no mercado: **avatar interativo em tempo real**, **edição de stream de vídeo em tempo real** e **controle agêntico com feedback visual durante a geração**.

O status precisa ser tratado com cuidado: em **12/09/2026**, o Vidu S2 ainda não está em disponibilidade geral. A página oficial do Vidu informa explicitamente que **“Vidu S2 will be available on September 15”**. Portanto, o status atual é **🟡 anunciado/limitado, com disponibilização prevista para 15/09/2026**. O catálogo público de API continua listando somente **Vidu S1** como modelo de streaming, em beta; não há endpoint público documentado do S2, preço específico nem anúncio de pesos abertos.

Fontes primárias:

- Paper Vidu S2: https://arxiv.org/abs/2609.11638
- Página oficial de streaming Vidu: https://www.vidu.com/vidu-stream
- Vidu API Model Map: https://platform.vidu.com/docs/model-map
- Vidu API Pricing: https://platform.vidu.com/docs/pricing

## O que mudou tecnicamente

### 1. Avatar em tempo real sobe de 540p para 720p

O Vidu S1, já disponível, trabalha com geração contínua de personagem em **540p a 25 FPS**, podendo chegar a 42 FPS em condições adequadas. O Vidu S2-Avatar eleva isso para **720p mantendo 25–42 FPS**.

Mais importante que a resolução, o S2 amplia o tipo de comportamento suportado. O S1 era fortemente orientado a talking heads e conversa. O S2 foi treinado para movimentos corporais mais amplos e instruções como dança, além de maior variedade de expressão e ação.

### 2. Referências visuais podem mudar durante a sessão

Essa é uma das diferenças mais interessantes em relação aos sistemas realtime que acompanhamos nos últimos dias.

No S2, o usuário pode introduzir **uma nova imagem de referência a qualquer momento durante o stream**. A referência pode representar, por exemplo:

- um objeto que o personagem deve pegar;
- uma roupa que deve vestir;
- um cenário para o qual deve se mover;
- um elemento que deve substituir outro.

Isso reduz uma limitação comum dos sistemas de vídeo contínuo atuais: o mundo deixa de depender apenas do prompt inicial e de instruções textuais subsequentes.

### 3. Edição de um vídeo que já está chegando em tempo real

O **Vidu S2-Editing** é um segundo modelo, separado do Avatar, dedicado a transformar um stream de entrada enquanto ele acontece. O paper descreve quatro operações principais:

- mudança de estilo visual;
- troca de roupa;
- substituição de personagem;
- substituição de fundo.

O modelo usa atenção alinhada por frame para preservar movimento e timing do vídeo de entrada enquanto aplica a nova aparência.

Isso aproxima o sistema de uma camada de efeitos/VFX generativos em tempo real, e não somente de um gerador de vídeo novo.

### 4. O próprio sistema contém um agente visual de autocorreção

Este é o ponto mais relevante para o Marketing Hub.

O paper descreve um **VLM agentic system**: o agente recebe instruções e imagens, produz o prompt para o gerador e, depois, **observa os frames gerados**. Ele tenta determinar se a ação solicitada:

- terminou corretamente;
- foi realizada apenas em parte;
- divergiu da instrução;
- ainda não pode ser confirmada.

A observação é usada para construir o prompt seguinte.

Um exemplo dado pelos autores é o comando para pegar um copo e depois sorrir. Quando o sistema observa que o copo foi realmente pego, os prompts seguintes passam a manter explicitamente o estado “personagem continua segurando o copo”. Se a ação falhar, o agente pode reformular a instrução.

Portanto, a arquitetura passa de:

```text
prompt 1 → vídeo 1
prompt 2 → vídeo 2
prompt 3 → vídeo 3
```

para:

```text
prompt
  ↓
geração
  ↓
inspeção visual do resultado
  ↓
estado confirmado / erro detectado
  ↓
próximo prompt
  ↺
```

Esse padrão é diretamente reutilizável em um harness audiovisual próprio.

## Comparação com outros sistemas realtime

| Sistema | Status em 12/09 | Resolução / stream | Controle durante execução | Referência visual nova durante stream | Edição de stream de entrada | API pública atual |
| --- | --- | --- | --- | --- | --- | --- |
| **Vidu S2** | 🟡 anunciado; disponibilidade 15/09 | 720p, 25–42 FPS | texto/voz + agente | **sim** | **sim** | ❌ ainda não documentada |
| **Vidu S1** | 🟢 ativo / API beta | 540p, 25–42 FPS | voz | não documentado | não | ✅ beta |
| **H3 Max Director / fal** | 🟡 ativo / API realtime alpha | 480p ou 768p, 24 FPS | novos prompts via WebRTC | imagem inicial fixa na configuração pública | não | ✅ WebRTC alpha |
| **Runway GWM Worlds 2** | 🟡 Research Preview | 720p/24 FPS + áudio 48 kHz | ações, fala, câmera e agentes | **não** depois do primeiro frame/prefill | continuação, não edição geral de stream | ❌ self-service |

Fontes de comparação:

- fal H3 Max Director: https://fal.ai/h3-max-director
- fal API Director: https://fal.ai/models/minimax/h3-max/director/api
- Runway GWM Worlds 2: https://runway.com/research/introducing-gwm-worlds-2

### Onde o Vidu S2 avança

O H3 Max Director continua sendo a opção mais acessível para um desenvolvedor experimentar **agora**, porque possui WebRTC e documentação de integração. GWM Worlds 2 continua mais sofisticado na representação de mundo persistente e eventos. O Vidu S2, porém, adiciona algo que não está documentado publicamente nesses dois: **referências visuais dinâmicas durante a sessão + edição de um stream externo + agente visual que verifica o que acabou de acontecer**.

Isso torna o Vidu S2 particularmente interessante para avatares que manipulam produtos, experiências de virtual try-on, publicidade interativa, apresentadores sintéticos, livestreams e pipelines em que o agente precisa corrigir visualmente o trabalho antes de continuar.

## Preço, API, pesos e licença

Em 12/09/2026:

- **Vidu S2:** não há preço público específico;
- **API S2:** ainda não aparece no Model Map público;
- **pesos abertos:** não anunciados;
- **licença comercial específica S2:** ainda não publicada em documentação própria do modelo.

O **Vidu S1**, que permanece ativo, custa atualmente **3 créditos a cada 2 segundos**. Como cada crédito custa US$0,005, isso equivale a aproximadamente **US$0,015 por 2 segundos**, ou **US$0,0075/s**, antes de possíveis custos adicionais de voz e concorrência. Esse preço não deve ser projetado para o S2 até que a ShengShu publique a tabela correspondente.

## Implicação para o Marketing Hub

O achado mais importante não é copiar o Vidu S2, e sim adotar seu princípio de **feedback visual fechado** no agente videomaker.

Um caminho de experimento seria:

```text
roteiro/tomada
   ↓
gerador escolhido pelo harness
   ↓
frames-chave da saída
   ↓
VLM revisor
   ├─ ação concluída?
   ├─ produto correto?
   ├─ personagem consistente?
   ├─ cenário consistente?
   ├─ artefatos graves?
   └─ estado final observado?
   ↓
aceitar / regenerar / corrigir próximo prompt
```

Isso é diferente de somente armazenar um estado persistente no prompt. O estado passa a ser **observado e confirmado** pelo agente antes de ser usado na continuação.

A hipótese deve ser testada em métricas de produção — taxa de regeneração, tempo até uma tomada aceitável, custo por criativo, erros de continuidade — e depois em métricas reais de funil. Não há evidência de que a arquitetura, sozinha, aumente CTR, checkout ou pagamento.

## Falso positivo descartado nesta rodada

Algumas páginas publicadas em 11–12 de setembro estão apresentando **Kling 3.0** como se fosse um novo lançamento. A verificação no site de relações com investidores da Kuaishou mostra que a família **Kling 3.0 foi lançada globalmente em 5 de fevereiro de 2026**. Portanto, essas matérias recentes são republicações/promocionais e **não contam como novidade desta rodada**.

Fonte oficial Kuaishou: https://ir.kuaishou.com/zh-hans/news-releases/news-release-details/keling30xiliemoxingquanmianshangxian

## Card criado

Foi criado um card porque o padrão é reutilizável independentemente do fornecedor:

- `cardKey`: `video-feedback-visual-agente-autocorrecao`
- ideia: adicionar ao videomaker um loop **geração → inspeção visual → estado confirmado → próxima instrução**;
- fonte revisada: `pesquisas/video/cards/fontes/2026-09-12-feedback-visual-agente-autocorrecao.md`;
- SHA-256: `ee4c5abbcb8a6f196a2ee5590b7f5e106bb3751226b1e0c5332dcc00d248eb59`.

O card é candidato a **DRAFT** e não transforma o paper em prova de impacto comercial.
