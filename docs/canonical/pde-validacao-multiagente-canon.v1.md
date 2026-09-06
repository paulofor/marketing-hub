# Validação multiagente de Produtos Digitais Experienciais — v1

## Decisão

Por decisão de produto de 2026-09-06, o Marketing Hub não depende de convites, recrutamento ou
leituras privadas de pessoas para homologar um PDE antes da comunicação comercial. O gate anterior
à comunicação passa a ser uma **validação multiagente**, executada pelos agentes existentes e por
testes determinísticos, com evidências segregadas de qualquer métrica de mercado.

Essa validação comprova prontidão técnica, coerência da experiência, segurança e integridade
comercial. Ela **não comprova** preferência humana, intenção de compra, satisfação, product-market
fit, venda ou receita. Essas evidências só podem vir do experimento comercial e da entrega a pessoas
reais, com origem e métricas persistidas.

Execuções históricas permanecem imutáveis. A mudança entra em `pde-construction-approval` v7, com a
referência `product:<id>@agent-validation-v1`; atividades históricas de leitura privada não podem
ser reescritas nem concluídas artificialmente por agentes.

## Alternativas consideradas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Recrutar duas pessoas antes da comunicação | Evidência humana direta antes do lançamento | Depende de uma capacidade operacional inexistente, atrasa aquisição e consome orçamento sem testar venda | Não adotar |
| Registrar QA ou resposta de agente como leitura humana | Mudança pequena e avanço rápido | Fabrica evidência, mistura tráfego interno com mercado e permite prova social falsa | Proibida |
| Homologar com agentes e validar valor no mercado | Fluxo executável, auditável e rápido; preserva gates independentes | Não antecipa preferência ou compra humana; exige disciplina na leitura dos resultados | Adotar |
| Publicar sem homologação | Menor tempo inicial | Expõe pessoas e orçamento a falhas de produto, segurança e mensuração | Não adotar |

## Processo canônico

O macroprocesso passa a se chamar **Protótipo, validação multiagente e aprovação do PDE** e segue:

`Dédalo → homologação técnica determinística → Psique → Têmis → gate do backend → comunicação`

1. **Dédalo materializa o PDE**: jornada, componentes, audiovisual quando previsto, acesso,
   continuidade, instrumentação e versão imutável do protótipo.
2. **O harness homologa tecnicamente**: executa a versão real em desktop e nos perfis móveis
   suportados, testa caminho feliz, retomada, entradas inválidas, falhas de integração, privacidade,
   acessibilidade básica e emissão dos eventos esperados.
3. **Psique valida a experiência por cenários**: usa o protótipo real em contexto novo e isolado,
   sem herdar a resposta de Dédalo, e avalia compreensão, esforço, utilidade, confiança, prazer,
   objeções e clareza do próximo passo.
4. **Têmis revisa integridade independentemente**: confirma fidelidade ao produto e à estratégia,
   limites honestos, segurança, privacidade, direitos, ausência de prova fabricada e segregação das
   métricas internas.
5. **O backend calcula o gate**: somente evidências persistidas, versionadas e aprovadas liberam o
   produto para `COMUNICACAO_E_JORNADA`. O produto permanece em `STOP`; essa passagem não publica,
   cobra, cria campanha nem autoriza gasto.
6. **O mercado valida o valor**: depois da comunicação, homologação comercial e autorização de
   mídia, Hermes mede pessoas reais desde o anúncio até compra, entrega, satisfação e reembolso.

Nenhum agente pode criar e aprovar o mesmo artefato. Atena e Plutus continuam responsáveis,
respectivamente, pela estratégia e pela economia anteriores à construção; Íris e Apolo continuam
responsáveis pela comunicação e pelo audiovisual; autorização humana permanece obrigatória para
preço, publicação, campanha e gasto, não para representar uma pessoa fictícia em teste privado.

## Cenários mínimos de Psique

Psique deve executar no mínimo três jornadas isoladas da mesma versão:

1. **Caminho aderente**: entrada válida e representativa do público, primeiro resultado utilizável,
   uso do resultado e compreensão do próximo passo.
2. **Fricção e recuperação**: pessoa sem conhecimento de IA, entrada incompleta ou ambígua, erro
   recuperável, retomada da sessão e instruções suficientes para continuar.
3. **Limite e segurança**: pedido fora do escopo, dado sensível ou situação de risco; o produto deve
   bloquear, explicar o limite e oferecer orientação segura sem inventar resultado.

Os cenários são avaliações sintéticas e devem usar `trafficClass=AGENT_VALIDATION` e marcador
interno equivalente a `mh_internal_test`. Podem explorar personas do público, mas nunca recebem
nome, consentimento, depoimento ou identificador de participante humana.

## Evidência obrigatória

Cada execução deve persistir:

- produto, URL e versão exatos;
- agente, execução, modelo, versão do prompt e versão do schema;
- cenário, entradas, saída funcional e decisão estruturada;
- request enviado e response bruto recebido do modelo;
- dispositivo, viewport, horários, duração, screenshots e artefatos;
- sequência de eventos, falhas, bloqueios, retomada e custo;
- critérios aprovados ou reprovados e causa-raiz do ajuste;
- confirmação de que pagamento, publicação, campanha e gasto permaneceram desativados.

Eventos de `AGENT_VALIDATION`, QA, smoke test e automação devem ser excluídos de visitantes,
conversão, checkout, venda, receita, CAC, satisfação, reviews e depoimentos. Agentes não podem
declarar “pessoas preferiram”, “clientes aprovaram” ou expressão equivalente.

## Gate para avançar

O backend libera a comunicação somente quando:

- a mesma versão passa em desktop e nos perfis móveis suportados;
- o caminho feliz entrega resultado pronto em até dez minutos e sem prompting ou montagem externa;
- os três cenários de Psique possuem evidência completa e nenhum bloqueio crítico;
- Têmis aprova segurança, verdade, privacidade e fidelidade aos contratos anteriores;
- eventos e artefatos internos estão segregados das métricas comerciais;
- não há pagamento, publicação, mídia ou gasto decorrente da homologação.

**Continuar:** todos os gates aprovados; preparar comunicação e experimento comercial.

**Ajustar:** há fricção, inconsistência ou falha corrigível; retornar à autoridade do artefato e
reexecutar somente depois da correção.

**Parar:** produto inseguro, promessa sem sustentação, economia inviável, falha de privacidade ou
resultado que transfere à cliente o trabalho de operar a IA.

## Prova comercial posterior

A primeira evidência humana do PDE passa a ocorrer no mercado, não em uma leitura privada
artificial. O experimento deve separar tráfego interno e medir, no mínimo:

- impressão, clique e sessão atribuída;
- início e conclusão da experiência;
- chegada e uso do primeiro resultado útil;
- CTA e checkout iniciados;
- pagamento aprovado, receita e CAC;
- entrega concluída, satisfação, suporte e reembolso.

Sem tráfego humano suficiente, o resultado é `EVIDÊNCIA_INSUFICIENTE`. Sem pagamento reconciliado,
não existe venda. Parecer de agente pode liberar o teste, mas nunca substituir esses fatos.
