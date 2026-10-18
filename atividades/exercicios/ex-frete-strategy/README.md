# Exercício: do `switch` de frete ao padrão *Strategy*

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 1.6.3 (Listagem 1.2) e Apêndice E
> (Seções E.4.2, sobre lógica de domínio com *Strategy*, e E.6.3, sobre ArchUnit).
> Este repositório é do estudante. Edite, envie, e o corretor automático devolve a nota.

## Contexto

O padrão *Strategy* define uma família de algoritmos, encapsula cada um e os torna
intercambiáveis, de modo que o algoritmo varie independentemente dos clientes que o usam
(GAMMA *et al.*, 1994). A Listagem 1.2 do livro aplica o padrão ao cálculo de frete de um
comércio eletrônico: o `Checkout` depende da abstração `PoliticaDeFrete` e não das
implementações concretas, o que satisfaz o princípio aberto-fechado (OCP) e o princípio da
inversão de dependência (DIP).

O módulo de *checkout* deste repositório ainda calcula o frete com um `switch` sobre a
modalidade de entrega. A equipe começou a refatoração, mas parou no meio:

1. a interface `PoliticaDeFrete` já existe no domínio;
2. `FreteExpresso` foi extraída e funciona, mas o `Checkout` a instancia **diretamente**, o que
   cria um **ciclo** `dominio ⇄ frete`;
3. `FreteEconomico` existe apenas como esqueleto, e a regra continua no `switch`, com dois
   defeitos: o peso é truncado em vez de arredondado para cima e a promoção de frete grátis
   nunca foi implementada;
4. não há política para a retirada na loja (o `switch` lança exceção).

## Regras de negócio

| Modalidade | Regra |
|---|---|
| `ECONOMICO` | R$ 10,00 + R$ 1,50 por kg iniciado; **grátis** para pedidos a partir de R$ 200,00 |
| `EXPRESSO` | R$ 20,00 + R$ 3,00 por kg iniciado; nunca é grátis |
| `RETIRADA` | sem frete |

"Kg iniciado" significa que 1,2 kg é cobrado como 2 kg (`Pedido.pesoKgArredondadoParaCima()`).
Pedido sem itens não tem frete (`IllegalStateException`).

## Tarefas

1. Implemente `FreteEconomico` e crie `FreteRetirada`, ambas no pacote `frete`. Cada
   modalidade deve ter uma implementação chamada `Frete<Modalidade>`
   (`FreteEconomico`, `FreteExpresso`, `FreteRetirada`).
2. Implemente a promoção de frete grátis **por composição**, como `FreteGratisAcimaDe` na
   Listagem 1.2: uma política que envolve outra e devolve zero a partir de um valor-limite.
3. Reescreva o `Checkout` para receber as políticas no construtor (por exemplo,
   `Checkout(Collection<? extends PoliticaDeFrete>)`) e escolher a política pela
   `modalidade()` de cada uma, **sem `switch`** e sem conhecer nenhuma classe concreta.
4. Transforme `App.novoCheckout()` em **raiz de composição**: é o único lugar que instancia as
   políticas concretas. Os testes obtêm o *checkout* por esse método.

Ao final, `make run` deve mostrar o frete econômico grátis para o pedido de exemplo (R$ 209,80),
pois a promoção passa a ser aplicada; o frete expresso não muda.

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
| **Comportamento** | 50 | proporcional aos testes de `ComportamentoTest` que passam (regras da tabela acima) |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `ArquiteturaTest` (ArchUnit) que passam |

As *fitness functions* verificam que:

- nenhuma classe do domínio depende de implementações concretas de frete, apenas da interface;
- nenhuma classe do domínio escolhe o frete com `switch` sobre `Modalidade`;
- existe ao menos uma implementação `Frete<Modalidade>` para cada valor de `Modalidade`;
- as implementações de `PoliticaDeFrete` ficam no pacote `frete`;
- não há ciclos entre os pacotes.

> Os testes de **arquitetura** não verificam *o que* o programa faz, e sim *como* ele está
> organizado: quem pode depender de quem (Richards & Ford, cap. 6).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.
