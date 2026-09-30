# PDE — restauração da linhagem limitada de Mira v14

Data: 30/09/2026

## Limite da correção

Esta atestação sucede a v13 sem alterá-la. Ela recoloca prova do produto, controle estático e vídeo
no conjunto direto de evidências entregue a Psique. Não muda a experiência, não autoriza mídia,
publicação ou reativação do experimento 93 e não converte dados de QA em prova comercial.

## Evidência da causa-raiz

- A v13 manteve somente código e testes em suas coleções diretas.
- O carregador não expande a v12 contida pela v13, pois essa expansão tornaria prompt e custo
  dependentes do tamanho de toda a história.
- A suíte completa encontrou zero item `ATTESTED_REFERENCE`, embora a v12 registrasse as três
  mídias por hash e resumo verificável.

## Correção e aceite

- a v13 permanece imutável e entra como referência atestada;
- os três arquivos de mídia permanecem byte a byte e entram somente por metadados, hash e resumo;
- o worker deve encontrar ao menos três referências atestadas sem carregar bytes binários;
- a suíte completa do Psique deve passar antes da publicação;
- Mira permanece `PAUSED`, sem deploy automático, campanha, gasto ou contato humano.
