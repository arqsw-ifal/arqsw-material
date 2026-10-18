# Exercício: pedidos em camadas (refatorando um código legado)

> Disciplina **Arquitetura de Software (ARQSW)** · Capítulo 6 e Apêndice E
> Este repositório é **seu**. Edite, envie, e o corretor automático dá o retorno.

## A situação

Você herdou o módulo de **pedidos** de uma loja virtual. Ele "funciona", mas a arquitetura
pretendida, desenhada lá atrás pela equipe, **não é respeitada** pelo código:

```
web  ──►  aplicacao  ──►  dominio  ◄──  infra
(controller)  (casos de uso)  (regras)   (persistência)
```

Hoje acontecem três atalhos:

1. o **controller** (`web`) fala direto com o repositório da **infraestrutura** e ainda
   calcula o total do pedido por conta própria (*Smart UI*, Evans cap. 4);
2. a entidade **`Pedido`** (`dominio`) sabe se salvar sozinha e, para isso, **depende da
   infraestrutura** (estilo *Active Record*, Fowler cap. 10), o que cria um **ciclo**
   `dominio ⇄ infra`;
3. duas **regras de negócio** do pedido nunca foram implementadas (veja os `TODO`).

## O que fazer

1. Implemente as regras marcadas com `TODO` em `dominio/Pedido.java`:
    - **regra 1:** pedido sem itens não pode ser fechado (`IllegalStateException`);
    - **regra 2:** pedido fechado não aceita novos itens (`IllegalStateException`).
2. Faça o `PedidoController` usar **somente** o `ServicoDePedidos` (recebido no construtor).
3. Remova a dependência do domínio para a infraestrutura (o `Pedido` não deve mais se salvar).
4. Ligue as camadas na **raiz de composição** (`App.java`): ela é o único lugar que pode
   conhecer todas as classes concretas.

O comportamento observável **não pode mudar**: `make run` deve imprimir o mesmo resultado.

## Como testar antes de enviar

```console
$ make test
```

Isso roda **o mesmo corretor** que o GitHub vai rodar. Você vê a nota na hora.
Outros atalhos: `make` (só compila), `make run` (executa o `App`), `make clean`.
Requer **Java 17+** e **Maven**.

## Como você é avaliado (100 pontos)

| Critério | Pontos | O que é verificado |
|---|---|---|
| **Compila** | 10 | `mvn test-compile` sem erros |
| **Comportamento** | 50 | proporcional aos testes de `ComportamentoTest` que passam |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `ArquiteturaTest` (ArchUnit) que passam |

> Os testes de **arquitetura** não olham *o que* o programa faz, e sim *como* ele está
> organizado: quem pode depender de quem. É assim que equipes reais impedem que a arquitetura
> "apodreça" com o tempo (Richards & Ford, cap. 6).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.
