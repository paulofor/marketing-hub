# Matriz de homologação — construção MUSA v7

## Escopo e segregação

Produto `metodo-musa-7-dias`, versão `musa-pde-entry-v7-espelho-antes-de-sair`. Todos os acessos usam
endereços `teste+<jobId>@sandbox.local`; eventos levam `mh_test=true` e ficam fora das métricas humanas.
Não há pagamento real, publicação, contato, mídia ou geração de vídeo nesta homologação.

## Matriz ponta a ponta

| Área | Caminho feliz | Validação/falha | Evidência esperada |
| --- | --- | --- | --- |
| Degustação | Quatro escolhas produzem primeiro ajuste | chave/valor livre é rejeitado | resposta local, zero token e nenhuma fila pendente |
| Cadastro | link mágico preserva a v7 | e-mail inválido não cria acesso | versão persistida e mensagem recuperável |
| Acesso gratuito | Dia 1 pode ser concluído | Dia 2 é bloqueado | `TRIAL` e mensagem de pagamento único |
| Compra segregada | rota interna autenticada cria acesso `INTERNAL_QA` | trava desligada ou segredo inválido bloqueiam acesso | `ACTIVE`, versão v7, expiração em 90 dias e zero venda |
| Pagamento real | webhook dispara reconciliação pelo hash | status, oferta, valor diferente de 6.700 centavos, moeda diferente de BRL ou transação reutilizada não liberam | retorno autenticado da Pepper e trilha financeira idempotente liberam acesso |
| Retomada | link mágico chega ao endereço verificado | produto e e-mail nas rotas legadas não devolvem token | `/register` e `/login` ausentes, sem bearer token em resposta ou log |
| Contrato categorial | escolhas oficiais persistem | texto livre, chave desconhecida e valor de outra pergunta falham | validação central cobre orientação e interação direta |
| Contrato comercial | Hub, backend PDE e Liquibase entregam o mesmo JSON v7 | contrato antigo do Hub não sobrescreve o canônico | comparação profunda automatizada e resposta v7 idêntica |
| Jornada | sete dias preservam ordem e progresso | salto de etapa é bloqueado | missões e respostas persistidas |
| Expiração | acesso vigente funciona | acesso vencido bloqueia dias pagos | `EXPIRED` e orientação de suporte |
| Integrações | e-mail usa `sandbox-mail:1025` | indisponibilidade fica observável | mensagem capturada e falha sem falso sucesso |
| Observabilidade | eventos possuem versão e correlação | `mh_test` não conta como venda | trilha de degustação até primeiro uso |
| Métricas | compra, ativação e conclusão têm denominadores | degustação não vira receita | vendas reais permanecem zero |
| Conteúdo pago | workspace ativo abre três materiais versionados | catálogo público e URL sem token não expõem conteúdo | autorização por acesso `ACTIVE`, conteúdo dos sete sinais e layout sem overflow |
| Privacidade | titular exporta, corrige e-mail ou exclui dados | token inválido e ação desconhecida falham | token original e correlatores removidos, auditoria anônima com novo token preservada |
| Retenção | executor externo chama contrato interno autenticado | credencial ausente, backend indisponível ou resposta inválida não viram sucesso | retry curto, correlação e anonimização física no MySQL 5.7 sem `@Scheduled` no backend |
| Desktop | Chromium desktop percorre degustação e área | sem overflow ou CTA oculto | screenshot e asserções Playwright |
| iPhone | iPhone 15 Pro percorre o fluxo | teclado/touch não bloqueiam escolhas | screenshot e asserções Playwright |
| Android | Pixel 7 percorre o fluxo | viewport não corta preço ou suporte | screenshot e asserções Playwright |

## Critério de conclusão

Se a primeira rodada não revelar defeito, ela conclui a homologação. Quando uma rodada revelar defeito,
a contagem reinicia e são exigidas duas rodadas completas consecutivas depois da última correção.

## Resultado final

Duas rodadas completas e consecutivas foram executadas após a última correção sobre o hash funcional
`2c48b3293397a5f4b9fc6bd0b0ee601db2dc109fe92aa2c0fb539c02f2e29245`. Cada rodada aprovou:

- 1.966 testes Java: 1.749 do backend principal, 109 do backend PDE, 26 de Dédalo, 53 de Têmis e
  29 de Psique;
- 27 testes Node: 13 dos contratos de agentes, 10 do worker de IA e 4 do worker de retenção;
- TypeScript, build Vite, fronteira de API, `npm audit` sem vulnerabilidades, Actionlint, Compose e
  contratos de deploy;
- Liquibase estático e aplicação física idempotente, duas vezes, no MySQL 5.7;
- retenção real de acesso vencido, removendo token e correlatores no MySQL 5.7;
- seis jornadas Playwright em desktop, iPhone 15 Pro e Pixel 7.

Não houve falha nas duas rodadas finais nem alteração entre seus snapshots funcionais.
