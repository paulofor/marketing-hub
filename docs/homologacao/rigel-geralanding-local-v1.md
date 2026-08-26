# Homologação local do GeraLanding de Rigel — v1

Data: 2026-08-26

## Objetivo

Comprovar localmente que o experimento 89 do produto Rigel percorre o subprocesso
`landing-page-generation` sem publicação externa, produz uma landing responsiva, coerente com a
oferta de R$ 349, ligada ao checkout canônico, instrumentada e aprovada independentemente por
Quality Review, Psique e Têmis.

O avanço da cadeia só pode ocorrer quando a evidência persistida comprovar o objetivo do
subprocesso. Execução técnica, HTML gerado ou impacto estimado isoladamente não comprovam conclusão.

## Dados segregados

- produto local: Rigel / Kit WhatsApp Pronto, derivado apenas dos contratos versionados e dos campos
  comerciais lidos pelos endpoints oficiais;
- experimento local: identificador de homologação próprio, correlacionado ao experimento produtivo
  89 apenas como referência de origem;
- e-mail: `teste+<jobId>@sandbox.local` quando houver validação de entrega;
- eventos: marcados como `INTERNAL_QA` ou equivalente e excluídos de visitantes, checkout e vendas;
- publicação, campanha, contato e mídia paga: proibidos nesta matriz.

## Matriz ponta a ponta

| Área | Cenário | Evidência obrigatória | Critério |
| --- | --- | --- | --- |
| Caminho feliz | Iniciar pela tela e consumir o `pending` canônico | execução, request, response, tokens/custo e artefatos persistidos por etapa | wireframe → copy → planejamento visual → geração de imagens → preset/HTML → Quality Review → Psique → Têmis |
| Oferta | Renderizar promessa, público, preço e CTA de Rigel | texto e links extraídos do DOM | R$ 349, pagamento único, CTA canônico e ausência de promessa de conversão garantida |
| Escopo | Conferir o contrato comercial completo | texto extraído do DOM e snapshot de entrada | 10–20 respostas, 5–10 perguntas, 3–5 follow-ups manuais, escalonamento, guia, checklist, revisão humana, prévia e entrega em até 48 horas |
| Confiança | Conferir fornecedor, suporte e políticas da mesma oferta | contrato público e links HTTP 200 | razão social, CNPJ, endereço, suporte, termos, privacidade e reembolso presentes, sem misturar outro experimento |
| Checkout | Acionar cada CTA de compra em test double; inspecionar o destino real sem pagar | URL capturada, tela local e resposta do provedor | todos os CTAs apontam ao checkout canônico do experimento 89; R$ 349, pagamento único e ausência de recorrência; bloqueio antibot real é registrado como limitação, não como aprovação |
| Prova | Exibir o produto real e a degustação sem prova social fabricada | ativos aprovados, SHA-256, imagens inspecionadas e vídeo reproduzido | nenhuma imagem DRAFT, placeholder ou depoimento inventado; seis criativos finais coerentes e vídeo H.264 1080×1920 de 30 segundos íntegro |
| Responsividade | Abrir desktop, iPhone 15 Pro e Pixel 7 | screenshots e verificação de overflow | nenhuma sobreposição, corte, rolagem horizontal ou CTA inacessível |
| Acessibilidade | Navegar por teclado e inspecionar estrutura | foco visível, título, headings, labels e alt | caminho principal utilizável sem mouse e sem imagem essencial sem descrição |
| Falha de modelo | Resposta inválida ou schema incompatível | execução `FALHA` com erro e payload bruto | não avançar nem tratar resposta inválida como sucesso |
| Falha de fila | payload acima de 256 KB e mais pendências que o lote | consumo limitado e sem `DataBufferLimitException` | no máximo o limite solicitado, com demais itens preservados |
| Idempotência | repetir callback e revisar HTML idêntico | contagem de execuções e hash | sem duplicação de artefato, custo, revisão ou avanço |
| Convergência | Quality Review reprovar a primeira versão | delegação Dédalo e nova evidência diferente | correção da causa mais antiga, máximo de quatro versões e teto de US$ 5 |
| Aprovação | Quality Review, Psique e Têmis aprovarem a mesma evidência final | score, dimensões, screenshots full-page e focados nas provas, hashes e decisões | Quality Review com score ≥ 85, dimensões essenciais ≥ 8 e nenhuma regeneração; Psique `APPROVED`; Têmis `APPROVED` com todos os gates verdadeiros |
| Versão comercial | Comparar produto, experimento e slot publicado | contratos v2 e teste físico MySQL 5.7 | o slot ativo publica `kit-whatsapp-pronto-pde-v2`, inclusive quando não existe rascunho; reaplicação e retomada são idempotentes |
| Cadeia | Consultar posição do produto após início e aprovação | resposta do backend | entrada em `4.2` somente quando a execução oficial começar; conclusão somente com objetivo comprovado; próximo movimento é a atividade `Integrar canal, checkout, acesso e eventos` do processo 4 |
| Observabilidade | Inspecionar backend, AI Worker e Dédalo | logs correlacionados por experimento/job e health | request, response, URL, falha completa e custo localizáveis |
| Métricas | Renderizar com analytics de homologação | eventos segregados | zero visitante humano, zero venda, zero receita e zero gasto externo |

## Resultado local de 2026-08-26

- a candidata final preservou três CTAs canônicos, quatro provas aprovadas e todo o contrato de
  escopo, entrega, fornecedor, suporte e políticas;
- desktop 1440 px, iPhone 15 Pro e Pixel 7 ficaram sem overflow, erro JavaScript ou link placeholder;
- os onze ativos aprovados tiveram SHA-256 conferido; as seis imagens comerciais foram inspecionadas
  e o vídeo foi reproduzido nos dois dispositivos móveis;
- o checkout local segregado confirmou produto, R$ 349, pagamento único, fornecedor e ausência de
  recorrência; o Mercado Pago real respondeu com bloqueio antibot HTTP 403 durante inspeção sem
  pagamento, por isso a abertura humana do destino continua sendo gate de preflight após publicação;
- Quality Review aprovou a mesma candidata com 89/100, Psique aprovou a percepção da cliente e Têmis
  aprovou todos os gates comerciais usando o contrato v2 reparado;
- nenhuma landing foi publicada, nenhum contato foi realizado e métricas comerciais permaneceram
  em zero. A persistência oficial e o avanço da cadeia aguardam o lote versionado ser publicado e a
  execução pela tela.

## Regra de rodada

Uma rodada completa sem defeito encerra a homologação. Se a rodada revelar defeito, a causa-raiz deve
ser corrigida e a matriz inteira reiniciada; depois da última correção são exigidas duas rodadas
completas e consecutivas sem falhas.
