# Microsoft Clarity no Estrategista de Experimentos

## Configuração

1. Crie um projeto Clarity para o domínio público das landings.
2. Cadastre o ID público do projeto como variável GitHub `CLARITY_PROJECT_ID`.
3. Gere em **Settings → Data Export** um token exclusivo e cadastre-o como secret GitHub
   `CLARITY_API_TOKEN`.
4. Mantenha o modo de consentimento do projeto habilitado. O Marketing Hub envia `consentv2` com
   armazenamento negado até existir consentimento explícito.

O token nunca entra na linha de comando, no repositório, no prompt ou no log. O workflow grava o valor
em arquivo restrito no host e o worker o recebe por secret montado somente leitura.

## Contrato operacional

- O Lead Portal injeta o coletor somente quando `CLARITY_PROJECT_ID` está configurado.
- Acesso com `mh_test=1` ou sessão marcada como teste não baixa o script externo.
- O Estrategista recebe apenas `consultar_snapshot_comportamental_agregado`.
- O backend limita PAGE, SOURCE e DEVICE a três consultas por execução e nove por dia.
- Toda resposta bruta agregada é persistida com custo direto USD 0.
- Falha, baixa amostra ou divergência com o funil interno não autoriza alterar campanha ou landing.

## Critérios comerciais

- **Continuar:** Clarity e funil apontam o mesmo padrão com amostra suficiente e o teste melhora
  checkout ou compra.
- **Ajustar:** a amostra é pequena ou as fontes divergem.
- **Parar:** surgir dado individual, risco de privacidade ou gasto sem atribuição confiável.
