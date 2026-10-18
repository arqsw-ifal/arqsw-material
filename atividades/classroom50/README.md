# Classroom 50 — instalação, configuração e criação de turmas

Automação para distribuir e corrigir os exercícios em Java da disciplina usando o
**[Classroom 50](https://classroom50.org)** (alternativa livre ao GitHub Classroom, que foi
descontinuado em 28/08/2026).

O corretor em si mora em [`../modelo-exercicio/`](../modelo-exercicio/); aqui ficam só as peças
que conversam com o Classroom 50.

| Arquivo | Para que serve |
|---|---|
| `autograder-java.py` | Roda o corretor no repo do aluno e gera o `result.json` da nota |
| `runtime-java.json` | Ambiente da correção: Ubuntu + OpenJDK 17 + Maven |
| `provisionar.sh` | **Cria turma e exercícios** (simula por padrão) |
| `coletar-notas.sh` | Baixa os envios e gera o `scores.csv` |
| `turma.conf.exemplo` | Modelo da configuração da turma |
| `exercicios.tsv` | Lista de exercícios (um por linha) |
| `alunos.csv.exemplo` | Modelo da lista de alunos |

> `turma.conf`, `alunos.csv` e `notas/` estão no `.gitignore` — **dados de aluno não são
> versionados**.

---

## 1. Instalação (uma vez por computador)

```bash
gh extension install foundation50/gh-teacher
```

!!! Atenção ao nome
    É **`gh-teacher`**, não `classroom50`. O projeto vive em `foundation50/classroom50`, mas
    extensões do GitHub CLI precisam de repositório com prefixo `gh-`. Instalar o nome errado
    falha com *"extension name must start with `gh-`"*.

Opcional, para você testar o fluxo pelo lado do aluno:

```bash
gh extension install foundation50/gh-student
```

Confira e mantenha atualizado (o projeto lança versão a cada 1–2 dias):

```bash
gh teacher --help
gh extension upgrade --all
```

## 2. Autenticação e preparação da organização (uma vez)

```bash
gh teacher login                 # pede os escopos admin:org, read:org, repo, workflow
gh teacher init engsoft-ifal     # cria o repositorio de configuracao engsoft-ifal/classroom50
```

O `init` cria o repositório `engsoft-ifal/classroom50`, onde ficam as turmas, os autograders e
as notas. **Não apague esse repositório** — é a base de tudo.

!!! Se o `init` pedir um token
    Ele pode exigir um **PAT fine-grained** com permissões de repositório
    *Contents: Read and write*, *Actions: Read and write*, *Administration: Read and write* e
    organização *Members: Read-only*. Nesse caso:
    ```bash
    CLASSROOM50_SERVICE_TOKEN=github_pat_XXX gh teacher init engsoft-ifal
    ```
    Para conferir depois: `gh teacher audit engsoft-ifal`.

**Requisito da organização:** plano **Team**, gratuito para educador verificado em
<https://education.github.com>. A verificação pode levar alguns dias — faça isso primeiro.

## 3. Publicar cada exercício como repositório-modelo

O Classroom 50 entrega ao aluno uma cópia de um **template repository**. Para o exercício que já
vem pronto:

```bash
cp -r ../modelo-exercicio /tmp/ex01 && cd /tmp/ex01
git init && git add . && git commit -m "exercicio 01"
gh repo create engsoft-ifal/arqsw-ex01-pedidos-camadas --private --source=. --push
```

Depois, no GitHub: **Settings → General → ✅ Template repository**.

> O conteúdo precisa ficar na **raiz** do repositório (não dentro de uma subpasta), senão o
> `scripts/run_tests.sh` não é encontrado na correção.

## 4. Configurar a turma

```bash
cp turma.conf.exemplo turma.conf
$EDITOR turma.conf
```

```bash
ORG="engsoft-ifal"                              # organizacao do passo 2
TURMA="arqsw-2026-2"                             # identificador curto, sem espacos
TURMA_NOME="Arquitetura de Software - 2026.2"  # nome exibido
TURMA_PERIODO="2026.2"
ROSTER_CSV="alunos.csv"                         # deixe "" para convidar manualmente
```

### Lista de alunos (opcional)

Copie `alunos.csv.exemplo` para `alunos.csv`. O formato exigido tem **5 colunas**:

```csv
username,first_name,last_name,email,section
maria-souza,Maria,Souza,maria.souza@aluno.ifal.edu.br,A
```

A coluna `username` é o **usuário do GitHub** do aluno (não a matrícula).

### Lista de exercícios

O `exercicios.tsv` tem **4 campos separados por TAB**:

```
slug<TAB>nome<TAB>template<TAB>prazo
```

```
ex01-pedidos-camadas	Pedidos em camadas	arqsw-ex01-pedidos-camadas	2026-08-20T23:59:00-03:00
ex02-strings	Manipulacao de strings	arqsw-ex02-strings	-
```

- **slug** — identificador na URL (sem espaços)
- **template** — **só o nome do repositório** do passo 3; a organização vem do `ORG` do
  `turma.conf`. (Se o template estiver em **outra** org, use `outra-org/repo`.)
- **prazo** — ISO-8601 com fuso (`-03:00`), ou `-` para sem prazo
- Linhas começando com `#` são ignoradas

!!! Cuidado com o TAB
    Se o seu editor converter TAB em espaços, o script não separa os campos. Para conferir:
    ```bash
    awk -F'\t' 'NR>1{print $1, "->", NF, "campos"}' exercicios.tsv   # tem que dar 4
    ```

## 5. Provisionar (o `provisionar.sh`)

**O script simula por padrão.** Nada é criado sem `--executar`.

```bash
./provisionar.sh              # SIMULA: imprime os comandos que rodaria
./provisionar.sh --executar   # cria de verdade
```

Sempre rode a simulação primeiro e leia a saída — criar turma e repositórios na organização
**não tem desfazer fácil**.

O que ele faz, nesta ordem:

1. cria a turma (`gh teacher classroom add`);
2. instala o autograder como **padrão** da turma;
3. importa a lista de alunos, se `ROSTER_CSV` estiver preenchido;
4. cria cada exercício do `exercicios.tsv`, já com o `--runtime`.

> O script **detecta sozinho** o autograder (`autograder-*.py`) e o runtime (`runtime-*.json`)
> desta pasta — por isso ele é idêntico entre disciplinas. Para trocar de linguagem, basta
> substituir esses dois arquivos.

### É idempotente

Rodar de novo **não duplica nada**: turma e exercícios já existentes são pulados (aparecem com
`=`). A **simulação também consulta** o que já existe, então o dry-run mostra fielmente o que
seria criado. É assim que você acrescenta exercícios ao longo do semestre:

```bash
echo -e "ex03-lista\tLista encadeada\tengsoft-ifal/arqsw-ex03-lista\t-" >> exercicios.tsv
./provisionar.sh              # confira: so o ex03 deve aparecer para criar
./provisionar.sh --executar
```

### Lendo a saída

| Símbolo | Significado |
|---|---|
| `$ comando` (cinza) | simulação — só mostrando o que faria |
| `✓` verde | executado com sucesso |
| `=` amarelo | já existia, pulado |
| `✗` vermelho | falhou (o motivo aparece ao lado) |

## 6. Distribuir aos alunos

Depois de provisionar, pegue o link do exercício em <https://classroom50.org> (ou
`gh teacher assignment list engsoft-ifal arqsw-2026-2`) e envie à turma. Quem aceitar recebe um
repositório próprio, já com enunciado, esqueleto e corretor.

Para convidar alguém fora da lista:

```bash
gh teacher invite engsoft-ifal usuario-github
gh teacher roster add engsoft-ifal arqsw-2026-2 usuario-github --first-name Ana --last-name Silva
```

## 7. Coletar as notas

```bash
./coletar-notas.sh ex01-pedidos-camadas
```

Ele dispara a agregação (que, sozinha, roda só de madrugada) e baixa os repositórios com um
`scores.csv` em `notas/<data>/` — pronto para o diário.

## Como a nota é calculada

O `autograder-java.py` **não reimplementa a correção**: ele executa
`scripts/run_tests.sh json` do repositório do aluno — exatamente o mesmo corretor que o aluno
roda com `make test` — e traduz o resultado para o formato do Classroom 50. Assim, a nota que o
aluno vê antes de enviar é a mesma que ele recebe.

| Item no boletim | Pontos |
|---|---|
| compila (`mvn test-compile`) | 10 |
| comportamento (testes JUnit de `*ComportamentoTest`) | 50, proporcional |
| arquitetura (*fitness functions* ArchUnit de `*ArquiteturaTest`) | 40, proporcional |

> Comportamento validado no exercício-modelo: código legado recebido → 45/100 (5 de 7 testes de
> comportamento, 0 de 4 de arquitetura); gabarito (`../gabaritos/modelo-exercicio`) → 100/100.

Para mudar os pesos, edite as variáveis `PTS_*` no topo de
[`../modelo-exercicio/scripts/run_tests.sh`](../modelo-exercicio/scripts/run_tests.sh) (precisam
somar 100).

## Nova turma no semestre seguinte

Não precisa refazer os passos 1–3. Só:

```bash
$EDITOR turma.conf      # TURMA="arqsw-2027-1", TURMA_NOME, TURMA_PERIODO
$EDITOR alunos.csv      # nova lista
./provisionar.sh && ./provisionar.sh --executar
```

Os repositórios-modelo dos exercícios são reaproveitados entre turmas.

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `extension name must start with gh-` | instalou `classroom50`; use `foundation50/gh-teacher` |
| `AVISO: a extensao gh-teacher nao parece instalada` | falta o passo 1 |
| `faltou o turma.conf` | copie de `turma.conf.exemplo` |
| Exercício criado sem prazo | o campo de prazo não estava separado por TAB |
| Correção falha com "run_tests.sh nao encontrado" | o template tem o conteúdo numa subpasta, não na raiz |
| Nota sai 0 sem explicação | veja o log do job em Actions no repositório do aluno |

Documentação oficial: [wiki do Classroom 50](https://github.com/foundation50/classroom50/wiki)
· [guia do professor (CLI)](https://github.com/foundation50/classroom50/wiki/CLI-Teacher-Guide)
