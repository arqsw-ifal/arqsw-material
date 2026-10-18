# Capítulo 4 - 50.000 pés: containers e implantação

!!! todo "Para desenvolver"
    Podcast do capítulo.

## Objetivos

Ao final deste capítulo, o leitor deverá ser capaz de:

- Identificar os *containers* de um sistema, no sentido do modelo C4, e as formas de comunicação
  entre eles.
- Distinguir camada lógica (*layer*) de nível físico (*tier*) e organização de implantação.
- Aplicar o conceito de quantum arquitetural para decidir entre arquiteturas monolíticas e
  distribuídas.
- Reconhecer os blocos de construção de sistemas em escala (balanceadores, réplicas, *caches*,
  CDN, filas) e a característica que cada um favorece.
- Relacionar decisões de implantação às falácias da computação distribuída.
- Reconstruir e registrar, em ADRs, as decisões de implantação dos sete sistemas estudados.

## 4.1 Introdução: a altitude da implantação

### 4.1.1 O que se observa a 50.000 pés

Descendo da altitude do contexto, a caixa única do Capítulo 3 se abre e revela suas partes
executáveis: aplicações web, processos de trabalho em segundo plano, bancos de dados, filas de
mensagens, armazenamentos de objetos, *caches*. No modelo C4, essas partes são chamadas de
***containers***: unidades que executam código ou armazenam dados separadamente das demais
(BROWN, 2018). O termo não se refere a contêineres Docker. Um *container* C4 pode ser implantado
num contêiner Docker, numa máquina virtual, num dispositivo embarcado ou num serviço gerenciado de
nuvem. A escolha entre essas formas de execução é justamente uma das decisões desta altitude.

A 50.000 pés interessam três perguntas. A primeira é **quais são os *containers*** e que
responsabilidade cada um tem. A segunda é **como eles se comunicam**: por chamadas síncronas, como
HTTP ou gRPC; por mensagens assíncronas, como filas e tópicos; ou por dados compartilhados. A
terceira é **onde e como eles são implantados**: em quantas instâncias, em que máquinas ou
plataformas, com que mecanismos de replicação, balanceamento e recuperação de falhas. Na
terminologia de Kruchten (1995), esta é a altitude da **visão física**. Na de Clements *et al.*
(2010), é a dos tipos de visão **componente e conector**, em escala grossa, e **alocação**
(Seção 1.3.2).

### 4.1.2 Camadas, níveis e implantação

Uma distinção conceitual orienta todo o capítulo. Fowler (2002, cap. 1) separa **camada**
(*layer*), uma divisão lógica de responsabilidades no código, de **nível** (*tier*), uma separação
física em processos ou máquinas distintos. A mesma organização lógica em três camadas pode ser
implantada num único processo ou distribuída por três máquinas. O guia da Microsoft retoma a
distinção e acrescenta padrões de implantação, como implantação distribuída ou não distribuída,
*clusters* com balanceamento de carga e *clusters* de *failover* (MICROSOFT, 2009, caps. 5 e 19).

A distinção importa porque cada fronteira física tem custos. Fowler (2002, cap. 7) formulou o
que chamou de primeira lei do projeto de objetos distribuídos: não distribua seus objetos. A
justificativa é que chamadas entre processos são ordens de grandeza mais lentas que chamadas
locais, estão sujeitas a falhas parciais e exigem interfaces de granularidade grossa. Toda vez
que um sistema estudado neste capítulo separa uma responsabilidade em outro *container*, é preciso
identificar que benefício justificou esse custo.

### 4.1.3 Monolítico ou distribuído: o quantum arquitetural

Richards e Ford (2020, cap. 7) propõem o **quantum arquitetural** como unidade de análise nesta
altitude. Um quantum é um artefato implantável de forma independente, com alta coesão funcional e
acoplamento síncrono entre suas partes. Um sistema cujas partes compartilham um único banco de
dados e são implantadas juntas tem um único quantum, ainda que seja composto por vários processos.
Um sistema cujos serviços têm dados próprios e se comunicam de forma assíncrona pode ter vários.

O conceito é útil porque as características arquiteturais valem **por quantum**. Se todas as partes
de um sistema precisam das mesmas características, um único quantum basta, e um estilo monolítico
tende a ser mais simples e mais barato. Se partes diferentes exigem características diferentes,
como escalabilidade extrema na ingestão de dados e consistência forte no faturamento, separá-las em
quanta distintos permite atender a cada uma sem impor a todas o custo da mais exigente (RICHARDS;
FORD, 2020, caps. 7 e 18). A Tabela 4.1 resume o catálogo de estilos de Richards e Ford segundo
essa divisão. O Apêndice D aprofunda cada estilo.

**Tabela 4.1** – Estilos monolíticos e distribuídos e sua ocorrência nos sistemas estudados.

| Estilo | Tipo | Número típico de quanta | Ocorrência neste capítulo |
|---|---|---|---|
| Camadas, com *jobs* assíncronos | Monolítico | Um | Mastodon (4.2); núcleo do Mattermost (4.3) |
| *Pipeline* | Monolítico ou distribuído | Um ou mais | PeerTube (4.4): um quantum com *workers* locais, dois ou mais com *runners* remotos |
| Microkernel | Monolítico | Um | Nextcloud (4.5); traços no sistema de *plugins* do Mattermost (4.3) |
| Monólito modular com extensões externas | Monolítico com satélites | Vários, acoplados | Saleor (4.6) |
| Orientado a eventos | Distribuído | Vários | ThingsBoard (4.7); difusão de eventos no Mattermost (4.3) |
| Microsserviços | Distribuído | Vários | ThingsBoard (4.7), na topologia alternativa |
| *Publish/subscribe* embarcado | Monolítico | Um | PX4 (4.8) |

Fonte: elaborado pelo autor (2026), com base em Richards e Ford (2020, caps. 9–17).

### 4.1.4 Blocos de construção e falácias

Os sistemas estudados combinam um repertório relativamente pequeno de blocos de construção.
Xu (2020, cap. 1) os apresenta na forma de uma evolução, de um único servidor a um sistema que
atende milhões de usuários:

- separação entre servidores web e bancos de dados;
- balanceadores de carga com servidores web sem estado (*stateless*), que permitem a escala
  horizontal;
- replicação do banco de dados com um primário e réplicas de leitura;
- *caches* e redes de distribuição de conteúdo (CDN);
- múltiplos centros de dados;
- filas de mensagens para desacoplar produtores e consumidores;
- particionamento horizontal dos dados (*sharding*).

Cada bloco melhora uma característica e introduz um novo problema. O *cache* melhora o desempenho,
mas cria a questão da consistência. A fila absorve picos, mas torna a resposta assíncrona e exige
tratar mensagens duplicadas. O *sharding* escala as escritas, mas complica consultas entre
partições. O Apêndice F percorre essa evolução em detalhe.

Toda distribuição expõe o sistema às **falácias da computação distribuída**, suposições falsas
que projetistas tendem a fazer: que a rede é confiável, que a latência é zero, que a banda é
infinita, que a rede é segura, que a topologia não muda, que há um único administrador, que o
custo de transporte é zero e que a rede é homogênea (RICHARDS; FORD, 2020, cap. 9). Nas seções de
aplicação, cada decisão de distribuição é examinada à luz dessas falácias: que mecanismo o
sistema adota para não depender delas?

### 4.1.5 Plataformas de implantação

Os sistemas estudados são distribuídos em formas diversas, e essa diversidade é instrutiva.
Mastodon, Mattermost, PeerTube, Nextcloud, Saleor e ThingsBoard são aplicações de servidor,
oferecidas com imagens de contêiner e arquivos de composição (Docker Compose) ou manifestos para
orquestradores. O empacotamento em contêineres, popularizado pelo Docker (MERKEL, 2014), e a
orquestração, de que o Kubernetes é o expoente (BURNS *et al.*, 2016), tornaram economicamente
viável implantar muitos processos independentes. Richards e Ford (2020, cap. 1) observam que
arquiteturas como a de microsserviços seriam proibitivas no início dos anos 2000, quando cada
servidor exigia licenças comerciais de sistema operacional, servidor de aplicação e banco de
dados. O PX4, por sua vez, é implantado num controlador de voo embarcado, e sua "implantação"
envolve a escolha de um sistema operacional de tempo real e de uma placa de *hardware*, decisões
que pertencem à arquitetura de sistemas (Seção 1.2).

### 4.1.6 Os produtos da análise a 50.000 pés

Cada seção de aplicação (4.2 a 4.8) produz:

1. o **diagrama de *containers*** (C4, nível 2), com responsabilidades, tecnologias e protocolos;
2. o **diagrama de implantação**, baseado na documentação oficial de instalação em produção;
3. a identificação do **estilo** e dos **quanta**;
4. a análise da **comunicação** (síncrona ou assíncrona) e da **localização dos dados**;
5. um ou mais **ADRs reconstruídos** (Apêndice B), que explicitam por que as decisões de
   implantação foram tomadas e que alternativas foram descartadas;
6. a **comparação com o sistema real** correspondente, quando há fontes públicas.

## Diagramas e animações

O interativo a seguir reconstrói, em onze passos, a evolução descrita por Xu (2020, cap. 1) e
analisada na Seção F.2: o diagrama de *containers* cresce de um servidor único até a arquitetura
com balanceador, réplicas, *cache*, CDN, filas e *sharding*. Em cada passo, o painel lateral
indica a característica que melhora, o novo problema e a falácia da computação distribuída que
passa a importar, e um medidor aplica as regras de composição de disponibilidade da Seção F.3.4.

<iframe src="../interativos/zero-a-milhoes.html" title="Do zero a milhões de usuários"
  style="width:100%; height:660px; border:1px solid #2A4468; border-radius:10px;"
  loading="lazy"></iframe>

## Vídeos

!!! todo "Para desenvolver"
    Vídeo (10–15 min) sobre a distinção entre camadas, níveis e *containers*.

## Exercícios

1. Explique a diferença entre camada e nível com um exemplo de sistema que tenha três camadas e um
   único nível.
2. Para cada sistema deste capítulo, conte os quanta. Em que casos a resposta depende da
   configuração de implantação escolhida?
3. Escolha duas falácias da computação distribuída e mostre, em dois sistemas diferentes, o
   mecanismo adotado para não depender delas.
4. Compare a implantação do Mastodon (Seção 4.2) com a do ThingsBoard (Seção 4.7). Por que um adota
   um monólito com processos de trabalho e o outro oferece também uma topologia de microsserviços?
5. Escreva um ADR para a decisão de introduzir uma fila de mensagens num dos sistemas. Inclua ao
   menos duas alternativas descartadas.
6. Por que o PX4 (Seção 4.8) não é decomposto em processos separados por rede, como os demais? Que
   características seriam prejudicadas?
7. Desenhe o diagrama de implantação de um dos sistemas para uma instituição com 500 usuários e
   para outra com 5 milhões. Que blocos de construção aparecem apenas no segundo?

## Glossário do capítulo

`Container (C4)`
:   Unidade que executa código ou armazena dados separadamente das demais, como uma aplicação web,
    um processo de trabalho ou um banco de dados. Não se confunde com contêiner Docker.

`Camada (layer)`
:   Divisão lógica de responsabilidades no código de um sistema.

`Nível (tier)`
:   Separação física de partes de um sistema em processos ou máquinas distintos.

`Quantum arquitetural`
:   Artefato implantável de forma independente, com alta coesão funcional e acoplamento síncrono
    entre suas partes; unidade na qual as características arquiteturais são definidas.

`Falácias da computação distribuída`
:   Oito suposições falsas comumente feitas no projeto de sistemas distribuídos, como a de que a
    rede é confiável ou de que a latência é zero.

`Balanceador de carga`
:   Componente que distribui requisições entre várias instâncias de um serviço.

`Réplica de leitura`
:   Cópia de um banco de dados, mantida sincronizada com o primário, usada para atender consultas.

## Referências

BROWN, S. **The C4 model for visualising software architecture**. 2018. Disponível em:
<https://c4model.com>. Acesso em: 18 out. 2026.

BURNS, B. *et al.* Borg, Omega, and Kubernetes. **Communications of the ACM**, v. 59, n. 5,
p. 50-57, 2016.

CLEMENTS, P. *et al.* **Documenting software architectures**: views and beyond. 2. ed. Boston:
Addison-Wesley, 2010.

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

KRUCHTEN, P. The 4+1 view model of architecture. **IEEE Software**, v. 12, n. 6, p. 42-50,
1995.

MERKEL, D. Docker: lightweight Linux containers for consistent development and deployment.
**Linux Journal**, v. 2014, n. 239, art. 2, 2014.

MICROSOFT. **Microsoft application architecture guide**. 2. ed. Redmond: Microsoft Press, 2009.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

XU, A. **System design interview**: an insider's guide. 2. ed. [S. l.]: Byte Code LLC, 2020.

## Material complementar

!!! todo "Para desenvolver"
    Seleção de textos de acesso aberto sobre implantação, contêineres e orquestração.
