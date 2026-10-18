# Slides (Reveal.js)

Slides de aula da disciplina **Arquitetura de Software** (ARQSW), em HTML/CSS/JS com
[Reveal.js](https://revealjs.com). O Reveal está **embutido** em `assets/reveal/` (não
depende de internet — pode apresentar offline em sala).

## Estrutura

```
slides/
  index.html              # pagina inicial com a lista de decks
  README.md               # este arquivo
  assets/
    reveal/               # Reveal.js 5.1 (vendorizado: dist + plugins notes/highlight)
    css/course-theme.css  # identidade visual do curso (navy/teal/dourado)
  capitulo-1/index.html    # deck da Capítulo 1
  ...                     # um deck por capítulo
```

## Como apresentar

Basta abrir o arquivo `index.html` do deck no navegador (duplo clique). Para navegar:

- **Setas** / **Espaço**: avançar e voltar
- **Esc** ou **O**: visão geral (grade de slides)
- **S**: modo apresentador (com notas e cronômetro)
- **F**: tela cheia
- **B** ou **.**: escurecer a tela (pausa)

Alguns decks têm slides **verticais** (aprofundamentos): use as setas para baixo.

## Criar um novo deck

Copie `capitulo-1/index.html` para uma nova pasta `capitulo-N/` e edite o conteúdo. Os
caminhos para `../assets/...` continuam válidos por estar um nível abaixo de `slides/`.

## Notas do apresentador

Adicione dentro de uma `<section>`:

```html
<aside class="notes">Texto que só aparece no modo apresentador (tecla S).</aside>
```
