# 2.6 Mercado Livre, Amazon e Saleor

O comércio eletrônico é, provavelmente, o domínio de negócio mais familiar a qualquer
estudante de computação: todos já procuraram um produto, colocaram itens num carrinho,
pagaram e acompanharam uma entrega. Essa familiaridade é enganosa. Por trás da tela de
*checkout* há subdomínios com linguagens, regras e ritmos de mudança muito diferentes: o
catálogo muda a cada hora, as regras fiscais mudam por ato normativo, os meios de pagamento
mudam por decisão de bancos e do Banco Central, e as transportadoras mudam de contrato e de
API sem consultar a loja. Dinheiro e estoque exigem consistência; a vitrine exige desempenho
e tolera dados levemente desatualizados. Esta seção apresenta os sistemas reais de
referência, a Amazon e o Mercado Livre, e o gêmeo de código aberto, o Saleor, que será
analisado nas quatro altitudes ao longo das Seções 3.6, 4.6, 5.6 e 6.6.

## 2.6.1 Os sistemas reais

### Amazon

A Amazon lançou sua loja em 1995, apoiada numa aplicação monolítica que tratava de todos os
aspectos do *site* (FOX; PATTERSON, 2021, seção 3.4). Por volta de 2002, segundo o relato
de Steve Yegge, ex-funcionário da empresa, Jeff Bezos determinou que todas as equipes
passassem a expor dados e funcionalidades exclusivamente por interfaces de serviço,
projetadas desde o início para poderem ser abertas a desenvolvedores externos, e proibiu
qualquer outra forma de comunicação entre equipes, como a leitura direta do banco de dados
de outra equipe (YEGGE, 2011). Fox e Patterson (2021, seção 3.4) usam esse episódio para
mostrar como a arquitetura orientada a serviços deixou de ser um jargão e se tornou prática
concreta, e o relacionam à possibilidade, explorada depois pela empresa, de oferecer seus
serviços internos a terceiros. O mandato é examinado em detalhe na Seção 4.6.

Sobre a arquitetura atual da varejista há pouca documentação primária. O que se sabe de forma
verificável sobre seu comportamento em pico vem, em grande parte, da cobertura jornalística de
incidentes, como a falha na abertura do Prime Day de 2018, analisada como caixa-preta na
Seção 4.6.

### Mercado Livre

O Mercado Livre (MercadoLibre, Inc.) é uma empresa incorporada no estado de Delaware, nos
Estados Unidos, com operação centrada na América Latina. Segundo seu relatório anual à
comissão de valores mobiliários americana, sua plataforma de comércio eletrônico está
presente em 18 países e seu braço financeiro, o Mercado Pago, em 8; o Mercado Pago foi
concebido inicialmente para facilitar as transações do *marketplace*, e o Mercado Envios é
descrito como uma solução de logística oferecida como serviço de valor agregado
(MERCADOLIBRE, 2026). O dado é relevante para a arquitetura: pagamento e logística, que numa
loja comum seriam subdomínios terceirizados, são para o Mercado Livre negócios próprios.

A empresa não publica a arquitetura de seu sistema em detalhe, e este livro limita-se ao que
é verificável em fontes públicas. Em estudo de caso publicado pela AWS, o Mercado Livre relata
a migração de uma arquitetura monolítica para uma orientada a microsserviços e a criação de
uma plataforma interna de desenvolvimento, a **Fury**, que abstrai serviços de banco de dados
e de infraestrutura para os desenvolvedores (AWS, 2026). Em palestra na PlatformCon 2023,
Juliano Marcos Martins, gerente técnico sênior da empresa, descreveu a Fury como uma
plataforma que gerencia mais de 26 mil microsserviços e atende a mais de 12 mil
desenvolvedores (MARTINS, 2023). Os números variam entre fontes e datas (o estudo da AWS
menciona aproximadamente 24 mil microsserviços), o que é esperado num sistema em evolução. O
ponto arquitetural, e não o número exato, é o que interessa: uma organização com milhares de
desenvolvedores optou por microsserviços e, para torná-los viáveis, precisou construir uma
plataforma inteira de engenharia.

## 2.6.2 O gêmeo de código aberto: Saleor

### Origem, licença e comunidade

O Saleor é uma plataforma de comércio eletrônico escrita em Python. O arquivo de licença do
repositório registra sua história em duas linhas de direitos autorais: a Mirumee Software,
uma empresa polonesa de desenvolvimento de software, entre 2010 e 2020, e a Saleor Commerce,
empresa criada para o produto, a partir de 2020; a licença é a **BSD de 3 cláusulas**,
permissiva, que admite uso comercial e modificação sem obrigação de publicar o código
derivado (SALEOR, 2026a, `LICENSE`). O modelo de negócio da Saleor Commerce combina o
código aberto com uma oferta gerenciada em nuvem, a Saleor Cloud, mencionada no README. O
desenvolvimento é público no GitHub, com lançamentos frequentes: a versão adotada neste livro,
**3.23.42**, foi publicada em 16 de outubro de 2026, uma semana depois da 3.23.41; em
paralelo, a linha 3.22 continuava recebendo correções.

### Proposta e tecnologias

O README define o Saleor como uma plataforma "GraphQL native, API-only" e afirma que APIs são
a única forma de interagir com o *backend*, configurá-lo ou estendê-lo (SALEOR, 2026a,
`README.md`). Isso caracteriza uma arquitetura *headless* (sem interface própria): o painel
administrativo (Saleor Dashboard) e a vitrine de referência são projetos separados, em
repositórios próprios, que consomem a mesma API pública. Entre os recursos declarados estão
multicanal (preços, moedas, estoques e produtos por canal), múltiplos depósitos, promoções e
cupons, devoluções e uma API de pagamentos extensível a qualquer meio.

A Tabela 2.6.1 reúne a ficha técnica da versão estudada, conferida no `pyproject.toml`, no
`Dockerfile` e no código da *tag* (SALEOR, 2026a) e no arquivo Docker Compose do repositório
`saleor-platform` (SALEOR, 2026d). A linha de interface e clientes é a que mais distingue o Saleor
dos demais gêmeos: o núcleo não tem interface própria, e tudo o que o usuário vê é cliente da API
GraphQL. A outra particularidade decorre da primeira: as integrações com pagamento, impostos,
frete e outros serviços externos são feitas por **apps**, aplicações externas, em qualquer linguagem, que se comunicam com o Saleor por *webhooks* e pela
API (SALEOR, 2026c).

**Tabela 2.6.1** – Ficha técnica do Saleor 3.23.42.

| Item | Descrição |
|---|---|
| **Linguagem(ns) do servidor/núcleo** | Python 3.12 |
| ***Frameworks* principais** | Django 5.2; Graphene (API GraphQL); Celery (tarefas assíncronas); Uvicorn (servidor ASGI) |
| **Interface e clientes** | nenhuma no núcleo (*headless*); Saleor Dashboard, vitrine de referência e *apps* são projetos separados que consomem a API GraphQL |
| **Banco(s) de dados** | PostgreSQL, pelo *driver* `psycopg`, com réplica de leitura opcional; PostgreSQL 15 no `saleor-platform` |
| ***Cache*, filas e mensageria** | Redis ou Amazon SQS como intermediário do Celery, com filas dedicadas a *webhooks*; Valkey 8.1 como *cache* e intermediário no `saleor-platform` |
| **Armazenamento de arquivos/objetos** | mídia fora do banco: volume compartilhado entre API e *worker* no `saleor-platform`, ou armazenamento de objetos em produção |
| **Protocolos e APIs expostas** | API GraphQL (`/graphql/`), única forma de interagir com o *backend*; *webhooks* síncronos e assíncronos, entregues por HTTP(S), Amazon SQS ou Google Pub/Sub |
| **Execução e implantação** | imagem Docker oficial (`ghcr.io/saleor/saleor`), com a aplicação ASGI na porta 8000; Docker Compose do `saleor-platform`; oferta gerenciada Saleor Cloud |
| **Licença (da versão adotada)** | BSD de 3 cláusulas |
| **Versão adotada no livro (*tag*)** | [`3.23.42`](https://github.com/saleor/saleor/tree/3.23.42) (*commit* `cb24c741`), publicada em 16 de outubro de 2026 |

Fonte: elaborado pelo autor (2026), com base em Saleor (2026a; 2026d) e na Seção 4.6.

### Como executar localmente

A documentação oficial recomenda o repositório `saleor-platform`, que reúne num arquivo
Docker Compose todos os serviços necessários (SALEOR, 2026b). A sequência de comandos
documentada é a seguinte:

**Listagem 2.6.1** – Comandos (shell, Saleor): execução local do Saleor com Docker Compose.

```bash
git clone https://github.com/saleor/saleor-platform.git
cd saleor-platform
docker compose pull
docker compose run --rm api python3 manage.py migrate
docker compose run --rm api python3 manage.py populatedb        # dados de exemplo (opcional)
docker compose run --rm api python3 manage.py createsuperuser
docker compose up
```

Fonte: Saleor (2026b).

O arquivo `docker-compose.yml` do `saleor-platform`, no *commit* `ab6315bd` consultado para
este livro, declara os serviços `api` (imagem `ghcr.io/saleor/saleor:3.23`, porta 8000),
`dashboard` (porta 9000), `db` (PostgreSQL 15), `cache` (Valkey 8.1, um derivado de código
aberto do Redis, usado como *cache* e como intermediário do Celery), `worker` (a mesma imagem
da API, executando o Celery), `jaeger` (rastreamento distribuído) e `mailpit` (servidor de
e-mail para testes) (SALEOR, 2026d). Depois do `docker compose up`, o painel fica disponível em
`localhost:9000` e a API GraphQL em `localhost:8000/graphql/`. A Seção 4.6 analisa essa
topologia como diagrama de *containers*.

## 2.6.3 Por que este par

O par Amazon/Mercado Livre e Saleor foi escolhido por três razões. A primeira é que o
comércio eletrônico é o domínio clássico do **projeto estratégico** do *Domain-Driven Design*
(Seção 1.9.2): o mesmo "produto" significa coisas diferentes no catálogo, no estoque, na
entrega e no faturamento, e as integrações com adquirentes de cartão, com o Pix, com
transportadoras e com as Secretarias da Fazenda obrigam a decidir, para cada fronteira,
entre conformar-se ao modelo do outro ou traduzi-lo. É neste par que o livro desenvolve o
mapa de contextos, a camada anticorrupção e a linguagem publicada (Seção 5.6).

A segunda razão é que o par ilustra, com nitidez, o **dilema entre monólito modular e
microsserviços**. A Amazon e o Mercado Livre são os exemplos canônicos da fragmentação em
serviços, motivada sobretudo pela escala das organizações. O Saleor adota uma posição
híbrida e deliberada: um núcleo monolítico, que preserva transações locais entre pedido,
pagamento e estoque, e extensões externas para os subdomínios que não lhe pertencem. O
próprio README discute esse compromisso, ao listar as vantagens das extensões externas e, em
seguida, reconhecer que, para um desenvolvedor único com uma loja pequena, a abordagem
orientada a serviços pode parecer mais complexa do que a de plataformas tradicionais
(SALEOR, 2026a, `README.md`). A Seção 4.6 retoma o dilema pelo critério do *quantum*
arquitetural de Richards e Ford (2020).

A terceira razão é que o Saleor torna visíveis, em código legível, os mecanismos de
**consistência sem transação distribuída** que todo comércio eletrônico precisa: a divisão
do *checkout* em transações sucessivas, com bloqueio semântico e compensações; chaves de
idempotência em pagamentos; e repetição automática de *webhooks*. Esses mecanismos, que nos
sistemas fechados só podem ser inferidos, são examinados linha a linha na Seção 6.6.

## 2.6.4 Ficha *kata*

Richards e Ford (2020, cap. 5), adaptando uma ideia de Ted Neward, propõem os *architecture
katas* como exercício de identificação de características arquiteturais a partir de uma
descrição curta de negócio. A ficha a seguir descreve um comércio eletrônico genérico, que
serve de denominador comum para a Amazon, o Mercado Livre e uma loja construída sobre o
Saleor.

**Tabela 2.6.2** – Ficha *kata* do comércio eletrônico.

| Item | Conteúdo |
|---|---|
| **Descrição** | Plataforma de comércio eletrônico com catálogo, busca, carrinho, *checkout*, pagamento, estoque, entrega, faturamento e pós-venda (devoluções e reembolsos). Num *marketplace*, terceiros também vendem pela plataforma. |
| **Usuários** | Compradores (de milhares a dezenas de milhões), lojistas e vendedores terceiros, equipes internas de atendimento, financeiro, logística e fiscal. |
| **Requisitos** | Navegar e buscar no catálogo; montar o carrinho; calcular frete e impostos; pagar com cartão, Pix ou boleto; reservar e baixar estoque; emitir a nota fiscal eletrônica; despachar pela transportadora; rastrear; devolver e reembolsar. |
| **Contexto adicional** | Picos extremos em datas promocionais; dinheiro e estoque exigem consistência; muitas integrações externas com modelos próprios (adquirentes, Pix, transportadoras, SEFAZ); equipes diferentes cuidam de partes diferentes do sistema. |

Fonte: elaborado pelo autor (2026).

**Tabela 2.6.3** – Plano de voo do par.

| | |
|---|---|
| **Sistemas reais** | Amazon e Mercado Livre (código fechado; análise baseada em fontes públicas) |
| **Gêmeo de código aberto** | [Saleor](https://github.com/saleor/saleor), *tag* `3.23.42` (*commit* `cb24c741`) · [documentação](https://docs.saleor.io); ficha técnica na Tabela 2.6.1 |
| **Estilo dominante** | monólito modular *headless*, estendido por *apps* externas; contraste com microsserviços |
| **Conceitos centrais** | subdomínios, contextos delimitados, mapa de contextos, camada anticorrupção, sagas, idempotência, *Remote Facade*, DTO, *Active Record* |
| **Atividade prática** | dois contextos delimitados (Pedido e Pagamento) que só se comunicam por camada anticorrupção, verificados com ArchUnit (Seção 6.6) |

Fonte: elaborado pelo autor (2026), com base em Saleor (2026a).

!!! warning "Versão estudada"
    Todos os caminhos, números de linha e trechos do Saleor citados nas Seções 3.6 a 6.6
    referem-se à *tag* `3.23.42`. Nomes de módulos e funções mudam entre versões; os
    *permalinks* apontam para essa *tag* e continuarão válidos mesmo que o ramo principal
    evolua.

## Referências

AWS. **AWS Innovator**: Mercado Libre. [*S. l.*]: Amazon Web Services, 2026. Disponível em:
<https://aws.amazon.com/solutions/case-studies/innovators/mercado-libre/>. Acesso em: 18 out.
2026.

FOX, A.; PATTERSON, D. **Engineering software as a service**: an agile approach using cloud
computing. 2. ed. beta. San Francisco: Strawberry Canyon, 2021.

MARTINS, J. M. **Efficiency and platform engineering**. Palestra apresentada na PlatformCon
2023. Disponível em: <https://2023.platformcon.com/talks/efficiency-and-platform-engineering>.
Acesso em: 18 out. 2026.

MERCADOLIBRE, INC. **Form 10-K**: annual report for the fiscal year ended December 31, 2025.
Washington, DC: U.S. Securities and Exchange Commission, 2026. Disponível em:
<https://www.sec.gov/Archives/edgar/data/1099590/000109959026000006/meli-20251231.htm>. Acesso
em: 18 out. 2026.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

SALEOR. **saleor/saleor**: repositório de código-fonte, versão 3.23.42. GitHub, 2026a.
Disponível em: <https://github.com/saleor/saleor/tree/3.23.42>. Acesso em: 18 out. 2026.

SALEOR. **Docker Compose**. Documentação do Saleor, 2026b. Disponível em:
<https://docs.saleor.io/setup/docker-compose>. Acesso em: 18 out. 2026.

SALEOR. **Apps overview**. Documentação do Saleor, 2026c. Disponível em:
<https://docs.saleor.io/developer/extending/apps/overview>. Acesso em: 18 out. 2026.

SALEOR. **saleor/saleor-platform**: docker-compose.yml, *commit* ab6315bd. GitHub, 2026d.
Disponível em:
<https://github.com/saleor/saleor-platform/blob/ab6315bd59c58b4815175df4c679107ff9695be4/docker-compose.yml>.
Acesso em: 18 out. 2026.

YEGGE, S. **Stevey's Google platforms rant**. 12 out. 2011. Publicado originalmente no
Google+. Cópia disponível em: <https://gist.github.com/chitchcock/1281611>. Acesso em: 18
out. 2026.
