# Radar IA para Vídeo — 2026-09-03

## 1. World Labs Atlas: novo paradigma de vídeo com controle espacial explícito

**Status em 03/09/2026:** 🟡 **LIMITADO / early access para parceiros selecionados**. Não há API pública do Atlas, preço público ou data de GA anunciada. O modelo foi apresentado em **1º de setembro de 2026** e não entrou na rodada daquele dia; esta atualização corrige essa omissão.

A World Labs, empresa cofundada por Fei-Fei Li, apresentou o **Atlas**, um *omni world model* multimodal treinado do zero para operar sobre texto, imagens, vídeo e 3D. A arquitetura é descrita como um **multimodal autoregressive diffusion transformer**, com entradas ancoradas em posições 3D para formar um “contexto espacial”.

### O que mudou de fato

O ponto mais relevante para produção audiovisual é que o Atlas trata a **trajetória da câmera como entrada geométrica nativa**, e não apenas como texto do tipo “pan para a direita” ou “crane shot”. A World Labs afirma que o modelo consegue:

- gerar vídeo a partir de 1 a 6 imagens de referência;
- controlar posição e orientação da câmera de forma explícita;
- produzir vídeos de até **1 minuto em 1440p**;
- reconstruir cenas reais em 3D a partir de poucas imagens;
- gerar novas perspectivas mantendo coerência geométrica;
- produzir point clouds e **3D Gaussian splats**;
- fazer *video reframing* / “bullet time” a partir de poucas câmeras comuns;
- converter gravações reais em ambientes de simulação para robótica (*real-to-sim*).

A demonstração oficial de vídeo longo usa uma trajetória de câmera desenhada manualmente e poucas imagens de referência para gerar **1 minuto em 1440p** dentro de um mundo coerente.

### Por que isso importa para vídeo

Modelos como Veo, Seedance, Wan, Kling e MiniMax são fortes em gerar tomadas a partir de prompt e referências, mas normalmente o controle de câmera é especificado de forma semântica. O Atlas tenta colocar a câmera em uma camada mais próxima de um pipeline 3D/VFX: o diretor define **onde a câmera está e por onde ela se move**, e o modelo renderiza o mundo a partir disso.

Isso é especialmente relevante para:

- publicidade de produto;
- virtual production;
- VFX;
- cenas com movimentos de câmera repetíveis;
- previs e storyboard cinematográfico;
- reconstrução de locações;
- efeitos de *bullet time*;
- games e experiências interativas;
- pipelines que precisam integrar vídeo gerado e geometria 3D.

### Comparação prática

| Sistema | Status | Controle de câmera | Duração / resolução | Áudio nativo | Melhor uso atual |
|---|---|---|---|---|---|
| **World Labs Atlas** | 🟡 Early access | **Geometria/camera pose nativa** | até 1 min / 1440p | não anunciado | VFX, previs, mundos coerentes, câmera precisa |
| **Veo 3.1** | 🟢 Ativo/GA | prompt + controles do endpoint | clipes curtos | sim | geração final de alta qualidade |
| **Seedance 2.5** | 🟢 Ativo | prompt + referências multimodais | até 30 s | sim | edição, referências e continuidade |
| **Wan 3.0** | 🟢 Ativo/GA | prompt + multimodal | até 30 s | sim | custo-benefício e peças mais longas |
| **Runway** | 🟢 Ativo | workflows/modelos + pós | variável | depende do modelo | pipeline profissional e integração criativa |

O Atlas **não é hoje a melhor opção de produção**, porque sua disponibilidade comercial real ainda é limitada. O valor do lançamento é arquitetural: ele sugere que o próximo salto em vídeo por IA pode vir de modelos que entendem explicitamente **espaço, câmera e geometria**, e não apenas de melhorias visuais em geradores de clipes.

### Disponibilidade, preço e licença

- **Atlas:** early access para parceiros selecionados.
- **API pública do Atlas:** não disponível.
- **Preço do Atlas:** não divulgado.
- **Pesos abertos:** não anunciados.
- **Marble / World API:** continua sendo o produto público da World Labs; Atlas deve alimentar versões futuras do Marble e outros produtos.
- Nos termos gerais da World Labs, **usuários pagos/API possuem os direitos sobre os outputs** e podem usá-los comercialmente, mas o Atlas em early access pode ter termos adicionais via acordo/Order Form; portanto, não convém assumir que as condições do Marble se aplicam integralmente ao Atlas sem verificar o contrato de acesso.

### Ressalvas

Os benchmarks de câmera e reconstrução publicados são da própria World Labs. A empresa compara Atlas com modelos de vídeo usando a trajetória da câmera como entrada nativa no Atlas e instruções textuais nos concorrentes, o que demonstra justamente a vantagem arquitetural proposta, mas não equivale a uma comparação perfeitamente simétrica de qualidade geral. Ainda faltam preço, API pública e validação independente em escala.

**Fonte oficial:** https://www.worldlabs.ai/blog/atlas

**Termos da World Labs:** https://www.worldlabs.ai/terms-of-service

---

## 2. Adobe for Slack: produção criativa passa a ser acionada por agente dentro da conversa

**Status em 03/09/2026:** 🟢 **ATIVO**, lançado em **2 de setembro de 2026** para clientes Slack **Business+ e Enterprise+**, globalmente em desktop, web e mobile.

A Adobe colocou mais de **70 ferramentas** de Firefly, Adobe Express, Photoshop, Premiere, Acrobat, InDesign, Illustrator, Stock, Lightroom e outras dentro do Slack. O usuário descreve o resultado desejado ao **Slackbot**, que pode buscar contexto de conversas, arquivos, canais e Canvases e chamar as ferramentas Adobe apropriadas.

Para vídeo, a integração permite transformar contexto de projeto em conteúdo, criar vídeos, reutilizar ativos do Creative Cloud e adaptar vídeos aprovados para diferentes formatos sociais. Isso é relevante porque desloca parte do workflow para um padrão **agente → ferramentas criativas**, em vez de o usuário operar manualmente cada aplicativo.

A integração não representa um novo modelo de vídeo e, por isso, é menos importante tecnicamente do que o Atlas. Mas é um avanço importante na camada de **harness/orquestração audiovisual**: briefing, feedback e referências já presentes no Slack podem se tornar entrada direta de um agente que chama Firefly/Premiere/Express.

**Fonte oficial:** https://blog.adobe.com/en/publish/2026/09/02/introducing-adobe-for-slack

---

## Conclusão da rodada

A novidade estrutural é o **Atlas**. Ele introduz um caminho diferente do gerador de vídeo tradicional: **mundo 3D coerente + câmera explícita + vídeo derivado desse espaço**. Ainda não é uma opção comercial madura por estar limitado a early access, mas merece monitoramento prioritário porque pode afetar VFX, cinema, publicidade e virtual production.

A Adobe, por sua vez, reforça outra tendência do radar: a produção audiovisual está sendo incorporada a **agentes que operam ferramentas criativas** com contexto do trabalho, e não apenas a modelos isolados de geração.
