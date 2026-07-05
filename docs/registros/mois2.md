# Registros — MOIS 2

> 🔴 **Arquivo canônico principal atual** para novos registros operacionais do MOIS.
> Toda nova decisão, correção, aprendizado ou orientação operacional relacionada ao MOIS deve ser registrada neste arquivo.
> O arquivo `docs/registros/mois1.md` fica preservado apenas como histórico.

## 2026-07-05 19:48:35 UTC-3
- criado o arquivo `docs/registros/mois2.md` para continuar os registros operacionais do MOIS sem aumentar ainda mais o tamanho de `mois1.md`.
- orientação operacional: a partir deste registro, decisões, correções, aprendizados e evidências do MOIS devem ser adicionados em `mois2.md`, preservando `mois1.md` como histórico.
- impacto de negócio: reduz atrito para consultar aprendizados recentes do MOIS e mantém os registros de campanhas, funis, dossiês, canais, criativos, métricas e oportunidades em um arquivo mais manejável.

## Template obrigatório de novo registro

```md
## YYYY-MM-DD HH:mm:ss UTC-3
- descrição breve do problema
- descrição breve do raciocínio para a solução
- registro do que foi feito
- documentos lidos para tratar a situação:
  - caminho/do/documento-1.md
  - caminho/do/documento-2.md
```

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only**: não apagar registros anteriores; apenas adicionar novos registros.

> Regra obrigatória de timestamp:
> Antes de adicionar qualquer novo registro, execute obrigatoriamente:
>
> ```bash
> TZ=America/Sao_Paulo date '+%Y-%m-%d %H:%M:%S UTC-3'
> ```
>
> Use exatamente a saída desse comando no título do novo registro.
> É proibido inventar, estimar, inferir ou reaproveitar data/hora a partir de:
> - contexto da conversa;
> - data do commit;
> - data do CI/build;
> - metadados do arquivo;
> - relógio UTC sem conversão explícita;
> - registros anteriores deste documento.
>
> O formato obrigatório do título é:
>
> ```md
> ## YYYY-MM-DD HH:mm:ss UTC-3
> ```

## 2026-07-05 20:27:59 UTC-3
- problema identificado: a resposta final da síntese dos dossiês MOIS podia chegar ao backend como representação técnica de record (`DossierDossierSynthesisOutput[...]`) em vez de JSON funcional limpo.
- causa-raiz: o worker `mois-sales-library-worker` usava `String.valueOf(result.output())` ao montar o callback de resposta local do pipeline `warmupecosystem.v1`, contaminando `resposta_final` com formato técnico.
- correção aplicada: o client do backend do dossiê MOIS v1 passou a serializar `StageResult.output()` com `ObjectMapper`, preservando a saída funcional como JSON consumível por preflight, oferta, página, criativos e relatório.
- prevenção de recorrência: adicionado teste de regressão garantindo que `DossierDossierSynthesisOutput` seja enviado como JSON e não como `record.toString()`.
- documentos lidos para tratar a situação:
  - `docs/registros/mois2.md`
