# Descoberta e priorização de oportunidade PDE — 27/08/2026

## Decisão

O processo terminou em **PESQUISAR MAIS**. Foram realizadas três tentativas independentes, mas
nenhuma produziu uma oportunidade comprovada similar ou superior ao benchmark interno de Rigel,
82/100. Nenhuma nota, pesquisa ou página de preço foi tratada como venda.

O melhor sinal para uma próxima rodada é **Auditoria de Saída 10min**, voltada a inquilinos que
receberam cobrança após a vistoria de saída. Ela não foi promovida a produto: ainda faltam prova
comercial suficiente, cobertura de canal e comportamento privado observado.

Não foram criados produto, oferta, preço, checkout, experimento, campanha ou ativo público. Não
houve contato com consumidor, pagamento, venda, receita, mídia ou publicação.

## Gargalo, métrica e critério

- **Gargalo real:** não existe nova oportunidade PDE com evidência suficiente para ser comparada
  ao Rigel.
- **Evidência inicial:** os ciclos oficiais 37, 38 e 39 foram criados pela tela e coletaram sinais
  públicos, mas terminaram com HTTP 422; nenhuma oportunidade foi persistida.
- **Métrica esperada:** ao menos uma candidata com dez ofertas pagas distintas e aderentes,
  cobertura do canal, duas leituras privadas aprovadas, consenso dos agentes e score estritamente
  superior a 82.
- **Continuar:** todos os gates passam sem inferir venda.
- **Ajustar:** a dor aparece, mas faltam fontes, preferência sobre o gratuito ou avanço ao checkout.
- **Parar:** evidência duplicada ou irrelevante, risco não controlável, gratuito preferido ou score
  até 82.

## Tentativas locais corrigidas

| Tentativa | Evidências públicas | Domínios independentes | Ofertas aderentes | Resultado |
| --- | ---: | ---: | ---: | --- |
| Vistoria de saída e dano preexistente | 9 | 7 | 0 | `RESEARCH_MORE`; qualidade de fonte pendente |
| Sinistro automotivo travado | 12 | 11 | 1 | `RESEARCH_MORE`; qualidade de fonte pendente |
| Voo cancelado e reembolso travado | 12 | 12 | 0 | `RESEARCH_MORE`; qualidade de fonte pendente |

As quantidades refletem a repetição local com Brave Search e snapshots comerciais consultados pelo
contrato oficial do backend. O radar editorial do mesmo dia encontrou quatro soluções adjacentes
para vistoria, uma para sinistro e concorrentes de assistência aérea, mas esse inventário continua
abaixo do gate canônico de dez alternativas realmente comparáveis.

## Comparação das três hipóteses

### 1. Auditoria de Saída 10min — melhor sinal, ainda não elegível

- **Benefício:** microvalor visual e verificável ao parear laudos e fotos de entrada e saída.
- **Potencial:** evita esforço manual em uma situação com cobranças públicas de centenas ou
  milhares de reais.
- **Risco:** privacidade residencial e interpretação jurídica indevida.
- **Lacuna:** a disposição de pagamento do inquilino B2C ainda não foi observada; as ferramentas
  existentes atendem principalmente vistoria e operação imobiliária.

### 2. Sinistro Claro 10min — dor mais intensa, risco maior

- **Benefício:** organiza linha do tempo, documentos, protocolos e pendências quando o veículo está
  indisponível.
- **Potencial:** o custo de demora pode ser alto e imediato.
- **Risco:** fronteira securitária e jurídica, além da expectativa de que corretor e seguradora já
  prestem esse apoio.
- **Lacuna:** somente uma alternativa aderente apareceu na execução corrigida.

### 3. Voo Resolve 10min — urgência alta, vantagem paga fraca

- **Benefício:** organiza comprovantes e ações em uma janela curta.
- **Potencial:** demonstração simples e dor facilmente reconhecível.
- **Risco:** aconselhamento jurídico e promessa implícita de reembolso.
- **Lacuna:** ANAC, companhia aérea, ChatGPT e serviços que cobram apenas no sucesso reduzem a
  vantagem de uma compra imediata.

## Causa-raiz encontrada durante a execução

A primeira implementação misturava dois estados diferentes:

1. o worker criava três candidatos genéricos sempre que encontrava qualquer evidência pública,
   mesmo sem atingir o gate de ofertas comparáveis;
2. o backend rejeitava também a conclusão honesta com zero oportunidades, convertendo pesquisa
   insuficiente em falha técnica.

Uma repetição local revelou um segundo efeito: termos incidentais e referências legadas duplicadas
faziam cursos de concursos, unhas, salário-maternidade e mentoria jurídica parecerem 16 ofertas de
vistoria. A contagem não representava concorrência nem demanda.

Foram comparadas três respostas:

| Alternativa | Benefício | Risco | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Reduzir o mínimo de evidência | libera candidatos rapidamente | aumenta falso positivo e desperdiça a fábrica | baixo | rejeitada |
| Promover manualmente a vistoria | preserva uma hipótese promissora | correção pontual e não auditável | baixo | rejeitada |
| Corrigir worker e backend | protege todos os ciclos futuros e aceita resultado vazio válido | exige contrato e testes nos dois módulos | médio | escolhida |

A correção local agora:

- conclui pesquisa insuficiente com zero oportunidades e `RESEARCH_MORE`;
- preserva o bloqueio de qualquer candidata com menos de dez ofertas;
- compara palavras completas e ignora termos comerciais genéricos;
- consolida snapshots do mesmo título e produtor no worker e novamente no backend;
- mantém anúncios fora da contagem e aceita somente Hotmart, ClickBank e páginas comerciais
  públicas como fontes comparáveis.

## Comparação com Rigel

O radar editorial atribuiu 76/80 à vistoria, mas essa escala não é comparável ao benchmark de
Rigel. No processo canônico, nenhuma das três candidatas chegou à avaliação final de 100 pontos,
porque falhou antes nos gates factuais. Portanto, o resultado correto não é “76 contra 82”; é
**Rigel permanece sem concorrente comprovada nesta rodada**.

Para a vistoria voltar ao processo, a próxima pesquisa deve encontrar dez alternativas nominais,
atuais e aderentes, confirmar o canal e somente então abrir duas leituras privadas que meçam uso do
resultado pronto, preferência frente à comparação manual/ChatGPT e início de checkout. Apenas
pagamento reconciliado poderá contar como venda.

## Evidências preservadas

- Radar comercial do dia: `pesquisas/momentos-de-compra-b2c/2026-08-27-momentos-de-compra-b2c.md`.
- Benchmark e execução anterior: `docs/marketing/descoberta-oportunidade-pde-2026-08-26.md`.
- Gate B2C/Instagram: `docs/homologacao/pde-validacao-momento-compra-v5.md`.
- Ciclos oficiais: 37, 38 e 39; todos com plano `deterministic-fallback-v1`, uma tentativa e zero
  oportunidades persistidas.
- Testes causais após o último ajuste: 60 testes do worker e 17 testes focados do backend
  aprovados.

O custo do provedor de busca não é persistido pelo ciclo e, por isso, permanece não registrado. Os
três planos oficiais não chamaram modelo de IA; nenhum custo de modelo foi atribuído às tentativas.
