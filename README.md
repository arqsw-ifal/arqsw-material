# Arquitetura de Software: um voo do negócio ao código

Material online da disciplina **Arquitetura de Software (ARQSW)** do BSI, IFAL-Maceió.
Docente: Leo Fernandes, PhD.

O curso estuda **7 sistemas reais**, sempre de cima para baixo: dos **100.000 pés** (negócio
e atributos de qualidade) aos **50.000** (estilos e containers), aos **10.000** (componentes e
padrões) e, por fim, ao **solo** (código). Cada sistema famoso e fechado é pareado com um
**gêmeo open source** cujo código os alunos leem.

## Requisitos

```bash
pip install -r requirements.txt
```

## Executar localmente

```bash
mkdocs serve
```

Depois abra http://127.0.0.1:8000. Antes de enviar, rode `mkdocs build --strict` (o CI falha
com links quebrados).

## Estrutura

- `mkdocs.yml`: configuração (tema, extensões, navegação)
- `docs/`: conteúdo (uma pasta por capítulo, 1 a 12, mais apêndices A a C)
- `docs/interativos/`: animações HTML autocontidas, embutidas por `iframe`
- `docs/assets/css/extra.css`: identidade visual (navy/teal/gold) e faixas de altitude
- `slides/`: apresentações Reveal.js (publicadas em `/slides`)
- Material docente (plano de ensino, roteiros, quizzes, podcasts) e gabaritos: no repositório
  privado [`arqsw-docente`](https://github.com/arqsw-ifal/arqsw-docente)
- `atividades/`: exercícios em Java com correção automática (JUnit + ArchUnit) via Classroom 50

As marcações **"Para desenvolver"** indicam pendências (podcasts, vídeos, interativos). As regras de escrita estão em `GUIA-DE-REDACAO.md`.

## Licença

O texto, as figuras e os slides estão sob a licença
[Creative Commons Atribuição-NãoComercial-CompartilhaIgual 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.pt-br)
(CC BY-NC-SA 4.0). Erratas e sugestões são bem-vindas por *issue* ou *pull request*.
