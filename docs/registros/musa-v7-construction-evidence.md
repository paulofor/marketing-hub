# Evidência da construção — MUSA v7 orientado ao desejo

## Origem e posição na cadeia

- Produto: `#4 — MUSA`, versão `musa-pde-entry-v7-espelho-antes-de-sair`.
- Plano Comercial: `#3`, concluído no processo **Plano Comercial e desenho da oferta PDE v4**.
- Próximo processo executado: **Construção e aprovação do PDE v4**.
- Processos posteriores, ainda fora deste gate: Comunicação e jornada de venda, Homologação comercial
  e ativação, Venda, entrega e aprendizado.

## Plano Comercial aprovado

- Entrega: experiência digital guiada de sete dias; vídeos são apoio opcional, não o produto.
- Público: mulheres urbanas que querem organizar sinais de presença usando o que já possuem.
- Oferta: pagamento único de R$ 67, sem assinatura, com acesso por 90 dias.
- Degustação: quatro escolhas categoriais e um primeiro ajuste local; a cliente pode escolher uma
  alternativa neutra ou não agir.
- Economia hipotética aprovada para planejamento: preço R$ 67, custo variável máximo de R$ 20,
  CAC máximo futuro de R$ 25, contribuição de R$ 22 e margem de 32,84%.
- Meta hipotética: 20 vendas e R$ 1.340 de receita. Tráfego, conversão de 0,8% e reembolso de 12%
  continuam hipóteses, não resultados.
- Canal inicial futuro: audiência própria e abordagem consentida. Mídia paga exige autorização
  separada; o teto de planejamento de R$ 800 não autoriza gasto.

## Decisão de materialização

Foram comparadas três alternativas:

1. manter a v7 baseada em vídeo e IA: menor esforço, mas dependência de custo sem ledger e promessa
   confusa;
2. converter em serviço de vídeos personalizados: potencial de valor percebido, mas incompatível com
   o preço e a margem aprovados;
3. evoluir a plataforma PDE guiada existente: escolhida por preservar o produto de sete dias,
   progresso, retomada e economia.

O contrato completo está em `docs/marketing/musa-v7-construction-contract.md`.

## Produto materializado localmente

- A experiência pública solicita somente `mainObstacle`, `presenceFocus`, `desiredSignal` e
  `startingResource`, com valores enumerados.
- O primeiro ajuste usa `MUSA_LOCAL_RULES_V1`; não chama OpenAI, não cria fila, não consome tokens e
  não registra custo de modelo.
- O JSON comercial canônico fica versionado no backend PDE e é aplicado pelo Liquibase no produto e
  na versão v7 do Marketing Hub; um teste de paridade profunda impede drift entre as fontes.
- Foto, texto livre, medidas, saúde, personalidade, raça, religião e inferências sobre terceiros não
  entram no contrato v7.
- O acesso pago persiste versão, pagamento e expiração; estados `TRIAL`, `ACTIVE` e `EXPIRED` são
  explícitos.
- O estado `EXPIRED` reconhece a compra anterior e direciona a suporte e direitos de dados sem
  apresentar paywall, renovação ou nova cobrança automática.
- Acesso interno de QA libera sete dias e materiais por 90 dias sem registrar compra ou receita; a
  capacidade exige segredo, trava local e fica desabilitada em produção.
- O webhook público não confia no payload recebido: acesso real só é liberado depois de consultar a
  Pepper pelo hash e confirmar status, oferta, exatamente R$ 67 e moeda BRL.
- A compra mantém trilha idempotente separada com transação, produto, oferta, valor, moeda e status;
  compradora e acesso aparecem somente como hashes não reutilizáveis.
- As rotas públicas legadas que devolviam token apenas pelo conhecimento do e-mail foram removidas;
  retomada usa link mágico entregue ao endereço ou Google verificado, e logs não registram tokens.
- Dias 3 a 7 também usam escolhas categoriais, e a validação central rejeita chave, texto livre e
  valor pertencente a outra pergunta mesmo quando a rota direta é chamada fora da interface.
- A linguagem evita diagnóstico científico individual, promessa de aprovação externa e garantia de
  transformação.
- Eventos preservam versão e correlação. QA interno fica segregado de visitantes, vendas e receita.
- Catálogo público não contém missões, materiais pagos ou pacote científico. Material é obtido por
  requisição autenticada, sem expor token na URL ou nos logs.
- O aviso visível e o contrato `docs/marketing/musa-v7-data-governance.md` documentam inventário,
  finalidade, retenção, destinos e exercício de direitos.
- As sete missões permanecem sem texto livre; a mensagem voluntária de suporte declara finalidade,
  retenção e alerta para não enviar dados sensíveis ou desnecessários.
- A área permite exportar, corrigir e-mail e excluir dados; a exclusão remove PII, respostas,
  orientações, progresso, token original e correlatores de eventos, preservando somente auditoria
  anônima com novo identificador. A retenção é acionada por executor externo em rota interna
  autenticada, com saúde, retry e correlação, sem agendamento operacional no backend.
- Os três materiais pagos foram recriados como HTML/CSV versionados, sem imagens externas quebradas,
  e o mapa apresenta semanticamente os sete sinais em desktop e mobile.
- O caminho neutro existe na degustação e nos sete dias. Depois do Dia 7, a interface encerra a
  jornada em vez de voltar silenciosamente ao primeiro dia.
- Um teste percorre os sete tipos de orientação da v7 e comprova que todos terminam por
  `MUSA_LOCAL_RULES_V1`, com zero token e nenhuma fila; o catálogo v7 também não expõe hero video nem
  pacote de alegações científicas.

## Histórico que restringe o uso de vídeo

- A auditoria histórica encontrou 678 tentativas Runway ligadas ao MUSA: 677 sem custo conhecido e
  todas sem atribuição confiável ao produto.
- No Estúdio global, 13.317 tentativas incluem 12.535 sem ledger, 695 com custo desconhecido e 730 sem
  produto atribuído.
- Por isso nenhum vídeo novo, chamada externa ou gasto foi executado. Vídeo só poderá voltar ao
  núcleo da entrega após atribuição por produto e ledger completo.

## Homologação local antes do gate final dos agentes

As correções encontradas nas revisões anteriores de Dédalo, Têmis e Psique foram materializadas. A
homologação exploratória fechou as causas-raiz antes das rodadas finais: o executor de retenção podia
iniciar antes do backend e aguardar 24 horas após falha; a anonimização referenciava coluna inexistente;
acesso expirado reutilizava o paywall; suporte contradizia a transparência sobre texto livre; rotas
públicas devolviam bearer token apenas pelo conhecimento do e-mail; logs podiam conter credencial;
interação direta contornava as categorias; e a reconciliação Pepper não exigia valor e moeda exatos
nem mantinha auditoria financeira durável. Os diagnósticos também passaram a distinguir DNS apontado
de experiência publicada.

Duas rodadas finais completas e consecutivas passaram sobre o mesmo hash funcional
`2c48b3293397a5f4b9fc6bd0b0ee601db2dc109fe92aa2c0fb539c02f2e29245`. Cada rodada cobriu:

- 1.749 testes do backend principal, 109 do backend PDE, 26 de Dédalo, 53 de Têmis e 29 de Psique,
  totalizando 1.966 testes Java com zero falha;
- 13 testes dos contratos de agentes, 10 do worker de IA e 4 do worker de retenção;
- build TypeScript/Vite, fronteira de API, `npm audit` sem vulnerabilidades, Actionlint, Compose e
  contratos de deploy;
- Liquibase estático e aplicação física idempotente duas vezes no MySQL 5.7;
- política de retenção real sobre acesso vencido e verificação SQL de token e correlatores;
- seis jornadas Playwright: caminho versionado v5/v6/v7 e caminho comercial v7 em desktop, iPhone
  15 Pro e Pixel 7.

As jornadas percorreram todos os sete dias e comprovaram resposta local com zero token, nenhuma fila
de IA, acesso `ACTIVE`, versão v7 preservada, expiração entre 89 e 90 dias, encerramento explícito,
suporte, material protegido, exportação e exclusão de dados, ausência de overflow e segregação do
tráfego Docker como `INTERNAL_QA`. Compras e acessos comerciais permaneceram zerados. A matriz
completa está em `docs/homologacao/musa-v7-construcao-v1.md`.

A primeira reavaliação de Dédalo aprovou o produto e apontou duas vulnerabilidades transitivas na
cadeia de build. `postcss` e `nanoid` foram atualizados dentro das faixas compatíveis, o `npm audit`
passou a zero e a contagem das duas rodadas completas foi reiniciada antes desta evidência final.
Na revisão final do diff, o fallback de identidade PDE também passou a registrar a exceção completa;
a matriz foi novamente executada duas vezes depois desse último ajuste de observabilidade.

## Estado comercial e critério do gate

Não houve publicação, contato, campanha, pagamento real, gasto ou venda. A homologação final local
foi concluída. Psique, Dédalo e Têmis reavaliaram este mesmo estado final e decidiram `APPROVE`:

- Psique aprovou compreensão, microvalor, baixo esforço, autonomia, retomada e tratamento de erro;
- Dédalo aprovou formato, jornada, entregáveis, acesso, privacidade, versão e custo operacional;
- Têmis aprovou preço, cobrança, promessa, privacidade, acesso, expiração e rastreabilidade.

O processo **Construção e aprovação do PDE v4** atingiu seu objetivo local. O próximo processo é
**Comunicação e jornada de venda do PDE**, ainda sem autorização implícita para publicar ou gastar.
O bundle JavaScript acima de 500 kB deve ser acompanhado antes de escala, mas não bloqueou as
jornadas homologadas. Venda, satisfação, desejo e transformação real permanecem não comprovados até
o processo comercial operar com clientes reais.

## Telemetria de agentes

- Plano Comercial: 3.340.393 tokens de entrada, 2.268.416 em cache, 86.142 de saída e
  **US$ 6,91811440** estimados em tarifa Standard.
- Construção, incluindo revisões que encontraram os defeitos: 10.543.235 tokens de entrada,
  9.635.584 em cache, 63.347 de saída e **US$ 8,75177760** estimados.
- Acumulado conhecido do Plano Comercial e da Construção: 13.883.628 tokens de entrada,
  11.904.000 em cache, 149.489 de saída e **US$ 15,66989200** estimados.

Uma tentativa final de Psique foi interrompida depois de 30 minutos aguardando resposta de rede e não
entregou telemetria. Ela não entra como custo zero nem no total conhecido; a repetição concluída está
integralmente contabilizada.
