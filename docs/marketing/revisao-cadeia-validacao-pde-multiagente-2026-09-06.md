# Revisão da cadeia PDE — validação multiagente

Data da revisão: 2026-09-06.

## Decisão de negócio

O Marketing Hub não possui operação para recrutar pessoas e entregar convites antes de cada produto.
Esse requisito é removido do gate de construção. Os agentes homologam o PDE; o mercado valida
preferência, compra e satisfação depois, em experimento comercial mensurado.

## Evidência do gargalo

- O banco de produção publica `pde-construction-approval` v6 com dez atividades.
- Quatro atividades dependem de `Operador humano`: aceitação do protótipo, duas leituras privadas e
  priorização final.
- Mira concluiu as cinco primeiras atividades e parou antes de `privateReading1`.
- O banco PDE contém uma única sessão de `QA_INTERNAL`, com os cinco eventos técnicos esperados, e
  nenhuma sessão `PRIVATE_READING`.
- O backend rejeita QA como leitura humana e exige confirmação de pessoa real. Portanto, repetir a
  atividade ou usar um agente pela tela não remove o bloqueio de forma legítima.

## Alternativas avaliadas

| Caminho | Benefício | Risco/custo | Avaliação |
| --- | --- | --- | --- |
| Recrutar pessoas por Instagram | Produz evidência humana antes da venda | Cria outro funil, consome orçamento e depende de operação indisponível | Inviável agora |
| Fazer agentes preencherem as leituras humanas | Exige pouca mudança | Falsifica participante, preferência e intenção | Rejeitado |
| Homologar com agentes e validar no mercado | Remove o bloqueio, preserva segurança e acelera receita | Só a campanha poderá comprovar desejo e compra | Escolhido |
| Pular toda homologação | Chega mais rápido à mídia | Arrisca experiência ruim, mensuração quebrada e desperdício | Rejeitado |

## Novo fluxo recomendado

1. Dédalo materializa e versiona a experiência.
2. O harness executa caminho feliz, retomada, falhas e privacidade em desktop, iPhone e Android.
3. Psique percorre três cenários isolados: aderente, fricção/recuperação e limite/segurança.
4. Têmis audita verdade, fidelidade à estratégia, privacidade e segregação das evidências internas.
5. O backend libera automaticamente a preparação de comunicação quando todos os gates passam.
6. Íris e Apolo materializam os criativos necessários.
7. Após autorização financeira, Hermes opera o experimento e mede resposta humana real.

## Métricas

### Antes da mídia

- três de três cenários sem falha crítica;
- resultado pronto em até dez minutos;
- jornada íntegra nos dispositivos suportados;
- zero evento interno contabilizado no funil comercial;
- zero pagamento, publicação ou gasto durante a homologação.

### Depois da mídia

- CPM, CTR e custo por sessão atribuída;
- chegada e uso do primeiro resultado útil;
- taxa de CTA e checkout iniciado;
- compra, receita e CAC reconciliados;
- entrega concluída, satisfação e reembolso.

## Aplicação a Mira

Mira não deve gerar convites nem campanha de recrutamento. Psique usa a versão real para testar uma
rotina válida, uma entrada incompleta recuperável e um pedido clínico que deve ser bloqueado. Têmis
revisa a mesma evidência. Se o gate passar, o produto avança para comunicação e para um experimento
de venda no Instagram, ainda condicionado a orçamento próprio.

Os R$ 100 antes autorizados para recrutamento permanecem sem uso e não migram automaticamente para
a campanha de venda. O codinome `Mira` continua restrito ao ambiente interno.

## Critérios de decisão

- **Continuar:** homologação, Psique e Têmis aprovados; preparar o experimento comercial.
- **Ajustar:** qualquer cenário revela fricção ou inconsistência corrigível; retornar a Dédalo.
- **Parar:** risco de segurança, privacidade, promessa sem sustentação ou economia inviável.

O sucesso comercial continua sendo venda entregue com satisfação. Aprovação de agente apenas reduz
o risco para chegar a esse teste; não conta como cliente nem receita.

## Estado operacional após a revisão

O cânone está atualizado, mas a produção continua em `pde-construction-approval` v6 e ainda exige
as duas leituras humanas. A implementação deve nascer como v7, preservar o histórico v6 e migrar
Mira para uma nova ocorrência `product:10@agent-validation-v1`. Esta revisão não alterou banco,
atividade, plano, campanha ou orçamento em produção.
