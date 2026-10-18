# Capítulo 2 - As sete aplicações

!!! todo "Para desenvolver"
    Podcast do capítulo.

## Objetivos

Ao final deste capítulo, o leitor deverá ser capaz de:

- Explicar os critérios que orientaram a escolha dos sistemas estudados neste livro.
- Descrever, para cada um dos sete pares, o sistema real, o gêmeo de código aberto e o dilema
  arquitetural que o par ilustra.
- Preparar um ambiente local para executar e explorar os sistemas de código aberto.
- Formular a ficha *kata* de cada sistema como ponto de partida das análises dos Capítulos 3 a 6.

## 2.1 Introdução: por que estes sistemas

### 2.1.1 O estudo de caso como método

A arquitetura de software é uma disciplina de compromissos dependentes de contexto (Seção 1.1.2),
e compromissos dependentes de contexto não se aprendem bem em abstrato. Por isso este livro
adota o **estudo de caso** como método central: sistemas reais, com história, escala e restrições
conhecidas, são examinados até que suas decisões possam ser explicadas. O próprio livro de Gamma
*et al.* (1994, cap. 2) adota esse procedimento ao introduzir os padrões de projeto por meio do
editor de documentos Lexi, no qual cada padrão emerge como resposta a um problema concreto.
Richards e Ford (2020, cap. 5) recomendam um exercício análogo, os *katas* de arquitetura, nos
quais pequenos grupos projetam sistemas a partir de uma descrição, de uma lista de usuários e de
requisitos e de um contexto adicional.

Um estudo de caso em arquitetura, contudo, enfrenta uma dificuldade prática: os sistemas mais
conhecidos e mais instrutivos, como as grandes redes sociais, plataformas de vídeo e
comércios eletrônicos, têm código fechado. O que se sabe sobre eles vem de artigos de engenharia,
palestras e relatos de incidentes, fontes valiosas, mas que não permitem descer até o código. Este
livro resolve a dificuldade com **pares**: cada sistema real é associado a um sistema de **código
aberto** de propósito e arquitetura comparáveis, aqui chamado de seu **gêmeo**. O sistema real
fornece o problema, a escala e a motivação; o gêmeo fornece um repositório que pode ser lido,
executado e modificado.

### 2.1.2 Critérios de seleção

Os sete pares foram escolhidos segundo cinco critérios.

1. **Relevância e familiaridade.** O sistema real deve ser conhecido do leitor, de modo que o
   problema de negócio seja compreendido sem esforço.
2. **Existência de um gêmeo maduro.** O gêmeo deve ser um projeto ativo, com comunidade, versões
   estáveis, documentação de instalação e uso efetivo em produção, e não um protótipo acadêmico.
3. **Diversidade de estilos.** O conjunto deve cobrir os principais estilos arquiteturais do
   catálogo de Richards e Ford (2020, caps. 10–17): camadas, *pipeline*, microkernel, baseado em
   serviços, orientado a eventos, microsserviços e uma arquitetura embarcada de tempo real.
4. **Diversidade de características dominantes.** Cada par deve ter um dilema arquitetural
   próprio: latência de leitura, tempo real, custo, consistência, modificabilidade, vazão,
   determinismo.
5. **Diversidade de linguagens e plataformas.** O leitor deve encontrar Ruby, Go, TypeScript, PHP,
   Python, Java e C++, o que mostra que os conceitos arquiteturais transcendem a linguagem.

A Tabela 2.1 apresenta os sete pares.

**Tabela 2.1** – Os sete pares de sistemas estudados.

| Seção | Sistema real | Gêmeo de código aberto | Linguagens principais | Estilo dominante | Dilema central |
|---|---|---|---|---|---|
| [2.2](mastodon.md) | Twitter/X | Mastodon | Ruby, JavaScript | Camadas, *jobs* assíncronos, federação | Latência de leitura em escala |
| [2.3](mattermost.md) | Slack, WhatsApp | Mattermost | Go, TypeScript | Orientado a eventos | Tempo real e soberania de dados |
| [2.4](peertube.md) | YouTube, Netflix | PeerTube | TypeScript | *Pipeline*, CDN, P2P | Custo de processamento e entrega |
| [2.5](nextcloud.md) | Dropbox, Google Drive | Nextcloud | PHP, C++ (cliente) | Microkernel, sincronização | Consistência e extensibilidade |
| [2.6](saleor.md) | Mercado Livre, Amazon | Saleor | Python | Monólito modular, DDD | Modificabilidade e consistência financeira |
| [2.7](thingsboard.md) | AWS IoT | ThingsBoard | Java | Eventos, atores, microsserviços | Vazão de telemetria |
| [2.8](px4.md) | DJI | PX4 Autopilot | C++ | Embarcado de tempo real, *publish/subscribe* | Determinismo e segurança física |

Fonte: elaborado pelo autor (2026).

### 2.1.3 Limites da analogia entre os pares

A relação entre o sistema real e seu gêmeo é de **analogia**, e não de identidade, e o leitor
deve ter presentes os seus limites. O gêmeo não é uma cópia do sistema real. Em geral, opera em
escala muito menor, com outras restrições de custo e de organização, e foi desenvolvido por uma
comunidade, e não por uma empresa com milhares de engenheiros. Algumas decisões do gêmeo derivam
justamente de ele **não** ser o sistema real: o Mastodon é federado porque recusa a centralização
do Twitter; o Mattermost enfatiza a instalação local porque atende a organizações que não podem
enviar seus dados a terceiros. Essas diferenças, longe de serem um defeito do método, são
oportunidades de análise: quando os dois sistemas resolvem o mesmo problema de formas diferentes, a
comparação torna visíveis as características que motivaram cada escolha.

### 2.1.4 Preparação do ambiente

Todos os gêmeos, à exceção do PX4, são aplicações de servidor que podem ser executadas localmente
com Docker e Docker Compose, a partir de arquivos de configuração oferecidos pelos próprios
projetos. O PX4 pode ser executado em simulação, sem *hardware*, por meio do modo *software in the
loop* descrito em sua documentação. Recomenda-se ao leitor:

1. instalar Git, Docker e um editor com busca no projeto inteiro;
2. clonar cada repositório e **fixar a versão** (*tag*) indicada na seção correspondente, pois os
   caminhos de arquivos citados neste livro referem-se a essas versões;
3. seguir a documentação oficial de instalação para desenvolvimento;
4. aplicar o roteiro de exploração de código do Apêndice C.

!!! warning "Versões e *permalinks*"
    Repositórios ativos mudam continuamente. Todos os trechos de código citados nos Capítulos 3 a 6
    apontam para *permalinks* de uma versão fixada. Ao explorar uma versão mais recente, é provável
    que nomes de arquivos e classes tenham mudado. A arquitetura, porém, tende a mudar muito mais
    devagar que os nomes, e reconhecer essa estabilidade é parte do aprendizado.

### 2.1.5 A ficha *kata*

Cada seção deste capítulo termina com uma **ficha *kata***, no formato proposto por Richards e Ford
(2020, cap. 5): descrição do sistema, usuários, requisitos e contexto adicional. A ficha é o ponto de
partida comum das quatro análises seguintes: a conversa de requisitos do Capítulo 3 começa onde a
ficha termina.

## Diagramas e animações

O mapa a seguir posiciona os sete pares da Tabela 2.1 segundo o estilo dominante, nas colunas, e
a característica prioritária expressa no dilema central, nas linhas. Pares cujo dilema combina
duas características aparecem em duas linhas, com destaque para a principal. A visualização
evidencia os critérios de diversidade da Seção 2.1.2, e cada ficha remete à seção correspondente
deste capítulo (Seções 2.2 a 2.8).

<iframe src="../interativos/mapa-dos-voos.html" title="Mapa dos voos: os sete pares"
  style="width:100%; height:660px; border:1px solid #2A4468; border-radius:10px;"
  loading="lazy"></iframe>

## Vídeos

!!! todo "Para desenvolver"
    Vídeo (5 min por sistema) com a execução local de cada gêmeo.

## Exercícios

1. Proponha um oitavo par (sistema real e gêmeo de código aberto) que satisfaça os cinco critérios
   da Seção 2.1.2. Que estilo e que dilema ele acrescentaria ao conjunto?
2. Execute localmente dois dos gêmeos e registre o tempo de preparação, os *containers* criados e as
   dificuldades encontradas. O que isso revela sobre a característica de instalabilidade de cada um?
3. Para um dos pares, liste três decisões do gêmeo que decorrem do fato de ele **não** ser o sistema
   real.
4. Escreva a ficha *kata* de um sistema que você usa diariamente e que não está entre os sete.

## Glossário do capítulo

`Estudo de caso`
:   Método de ensino e de pesquisa que examina em profundidade um sistema real em seu contexto.

`Gêmeo de código aberto`
:   Sistema de código aberto de propósito e arquitetura comparáveis aos de um sistema real de código
    fechado, usado neste livro para estender a análise até o código.

`Kata de arquitetura`
:   Exercício de projeto arquitetural a partir de uma descrição curta, de usuários, de requisitos e
    de contexto.

`Tag`
:   Marcador que identifica uma versão específica de um repositório Git.

## Referências

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

## Material complementar

!!! todo "Para desenvolver"
    *Links* para os repositórios, a documentação e os blogs de engenharia de cada sistema.
