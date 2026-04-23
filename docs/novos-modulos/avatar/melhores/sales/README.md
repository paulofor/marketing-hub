# Avatar de Venda — Visão Geral

## Objetivo
Criar um módulo separado de **Avatar de Venda** dentro do Marketing Hub para transformar visitantes em compradores de produtos digitais por meio de diálogo orientado à oferta, clareza, prova e CTA.

## Relação com o módulo de Avatar existente
O módulo `docs/novos-modulos/avatar/` continua responsável por:
- cadastro e gestão do avatar;
- treino/renderização;
- composição de cena;
- integração com providers;
- armazenamento e observabilidade de mídia.

O módulo `docs/novos-modulos/avatar/sales/` passa a ser responsável por:
- estratégia de conversa;
- entendimento da oferta;
- qualificação do visitante;
- manejo de objeções;
- provas e CTA;
- experimentação orientada a conversão.

## Princípios
1. **Persuasão honesta**: explicar valor, reduzir dúvida e orientar a decisão sem promessas enganosas.
2. **Transparência**: o visitante deve entender que está falando com um assistente virtual.
3. **Oferta primeiro**: o avatar não improvisa a proposta comercial; ele responde com base em um schema estruturado da oferta.
4. **Modularidade**: o motor de vendas não depende do provider de vídeo.
5. **Medição obrigatória**: toda conversa relevante gera eventos de funil.

## Escopo incluído
- saudação contextual;
- descoberta de necessidade;
- recomendação de produto;
- explicação de módulos, bônus e garantia;
- tratamento de objeções comuns;
- CTA para checkout, demo, lead capture ou conversa humana;
- instrumentação de funil.

## Fora do escopo
- geração/renderização do vídeo em si;
- billing do checkout;
- CRM outbound;
- moderação jurídica completa por jurisdição.

## Estrutura sugerida
- `avatar-sales-dialogue-orchestration.md`
- `avatar-sales-offer-knowledge-schema.md`
- `avatar-sales-character-profiles.md`
- `avatar-sales-objection-playbook.md`
- `avatar-sales-conversion-events-spec.md`

## Fluxo resumido
1. Visitante entra na página.
2. Sistema identifica contexto de origem, produto e estágio do funil.
3. Avatar inicia conversa curta e contextual.
4. Motor de diálogo identifica intenção e barreiras.
5. Resposta usa base estruturada da oferta.
6. Objeções são tratadas com prova, clareza e CTA.
7. Eventos são enviados para analytics.

## Roadmap recomendado
### Fase 1 — Texto + imagem
- chat de venda com persona visual estática;
- foco em aprendizado de objeções e conversão.

### Fase 2 — Vídeos curtos prontos
- mensagens em vídeo para boas-vindas, prova e fechamento;
- fallback para texto quando o vídeo não for necessário.

### Fase 3 — Personalização
- segmentação por origem do tráfego;
- versão por produto;
- variações por persona e por estágio do visitante.
