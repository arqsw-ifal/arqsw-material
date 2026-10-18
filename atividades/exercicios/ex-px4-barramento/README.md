# Exercício: barramento *publish/subscribe* com tópicos tipados (PX4)

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 6.8 (PX4: o código), em especial a
> Seção 6.8.10 e as Listagens 6.8.12 e 6.8.13.
> Este repositório é do estudante. Edite, envie, e o corretor automático devolve a nota.

## Contexto

No PX4, os módulos de voo (estimador, controladores, *commander*) não se chamam diretamente:
comunicam-se exclusivamente pelo **uORB**, um barramento *publish/subscribe* em que cada tópico
tem um tipo de mensagem definido em um arquivo `.msg` (Seções 6.8.1 a 6.8.3). A decisão,
registrada como ADR-PX4-001 no capítulo, garante que um módulo possa ser substituído, replicado
ou testado isoladamente, desde que publique e assine os mesmos tópicos.

Este repositório contém uma versão didática, em Java 17, da cascata de controle de atitude:

```
sensor_combined ──► [Estimador] ──► vehicle_attitude ──► [ControleAtitude]
                                                              │
vehicle_torque_setpoint ◄── [ControleTaxa] ◄── vehicle_rates_setpoint
```

O código, porém, foi escrito às pressas e **não respeita** a arquitetura pretendida:

1. o `Barramento` guarda a última amostra de cada tópico, mas **não notifica os assinantes**;
2. cada módulo **instancia e chama diretamente** o módulo seguinte da cascata
   (`Estimador` → `ControleAtitude` → `ControleTaxa`), e a `App` aciona o estimador por uma
   chamada de método, e não por uma publicação no tópico de sensores;
3. o `ControleTaxa` consulta um **campo estático** do `Estimador` para obter o instante da
   amostra, o que fecha um **ciclo** entre os módulos;
4. o `ControleTaxa` não satura o torque no intervalo [-1, 1] (veja os `TODO`).

As mensagens (`mensagens/`) e o tipo `Topico` (`barramento/`) já estão corretos e funcionam
como o contrato entre os módulos.

## Tarefas

1. Complete o `Barramento.publicar`: após atualizar a última amostra, notifique os assinantes
   do tópico, na ordem em que assinaram.
2. Faça cada módulo **assinar** o tópico que o dispara, no próprio construtor, e **publicar**
   o resultado, sem conhecer nenhum outro módulo:
    - `Estimador`: dispara com `SENSORES`, publica `ATITUDE` (quatérnio normalizado);
    - `ControleAtitude`: dispara com `ATITUDE`, publica `SETPOINT_TAXA`;
    - `ControleTaxa`: dispara com `SETPOINT_TAXA`, publica `SETPOINT_TORQUE`.
3. Elimine o campo estático do `Estimador`. O `ControleTaxa` deve obter o instante da amostra
   lendo a última `ATITUDE` publicada (`barramento.ultima(...)`), em analogia com uma assinatura
   uORB que é lida, mas não dispara o módulo (Listagem 6.8.4). Sem atitude publicada, use 0.
4. Implemente a saturação do torque em [-1, 1] em cada eixo.
5. Transforme a `App` em **raiz de composição**: ela cria o barramento e os três módulos e
   publica a leitura de sensores no tópico `SENSORES`.

O comportamento observável **não pode mudar**: `make run` deve imprimir o mesmo
`SetpointTorque` que imprime hoje.

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
| **Comportamento** | 50 | proporcional aos testes de `ComportamentoTest` que passam (barramento, cada módulo isolado e a cadeia completa) |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `ArquiteturaTest` (ArchUnit) que passam |

As *fitness functions* verificam que:

- nenhum módulo depende de outro módulo (ADR-PX4-001, Listagem 6.8.13);
- não há ciclos de dependência entre os módulos;
- os módulos dependem apenas do barramento, das mensagens e da biblioteca padrão do Java;
- as mensagens não dependem dos módulos;
- o barramento é genérico e não conhece mensagens nem módulos.

> Os testes de **arquitetura** não verificam *o que* o programa faz, e sim *como* ele está
> organizado: quem pode depender de quem (Richards & Ford, cap. 6).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.
