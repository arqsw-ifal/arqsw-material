# Diagramas D2 (figuras de 50.000 pés)

As figuras de *containers* da altitude de 50.000 pés (Seções 4.2 a 4.8) seguem o estilo dos
diagramas de Xu (2020): ícones reais dos componentes, faixas por camada e o caminho de uma operação
com setas numeradas. Elas são escritas em [D2](https://d2lang.com). Os demais diagramas do livro
continuam em Mermaid.

```
diagramas/
├── README.md        <- este guia
├── render.sh        <- renderiza d2/*.d2 para docs/assets/diagramas/*.svg
└── d2/<sistema>-50k.d2
```

Os SVGs gerados ficam em `docs/assets/diagramas/` e são versionados. O deploy do site não precisa
do D2 instalado. Depois de editar um `.d2`, rode:

```bash
brew install d2          # uma vez
./diagramas/render.sh
```

## Convenções obrigatórias

O modelo de referência é `d2/mastodon-50k.d2`. Copie o bloco `vars` e as `classes` dele.

1. **Layout**:
   - `layout-engine: elk`;
   - grade de nível superior com `grid-columns: 4`: usuários e sistemas externos, borda, aplicação e dados, nessa ordem, da esquerda para a direita;
   - dentro de cada faixa, `grid-rows`.
2. **Faixas**: classe `faixa`, nomeadas "Borda", "Aplicação" e "Dados". Use outras faixas só quando o sistema exigir, como "Dispositivos" ou "Embarcado".
3. **Nós**:
   - classe `no` (caixa branca, texto no alto e ícone embaixo);
   - classe `banco` para bancos de dados e filas persistentes (cilindro);
   - usuários e sistemas externos sem caixa (`shape: image`).
4. **Ícones**: só de `https://icons.terrastruct.com/`. Confira se a URL responde 200 antes de usar. O D2 embute o ícone no SVG.
5. **Setas** (convenção do livro, Richards e Ford, 2020, cap. 21):
   - `sinc`: linha contínua, comunicação síncrona;
   - `assinc`: linha tracejada, comunicação assíncrona;
   - `apoio`: linha fina cinza, sem número. É uma relação que existe, mas não pertence ao fluxo numerado.
6. **Rótulos**: só o número do passo ("1", "5, 7, 8"). A descrição de cada passo vai numa lista numerada logo abaixo da figura, no texto do livro.
7. **Sem sobreposição**: duas setas entre o mesmo par de nós se sobrepõem. Junte-as num único rótulo ou redirecione uma delas, explicando a escolha no texto.
8. **Fidelidade**: todos os *containers* e relações da figura Mermaid substituída devem continuar presentes, numerados ou como `apoio`.

## Inserção no livro

````markdown
**Figura 4.k.N** – *Containers* de ... e o caminho de ...

![texto alternativo descritivo](../assets/diagramas/<sistema>-50k.svg)

Passos numerados (...):

1. ...

Fonte: elaborado pelo autor (2026), com base em ... e no estilo de diagrama de Xu (2020).
Linhas contínuas: comunicação síncrona; linhas tracejadas: comunicação assíncrona.
Código-fonte da figura: `diagramas/d2/<sistema>-50k.d2`.
````
