# Atividades com correção automática

Exercícios de **Arquitetura de Software** em **Java 17 + Maven**, corrigidos automaticamente
pelo GitHub Actions e distribuídos pelo **Classroom 50**.

A nota tem três partes, e a terceira é o que diferencia esta disciplina:

| Parte | Peso | Ferramenta |
|---|---|---|
| Compila | 10 | Maven |
| Comportamento | 50 | JUnit 5 |
| **Arquitetura** | 40 | **ArchUnit**: *fitness functions* que verificam camadas, ciclos e dependências proibidas (Richards & Ford, cap. 6) |

```
atividades/
├── modelo-exercicio/     <- exercício-modelo (pedidos em camadas): copie para criar um novo
├── exercicios/           <- os 9 exercícios dos capítulos (mesma estrutura do modelo)
│   ├── pom.xml
│   ├── src/main/java/... <- código "legado" que o aluno refatora (com TODOs)
│   ├── src/test/java/... <- ComportamentoTest (JUnit) + ArquiteturaTest (ArchUnit)
│   ├── scripts/run_tests.sh   <- o corretor (modos: tudo | compila | comportamento | arquitetura | json)
│   ├── Makefile               <- make, make test, make run
│   └── .github/workflows/autograding.yml
├── gabaritos/            <- soluções (NÃO publicar nos repositórios dos alunos)
├── classroom50/          <- distribuição e coleta de notas (ver classroom50/README.md)
└── SEGURANCA.md          <- modelo de ameaça do corretor
```

## Criar um exercício novo

1. Copie `modelo-exercicio/` e troque o pacote/código em `src/main/java`.
2. Escreva os testes: `*ComportamentoTest.java` (o que o sistema faz) e `*ArquiteturaTest.java`
   (como ele deve estar organizado). O corretor encontra as categorias **pelo sufixo do nome**.
3. Escreva a solução em `gabaritos/<slug>/` e confirme que ela tira 100:
   copie `scripts/` para junto dela e rode `./scripts/run_tests.sh`.
4. Confirme que o código entregue ao aluno **compila** e tira uma nota parcial.
5. Acrescente uma linha em `classroom50/exercicios.tsv` e rode `classroom50/provisionar.sh`.

## Exercícios disponíveis

| Seção | Exercício | Pasta | Legado → gabarito |
|---|---|---|---|
| Ap. E / Cap. 6 | Pedidos em camadas | `modelo-exercicio` | 45 → 100 |
| 1.6.3 / Ap. E | Frete: do `switch` ao **Strategy** | `exercicios/ex-frete-strategy` | 49 → 100 |
| 6.2 (Mastodon) | *Timeline* com *fanout* híbrido e cache, atrás de portas | `exercicios/ex-mastodon-timeline` | 47 → 100 |
| 6.3 (Mattermost) | *Hub* de eventos com **Observer** | `exercicios/ex-mattermost-hub` | 57 → 100 |
| 6.4 (PeerTube) | *Pipeline* de transcodificação (**pipes and filters**) | `exercicios/ex-peertube-pipeline` | 37 → 100 |
| 6.5 (Nextcloud) | Microkernel de armazenamento (**Adapter** + **Decorator**) | `exercicios/ex-nextcloud-microkernel` | 43 → 100 |
| 6.6 (Saleor) | Contextos Pedido e Pagamento com **ACL** e idempotência | `exercicios/ex-saleor-contextos` | 40 → 100 |
| 6.7 (ThingsBoard) | Motor de regras (**Chain of Responsibility** + **Strategy**) | `exercicios/ex-thingsboard-regras` | 46 → 100 |
| 6.8 (PX4) | Barramento *publish/subscribe* com tópicos tipados | `exercicios/ex-px4-barramento` | 45 → 100 |
| Ap. F | Limitador de taxa com **Strategy** e relógio injetável | `exercicios/ex-limitador-de-taxa` | 46 → 100 |

Todos foram verificados com o corretor: o código entregue ao aluno compila e tira nota parcial; o
gabarito tira 100/100. As fitness functions de cada exercício estão descritas no README dele.