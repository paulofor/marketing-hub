# Vega — recuperação da tarefa #458 por candidata final, suporte e direitos — v1

## Escopo e decisão

Data: 19/09/2026. Produto Vega, experimento #92, processo Opala #77, atividade
`commercialIntegrityReview`. A tarefa #458 superou a falha de cartões da #457, mas bloqueou porque
o contexto não distinguia o criativo efetivo das versões históricas nem entregava provas existentes
de suporte e direitos de mídia.

Foram comparadas três alternativas antes da implementação:

| Alternativa | Benefício | Risco | Custo/esforço e aderência |
| --- | --- | --- | --- |
| Remover ou simplificar o gate | Conclusão imediata | Publicar promessa, direitos ou atendimento inconsistentes | Baixo esforço; baixa aderência à proteção de receita e cliente |
| Repetir Têmis sem mudar o pacote | Nenhuma alteração | Novo custo e decisão estocástica sobre a mesma lacuna | Baixo esforço técnico; não resolve causa-raiz |
| Corrigir copy e completar o contexto versionado | Oferta clara, prova auditável e reutilizável | Exige backend, manifesto e nova homologação | Esforço intermediário; escolhida por proteger conversão, conformidade e recorrência |

A criativa #530 foi criada e aprovada pelo frontend administrativo como descendente da #529, dentro
dos limites do canal, com preço de R$ 67, pagamento único, acesso por 90 dias e sem renovação. Esta
entrega não autoriza publicação, mídia, cobrança ou contato.

## Matriz definida antes dos testes

| Dimensão | Critérios de aceite |
| --- | --- |
| Caminho feliz | Contexto marca somente #530 como final; expõe #528 → #529 → #530; anexa governança `VERIFIED`; manifesto v10 declara suporte; prompt v3 aceita as provas e mantém cartões obrigatórios |
| Validações e falhas | Versão supersedida não vira final; `READY` sem parecer aprovado não vira final; suporte vazio é recusado; hash divergente bloqueia pacote; ausência ou duplicidade de candidata continua bloqueante |
| Integrações | Backend entrega contexto ao pending oficial; Catálogo Vivo fixa prompt v3; worker seleciona manifesto v10; callback pertence ao mesmo produto/ciclo/experimento; nenhuma chamada direta ao banco pelo worker |
| Observabilidade | Tarefa preserva prompt, contexto, resposta bruta, evidências, decisão, erro, tokens e custo; linhagem e IDs permanecem auditáveis |
| Métricas e segregação | QA usa e-mail `@sandbox.local` e não conta como venda, receita ou comportamento humano; não há campanha, cobrança ou contato real |
| Navegação | Retomada e histórico em Chromium desktop, iPhone 15 Pro e Pixel 7; emulação móvel não é alegada como Safari real |
| Continuidade | Psique e Têmis são renovados somente se a mudança material invalidar o parecer; backend continua decidindo o avanço; autorização humana e preflight permanecem posteriores |

## Evidências e resultados

A rodada local isolada foi concluída antes do PR com os seguintes resultados:

| Validação | Resultado |
| --- | --- |
| Backend principal completo | 3.236 testes; 0 falhas; 0 erros; 17 ignorados por condições já previstas |
| Backend PDE completo | 184 testes; 0 falhas; 0 erros; 0 ignorados |
| Worker de revisão Meta completo | 102 testes; 0 falhas; 0 erros; 1 ignorado por condição prevista |
| Contexto comercial e governança de mídia | 18 testes direcionados; candidata #530 única; versões anteriores apenas para auditoria; evidência somente na final |
| Contrato de suporte | Pedido válido persistido e recuperado após reinício do serviço; mensagem vazia recusada sem alterar o workspace |
| Catálogo Vivo no MySQL 5.7 | 20 testes; prompt v3 ativo; três versões preservadas; validação estática das mudanças aprovada |
| Pacote comercial | 13 testes; 184 arquivos e 28 manifestos carregados com hashes válidos; v10 selecionada pelo worker |
| Isolamento | Nenhuma publicação, campanha, cobrança, mensagem real ou métrica humana foi produzida; topologia temporária removida com volumes e órfãos |

O criativo #530 foi criado e aprovado pela interface administrativa, mantendo a linhagem #528 →
#529 → #530 e sem autorizar veiculação. A candidata final agora carrega os termos materiais da
oferta dentro dos limites do canal, e o contexto associa a ela a evidência verificável da mídia.

A etapa operacional posterior ao merge deve comprovar, pela mesma tela, que o runtime carregou o
prompt v3 e o manifesto v10, que o backend aceitou o callback e que a atividade
`commercialIntegrityReview` chegou ao estado concluído. Uma resposta favorável do modelo sem
callback persistido não é evidência suficiente.
