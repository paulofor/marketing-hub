# PDE — restauração da linhagem comercial limitada v19

Data: 30/09/2026

## Limite da correção

Esta atestação sucede a v18 sem alterá-la. Ela restaura no manifesto vigente somente os sinais
comerciais mínimos que Psique precisa confrontar com a experiência Vega v12. Não autoriza
publicação, checkout, campanha, mídia, cobrança, contato humano ou reativação do experimento 92.

## Evidência da causa-raiz

- A v18 declarou apenas a v17 como prova integral.
- O carregador do worker lê deliberadamente somente o manifesto vigente e suas referências
  diretas, para manter o prompt abaixo do limite preventivo.
- A v17 contém a v16, que contém a v12, mas essa cadeia não é expandida. Por isso CTA e condição
  de acesso deixaram o prompt, e o teste comercial bloqueou o deploy de Psique antes de qualquer
  chamada de modelo.
- A mesma suíte passava antes da v18 e a falha foi reproduzida localmente com o manifesto vigente.

## Alternativas comparadas

1. Expandir recursivamente toda a cadeia: recupera o histórico, mas aumenta prompt, custo e risco
   de ciclo ou duplicação.
2. Reescrever a v18: corrige com pouco código, mas torna mutável uma evidência já integrada.
3. Criar a v19: referencia a v18 por hash e declara diretamente CTA, condições e identidade atuais.

A terceira alternativa preserva a auditoria e mantém a entrada de Psique pequena e verificável.

## Critério de homologação

- a v18 deve permanecer idêntica e ser conferida pelo SHA-256 declarado;
- o contrato vigente deve expor CTA, condição de pagamento e acesso sem renovação;
- a referência histórica deve chegar como `ATTESTED_REFERENCE`, sem duplicar o arquivo integral;
- o teste de prompt comercial e a suíte completa do worker devem passar sem inferência externa;
- Vega permanece `INVALIDATED`, sem deploy automático ou autorização de mídia.
