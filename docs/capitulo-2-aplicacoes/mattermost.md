# 2.3 Slack e Mattermost

O segundo par de sistemas estudados no livro pertence ao domínio das **mensagens em tempo real
para equipes**. O sistema real é o Slack, tendo o WhatsApp como contraponto; o gêmeo open source é
o Mattermost. Esta seção apresenta os três sistemas, justifica a escolha do par, indica a versão
do Mattermost adotada no livro e registra a ficha *kata* que orienta a descida pelas altitudes nos
Capítulos 3 a 6.

## 2.3.1 O sistema real: Slack

O Slack é uma plataforma comercial de mensagens para equipes, organizada em espaços de trabalho,
canais públicos e privados, mensagens diretas, *threads* e integrações. Sua origem é incomum. O
produto foi desenvolvido pela Tiny Speck, estúdio fundado por Stewart Butterfield e outros
ex-funcionários do Flickr, cujo primeiro projeto foi um jogo multijogador chamado Glitch. O jogo
foi encerrado em novembro de 2012, e a empresa passou a se dedicar ao Slack, que, em agosto de
2013, estava em teste com 45 grupos de trabalho e era usado pela própria Tiny Speck, então com 45
pessoas em dois escritórios e muitos trabalhadores remotos (LUNDEN, 2013). Em dezembro de 2020, a
Salesforce anunciou a compra da empresa, concluída em julho de 2021 (SALESFORCE, 2021).

Do ponto de vista arquitetural, o Slack interessa por duas razões. A primeira é que a equipe de
engenharia publicou uma descrição relativamente detalhada de seu sistema de mensagens em tempo
real, com servidores especializados para canais, conexões e presença, escritos em Java, e com
roteamento por *hashing* consistente (THANGUDU, 2023). A segunda é que a empresa publicou relatos
francos de incidentes, como o de 4 de janeiro de 2021, que mostram o comportamento do sistema sob
carga anormal (NOLAN, 2021). O código do Slack é fechado; por isso, neste livro, toda afirmação
sobre ele se apoia exclusivamente nessas publicações da própria empresa.

## 2.3.2 O contraponto: WhatsApp

O WhatsApp é um aplicativo de mensagens pessoais, e não uma ferramenta corporativa, mas serve de
contraponto útil por duas características. A primeira é a **criptografia de ponta a ponta**: o
conteúdo das mensagens é cifrado nos dispositivos com base no protocolo Signal, de modo que o
servidor encaminha mensagens que não consegue ler (WHATSAPP, 2016). A segunda é a ênfase na
**densidade de conexões**: em 2012, a equipe relatou ter levado um único servidor, com FreeBSD e
Erlang, a mais de dois milhões de conexões TCP simultâneas (WHATSAPP, 2012). Ambas as
características ajudam a compreender, por contraste, as escolhas do Mattermost em privacidade e em
custo de conexões persistentes. O WhatsApp aparece ainda no incidente de 4 de outubro de 2021,
quando uma falha na rede da Meta tornou indisponíveis Facebook, Instagram e WhatsApp (JANARDHAN,
2021), discutido na [Seção 4.3](../capitulo-4-50-mil-pes/mattermost.md).

## 2.3.3 O gêmeo open source: Mattermost

### Origem

O Mattermost nasceu, como o Slack, dentro de um estúdio de jogos. Em junho de 2015, Ian Tien
anunciou o projeto como uma alternativa ao Slack, de código aberto e instalável na infraestrutura
do próprio usuário, explicando que a equipe, uma pequena empresa de jogos apoiada pela Y
Combinator, havia construído o sistema para resolver seus próprios problemas e o usava
internamente desde o ano anterior; o código foi aberto porque muitos conhecidos queriam usá-lo por
razões diversas (TIEN, 2015). O mesmo texto expõe o argumento que acompanha o produto até hoje:
diferentemente do Slack, o Mattermost pode ser baixado, hospedado pela própria organização e
modificado, mantendo os dados em servidores próprios (TIEN, 2015). O repositório público atual,
`mattermost/mattermost`, foi criado em junho de 2015 (MATTERMOST, 2026a).

### Licença e modelo de negócio

O código-fonte do servidor é distribuído sob a licença GNU AGPL v3.0, enquanto os diretórios de
ferramentas de administração e de configuração, entre os quais `server/public/` e `webapp/`, são
distribuídos sob a licença Apache 2.0; os binários compilados pela empresa são licenciados sob a
licença MIT (MATTERMOST, 2026a, arquivo `LICENSE.txt`). Funcionalidades como o *cluster* de alta
disponibilidade exigem licença comercial (MATTERMOST, 2026b), caracterizando o modelo conhecido
como *open core*: um núcleo aberto, completo para uso em servidor único, e funcionalidades
corporativas pagas. A [Seção 5.3](../capitulo-5-10-mil-pes/mattermost.md) mostra que essa fronteira comercial coincide com uma fronteira de
código, a interface `ClusterInterface`.

### Público e posicionamento

A documentação oficial apresenta o Mattermost como plataforma de **colaboração soberana**: uma
resposta ao endurecimento das leis de proteção de dados, ao risco geopolítico e aos conflitos de
conformidade entre jurisdições, com implantações em nuvem privada, em nuvens soberanas e em redes
isoladas e desconectadas (MATTERMOST, 2026d). Esse posicionamento explica a presença do produto
em órgãos públicos, em organizações de defesa e em empresas de setores regulados, e explica também
por que a soberania de dados ocupa, no Mattermost, o lugar que a escalabilidade ocupa no Slack
([Seção 3.3](../capitulo-3-100-mil-pes/mattermost.md)).

### Tecnologias

O servidor é escrito em Go e entregue como um **binário único** que contém a API REST, o serviço
de WebSocket, o serviço de notificações e o gerenciamento de dados (MATTERMOST, 2026c). O cliente
web é escrito em React e TypeScript; há também clientes para *desktop* e para dispositivos
móveis. A persistência usa PostgreSQL, na versão 14 ou superior segundo a documentação atual
(MATTERMOST, 2026e). Os anexos são guardados em sistema de arquivos local, em armazenamento
compatível com S3 ou em NAS. A busca pode usar um índice embutido, o Bleve, cujo diretório
aparece no arquivo Docker Compose oficial (MATTERMOST, 2026j), ou, em implantações com mais de 2,5
milhões de mensagens, um mecanismo de busca corporativo recomendado pela documentação
(MATTERMOST, 2026b). As extensões de servidor
(*plugins*) são programas Go separados, executados como subprocessos e acessados por RPC por meio
da biblioteca `go-plugin` da HashiCorp (MATTERMOST, 2026g).

### Versão adotada no livro

Todos os trechos de código citados neste livro pertencem à *tag* **v11.11.1** do repositório
`mattermost/mattermost`, publicada em 24 de setembro de 2026, e foram conferidos nessa versão. A
*tag* v12.0.0 foi publicada em 14 de outubro de 2026, poucos dias antes da redação desta seção; a
opção por uma versão de correção da série anterior privilegia a estabilidade dos *links*
permanentes. O leitor que consultar o ramo principal encontrará arquivos renomeados e números de
linha diferentes, razão pela qual todos os *links* apontam para a *tag*.

### Como executar localmente

A documentação oficial oferece dois caminhos com Docker (MATTERMOST, 2026i). Para uma primeira
exploração, há uma imagem de demonstração autocontida, com banco de dados interno:

```sh
docker run --name mattermost-preview -d --publish 8065:8065 --publish 8443:8443 mattermost/mattermost-preview
```

Depois de baixada a imagem, a interface fica disponível em `http://localhost:8065/`. A
documentação adverte que esse modo não deve ser usado em produção: usa uma senha conhecida, não
guarda dados persistentes fora do *container* e não admite atualizações (MATTERMOST, 2026i).

Para uma implantação mais próxima da realidade, o repositório oficial `mattermost/docker` fornece
um arquivo Docker Compose com dois serviços, `postgres` e `mattermost`, e variações com e sem um
*proxy* NGINX (MATTERMOST, 2026j). O roteiro documentado é clonar o repositório, copiar
`env.example` para `.env` e ajustar ao menos a variável `DOMAIN`, criar os diretórios de volumes
(configuração, dados, *logs*, *plugins* e índices de busca) com o dono correto e subir os serviços:

```sh
git clone https://github.com/mattermost/docker
cd docker
cp env.example .env          # editar ao menos DOMAIN
mkdir -p ./volumes/app/mattermost/{config,data,logs,plugins,client/plugins,bleve-indexes}
sudo chown -R 2000:2000 ./volumes/app/mattermost
docker compose -f docker-compose.yml -f docker-compose.without-nginx.yml up -d
```

Fonte: Mattermost (2026i).

Os volumes declarados no arquivo Compose já revelam parte da arquitetura que será examinada no
Capítulo 4: configuração, dados (anexos), *logs*, *plugins* de servidor, *plugins* de cliente e
índices de busca são estados que o *container* da aplicação precisa preservar fora de si
(MATTERMOST, 2026j).

## 2.3.4 Por que este par e que dilema ele ilustra

O par Slack–Mattermost foi escolhido porque os dois sistemas resolvem o mesmo problema funcional,
a entrega de mensagens em tempo real a equipes, sob **restrições de contexto opostas**. O Slack é
um serviço multiorganização operado por uma única empresa, que pode especializar servidores,
escalar cada tipo de forma independente e absorver a complexidade operacional. O Mattermost é um
produto instalado por milhares de organizações diferentes, muitas com um único servidor, algumas em
redes sem acesso à internet. A mesma pergunta arquitetural, como levar um evento a milhares de
conexões abertas, recebe respostas diferentes porque as características priorizadas são
diferentes, ilustrando a primeira lei da arquitetura de software (ver [Seção 1.1.2](../capitulo-1-fundamentos/index.md)).

O dilema central que o par ilustra é o do **serviço com estado**. Para entregar mensagens com baixa
latência, o servidor mantém conexões WebSocket abertas por horas, o que o torna *stateful*: não
pode ser substituído livremente, exige mecanismos para saber onde cada usuário está conectado e
sofre com reconexões em massa. O Slack responde com especialização e roteamento; o Mattermost, com
um binário único e difusão de eventos entre todos os nós. Um segundo dilema é o da
**extensibilidade segura**: como admitir código de terceiros no processo que guarda as conversas
de uma organização. A resposta do Mattermost, *plugins* em processos separados, será reconstruída
como decisão arquitetural na [Seção 4.3](../capitulo-4-50-mil-pes/mattermost.md) e verificada no código na [Seção 6.3](../capitulo-6-solo/mattermost.md).

## 2.3.5 Ficha *kata*

- **Descrição:** mensageria em equipe, com canais públicos e privados, mensagens diretas e em
  grupo, *threads*, reações, anexos, busca, notificações e integrações.
- **Usuários:** equipes de empresas e de órgãos públicos; no Mattermost, são frequentes as
  implantações em infraestrutura própria (*self-hosted*), inclusive em redes isoladas
  (*air-gapped*), por exigência de soberania de dados.
- **Requisitos funcionais centrais:** enviar e receber mensagens em tempo real; manter o histórico
  completo; indicar presença (*online*, ausente, ocupado, *offline*); enviar notificações *push* a
  dispositivos móveis; buscar mensagens; integrar *bots*, *webhooks*, comandos de barra e
  *plugins*.
- **Contexto adicional:** muitas conexões abertas ao mesmo tempo; mensagens devem aparecer na
  mesma ordem para todos os membros de um canal; cada usuário pode estar conectado por vários
  dispositivos; a rede dos clientes móveis é instável; a organização usuária, e não o fornecedor,
  opera a infraestrutura.

**Tabela 2.3.1** – Resumo do par estudado.

| Item | Descrição |
|---|---|
| **Sistema real** | Slack (mensagens para equipes); WhatsApp como contraponto |
| **Gêmeo open source** | [Mattermost](https://github.com/mattermost/mattermost), *tag* v11.11.1 · [documentação](https://docs.mattermost.com) |
| **Tecnologias do gêmeo** | Go, React/TypeScript, PostgreSQL, WebSocket, *plugins* em subprocessos via RPC |
| **Licença** | AGPL v3.0 (núcleo), Apache 2.0 (`server/public/`, `webapp/`), *open core* |
| **Estilo dominante** | núcleo monolítico com difusão orientada a eventos e extensão por *plugins* |
| **Dilema ilustrado** | serviço *stateful* de conexões; extensibilidade segura; soberania × conveniência |

Fonte: elaborado pelo autor (2026).

## Referências

JANARDHAN, S. **More details about the October 4 outage**. Engineering at Meta, 5 out. 2021.
Disponível em: <https://engineering.fb.com/2021/10/05/networking-traffic/outage-details/>.
Acesso em: 18 out. 2026.

LUNDEN, I. **Slack, the newest enterprise social network, is the latest effort from Flickr
co-founder Stewart Butterfield**. TechCrunch, 14 ago. 2013. Disponível em:
<https://techcrunch.com/2013/08/14/say-hello-to-slack-the-newest-enterprise-social-network-and-the-latest-effort-from-flickr-co-founder-stewart-butterfields-tiny-speck>.
Acesso em: 18 out. 2026.

MATTERMOST. **mattermost/mattermost**: repositório de código-fonte, versão v11.11.1. GitHub,
2026a. Disponível em: <https://github.com/mattermost/mattermost/tree/v11.11.1>. Acesso em:
18 out. 2026.

MATTERMOST. **High availability cluster-based deployment**. Mattermost Documentation, 2026b.
Disponível em:
<https://docs.mattermost.com/administration-guide/scale/high-availability-cluster-based-deployment.html>.
Acesso em: 18 out. 2026.

MATTERMOST. **Mattermost Server**. Mattermost Documentation, 2026c. Disponível em:
<https://docs.mattermost.com/product-overview/server>. Acesso em: 18 out. 2026.

MATTERMOST. **Self-sovereign collaboration**. Mattermost Documentation, 2026d. Disponível em:
<https://docs.mattermost.com/use-case-guide/self-sovereign-collaboration.html>. Acesso em:
18 out. 2026.

MATTERMOST. **Software and hardware requirements**. Mattermost Documentation, 2026e.
Disponível em: <https://docs.mattermost.com/deployment-guide/software-hardware-requirements.html>.
Acesso em: 18 out. 2026.

MATTERMOST. **Server plugins**. Mattermost Developers, 2026g. Disponível em:
<https://developers.mattermost.com/integrate/plugins/components/server/>. Acesso em:
18 out. 2026.

MATTERMOST. **Install using Docker**. Mattermost Documentation, 2026i. Disponível em:
<https://docs.mattermost.com/deployment-guide/server/containers/install-docker.html>. Acesso em:
18 out. 2026.

MATTERMOST. **mattermost/docker**: the official Docker deployment solution for Mattermost.
GitHub, 2026j. Disponível em: <https://github.com/mattermost/docker>. Acesso em: 18 out. 2026.

NOLAN, L. **Slack's outage on January 4th 2021**. Slack Engineering, 1 fev. 2021. Disponível em:
<https://slack.engineering/slacks-outage-on-january-4th-2021/>. Acesso em: 18 out. 2026.

SALESFORCE. **Salesforce completes acquisition of Slack**. Salesforce News, 21 jul. 2021.
Disponível em: <https://www.salesforce.com/news/press-releases/2021/07/21/salesforce-slack-deal-close/>.
Acesso em: 18 out. 2026.

THANGUDU, S. **Real-time messaging**. Slack Engineering, 11 abr. 2023. Disponível em:
<https://slack.engineering/real-time-messaging/>. Acesso em: 18 out. 2026.

TIEN, I. **Open source, on-premises, Slack-alternative**. Mattermost Blog, 24 jun. 2015.
Disponível em: <https://mattermost.com/blog/on-premises-open-source-slack-alternative/>. Acesso
em: 18 out. 2026.

WHATSAPP. **1 million is so 2011**. WhatsApp Blog, 6 jan. 2012. Disponível em:
<https://blog.whatsapp.com/1-million-is-so-2011>. Acesso em: 18 out. 2026.

WHATSAPP. **WhatsApp encryption overview**: technical white paper. [*S. l.*]: WhatsApp, 2016.
Disponível em: <https://www.whatsapp.com/security/WhatsApp-Security-Whitepaper.pdf>. Acesso em:
18 out. 2026.
