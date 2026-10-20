# Arquitetura de Software: um voo do negócio ao código

**Disciplina:** Arquitetura de Software (ARQSW)<br>
**Curso:** Bacharelado em Sistemas de Informação &mdash; IFAL, Campus Maceió<br>
**Autor:** Leo Fernandes, PhD

---

## Apresentação

Este livro trata da arquitetura de software a partir da análise de **sistemas reais de grande
porte**. A premissa que o orienta é que os conceitos da disciplina, como atributos de qualidade,
estilos, padrões, contextos delimitados e decisões, só são plenamente compreendidos quando
observados em sistemas concretos, com sua história, suas restrições e suas imperfeições. Por essa
razão, o livro acompanha sete sistemas do início ao fim.

Cada um deles é um par: um sistema amplamente conhecido, de código fechado, e um sistema de
**código aberto** de arquitetura comparável, aqui chamado de seu **gêmeo**. O sistema conhecido fornece o
problema, a escala e a motivação. O gêmeo permite estender a análise até o código-fonte, sem a
qual as decisões arquiteturais permanecem abstratas.

## O método: quatro altitudes

A análise segue uma descida em quatro níveis de abstração, que o livro denomina **altitudes**, em
analogia com a aproximação de uma aeronave. Cada capítulo corresponde a uma altitude e percorre os
sete sistemas nessa altitude, à maneira de Xu (2020): primeiro os requisitos, revelados numa
conversa com o cliente em que o desenho evolui a cada nova informação; depois a implantação; em
seguida os componentes; por fim, o código. As altitudes correspondem aos quatro
níveis do modelo C4 de Brown (2018), conforme a Tabela 1.

**Tabela 1** – As quatro altitudes de análise.

| Altitude | Nível C4 | Objeto | Questão típica |
|---|---|---|---|
| <span class="altitude a100">100.000 pés</span> | Contexto | Negócio, usuários, ecossistema, restrições | Que problema o sistema resolve, para quem, e que propriedades não podem falhar? |
| <span class="altitude a50">50.000 pés</span> | *Containers* | Unidades de execução e de dados, comunicação, implantação | O sistema é monolítico ou distribuído? Onde residem os dados? |
| <span class="altitude a10">10.000 pés</span> | Componentes | Organização interna de cada *container* | Que componentes, camadas e contextos existem, e por quê? |
| <span class="altitude solo">Solo</span> | Código | Classes, interfaces e funções | Onde cada decisão se materializa? Que padrões a realizam? |

Fonte: elaborado pelo autor (2026).

A descida termina com uma **subida de verificação**. Partindo de um trecho de código real, o
analista percorre as altitudes no sentido inverso e pergunta que componente o código realiza, que
decisão o componente concretiza e que característica arquitetural a decisão protege. O método é
apresentado em detalhe na Seção 1.11.

## Organização do livro

| Capítulo | Altitude | Tema |
|---|---|---|
| [1](capitulo-1-fundamentos/index.md) | — | Fundamentos: definições, arquitetura de sistemas, descrição de arquiteturas, atributos de qualidade, princípios e padrões de projeto, arquiteturas em camadas, hexagonal, em cebola e limpa, *Domain-Driven Design*, decisões arquiteturais e o método do livro |
| [2](capitulo-2-aplicacoes/index.md) | — | As sete aplicações: os sistemas reais, seus gêmeos de código aberto e a ficha *kata* de cada um |
| [3](capitulo-3-100-mil-pes/index.md) | <span class="altitude a100">100.000 pés</span> | Requisitos e contexto: a conversa com o cliente e o diagrama de contexto que evolui |
| [4](capitulo-4-50-mil-pes/index.md) | <span class="altitude a50">50.000 pés</span> | *Containers* e implantação: onde e como cada sistema executa, e por quê |
| [5](capitulo-5-10-mil-pes/index.md) | <span class="altitude a10">10.000 pés</span> | Componentes e comunicação, pelas lentes da arquitetura limpa, de Fowler e do DDD |
| [6](capitulo-6-solo/index.md) | <span class="altitude solo">Solo</span> | O código: modelo de domínio, DDD tático, padrões de projeto e a subida de verificação |
| [7](capitulo-7-sintese/index.md) | — | Síntese: escolha de estilos, avaliação de arquiteturas e arquitetura evolutiva |

Os sete sistemas atravessam os Capítulos 2 a 6. A tabela abaixo permite acompanhar um mesmo
sistema ao longo da descida.

| Sistema real | Gêmeo | Apresentação | 100.000 pés | 50.000 pés | 10.000 pés | Solo |
|---|---|---|---|---|---|---|
| Twitter/X | Mastodon | [2.2](capitulo-2-aplicacoes/mastodon.md) | [3.2](capitulo-3-100-mil-pes/mastodon.md) | [4.2](capitulo-4-50-mil-pes/mastodon.md) | [5.2](capitulo-5-10-mil-pes/mastodon.md) | [6.2](capitulo-6-solo/mastodon.md) |
| Slack, WhatsApp | Mattermost | [2.3](capitulo-2-aplicacoes/mattermost.md) | [3.3](capitulo-3-100-mil-pes/mattermost.md) | [4.3](capitulo-4-50-mil-pes/mattermost.md) | [5.3](capitulo-5-10-mil-pes/mattermost.md) | [6.3](capitulo-6-solo/mattermost.md) |
| YouTube, Netflix | PeerTube | [2.4](capitulo-2-aplicacoes/peertube.md) | [3.4](capitulo-3-100-mil-pes/peertube.md) | [4.4](capitulo-4-50-mil-pes/peertube.md) | [5.4](capitulo-5-10-mil-pes/peertube.md) | [6.4](capitulo-6-solo/peertube.md) |
| Dropbox, Google Drive | Nextcloud | [2.5](capitulo-2-aplicacoes/nextcloud.md) | [3.5](capitulo-3-100-mil-pes/nextcloud.md) | [4.5](capitulo-4-50-mil-pes/nextcloud.md) | [5.5](capitulo-5-10-mil-pes/nextcloud.md) | [6.5](capitulo-6-solo/nextcloud.md) |
| Mercado Livre, Amazon | Saleor | [2.6](capitulo-2-aplicacoes/saleor.md) | [3.6](capitulo-3-100-mil-pes/saleor.md) | [4.6](capitulo-4-50-mil-pes/saleor.md) | [5.6](capitulo-5-10-mil-pes/saleor.md) | [6.6](capitulo-6-solo/saleor.md) |
| AWS IoT | ThingsBoard | [2.7](capitulo-2-aplicacoes/thingsboard.md) | [3.7](capitulo-3-100-mil-pes/thingsboard.md) | [4.7](capitulo-4-50-mil-pes/thingsboard.md) | [5.7](capitulo-5-10-mil-pes/thingsboard.md) | [6.7](capitulo-6-solo/thingsboard.md) |
| DJI | PX4 Autopilot | [2.8](capitulo-2-aplicacoes/px4.md) | [3.8](capitulo-3-100-mil-pes/px4.md) | [4.8](capitulo-4-50-mil-pes/px4.md) | [5.8](capitulo-5-10-mil-pes/px4.md) | [6.8](capitulo-6-solo/px4.md) |

**Apêndices** (aprofundamentos): [A](apendice-a-c4/index.md) – O modelo C4 e as altitudes;
[B](apendice-b-adr/index.md) – Registro de decisão arquitetural;
[C](apendice-c-codigo-legado/index.md) – Explorando código legado;
[D](apendice-d-caracteristicas-estilos-decisoes/index.md) – Características arquiteturais, estilos e decisões;
[E](apendice-e-modularidade-padroes-de-aplicacao/index.md) – Modularidade, padrões de aplicação e governança arquitetural;
[F](apendice-f-sistemas-em-escala/index.md) – Sistemas em escala: blocos de construção e um voo de instrução.

## Atividades práticas

As atividades de programação são escritas em **Java 17** e corrigidas automaticamente. A avaliação
considera três dimensões: compilação, comportamento (testes JUnit) e **arquitetura**. A dimensão
arquitetural é verificada por funções de aptidão (*fitness functions*), testes escritos com a
biblioteca ArchUnit que confirmam o respeito às camadas, a ausência de ciclos e a inexistência de
dependências proibidas (FORD; PARSONS; KUA, 2017; RICHARDS; FORD, 2020).

## Fontes

A referência principal é Richards e Ford (2020). A análise de sistemas em escala apoia-se em Xu
(2020), e o vocabulário e o currículo, no SWEBOK v4 (WASHIZAKI, 2024). Para o domínio e os padrões,
as obras centrais são Evans (2003), Fowler (2002), Gamma *et al.* (1994) e, em português, Shvets
(2022). A lista completa e o mapa entre obras e capítulos estão na página de
[Referências](referencias.md).

## Referências

BROWN, S. **The C4 model for visualising software architecture**. 2018. Disponível em:
<https://c4model.com>. Acesso em: 18 out. 2026.

FORD, N.; PARSONS, R.; KUA, P. **Building evolutionary architectures**: support constant change.
Sebastopol: O'Reilly, 2017.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

XU, A. **System design interview**: an insider's guide. 2. ed. [*S. l.*]: Byte Code LLC, 2020.
