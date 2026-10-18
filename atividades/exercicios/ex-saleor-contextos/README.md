# Exercício: dois contextos delimitados e uma camada anticorrupção (Saleor)

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 6.6 (Saleor: o código)
> Este repositório é **seu**. Edite, envie, e o corretor automático dá o retorno.

## O sistema real

No Saleor, a finalização da compra (`complete_checkout`) é uma **saga em processo**
(Seção 6.6.2): reserva o estoque (passo compensável), cobra o pagamento (passo pivô) e cria o
pedido (passo retentável); se a cobrança falha, as compensações desfazem a reserva
(Seção 6.6.3). Os pagamentos usam **chave de idempotência** protegida por restrição de
unicidade, para que uma nova tentativa não cobre o cliente duas vezes (Seção 6.6.4). E cada
*gateway* é um tradutor entre o modelo do provedor e a linguagem comum do núcleo, uma
**camada anticorrupção** (*anticorruption layer*, ACL) nos termos de Evans (2003, cap. 14)
(Seção 6.6.5).

Este exercício reproduz essas decisões em Java com dois **contextos delimitados**
(*bounded contexts*):

```
 pedido.dominio  ◄── pedido.aplicacao          (contexto de Pedido)
      ▲ porta
 pedido.acl  ─────────────►  pagamento.aplicacao ──► sdk.xpay   (contexto de Pagamento + provedor)
```

**Só a ACL de pedido conhece o contexto de Pagamento; o contexto de Pedido não conhece o SDK
do provedor; o contexto de Pagamento não conhece Pedido.**

## A situação

O código que você herdou compila e confirma pedidos no caminho feliz, mas:

1. `Pedido` (domínio) usa `RespostaDeCobranca`, um tipo do contexto de **Pagamento**;
2. `ServicoDeConfirmacao` chama `ServicoDeCobranca` diretamente e trata uma exceção do **SDK**
   do provedor (`XPayTimeoutException`);
3. a cada nova tentativa, ele gera uma chave com `UUID.randomUUID()`: a chave identifica a
   **tentativa**, e não a **intenção**. Quando a resposta do provedor se perde, o cliente é
   cobrado duas vezes (veja a caixa-preta da Seção 6.6.4);
4. a compensação e a idempotência não foram implementadas (veja os `TODO`).

## O que fazer

1. Implemente os `TODO`:
    - **regras 1 e 2 (compensação):** pagamento recusado devolve o estoque reservado, cancela o
      pedido e devolve `CANCELADO`, sem lançar exceção;
    - **regra 3:** confirmar um pedido já confirmado não reserva nem cobra de novo;
    - **regra 4 (idempotência em `ServicoDeCobranca`):** mesma chave e mesmo valor devolvem o
      resultado guardado **sem chamar o provedor**; mesma chave com outro valor lança
      `ConflitoDeIdempotenciaException` (Listagem 6.6.10);
    - **regra 5:** as novas tentativas após `XPayTimeoutException` ficam dentro do contexto de
      Pagamento, sempre com a **mesma** chave; esgotadas, lança `PagamentoIndisponivelException`.
2. Derive a chave de idempotência do **pedido** (por exemplo, `"pedido-" + id`).
3. Defina em `pedido.dominio` uma **porta** de pagamento (interface) com tipos da linguagem de
   Pedido, e implemente-a num adaptador em `pedido.acl`, que traduz para `ServicoDeCobranca` e de
   volta.
4. Ligue tudo na raiz de composição (`App.montar`), o único lugar que conhece os dois contextos.

`make run` deve continuar imprimindo o mesmo resultado.

**Não altere** o pacote `sdk.xpay`: ele faz o papel de uma biblioteca externa.

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
| **Comportamento** | 50 | proporcional aos testes de `ComportamentoTest` que passam (saga, compensação, idempotência) |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `ArquiteturaTest` (ArchUnit): domínio de pedido isolado, só a ACL acessa Pagamento, pedido sem SDK, Pagamento sem Pedido |

> As *fitness functions* tornam explícita a fronteira entre os contextos, que o Saleor deixa
> implícita (Seções 5.6.7 e 6.6.10 do livro).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.
