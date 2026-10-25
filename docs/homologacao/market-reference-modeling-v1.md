# Homologação — modelagem por referências v1

Matriz definida antes da rodada integrada. Dados sintéticos e dependências externas simuladas; nenhuma compra de teste será misturada a evidência comercial.

| Dimensão | Critério de aceite |
|---|---|
| Entrada | Produto existente e novo; 1–5 URLs HTTPS públicas; resultado desejado obrigatório; idempotência; salvar não abre tarefa nem fila |
| Pesquisa | Início explícito abre auditoria e fila uma única vez; STOP recusa; falha preserva pesquisa; Argos registra fontes e contrapontos |
| Passagem | Novo produto preserva materialização canônica; existente não duplica produto. Pesquisa concluída pode ser adotada somente por ciclo do próprio produto, ainda não executado |
| Isolamento | Outro produto, referência antiga, ciclo encerrado, tarefa anterior e tentativa de trocar adoção são recusados ou preservados; não herda orçamento |
| Agentes | Atena compara três alternativas; trace cita fontes persistidas, exige fato, limitação, aprendizado, decisão, entrega e métrica. Contratos históricos continuam válidos |
| Continuação | Contexto é entregue pelas referências oficiais do experimento e construção privada; serviços da cadeia conservam orçamento, gates e passagem entre agentes |
| Persistência | MySQL 5.7 aplica migração e reaplica sem duplicar; dados continuam após reinício; adoção guarda snapshot |
| Interface | Desktop Chromium, iPhone e Pixel emulados: preparar, erros, acompanhar e vínculo; sem rolagem horizontal; UUID funciona em HTTP |
| Observabilidade | Status vem do backend; pesquisa concluída não aparece como produto entregue ou venda; tarefas e fontes navegáveis |
| Métricas | Compra, entrega, reembolso e contribuição continuam nas métricas da cadeia. Hipóteses e projeções explicitamente distintas de resultados |

Não exigir entrevistas, recrutamento ou opiniões solicitadas. Piloto Vega com Whering/Indyx preparado pela UI; execução paga exige autorização aplicável e não é inferida da implementação.

## Resultado local — 25/10/2026

- Backend: 311 testes aprovados, incluindo arquitetura, contexto dos agentes, descoberta, Big Idea, continuidade do ciclo e novas regressões de modelagem.
- Atena: suíte completa, 71 testes aprovados. Argos: 162 testes aprovados. Frontend: 13 testes da tela/menu, tipagem e build aprovados.
- API real com MySQL 5.7: preparo sem tarefa, replay, início concorrente sem duplicação, vinculação ao ciclo, rejeição de outro produto, fontes congeladas e recuperação após reinício aprovados.
- Tela real em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: campos, erro de URL, gravação, estado do rascunho e ausência de rolagem horizontal aprovados. A regressão remove `Crypto.prototype.randomUUID` para reproduzir a capacidade do HTTP administrativo.
- Depois do ajuste final da passagem de decisão, foram repetidos os testes afetados e a integração de persistência. A rodada visual já aprovada foi preservada.
- A primeira rodada visual detectou a rota no bloco público errado de App; foi corrigida na sandbox antes da publicação. A primeira tentativa de build atingiu o limite local de heap; o mesmo build passou com 4 GiB definidos somente no processo.

Topologia temporária removida com volumes. Fontes externas, pagamentos e chamadas de modelos foram simulados; custo externo e eventos comerciais de teste: zero. Logs e capturas locais ficam em `artifacts/product-modeling/local` (não são provas de demanda).

## Compatibilidade das provas anteriores

A única atestação vigente afetada foi Mira privada v45, que fixa o hash do consumidor compartilhado de estratégia. A sucessora v46 preserva experiência, publisher e provas históricas, registrando somente compatibilidade com a validação opcional de modelagem. Os 71 testes de Atena incluem contratos e recuperações anteriores; contextos sem modelagem permanecem aceitos. Isso não renova homologação comercial de Mira nem autoriza publicação de outra superfície.

## Decisão de implementação

Comparadas três alternativas: relatório separado (menor esforço, perde passagem), cadeia nova (duplica agentes e governança) e entrada integrada (reutiliza descoberta, planejamento e gates). Escolhida a terceira. Para produto existente, a adoção explícita no ciclo ainda não executado evita substituir seu histórico. Para novo produto, conserva a materialização existente. A vinculação não exige copiar saídas entre agentes.
