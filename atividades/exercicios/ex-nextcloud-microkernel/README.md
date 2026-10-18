# Exercício: um microkernel de armazenamento (Nextcloud)

> Disciplina **Arquitetura de Software (ARQSW)** · Seção 6.5 (Nextcloud: o código)
> Este repositório é **seu**. Edite, envie, e o corretor automático dá o retorno.

## O sistema real

O Nextcloud organiza o acesso a arquivos como um **microkernel** (Richards e Ford, 2020,
cap. 12). O núcleo define o contrato `IStorage`; cada serviço externo (S3, SMB, FTP, disco
local...) entra como um *plug-in* que **adapta** esse contrato à API do serviço (padrão
*Adapter*, Seção 6.5.4). Responsabilidades opcionais, como cota e criptografia, são
*wrappers* que envolvem qualquer armazenamento (padrão *Decorator*, Seção 6.5.6). O núcleo
descobre os *plug-ins* por configuração, sem mencioná-los no código-fonte (Seção 6.5.5).

Este exercício reproduz essa arquitetura em Java, seguindo as Listagens 6.5.6, 6.5.10 e 6.5.11
do livro:

```
                 nucleo  (contrato PluginDeArmazenamento, RegistroDePlugins, Envoltorio, Cota)
                ▲   ▲   ▲
   plugin.memoria  plugin.local  plugin.s3 ──► plugin.s3.sdk (biblioteca "externa")
```

As setas apontam para quem é conhecido: **os plug-ins conhecem o núcleo; o núcleo não conhece
nenhum plug-in; os plug-ins não se conhecem.**

## A situação

O código que você herdou compila e "funciona", mas viola o estilo:

1. o `RegistroDePlugins` (núcleo) "descobre" os plug-ins com `new`, e por isso **importa**
   `ArmazenamentoEmMemoria`, `ArmazenamentoS3` e até o SDK do S3;
2. o plug-in **S3** reaproveita `Caminhos`, um utilitário do plug-in **local**: dois plug-ins
   acoplados;
3. há regras de comportamento não implementadas (veja os `TODO`).

## O que fazer

1. Implemente os `TODO`:
    - **regra 1:** `RegistroDePlugins.obter` com identificador desconhecido lança
      `IllegalArgumentException`;
    - **regra 2:** `RegistroDePlugins.registrar` com identificador já registrado lança
      `IllegalStateException`;
    - **regra 3:** o *Decorator* `Cota` recusa (com `CotaExcedidaException`) a gravação que não
      cabe no limite, **sem** chamar o plug-in envolvido, e informa `usadoBytes()`;
      sobrescrever um arquivo libera o espaço que ele ocupava;
    - **regra 4:** o *Adapter* `ArmazenamentoS3` converte `S3Exception` (do SDK) em
      `ArmazenamentoIndisponivelException` (do contrato), em todas as operações.
2. Faça `carregarDoClasspath()` descobrir os plug-ins com `java.util.ServiceLoader`
   (Listagem 6.5.10), declarando o plug-in em memória no arquivo
   `src/main/resources/META-INF/services/br.edu.ifal.arqsw.armazenamento.nucleo.PluginDeArmazenamento`.
   O núcleo não pode citar nenhuma classe de plug-in.
3. Elimine a dependência `plugin.s3 → plugin.local` (por exemplo, levando a normalização de
   caminhos para o núcleo, que todos os plug-ins já conhecem).
4. Plug-ins que precisam de parâmetros (como o S3) são criados e registrados na **raiz de
   composição** (`App.java`).

`make run` deve continuar imprimindo o mesmo resultado.

**Não altere** o pacote `plugin.s3.sdk`: ele faz o papel de uma biblioteca externa.

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
| **Comportamento** | 50 | proporcional aos testes de `ComportamentoTest` que passam (registro, plug-ins, *Adapter* S3, *Decorator* de cota) |
| **Arquitetura** | 40 | proporcional às *fitness functions* de `ArquiteturaTest` (ArchUnit): núcleo sem plug-ins, plug-ins isolados, contrato implementado, SDK confinado, sem ciclos |

> As *fitness functions* traduzem em teste as restrições essenciais do microkernel: o núcleo
> depende apenas de abstrações, e os *plug-ins* são independentes entre si (Richards e Ford,
> 2020, cap. 12; Seção 6.5.12 do livro).

**Não altere** os arquivos em `src/test/` nem `scripts/`: o corretor do professor usa as
versões originais.
