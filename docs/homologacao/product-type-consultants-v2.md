# Matriz de homologação — consultores enriquecidos por pesquisa v2

## Objetivo

Comprovar localmente que Turmalina e Fluorita transformam os sinais da coleção `/pesquisas` em
contratos de construção acionáveis, sem confundir artigo, comentário ou hipótese com venda.

## Alternativas e decisão

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Produzir somente relatório | Preserva todos os achados | Não muda a construção futura | Rejeitada |
| Enriquecer os nove cards atuais | Fica visível e editável sem ampliar o schema | Exige textos objetivos | Escolhida |
| Criar novos campos no catálogo | Máxima decomposição estrutural | Migração e UI prematuras sem uso real | Adiada |

## Contratos a comprovar

| Área | Caminho feliz | Falha que deve bloquear ou ajustar | Evidência local |
| --- | --- | --- | --- |
| Momento e microvalor | tipo declara situação concreta e trabalho verificável | conselho genérico tratado como produto | Changelog e cânone |
| WhatsApp | primeira resposta informa marca, origem, motivo, escopo e controle | contato ambíguo ou reativação sem opt-in | Blueprint Fluorita v2 |
| PWA | link entrega valor antes de instalação ou cadastro pesado | instalação exigida antes do valor | Blueprint Turmalina v2 |
| Personalização | dado declarado prevalece e inferência continua revisável | atributo íntimo inferido de foto/comportamento | Cânone e envelope v2 |
| Controle | ação de alto impacto exige confirmação ou ajuda humana | compra, publicação ou envio autônomo | Cânone e envelope v2 |
| Memória | contexto retorna por tenant, produto e cliente | mistura de clientes ou histórico global | Suíte do Harness SDK |
| Observabilidade | mede microvalor, utilidade, retorno e pagamento reconciliado | clique ou intenção contados como venda | Blueprint e fixture MySQL |
| Recorrência | PWA mede D1/D7/D30 e WhatsApp mede segundo contato | presença ou notificação sem utilidade | Blueprint v2 |

## Critério comercial

- **Continuar:** microvalor e utilidade são observados, existe retorno e pagamento reconciliado com
  margem positiva.
- **Ajustar:** a pessoa conversa ou usa a demonstração, mas não percebe vantagem sobre a alternativa
  gratuita, não aceita a próxima ação ou abandona antes do valor.
- **Parar:** existe mistura de clientes, inferência sensível, ação sem permissão, ausência de
  benefício operacional ou margem negativa.

Os eventos de QA usam identificadores próprios e nunca entram nas métricas comerciais do Rigel ou
de qualquer outro produto.
