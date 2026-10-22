# Prova comercial da interface Opala v1

Matriz definida antes da validação em 22/10/2026, solicitação 3709.

| Verificação | Aceite |
| --- | --- |
| Primeiro valor | Executar a interface real com entrada “DEMONSTRAÇÃO”, sem cadastro, compra, provedor externo ou nova inferência |
| Materialização | Capturar o resultado completo, com legibilidade, identificando versão, fonte, entrada, saída, arquivo e SHA-256 |
| Passagem | Preservar a copy de Íris e versionar o anúncio; não sobrescrever a imagem privada nem o parecer comercial anterior |
| Origem | Conferir tarefa, artefato, código do capturador e pixels; a captura própria sem mídia externa não exige licença de um gerador inexistente |
| Checkout | Conciliar contrato publicado, experimento, preço, cobrança e URL; observar o checkout sem comprar |
| Preparação | Manter `checkoutLinkedFromLanding=false` quando o link não está ativo; a configuração candidata tem evidência separada e não autoriza venda |
| Falhas | Bloquear resposta externa, arquivo alterado, versão misturada, origem ausente e callback de outra tarefa |
| Regressão | Outra identidade, comunicação ainda sem aceite, vídeo, campos não suportados e caminho de inferência anterior preservados |
| Observabilidade | Entrada integral, saída funcional, recibo, erro e custo incremental de modelo zero persistidos; dados de teste excluídos de prova comercial |
| Navegação | Captura no Chromium empacotado; destino previamente homologado em desktop, iPhone e Pixel; emulação não representa Safari nativo |

## Evidência e escolha

A atividade 800 recebeu bloqueio funcional correto. A imagem 541 conservava o aviso
“Dias 2 a 7 indisponíveis”, pertencente à preparação privada, enquanto a candidata
comercial oferece continuidade de sete dias. O armazenamento estava íntegro; o HTTP
403 de um cliente de diagnóstico não se reproduziu no navegador e o arquivo público
correspondia ao SHA-256 da peça privada. Alterar o storage não corrigiria a causa.

Comparados: reaproveitar a imagem privada; gerar nova mídia por modelo; capturar o
primeiro valor do software atual. Adotada a captura própria: demonstra capacidade
executada sem nova inferência e permite preservar a copy aprovada de Íris. Dédalo
produz a prova do PDE; o backend forma uma nova candidata e solicita revisão própria.
Não substitui a composição privada aprovada, não cria fotografia nem promete resultado
pessoal. O resultado capturado é completo e identificado como demonstração.

A segunda falha era uma dependência circular de aceites: a revisão preparatória exigia
um link de compra já ativo, mas a ativação depende da própria revisão. A nova evidência
`PDE_PREPARED_CHECKOUT_REVIEW_V1` separa configuração candidata de ativação. Backend e
ferramenta verificam identidade e oferta observada em rede; o revisor vê o checkout.
O booleano do link real permanece falso. Os gates financeiros, comerciais e de mídia
continuam obrigatórios antes de abrir a compra e ativar anúncios.

## Regressões executáveis

- `OpalaCommercialUiProofTest`: identidade, copy, código, pixels, custo local e ausência de aceite.
- `OpalaCommercialUiProofMaterializationTest`: nova versão, idempotência e preservação da rejeição.
- `CreativePreparationEvidenceResolverTest`: origem, estado PLANNED, preço, contrato e versão.
- `CatalogoVivoDeterministicProofTest`: somente a captura explícita admite rotina sem inferência; demais atividades continuam exigindo sua auditoria de modelo.
- `landing-generator-agent-worker/scripts/test-opala-ui-proof.mjs`: navegador real, outro produto, provedor externo e falha antes de artefato; executado também na imagem versionada.
- `meta-ad-approver-worker/scripts/prepared-checkout.test.mjs`: navegador, contrato de rede, checkout, ausência real do link, zero compras e divergências.
- `meta-ad-approver-worker/scripts/static-ui-proof.test.mjs`: inspeção MCP e bloqueio de pixels adulterados.

Adoção limitada ao adaptador `MUSA_LOCAL_FIRST_VALUE_V1`, contratado pelo backend por
capacidade e campos, nunca por ID do produto. Vídeos e experiências não suportadas
preservam seus percursos. Revisões independentes e decisões humanas não são herdadas.
Testes locais comprovam comportamento, não demanda, margem realizada ou aumento de vendas.

## Resultado local observado

O capturador da imagem versionada de Dédalo executou a versão real local: 1035 × 1109 pixels, entrada DEMONSTRAÇÃO, três ações completas e download visível. O recibo confirmou modelo local, zero tokens e zero custo de modelo. Os hashes do capturador e do adaptador coincidiram com os arquivos empacotados.

Foram aprovados 104 testes Java de Dédalo, três cenários de navegador dentro da imagem e cinco testes Node de Têmis. Os relatórios locais relevantes do backend somam 758 testes sem falhas, com 11 casos opcionais ignorados. A regressão de transporte JSON foi reproduzida antes da correção e aprovada depois. A topologia temporária foi removida após a homologação.
