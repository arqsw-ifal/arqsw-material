# Exercício: *hub* de eventos do Mattermost com *Observer*

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 6.3 (Mattermost: o código), Seções 6.3.3 e 6.3.12, Listagens 6.3.13 e 6.3.14
> Este repositório é **seu**. Edite, envie, e o corretor automático dá o retorno.

## O sistema real

No servidor do Mattermost, toda mudança que interessa aos clientes conectados (mensagem nova,
edição, alguém digitando, alteração de presença) vira um evento entregue por WebSocket. Os
produtores desses eventos nunca acessam as conexões: eles chamam apenas `Publish`
(Listagem 6.3.5). Quem entrega é o *hub* (`web_hub.go`, Seção 6.3.3): ele consulta o índice de
conexões, encontra os destinatários pelo canal ou pelo usuário, pergunta a cada conexão se o evento
lhe diz respeito (`ShouldSendEvent`) e só então a notifica (Listagem 6.3.4).

Dois padrões aparecem nesse desenho. Para quem publica, o *hub* é um *Mediator*: o produtor só
conhece a operação "publicar". Para as conexões, o *hub* é o sujeito de um *Observer*: ele notifica
assinantes por meio de uma interface, sem saber o que cada um fará com o evento. A Seção 6.3.12
recria essa essência em Java 17 (Listagem 6.3.13):

```
produtores ──► Publicador (interface) ◄── HubDeEventos ──► Assinante (interface) ◄── conexoes
(ServicoDeMensagens)                        (índice)                            (ConexaoWebSocket,
                                                                                 NotificadorMovel)
```

## A situação

O código recebido é **legado**: compila e funciona em parte, mas tem dois problemas.

1. **Arquitetura violada.** O `ServicoDeMensagens` recebe um `Publicador`, mas o converte para o
   `HubDeEventos` concreto, lê o índice interno do *hub* e entrega a mensagem **ele mesmo**, só às
   instâncias de `ConexaoWebSocket`. O produtor passou a conhecer os assinantes concretos. A
   consequência é imediata: o `NotificadorMovel`, um assinante de outro tipo, não recebe nada, e
   o filtro de cada assinante (`deveReceber`) é ignorado.
2. **Comportamento incompleto** (veja os `TODO` em `hub/HubDeEventos.java`):
    - **regra 1:** eventos com alcance de **usuário** (`usuarioId` preenchido, `canalId` nulo) não
      chegam a ninguém;
    - **regra 2:** o *hub* não consulta `deveReceber` (o análogo de `ShouldSendEvent`);
    - **regra 3:** `remover` não remove o assinante.

## O que fazer

1. Faça o `ServicoDeMensagens` depender **somente** de `Publicador` e `Evento`: ele cria o evento
   e chama `publicar`; quem decide os destinatários é o *hub*.
2. Retire do `HubDeEventos` os métodos que expõem o índice interno (`assinantes()` e
   `membrosDo(...)`), que só existiam para o atalho.
3. Implemente as regras 1, 2 e 3 no `HubDeEventos`. Um assinante recebe o evento se for membro do
   canal do evento **ou** for o usuário do evento, **e** se `deveReceber` devolver verdadeiro.
4. Mantenha a raiz de composição (`App.java`) como o único lugar que conhece o *hub* concreto e os
   assinantes concretos.

Com tudo pronto, `make run` deve mostrar que o navegador e o celular da Bia receberam a mensagem.

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
| **Comportamento** | 50 | proporcional aos testes de `HubComportamentoTest` que passam: entrega aos membros do canal, a todas as conexões do usuário e na ordem de publicação; rejeição de mensagem vazia; entrega a qualquer tipo de assinante; respeito ao filtro do assinante; eventos com alcance de usuário; remoção de assinante |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `HubArquiteturaTest` (ArchUnit): os produtores não conhecem os assinantes concretos nem o *hub* concreto (Listagem 6.3.14), o *hub* não depende das conexões concretas e não há ciclos entre pacotes |

> Os testes de **arquitetura** não olham *o que* o programa faz, e sim *como* ele está
> organizado: quem pode depender de quem. É assim que equipes reais impedem que a arquitetura
> "apodreça" com o tempo (Richards & Ford, cap. 6).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.

## Para pensar

- O modelo omite a concorrência e a contrapressão: no Mattermost, uma conexão cujo canal de envio
  está cheio é fechada em vez de bloquear o *hub* (Listagem 6.3.4). Onde essa política entraria
  aqui, e por que ela não pode ficar no produtor?
- Como o *hub* deveria se comportar num *cluster*, em que o assinante pode estar em outro nó
  (Seção 6.3.4)?
