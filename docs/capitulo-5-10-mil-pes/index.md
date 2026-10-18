# Capítulo 5 - 10.000 pés: componentes e comunicação

!!! todo "Para desenvolver"
    Podcast do capítulo.

## Objetivos

Ao final deste capítulo, o leitor deverá ser capaz de:

- Decompor um *container* em componentes com responsabilidades e interfaces explícitas.
- Distinguir particionamento técnico de particionamento por domínio e relacionar a escolha à
  Lei de Conway.
- Analisar a organização interna de um sistema pelas lentes da arquitetura em camadas, hexagonal e
  limpa e dos padrões de aplicação corporativa de Fowler.
- Representar a comunicação entre componentes em diagramas de componentes e de sequência, e
  distinguir comunicação síncrona, assíncrona e por eventos.
- Identificar contextos delimitados e as relações entre eles (DDD estratégico).
- Reconstruir, para os sete sistemas estudados, as decisões e os compromissos da organização
  interna.

## 5.1 Introdução: a altitude dos componentes

### 5.1.1 O que se observa a 10.000 pés

Na altitude anterior, cada *container* era uma caixa: a aplicação web, o processo de trabalho, o
banco de dados. A 10.000 pés, os *containers* que executam código se abrem e revelam seus
**componentes**: agrupamentos de funcionalidade com responsabilidade definida e interface
explícita. No modelo C4, esse é o nível 3 (BROWN, 2018). Richards e Ford (2020, cap. 8) definem
componente como o bloco de construção físico de uma aplicação, em geral materializado como um
pacote, módulo ou *namespace*, e advertem que a identificação dos componentes é uma das primeiras
e mais importantes tarefas do arquiteto, porque é sobre eles que se distribuem as
responsabilidades do sistema.

Esta é a altitude em que as decisões arquiteturais começam a se tornar **visíveis no código**. Uma
regra como "o domínio não depende da infraestrutura" é uma decisão de 10.000 pés que se verifica
na estrutura de pacotes e nas declarações de importação. Por isso, este capítulo prepara a descida
ao solo do Capítulo 6, e o leitor deve acompanhar cada seção de aplicação com o repositório
correspondente aberto, seguindo o roteiro do Apêndice C.

### 5.1.2 Particionamento técnico e por domínio

Há duas maneiras fundamentais de organizar os componentes de primeiro nível de um sistema
(RICHARDS; FORD, 2020, cap. 8). No **particionamento técnico**, os componentes correspondem a
responsabilidades técnicas: apresentação, regras de negócio, persistência. É a forma da
arquitetura em camadas e de muitos *frameworks* que organizam o código em pastas como
`controllers`, `models` e `services`. No **particionamento por domínio**, os componentes
correspondem a áreas do negócio: catálogo, pedidos, pagamentos, entrega. Cada componente contém,
internamente, as responsabilidades técnicas de que precisa.

A escolha tem consequências. No particionamento técnico, uma mudança num conceito de negócio tende
a atravessar todos os componentes; no particionamento por domínio, tende a ficar confinada a um
deles. Em contrapartida, o particionamento técnico facilita a especialização das equipes por
tecnologia. Conway (1968) observou que as organizações produzem sistemas cuja estrutura reproduz a
estrutura de comunicação da própria organização, e Richards e Ford (2020, cap. 8) lembram que a
"manobra inversa de Conway" consiste em organizar as equipes de acordo com a arquitetura desejada.
Nos sistemas estudados, essa relação é observável: projetos mantidos por comunidades que se
organizam por área de funcionalidade tendem a apresentar particionamento por domínio, ao passo que
aplicações construídas sobre *frameworks* convencionais tendem ao particionamento técnico, ao
menos no primeiro nível.

### 5.1.3 Três lentes de análise

Para examinar a organização interna dos sistemas, este capítulo usa três lentes complementares,
todas apresentadas no Capítulo 1 e aprofundadas no Apêndice E.

**A lente das dependências.** As arquiteturas em camadas, hexagonal, em cebola e limpa
(Seção 1.8) diferem quanto ao **sentido das dependências** entre componentes. A pergunta central é:
as regras de negócio dependem dos mecanismos técnicos ou os mecanismos técnicos dependem das
regras de negócio? A resposta se obtém examinando que pacotes importam quais, e é sobre ela que
incidem as funções de aptidão escritas com ArchUnit (Apêndice E). Martin (2017) formula o critério
na regra da dependência: as dependências de código-fonte devem apontar para as políticas de mais
alto nível. Cockburn (2005) o formula na distinção entre a aplicação e seus adaptadores.

**A lente dos padrões de aplicação.** Fowler (2002) catalogou os padrões com que aplicações
corporativas organizam a lógica de domínio (*Transaction Script*, *Domain Model*, *Service
Layer*), o acesso a dados (*Active Record*, *Data Mapper*, *Repository*) e a apresentação (*Model
View Controller*, *Front Controller*). Identificar esses padrões permite descrever com precisão a
organização de um sistema e prever suas dificuldades. Um sistema que adota *Active Record* tende,
por exemplo, a acoplar regras de negócio ao esquema do banco, e esse é precisamente o problema que
o *Data Mapper* procura evitar.

**A lente do domínio.** O projeto estratégico do DDD (Seção 1.9.2) pergunta onde estão as fronteiras
entre modelos distintos e como os contextos se relacionam. Mesmo em sistemas monolíticos, é
possível identificar contextos delimitados, às vezes explícitos em módulos e às vezes apenas
implícitos na linguagem do código. Evans (2003, cap. 14) recomenda tornar o mapa de contextos
explícito, e esse exercício é feito para cada sistema estudado.

### 5.1.4 A comunicação entre componentes

Os componentes de um mesmo *container* comunicam-se, em geral, por chamadas locais de métodos ou
funções. Ainda assim, a forma dessa comunicação é uma decisão de projeto. Uma chamada direta acopla
o chamador à interface do chamado; um evento publicado num barramento interno desacopla emissor e
receptores; uma fila de trabalhos transfere a execução para outro momento e, possivelmente, para
outro processo. Hohpe e Woolf (2003) catalogaram os padrões de integração por mensagens que
estruturam essas escolhas, como canais ponto a ponto, canais de publicação e assinatura,
roteadores e tradutores. Richards e Ford (2020, cap. 14) analisam as duas topologias da arquitetura
orientada a eventos, com *broker* e com mediador, e seus compromissos de desempenho, escalabilidade
e tratamento de erros.

A distinção entre comunicação **síncrona** e **assíncrona** é central. Richards e Ford (2020,
cap. 18) recomendam usar comunicação síncrona por padrão e assíncrona quando necessário, porque a
assincronia melhora a escalabilidade e a elasticidade, mas complica a consistência, o tratamento de
erros e a depuração. Para representar a comunicação, este capítulo usa dois tipos de diagrama:
o **diagrama de componentes** (C4, nível 3), que mostra quem depende de quem, e o **diagrama de
sequência**, que mostra a ordem das interações numa operação-chave de cada sistema.

### 5.1.5 Os produtos da análise a 10.000 pés

Cada seção de aplicação (5.2 a 5.8) produz:

1. o **diagrama de componentes** de um ou dois *containers* principais;
2. o **diagrama de sequência** de uma operação-chave, como publicar uma mensagem, enviar um vídeo,
   sincronizar um arquivo, fechar um pedido, processar uma leitura de telemetria ou estabilizar a
   aeronave;
3. a análise pelas **três lentes** (dependências, padrões de aplicação e domínio);
4. o **mapa de contextos**, quando pertinente;
5. as **decisões e compromissos** da organização interna, com remissão aos ADRs do Capítulo 4.

A Tabela 5.1 antecipa a organização interna predominante de cada sistema.

**Tabela 5.1** – Organização interna predominante dos sistemas estudados.

| Seção | Sistema | Organização interna predominante | Comunicação interna de destaque |
|---|---|---|---|
| [5.2](mastodon.md) | Mastodon | Camadas abertas por convenção do Rails; inversão de dependência só na fronteira do ActivityPub | Filas de trabalhos (Sidekiq) |
| [5.3](mattermost.md) | Mattermost | Camadas `api4` → `app` → `store`; portas no armazenamento, no *cluster* e nos *plugins* | *Hub* de eventos via WebSocket |
| [5.4](peertube.md) | PeerTube | Camadas abertas; adaptadores para ffmpeg, armazenamento e ActivityPub | Fila de trabalhos de transcodificação |
| [5.5](nextcloud.md) | Nextcloud | Microkernel recursivo; hexagonal na fronteira com armazenamentos e *apps* | Eventos e *hooks* do núcleo |
| [5.6](saleor.md) | Saleor | Módulos por domínio; hexagonal na escala do sistema | *Webhooks* síncronos com disjuntor; eventos |
| [5.7](thingsboard.md) | ThingsBoard | Hexagonal no transporte e na fila; regra da dependência no motor de regras | Atores e filas |
| [5.8](px4.md) | PX4 | Barramento de módulos, e não camadas | *Publish/subscribe* (uORB) |

Fonte: elaborado pelo autor (2026). As classificações são detalhadas e justificadas nas seções de
cada sistema.

## Diagramas e animações

A lente da inversão de dependência, usada nas Seções 5.2 a 5.8, pode ser revista na animação a
seguir, que inverte, passo a passo, as setas de dependência de uma aplicação em camadas até a
arquitetura limpa, com base nas Seções 1.8.1 a 1.8.4.

<iframe src="../interativos/dependencias-camadas-vs-limpa.html" title="Das camadas à arquitetura limpa"
  style="width:100%; height:660px; border:1px solid #2A4468; border-radius:10px;"
  loading="lazy"></iframe>

## Vídeos

!!! todo "Para desenvolver"
    Vídeo (10–15 min) sobre particionamento técnico e por domínio, com exemplos de dois dos sete
    repositórios.

## Exercícios

1. Classifique o particionamento de primeiro nível dos sete sistemas e justifique cada
   classificação com a estrutura de pastas do repositório.
2. Escolha um sistema com particionamento técnico e proponha uma reorganização por domínio. Que
   mudanças futuras ficariam mais baratas? Quais ficariam mais caras?
3. Para dois sistemas, verifique empiricamente o sentido das dependências entre a lógica de negócio
   e a persistência. Eles seguem a regra da dependência da arquitetura limpa?
4. Compare a comunicação por filas de trabalhos (Mastodon, PeerTube) com a comunicação por
   *publish/subscribe* (PX4). Que características cada forma favorece?
5. Desenhe o mapa de contextos do Saleor (Seção 5.6) e identifique uma relação de camada
   anticorrupção.
6. Escreva uma regra ArchUnit que expresse uma decisão de 10.000 pés de um dos sistemas, como se ele
   fosse escrito em Java.

## Glossário do capítulo

`Componente`
:   Agrupamento de funcionalidade com responsabilidade definida e interface explícita, materializado
    em geral como pacote, módulo ou *namespace*.

`Particionamento técnico`
:   Organização dos componentes de primeiro nível segundo responsabilidades técnicas, como
    apresentação, negócio e persistência.

`Particionamento por domínio`
:   Organização dos componentes de primeiro nível segundo áreas do negócio.

`Manobra inversa de Conway`
:   Organização deliberada das equipes de acordo com a arquitetura que se deseja obter.

`Diagrama de sequência`
:   Diagrama que mostra a ordem das interações entre participantes numa operação.

`Comunicação assíncrona`
:   Forma de comunicação em que o emissor não aguarda a conclusão do processamento pelo receptor.

## Referências

BROWN, S. **The C4 model for visualising software architecture**. 2018. Disponível em:
<https://c4model.com>. Acesso em: 18 out. 2026.

COCKBURN, A. **Hexagonal architecture**. 2005. Disponível em:
<https://alistair.cockburn.us/hexagonal-architecture/>. Acesso em: 18 out. 2026.

CONWAY, M. E. How do committees invent? **Datamation**, v. 14, n. 4, p. 28-31, 1968.

EVANS, E. **Domain-driven design**: tackling complexity in the heart of software. Boston:
Addison-Wesley, 2003.

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

HOHPE, G.; WOOLF, B. **Enterprise integration patterns**: designing, building, and deploying
messaging solutions. Boston: Addison-Wesley, 2003.

MARTIN, R. C. **Clean architecture**: a craftsman's guide to software structure and design.
Boston: Prentice Hall, 2017.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

## Material complementar

!!! todo "Para desenvolver"
    Seleção de textos de acesso aberto sobre componentes, integração e arquitetura limpa.
