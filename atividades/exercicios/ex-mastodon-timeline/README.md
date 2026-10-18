# Exercício: *timeline* do Mastodon com *fanout* híbrido e cache

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 6.2 (Mastodon: o código), Listagens 6.2.14 e 6.2.15
> Este repositório é **seu**. Edite, envie, e o corretor automático dá o retorno.

## O sistema real

No Mastodon, a *timeline* inicial de cada usuário é pré-computada: quando alguém publica, um
*job* (`DistributionWorker` → `FanOutOnWriteService`) insere o ID da publicação na *timeline* de
cada seguidor, guardada no Redis como um conjunto ordenado (`FeedManager`, Seção 6.2.3). Essa
estratégia se chama *fanout on write* (distribuição na escrita). Ela torna a leitura barata, mas
custa caro quando o autor tem muitos seguidores. Por isso, Xu (2020, cap. 11) propõe um modelo
híbrido: os autores comuns fazem *fanout on write*, e as **celebridades** são buscadas na leitura
(*fanout on read*). O Mastodon também restringe o *fanout* aos usuários **ativos** recentemente
(`USER_ACTIVE_DAYS`, Listagem 6.2.8).

A Seção 6.2.10 traduz essa decisão para Java 17 (Listagem 6.2.14). O serviço de *timeline* depende
apenas de três **portas de saída**, no estilo da arquitetura hexagonal (Seção 1.8.2):

```
               ┌──────────── aplicacao ────────────┐
App ──────────►│ ServicoDeTimeline                  │
(raiz de       │   ├─► CacheDeTimelines  (porta)  ◄─┼── cache.CacheEmMemoria     (simula o Redis)
 composição)   │   ├─► GrafoSocial       (porta)  ◄─┼── banco.GrafoSocialNoBanco  (simula o PostgreSQL)
               │   └─► PostsPorAutor     (porta)  ◄─┼── banco.PostsPorAutorNoBanco
               └────────────────────────────────────┘
```

Neste repositório, o limiar de celebridade é passado à porta `GrafoSocial` como argumento de
`celebridadesSeguidasPor`, para que o valor fique definido em um único lugar (o serviço).

## A situação

O código recebido é **legado**: compila e funciona em parte, mas tem dois problemas.

1. **Arquitetura violada.** O `ServicoDeTimeline` recebe as portas no construtor, mas "fura" a
   porta do grafo social: converte o `GrafoSocial` para o adaptador concreto
   (`GrafoSocialNoBanco`) e consulta o `BancoDeDados` diretamente. Isso cria uma dependência
   `aplicacao → banco` e, como os adaptadores do banco implementam as portas da aplicação, um
   **ciclo** `aplicacao ⇄ banco`. O serviço deixou de funcionar com qualquer outra
   implementação da porta.
2. **Comportamento incompleto** (veja os `TODO` em `aplicacao/ServicoDeTimeline.java`):
    - **regra 1:** o *fanout on write* chega também aos seguidores **inativos**;
    - **regra 2:** o caminho de leitura nunca foi terminado. O corte das celebridades na escrita
      já existe, mas a mescla na leitura não, e as publicações das celebridades "somem" das
      *timelines* dos seguidores.

## O que fazer

1. Faça o `ServicoDeTimeline` usar **somente** as portas `CacheDeTimelines`, `GrafoSocial` e
   `PostsPorAutor`. Ele não pode importar nada dos pacotes `banco` nem `cache`.
2. Remova o "atalho" do adaptador (o método `banco()` de `GrafoSocialNoBanco` só existe para ele).
3. Implemente a **regra 1**: só os seguidores ativos recebem o *fanout on write*.
4. Implemente a **regra 2** (*fanout on read*): a *timeline* mescla a lista pré-computada do cache
   com as publicações recentes das celebridades seguidas, sem repetições, do maior ID (mais
   recente) para o menor, respeitando o limite pedido.
5. Mantenha a raiz de composição (`App.java`) como o único lugar que conhece as classes concretas.

Com tudo pronto, `make run` deve imprimir `timeline da Bia: [200, 180, 150, 100]`.

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
| **Comportamento** | 50 | proporcional aos testes de `TimelineComportamentoTest` que passam: autor vê a própria publicação, *fanout on write* só para ativos, celebridades sem *fanout* na escrita, mescla na leitura, ordem, limite e ausência de repetições |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `TimelineArquiteturaTest` (ArchUnit): a aplicação não depende do banco (Listagem 6.2.15), depende só das próprias portas e do JDK, as portas são interfaces e não há ciclos entre pacotes |

> Os testes de **arquitetura** não olham *o que* o programa faz, e sim *como* ele está
> organizado: quem pode depender de quem. É assim que equipes reais impedem que a arquitetura
> "apodreça" com o tempo (Richards & Ford, cap. 6).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.

## Para pensar

- O limiar de celebridade é um parâmetro, e não uma constante escondida, pela mesma razão que o
  Mastodon expõe `USER_ACTIVE_DAYS`: o ponto de corte entre *push* e *pull* depende de medições.
- Num sistema real, `aoPublicar` rodaria num *job* assíncrono. Que garantias de ordem e de
  consistência se perdem com isso? (Compare com a caixa-preta da Seção 5.2.)
