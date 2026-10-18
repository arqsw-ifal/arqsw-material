# Exercício: motor de regras para telemetria (ThingsBoard)

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 6.7 (ThingsBoard: o código)
> Este repositório é **seu**. Edite, envie, e o corretor automático dá o retorno.

## O sistema real

No ThingsBoard, os dispositivos enviam telemetria por vários protocolos (MQTT, HTTP, CoAP).
Cada transporte tem um **adaptador** que converte a mensagem do protocolo num formato interno
comum (padrão *Adapter*, Seção 6.7.5), e o restante da plataforma ignora protocolos. Depois, a
mensagem percorre o **motor de regras**: uma cadeia de nós (`TbNode`) em que cada nó filtra,
enriquece ou age, e decide por qual relação a mensagem segue (padrões *Chain of
Responsibility* e *Strategy*, Seção 6.7.4).

Este exercício reproduz essa separação em Java, seguindo as Listagens 6.7.12 e 6.7.13 do livro:

```
 transporte.http ──┐
                   ├──►  regras  (Leitura, Regra, SomenteGrandeza, Alarme)  ──►  regras.condicao (Strategy)
 transporte.mqtt ──┘
```

**Os transportes conhecem as regras; as regras não conhecem nenhum transporte.**

## A situação

O código que você herdou compila, mas:

1. `Alarme` publica o alarme direto no broker MQTT (`transporte.mqtt.PublicadorDeAlarmesMqtt`):
   a regra ficou amarrada a um protocolo, e há um **ciclo** `regras ⇄ transporte`;
2. a tradução dos formatos de transporte (corpo HTTP, carga útil MQTT) mora em
   `regras.ParserDeTelemetria`: o pacote das regras conhece os protocolos, e essa classe nem é
   um elo da cadeia;
3. há regras de comportamento não implementadas (veja os `TODO`).

## O que fazer

1. Implemente os `TODO`:
    - **regra 1:** `Regra.tratar` repassa a leitura ao próximo elo quando o elo atual deixa
      seguir (é isso que faz a corrente funcionar);
    - **regra 2:** as condições `AbaixoDe` (estritamente abaixo) e `ForaDaFaixa` (abaixo do
      mínimo ou acima do máximo; os limites pertencem à faixa);
    - **regra 3:** corpo HTTP malformado (campos faltando, número inválido) gera
      `IllegalArgumentException`;
    - **regra 4:** o adaptador MQTT entrega **todas** as grandezas da carga útil, na ordem.
2. Retire de `Alarme` a publicação no MQTT: a regra só registra o evento na lista; quem
   quiser publicar alarmes faz isso **no transporte**, com a lista devolvida.
3. Leve a tradução de cada formato para o adaptador do respectivo transporte e elimine
   `ParserDeTelemetria` do pacote `regras`.

Hoje `make run` imprime listas vazias, porque a corrente não repassa as leituras. Depois da
refatoração, ele deve imprimir um alarme para a leitura HTTP e outro para a publicação MQTT.

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
| **Comportamento** | 50 | proporcional aos testes de `ComportamentoTest` que passam (cadeia, condições, adaptadores HTTP e MQTT) |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `ArquiteturaTest` (ArchUnit): regras sem transporte, toda classe de `regras` é um elo, condições implementam a *Strategy*, transportes isolados, sem ciclos |

> A *fitness function* central é a da Listagem 6.7.13: "o motor de regras deve funcionar com
> qualquer protocolo". Um novo transporte (CoAP, por exemplo) deve exigir apenas um novo
> adaptador, sem tocar nas regras.

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.
