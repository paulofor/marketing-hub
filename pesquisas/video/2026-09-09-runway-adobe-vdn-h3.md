# Radar IA para vídeo — 2026-09-09

## Resumo executivo

Dois achados passaram o filtro de relevância desta rodada:

1. **Runway Plugins para Adobe Premiere Pro e After Effects** — lançado em 8 de setembro de 2026 e já ativo. O avanço não é um novo modelo, mas uma integração de produção importante: geração, edição, vídeo-para-vídeo, HDR, upscale e remoção de fundo passam a acontecer dentro da timeline do Adobe, usando modelos da Runway e de terceiros.
2. **VDN-H3 / Video DeltaNet** — o projeto abriu código de treinamento/inferência e pesos derivados do MiniMax H3 e, em 8 de setembro, ampliou o mesmo checkpoint para image-to-video, first/last-frame-to-video e last-frame-to-video. O benchmark dos autores mostra geração de 14,4 s de vídeo 768p em 11,23 s de denoising com 8× NVIDIA B200, mas esse número exclui várias etapas da latência ponta a ponta.

Há ainda um status operacional que deve permanecer explícito em qualquer comparação: **Sora não é uma opção de longo prazo**. O produto web/app foi encerrado em 26 de abril de 2026 e a OpenAI informa que a API será encerrada em **24 de setembro de 2026**.

---

## 1. Runway entra diretamente no Premiere Pro e After Effects

**Status: ATIVO.**

A Runway lançou em **8 de setembro de 2026** plugins oficiais para **Adobe Premiere Pro e After Effects**, em macOS e Windows. O plugin é gratuito para instalar; as gerações usam os créditos do plano Runway e estão disponíveis nos planos pagos.

### O que mudou

Dentro do painel da Runway no Adobe é possível:

- gerar vídeo com **Gen-4.5, Seedance 2.5, Kling 3.0 Pro e Veo 3.1**;
- gerar imagens com Gen-4 Image, Nano Banana e GPT Image 2;
- colocar os resultados diretamente no playhead/timeline;
- aplicar vídeo-para-vídeo com **Aleph 2.0**;
- converter ou gerar em HDR com **Runway Ruby**;
- fazer upscale e remoção de fundo sem sair do Premiere/After Effects;
- reutilizar a biblioteca e os workspaces da conta Runway.

### Por que importa

A mudança relevante é de **workflow**, não de benchmark de modelo. Até agora, muitos fluxos de IA exigiam exportar um frame ou clipe, abrir uma aplicação web, gerar, baixar e importar novamente. O plugin elimina esse ciclo para várias operações.

Também torna o Premiere/After Effects uma superfície **multimodelo**: Veo, Seedance e Kling passam a poder ser usados no mesmo ambiente em que o editor já está montando o material, enquanto Aleph 2 e Ruby cuidam de transformações de footage e finishing.

Isso favorece produção profissional, publicidade, social, clipes, curtas e VFX porque reduz a distância entre geração e decisão editorial. Para um harness audiovisual, a implicação é clara: o valor tende a migrar de apenas “escolher o melhor modelo” para **orquestrar o modelo certo dentro do estágio correto da timeline**.

### Comparação

- **Runway Plugins:** ativo e integrado diretamente ao NLE; forte em orquestração multimodelo e pós-produção.
- **Firefly no ecossistema Adobe:** continua relevante por integração nativa e recursos próprios, mas o plugin da Runway amplia o conjunto de modelos disponíveis no mesmo ambiente.
- **APIs isoladas de Veo/Seedance/Kling:** continuam mais adequadas para automação server-side; o plugin da Runway resolve principalmente a etapa humana de edição/finishing.

Fontes primárias:

- https://runway.com/news/company-news/runway-for-adobe
- https://runway.com/plugins

---

## 2. VDN-H3: geração acima do tempo real com pesos disponíveis

**Status: ATIVO como projeto de pesquisa/self-hosted; sem API pública hospedada pelos autores.**

O projeto **OpenVDN / VideoDeltaNet-H3** publicou código de treinamento, inferência e pesos derivados do **MiniMax H3**. Em **8 de setembro de 2026**, o projeto anunciou suporte, com o mesmo checkpoint, a:

- image-to-video (I2VA);
- first/last-frame-to-video (FL2VA);
- last-frame-to-video (L2VA);
- além do fluxo de texto para vídeo já publicado.

### Velocidade

No benchmark divulgado pelos autores, a configuração VDN-H3 de 8 passos em **8× NVIDIA B200** produz **14,4 segundos de vídeo 768p em 11,23 segundos de denoising**. Isso cruza o limiar de geração mais rápida que a duração do próprio clipe.

A ressalva é importante: os autores excluem desse tempo **carregamento do modelo, warm-up, decodificação VAE e codificação MP4**. Em uma aplicação real ainda entram fila, prompt rewriting, rede e orquestração. Portanto, o resultado não deve ser apresentado como latência ponta a ponta de 11,23 s.

### Arquitetura

O VDN-H3 combina o backbone MiniMax H3 com um ramo de atenção linear e adaptadores LoRA. A proposta é reduzir o custo da atenção mantendo um caminho softmax para preservar qualidade e consistência. O fato de treinamento e inferência estarem publicados torna o avanço mais auditável do que uma alegação de velocidade de um serviço fechado.

### Licença

A abertura precisa ser descrita com precisão:

- **código VDN-H3:** Apache 2.0;
- **pesos VDN-H3:** derivados do MiniMax H3 e sujeitos à **MiniMax H3 Community License**;
- o NOTICE oficial informa que o território aplicável exclui **União Europeia, Reino Unido, República da Coreia e Estados Unidos**.

Portanto, “pesos abertos” não significa licença permissiva global. Para qualquer uso comercial, a licença vigente deve ser revisada antes da implantação, inclusive considerando a região em que a inferência e a exibição ocorrerão.

### Por que importa

Este achado fortalece a tendência observada nos últimos dias com H3 Max Director e Yoroll: **vídeo pode começar a ser produzido rápido o suficiente para que a próxima cena seja criada enquanto a atual ainda está sendo exibida**.

O novo suporte a primeiro/último frame e last-frame-to-video adiciona algo especialmente útil para um harness: pontos de ancoragem entre cenas. Isso pode permitir uma cadeia do tipo:

`cena atual -> frame final aprovado -> geração da próxima cena -> frame final da próxima -> continuação`

Esse desenho pode ser útil em experiências interativas, variações de anúncios, histórias ramificadas e produção agêntica, desde que a continuidade real e a latência total sejam validadas em infraestrutura de produção.

Fontes primárias:

- https://github.com/OpenVDN/vdn-minimax-h3
- https://github.com/OpenVDN/vdn-minimax-h3/blob/main/NOTICE
- https://huggingface.co/OpenVDN/vdn-minimax-h3

---

## 3. Status que não pode ser confundido com disponibilidade atual: Sora

**Sora web/app: DESCONTINUADO desde 26/04/2026.**  
**Sora API: ENCERRAMENTO ANUNCIADO para 24/09/2026.**

A OpenAI mantém uma página de descontinuação informando que as experiências web e app do Sora foram encerradas em abril e que a API será desligada em 24 de setembro de 2026. Assim, mesmo que Sora 2/Sora 2 Pro ainda apareçam em catálogos ou provedores até essa data, não devem ser tratados como escolha estratégica para uma nova integração.

Fonte primária:

- https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation

---

## Conclusão

A novidade de maior impacto prático nesta rodada é a **Runway dentro do Premiere/After Effects**, porque aproxima IA generativa do local em que a decisão editorial realmente acontece. Já o **VDN-H3** é mais importante tecnologicamente para o horizonte de produção agêntica: ele oferece uma implementação aberta/auditável da ideia de gerar vídeo mais rápido que sua reprodução e adiciona controles de primeiro/último frame úteis para continuidade.

Para o Marketing Hub, a principal implicação é continuar tratando o **harness** como camada central: selecionar o modelo por etapa, preservar estado visual, usar frames de ancoragem e separar geração rápida para exploração da geração/finishing final. A evidência técnica não prova ganho comercial; isso precisa ser medido no funil real.