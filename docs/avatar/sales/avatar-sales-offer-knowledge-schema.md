# Avatar de Venda — Offer Knowledge Schema

## Objetivo
Definir a estrutura única de conhecimento comercial que o Avatar de Venda usa para responder com consistência.

## Regras gerais
- toda resposta comercial deve derivar deste schema;
- campos vazios não podem ser inventados pelo motor;
- provas, urgência e garantia exigem fonte explícita.

## Schema canônico
```json
{
  "offer_id": "offer_001",
  "product": {
    "name": "Nome do Produto",
    "type": "curso|ebook|mentoria|template|software|assinatura",
    "category": "marketing digital",
    "headline": "Promessa principal do produto",
    "one_liner": "Resumo curto do produto em 1 frase"
  },
  "audience": {
    "ideal_customer": [
      "quem quer vender um produto digital",
      "quem precisa de um processo mais claro"
    ],
    "not_for": [
      "quem busca dinheiro rápido sem executar",
      "quem quer suporte fora da proposta do produto"
    ],
    "awareness_stage": "cold|warm|hot"
  },
  "pain_points": [
    "não sabe como apresentar o produto",
    "tem dificuldade de converter visitas em compras",
    "não consegue explicar valor com clareza"
  ],
  "desired_outcomes": [
    "aumentar conversão",
    "explicar melhor a oferta",
    "economizar tempo"
  ],
  "mechanism": {
    "core_method_name": "Nome do método",
    "summary": "Como o produto resolve o problema",
    "steps": [
      "passo 1",
      "passo 2",
      "passo 3"
    ]
  },
  "contents": {
    "modules": [
      {
        "title": "Módulo 1",
        "summary": "O que a pessoa aprende",
        "outcome": "Resultado desse módulo"
      }
    ],
    "bonuses": [
      {
        "title": "Bônus 1",
        "summary": "O que entrega",
        "value_anchor": "valor percebido opcional"
      }
    ]
  },
  "offer": {
    "price": 97,
    "currency": "BRL",
    "payment_options": ["à vista", "parcelado"],
    "guarantee": {
      "type": "7_dias",
      "summary": "Descrição da garantia"
    },
    "urgency": {
      "type": "none|date|bonus_expiry|price_change",
      "summary": "Motivo da urgência",
      "expires_at": null
    }
  },
  "proof": {
    "testimonials": [
      {
        "name": "Cliente A",
        "summary": "Resultado ou percepção",
        "source_type": "text|video|image"
      }
    ],
    "case_studies": [
      {
        "title": "Caso 1",
        "summary": "Contexto e resultado"
      }
    ],
    "credibility": [
      "número de alunos",
      "anos de experiência",
      "marcas atendidas"
    ]
  },
  "faq": [
    {
      "question": "Isso serve para iniciantes?",
      "answer": "Resposta oficial"
    }
  ],
  "objections": [
    {
      "key": "price",
      "visitor_language_examples": ["está caro", "não sei se vale"],
      "official_answer": "Resposta oficial para objeção de preço"
    }
  ],
  "cta": {
    "primary": "Ir para o checkout",
    "secondary": "Ver conteúdo do produto",
    "lead_capture": "Receber detalhes no WhatsApp"
  },
  "compliance": {
    "claims_not_allowed": [
      "resultado garantido",
      "dinheiro fácil",
      "sem esforço"
    ],
    "required_disclosures": [
      "assistente virtual",
      "resultados podem variar"
    ]
  }
}
```

## Regras de validação
- `product.name`, `product.type`, `offer.price` e `cta.primary` são obrigatórios;
- `proof` e `guarantee` são opcionais, mas não podem ser citados se estiverem vazios;
- `urgency.expires_at` é obrigatório quando `urgency.type` for `date`;
- `not_for` deve existir para reduzir venda errada;
- `claims_not_allowed` deve ser lido antes de qualquer resposta gerada.

## Exemplo mínimo preenchido
```json
{
  "offer_id": "mh_avatar_sales_001",
  "product": {
    "name": "Avatar de Venda para Produtos Digitais",
    "type": "software",
    "category": "marketing digital",
    "headline": "Explique sua oferta com um personagem de IA e converta visitantes com mais clareza",
    "one_liner": "Um assistente virtual que conversa com o visitante e apresenta seu produto antes da compra"
  },
  "audience": {
    "ideal_customer": [
      "quem vende infoprodutos",
      "quem recebe tráfego e não consegue converter bem"
    ],
    "not_for": [
      "quem quer promessas irreais",
      "quem não pretende testar a oferta"
    ],
    "awareness_stage": "warm"
  },
  "pain_points": [
    "a página sozinha não convence",
    "o visitante não entende o valor",
    "faltam respostas para objeções"
  ],
  "desired_outcomes": [
    "explicar melhor o produto",
    "aumentar engajamento",
    "levar mais gente ao checkout"
  ],
  "offer": {
    "price": 197,
    "currency": "BRL",
    "payment_options": ["à vista", "12x"],
    "guarantee": {
      "type": "7_dias",
      "summary": "Garantia padrão de 7 dias"
    },
    "urgency": {
      "type": "none",
      "summary": "",
      "expires_at": null
    }
  },
  "cta": {
    "primary": "Quero ativar meu avatar de venda",
    "secondary": "Ver como funciona",
    "lead_capture": "Receber demonstração"
  },
  "compliance": {
    "claims_not_allowed": [
      "vai dobrar suas vendas",
      "resultado garantido"
    ],
    "required_disclosures": [
      "assistente virtual"
    ]
  }
}
```
