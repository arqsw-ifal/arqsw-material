# Exercício: *pipeline* de transcodificação do PeerTube (*pipes and filters*)

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 6.4 (PeerTube: o código), Seções 6.4.5 e 6.4.10, Listagens 6.4.16 e 6.4.17
> Este repositório é **seu**. Edite, envie, e o corretor automático dá o retorno.

## O sistema real

No PeerTube, cada vídeo recém-enviado percorre um *pipeline* de *jobs*: torrent, *storyboard*,
etiquetagem automática, notificação, federação e transcodificação (Listagem 6.4.7). Lida como
arquitetura, a lista é um *pipeline* declarado como dado: cada elemento é um **filtro**, e o
**cano** entre eles é a fila. A ordem dos filtros codifica regras de negócio. Por exemplo, a
etiquetagem vem antes da transcodificação para que um *plugin* analise o arquivo ainda intacto.

A Seção 6.4.10 reduz o estilo à sua essência, em Java 17 (Listagem 6.4.16). `Tarefa` é a mensagem
imutável que trafega nos canos. `Filtro` é a única interface entre os filtros. `Pipeline` é o
conector: encadeia os filtros na ordem dada e **não conhece nenhum filtro concreto**. Um filtro
**testador** pode descartar a tarefa devolvendo `Optional.empty()`, e um filtro
**transformador** acrescenta artefatos.

```
App (raiz de composição: escolhe os filtros e a ordem)
 │
 ▼
Pipeline ──► Filtro (interface) ◄── FiltroValidacao, FiltroTranscodificacao(480), (720),
   │              │                  FiltroMiniatura, FiltroManifesto
   └──────► Tarefa (mensagem) ◄──────┘
```

## A situação

O código recebido é **legado**: compila e funciona em parte, mas tem dois problemas.

1. **Arquitetura violada.**
    - O `Pipeline` tem um método `padrao()` que cria os filtros concretos. O conector passou a
      depender dos filtros, e como os filtros dependem do contrato no pacote `pipeline`, surgiu um
      **ciclo** `pipeline ⇄ filtros`.
    - O `FiltroMiniatura` revalida a tarefa "por garantia" chamando o `FiltroValidacao`
      diretamente. Um filtro passou a conhecer outro, e o *pipeline* virou, em parte, uma cadeia
      rígida de chamadas.
2. **Comportamento incompleto** (veja os `TODO`):
    - **regra 1** (`pipeline/Pipeline.java`): o descarte feito por um testador é ignorado, e a
      tarefa segue adiante;
    - **regra 2** (`filtros/FiltroValidacao.java`): formatos não suportados não são recusados;
    - **regra 3** (`filtros/FiltroManifesto.java`): o manifesto é gerado mesmo quando não há
      nenhuma versão transcodificada para listar.

## O que fazer

1. Retire o método `padrao()` do `Pipeline` e monte o *pipeline* na raiz de composição
   (`App.java`). O pacote `pipeline` não pode importar nada de `filtros`.
2. Faça cada filtro depender **apenas** de `Filtro`, `Tarefa` e do JDK: o `FiltroMiniatura` não
   chama mais o `FiltroValidacao`, porque a validação é papel do filtro posicionado antes dele.
3. Implemente a **regra 1**: quando um filtro devolve `Optional.empty()`, o fluxo para e o
   *pipeline* devolve `Optional.empty()`, sem chamar os filtros seguintes.
4. Implemente a **regra 2**: o `FiltroValidacao` aceita só resolução positiva e caminhos
   terminados em `.mp4`, `.mov`, `.mkv` ou `.webm`, sem diferenciar maiúsculas de minúsculas.
5. Implemente a **regra 3**: o `FiltroManifesto` só acrescenta `<id>-master.m3u8` se houver ao
   menos um artefato `.mp4`.

Com tudo pronto, `make run` deve mostrar os quatro artefatos do vídeo `v1` e `Optional.empty` para
o vídeo `v2`, que está em `.avi`.

## Como testar antes de enviar

```console
$ make test
```

Isso roda **o mesmo corretor** que o GitHub vai rodar, e a nota aparece na hora.
Outros atalhos: `make` (só compila), `make run` (executa o `App`), `make clean`.
Requer **Java 17+** e **Maven**.

## Como você é avaliado (100 pontos)

| Critério | Pontos | O que é verificado |
|---|---|---|
| **Compila** | 10 | `mvn test-compile` sem erros |
| **Comportamento** | 50 | proporcional aos testes de `PipelineComportamentoTest` que passam: artefatos e ordem para um vídeo 1080p, nenhuma versão maior que a original, imutabilidade da tarefa, *pipeline* vazio, ordem definida pela lista de filtros, descarte por resolução e por formato, interrupção do fluxo após o descarte, manifesto só quando há versões |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `PipelineArquiteturaTest` (ArchUnit): os filtros não dependem uns dos outros (Listagem 6.4.17), dependem apenas de `Filtro`, `Tarefa` e do JDK, o conector não conhece os filtros concretos e não há ciclos entre pacotes |

> Os testes de **arquitetura** não olham *o que* o programa faz, e sim *como* ele está
> organizado: quem pode depender de quem. É assim que equipes reais impedem que a arquitetura
> "apodreça" com o tempo (Richards & Ford, cap. 6).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.

## Para pensar

- A regra da Listagem 6.4.17 não impede que um filtro dependa de uma classe utilitária que chama
  outro filtro. A segunda *fitness function* deste exercício fecha essa brecha. Que flexibilidade
  legítima ela retira?
- No PeerTube, o cano é uma fila do BullMQ, e cada *job* pai só fica elegível depois que os filhos
  terminam (Seção 6.4.5). O que muda na classe `Pipeline` se os filtros rodarem em processos
  diferentes?
