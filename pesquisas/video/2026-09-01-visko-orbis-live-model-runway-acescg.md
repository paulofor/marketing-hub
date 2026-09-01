# Radar de IA para vídeo — 2026-09-01

## Resumo executivo

Duas mudanças passaram o critério de relevância nesta rodada:

1. **Visko abriu acesso público ao Orbis 1.0**, um modelo de vídeo interativo em tempo real com memória persistente e geração de longa duração. É uma mudança de paradigma em relação aos geradores convencionais de clipes: o vídeo continua sendo gerado enquanto o usuário altera o prompt.
2. **Runway adicionou entrega ACEScg/OpenEXR** aos fluxos HDR da API, aproximando ainda mais vídeo gerado por IA de pipelines profissionais de composição e finalização.

---

## 1. Visko Orbis 1.0 — vídeo que continua rodando e pode ser dirigido em tempo real

**Mudança:** em **1º de setembro de 2026**, a Visko anunciou e abriu acesso público ao Orbis.

**Status atual:** 🟢 **ATIVO / acesso público por playground e demos**.

**API pública:** ⚠️ **não encontrada/documentada**.

**Preço público:** ⚠️ **não encontrado**.

**Licença/uso comercial:** ⚠️ **ainda não está suficientemente definida publicamente**; a página de Termos de Serviço da Visko está marcada como “Coming soon”. Portanto, não deve ser adotado ainda como componente comercial crítico sem contrato/termos explícitos.

### O que o Orbis faz

O Orbis é descrito pela Visko como um **Live Model**: em vez de receber um prompt, renderizar um clipe fechado e encerrar, ele mantém um processo contínuo de geração. O usuário pode mudar o prompt enquanto o vídeo está sendo produzido e a nova direção é incorporada à sequência em tempo real.

Capacidades publicadas:

- texto → mundo/vídeo;
- imagem → mundo/vídeo;
- continuação de vídeo;
- troca de prompt durante a geração;
- memória persistente de sujeito, cena e estilo;
- geração em escala de **uma hora ou mais** nos testes publicados;
- saída entregue em **4K a 24 fps em tempo real**;
- interação por texto e, em demos, por voz;
- aplicações demonstradas em storytelling, live streaming, personagens virtuais, e-commerce, robótica e mundos interativos.

### Atenção ao “4K”

O relatório técnico descreve um **gerador em streaming + um upscaler de vídeo em streaming**. Portanto, o 4K deve ser entendido como **saída 4K do pipeline**, e não como evidência de que cada quadro seja gerado nativamente em 4K pelo modelo-base.

### O que realmente muda

Os geradores atuais de produção — Veo, Wan, Seedance, H3, LTX, Runway etc. — trabalham principalmente com a lógica:

`prompt → render → clipe → nova chamada`

O Orbis tenta operar como:

`mundo persistente → stream contínuo → intervenção do usuário/agente → mundo continua`

Isso pode alterar profundamente:

- filmes e narrativas interativas;
- transmissões geradas em tempo real;
- influenciadores virtuais que respondem ao público;
- anúncios e live commerce adaptativos;
- jogos e ambientes de treinamento;
- pré-visualização/virtual production;
- simulação para robótica.

### Qualidade e evidência

No benchmark publicado pela própria Visko, o Orbis obteve o maior Elo geral entre oito sistemas de vídeo interativo/long-form avaliados (**1838**) e o maior Elo de estabilidade temporal (**1940**). Porém, é importante tratar isso como **benchmark do próprio desenvolvedor**, não como validação independente definitiva.

### Comparação prática

| Sistema | Status | Paradigma | Duração | Interação durante geração | Melhor uso hoje |
|---|---|---|---|---|---|
| **Visko Orbis 1.0** | 🟢 público, sem API/preço claros | stream persistente | hora+ nos testes | ✅ | mundos e vídeo interativo ao vivo |
| **Wan 3.0** | 🟢 ativo/GA | clipe por chamada | até 30 s | ❌ | publicidade, referência multimodal |
| **Seedance 2.5** | 🟢 ativo | clipe/edição por chamada | até 30 s | ❌ | controle e referências multimodais |
| **Gemini Omni 1.1 Flash** | 🟢 GA na Gemini API | geração/edição conversacional | até 40 s acumulados | por turnos | edição e extensão iterativa |
| **Runway Gen-4.5** | 🟢 ativo | geração por chamada | clipes | ❌ | produção e pós-produção profissional |

**Leitura:** Orbis não é hoje um substituto direto de Veo/Wan/Seedance para entregar um anúncio final. Ele é mais interessante como **nova infraestrutura de vídeo interativo e persistente**. A maior limitação comercial atual é a ausência de API, preço e termos de uso públicos claros.

---

## 2. Runway — ACEScg/OpenEXR para HDR profissional

**Mudança:** em **31 de agosto de 2026**, a Runway adicionou suporte a **ACEScg OpenEXR** nos fluxos HDR da API.

**Status atual:** 🟢 **ATIVO na API**.

A Runway agora pode entregar HDR como sequências OpenEXR scene-referred em **ACES 1.3 ou ACES 2.0**, tanto pelo endpoint `/v1/video_to_hdr` quanto em gerações HDR do Gen-4.5.

Isso permite que os frames entrem diretamente em pipelines ACES usados em composição, VFX e finishing, com sidecars de colorimetria/proveniência e `audio.wav` quando o material-fonte possui áudio.

Os mesmos adicionais de HDR continuam valendo: **20 créditos/s**, ou **40 créditos/s** para saídas maiores que aproximadamente 4 MP.

### Por que isso importa

Essa mudança não melhora a semântica ou o realismo do modelo em si; ela melhora **a integração industrial do material gerado**. Um pipeline pode agora ser:

`Gen-4.5 / vídeo externo → HDR/ACEScg EXR → Nuke/Fusion/Resolve → composição e grade → master`

É mais um sinal de que a disputa está saindo apenas de “quem gera o melhor clipe” e avançando para **quem entrega ativos prontos para workflows profissionais de cinema, publicidade e VFX**.

---

## Conclusão

A novidade mais estratégica é o **Visko Orbis**. Se o modelo conseguir manter, fora dos demos do fabricante, baixa latência e continuidade por longos períodos, ele inaugura uma categoria diferente da geração convencional de vídeo: **vídeo como processo persistente e interativo**, não como arquivo renderizado por chamada.

Para produção comercial convencional hoje, porém, Wan, Seedance, Veo, Gemini Omni, MiniMax e Runway continuam mais maduros por terem APIs, preços e termos comerciais melhor definidos. O Orbis deve ser acompanhado de perto principalmente pela abertura de **API, preços, direitos comerciais e benchmarks independentes**.

## Fontes

- Visko — anúncio oficial de 01/09/2026: https://www.visko.ai/news/visko-closes-10-million-pre-seed-round-and-launches-orbis
- Visko — página de modelos/demos: https://www.visko.ai/models
- Visko Orbis 1.0 — paper: https://arxiv.org/abs/2607.26694
- Visko — Termos de Serviço (ainda “Coming soon” em 01/09/2026): https://www.visko.ai/terms
- Runway — API Changelog: https://docs.dev.runwayml.com/api-details/api_changelog/
