# Guia de redação do livro

Este guia vale para todos os capítulos de `docs/`. O Capítulo 1
(`docs/capitulo-1-fundamentos/index.md`) é a referência concreta de tom, profundidade e formato.

## 1. Registro: redação científica

- **Impessoal**, em terceira pessoa: "observa-se", "argumenta-se", "este capítulo examina",
  "o leitor pode verificar". Evite "você", "a gente", gírias e exclamações. Nos exercícios, o
  imperativo é aceito ("Explique...", "Compare...").
- **Toda afirmação não trivial tem fonte.** Definições, números, histórico, incidentes e
  afirmações sobre o código de um sistema vêm com citação. O que for interpretação do autor deve
  aparecer como tal ("argumenta-se aqui que...").
- **Prefira a paráfrase à citação direta.** Use citação direta só quando a formulação original
  importa (definições canônicas, leis). Citações curtas (até 3 linhas) vão entre aspas no texto.
  As longas vão em bloco (`>`), sempre com autor, ano e página.
- **Precisão terminológica.** Termos estrangeiros vão em itálico na primeira ocorrência, com
  tradução ou explicação: *bounded context* (contexto delimitado). Depois disso, use sempre o
  mesmo termo. Mantenha consistência com o glossário.
- **Densidade.** Parágrafos de 4 a 8 frases, que desenvolvem argumento, contexto histórico,
  contraexemplos e limitações. Listas servem para enumerações genuínas, não para substituir o
  texto corrido.
- **Português do Brasil**, ortografia vigente.

## 2. Citações e referências (ABNT)

- **No texto**, sistema autor-data (ABNT NBR 10520:2023):
  - "Segundo Parnas (1972), ..." ou "... (PARNAS, 1972)."
  - Dois autores: (RICHARDS; FORD, 2020). Três ou mais: (GAMMA *et al.*, 1994).
  - Com localização: (EVANS, 2003, cap. 4) ou (RICHARDS; FORD, 2020, p. 19).
  - Obrigatória em citação direta. Use página só quando ela foi conferida no PDF; caso contrário,
    cite o capítulo.
- **Ao fim de cada capítulo**, a seção `## Referências` lista **apenas** as obras citadas, em
  ordem alfabética, no formato da ABNT NBR 6023:2018. Exemplos:
  - Livro: `RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach. Sebastopol: O'Reilly, 2020.`
  - Artigo: `PARNAS, D. L. On the criteria to be used in decomposing systems into modules. **Communications of the ACM**, v. 15, n. 12, p. 1053-1058, 1972.`
  - Online: `NYGARD, M. **Documenting architecture decisions**. 2011. Disponível em: <URL>. Acesso em: 18 out. 2026.`
  - Software: `MASTODON. **mastodon/mastodon**: repositório de código-fonte, versão v4.x.y. GitHub, 2025. Disponível em: <URL>. Acesso em: ...`
- **Nunca invente referência, página, citação ou dado.** Se não for possível confirmar, não cite.
  Os livros desta pasta estão extraídos em texto e devem ser consultados para conferir cada uso.
  Fontes externas precisam ser obras reais e conhecidas.
- **Repositórios de software:** fixe a versão (*tag*) e use *permalinks* para arquivos. Confirme
  a existência de cada caminho, classe ou arquivo citado antes de publicá-lo.

## 3. Estrutura de um capítulo

```
# Capítulo N - Título

!!! todo "Para desenvolver"           <- podcast (pendência real)

## Objetivos                          <- "Ao final deste capítulo, o leitor deverá ser capaz de:"
## Introdução                         <- motivação, problema, roteiro do capítulo
## N.1 Seção numerada                 <- corpo: seções e subseções numeradas (N.1, N.1.1...)
...
## Síntese do capítulo                <- 2 a 4 parágrafos
## Diagramas e animações              <- interativos (iframe) e pendências
## Vídeos
## Exercícios                         <- 10 a 15, de conceituais a aplicados
## Glossário do capítulo              <- formato def_list: `termo` + ":   definição"
## Referências                        <- ABNT, só o que foi citado
## Material complementar
```

Os capítulos de voo (5 a 11) organizam o corpo nesta ordem: **Plano de voo** (ficha *kata* e
estimativas) → **100.000 pés** → **50.000 pés** → **10.000 pés** → **Pouso no código** →
**Subida de verificação** → **Caixa-preta**.

## 4. Elementos gráficos e código

- **Figuras** (Mermaid), **tabelas** e **listagens** são numeradas por capítulo e têm legenda e
  fonte:
  - Legenda acima da figura: `**Figura 1.3** – Título da figura.`
  - Fonte abaixo: `Fonte: elaborado pelo autor (2026).` ou `Fonte: adaptado de Cockburn (2005).`
- Tabelas seguem a mesma regra (`**Tabela 2.1** – ...`). Código usa `**Listagem 1.2** – ...`.
- **Código em Java 17**, curto, compilável sempre que possível e comentado em português.
  Trechos de sistemas reais devem ser **literais, curtos** e com link permanente. Do contrário,
  devem vir rotulados como "ilustrativo e simplificado".
- **Admonitions** com parcimônia:
  - `!!! note "Definição N.M"` para definições formais;
  - `!!! quote` para leis e citações canônicas;
  - `!!! caixa-preta` para incidentes reais;
  - `!!! todo "Para desenvolver"` apenas para pendências reais (podcast, vídeo, interativo ainda
    não feito).
- Interativos são embutidos com o `iframe` padrão (ver Capítulo 1).

## 5. Extensão

- Capítulos conceituais (1 a 4 e 12): **8.000 a 14.000 palavras**.
- Capítulos de voo (5 a 11): **8.000 a 12.000 palavras**.
- Antes de enviar, rode `mkdocs build --strict`: nenhum aviso é aceito.
