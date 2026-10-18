# Exercício: limitador de taxa com o padrão *Strategy*

> Disciplina **Arquitetura de Software (ARQSW)** · Apêndice F, Seção F.5 (Limitador de taxa),
> em especial F.5.3 (algoritmos) e F.5.5 (implementação com *Strategy*, Listagens F.6 a F.9).
> Este repositório é do estudante. Edite, envie, e o corretor automático devolve a nota.

## Contexto

Um limitador de taxa controla quantas requisições um cliente pode fazer em um intervalo de
tempo; a requisição excedente recebe o código HTTP 429 (*Too Many Requests*) e o cabeçalho
`Retry-After` (Seção F.5.1). Xu (2020, cap. 4) descreve vários algoritmos com compromissos
diferentes de memória, precisão e tolerância a rajadas (Tabela F.12). Como o algoritmo pode ser
trocado quando o monitoramento indicar, o livro o isola atrás da interface `LimitadorDeTaxa`,
uma aplicação do padrão *Strategy*: o *middleware* HTTP (`FiltroDeLimite`) depende apenas da
interface, e cada algoritmo é uma implementação intercambiável (Seção F.5.5).

```
http.FiltroDeLimite ──► api.LimitadorDeTaxa ◄── algoritmos.BaldeDeFichas
                                             ◄── algoritmos.JanelaDeslizante
```

O código deste repositório não respeita esse desenho:

1. o `FiltroDeLimite` recebe um `LimitadorDeTaxa`, mas o **converte para `BaldeDeFichas`** e
   usa um método exclusivo dessa classe; com qualquer outra estratégia, ele falha;
2. a resposta 429 não traz o cabeçalho `Retry-After`;
3. o `BaldeDeFichas` não informa quanto tempo falta para a próxima ficha (`esperarMs`);
4. a `JanelaDeslizante` ignora o relógio injetado, lê `System.currentTimeMillis()` e, na
   prática, é um contador de **janela fixa**, com o defeito da rajada na fronteira (F.5.3).

## Tarefas

1. Faça o `FiltroDeLimite` depender **somente** da interface `LimitadorDeTaxa`, sem conversões
   nem métodos de classes concretas.
2. Na resposta 429, inclua `Retry-After` em segundos inteiros: `esperarMs` arredondado para cima,
   com mínimo de 1 segundo.
3. No `BaldeDeFichas`, calcule `esperarMs` como o tempo até a próxima ficha,
   `(1 - fichas) / fichasPorMs`, arredondado para cima.
4. Implemente a `JanelaDeslizante` segundo a fórmula de Xu: estimativa = contador da janela atual
   + contador da janela anterior × fração da janela anterior que ainda se sobrepõe à janela
   móvel, arredondada para baixo. Se uma janela inteira se passou sem tráfego, o contador anterior
   é zero. Use **sempre** o relógio injetado (`LongSupplier relogioMs`), o que torna os testes
   determinísticos.

O comportamento de `make run` (quatro respostas 200 seguidas de duas 429) não deve mudar.

## Como testar antes de enviar

```console
$ make test
```

O comando executa **o mesmo corretor** usado pelo GitHub e mostra a nota imediatamente.
Outros atalhos: `make` (só compila), `make run` (executa a `App`), `make clean`.
Requer **Java 17+** e **Maven**.

## Critérios de avaliação (100 pontos)

| Critério | Pontos | O que é verificado |
|---|---|---|
| **Compila** | 10 | `mvn test-compile` sem erros |
| **Comportamento** | 50 | proporcional aos testes de `ComportamentoTest` que passam (os exemplos numéricos de Xu, com relógio controlado, e o contrato HTTP) |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `ArquiteturaTest` (ArchUnit) que passam |

As *fitness functions* verificam que:

- o filtro HTTP não depende do pacote `algoritmos`;
- o filtro HTTP não depende de nenhuma implementação de `LimitadorDeTaxa`, apenas da interface;
- os algoritmos não leem o relógio do sistema (`System.currentTimeMillis`, `System.nanoTime`,
  `Instant.now`): o tempo chega pelo relógio injetado;
- os algoritmos e a interface não conhecem o HTTP;
- as implementações de `LimitadorDeTaxa` ficam no pacote `algoritmos`.

> Os testes de **arquitetura** não verificam *o que* o programa faz, e sim *como* ele está
> organizado: quem pode depender de quem (Richards & Ford, cap. 6).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.
