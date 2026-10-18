# Glossário

Termos consolidados de todos os capítulos e apêndices. A indicação entre colchetes remete aos locais em que o termo é definido.

`Abordagem projetiva`
:   Construção das visões como projeções de um modelo único, em oposição à abordagem sintética, em que as visões são construídas separadamente e integradas por correspondência. [Ap. A]

`Abstração`
:   Proporção de classes abstratas e interfaces em um componente. [Ap. E]

`Acoplamento aferente`
:   Número de elementos externos que dependem de um módulo ($C_a$). [Ap. E]

`Acoplamento eferente`
:   Número de elementos externos dos quais um módulo depende ($C_e$). [Ap. E]

`Acoplamento temporal`
:   Tendência de dois ou mais arquivos mudarem juntos nos mesmos *commits*. [Ap. C]

`Active Record`
:   Objeto que envolve uma linha de tabela, encapsula o acesso ao banco e acrescenta lógica de domínio a esses dados. [Seção 6.6; Ap. E]

`ADR`
:   Registro de decisão arquitetural: documento curto que registra uma decisão, seu contexto e suas consequências. [Cap. 1; Ap. B]

`ADR reconstruído`
:   Registro inferido retrospectivamente do código e da documentação de um sistema, declarado como tal. [Ap. B]

`Agregado`
:   Grupo de entidades e objetos de valor tratado como unidade de consistência, acessado exclusivamente por sua raiz. [Cap. 1]

`Antipadrão`
:   Solução comumente adotada que produz consequências negativas. [Cap. 1]

`API pública (OCP)`
:   Espaço de nomes do Nextcloud que reúne as interfaces e classes que os *apps* podem usar, mantidas estáveis segundo uma política de depreciação. [Seção 6.5]

`Architecture sinkhole`
:   Antipadrão em que as requisições atravessam camadas que nada acrescentam. [Ap. D]

`Armadilha da entidade`
:   Antipadrão em que cada entidade de dados se torna um componente gerenciador, ignorando os fluxos de trabalho. [Ap. E]

`Arquitetura de sistemas`
:   Definição da estrutura de alto nível de um sistema composto por *hardware*, software, dados, pessoas e processos, e das interfaces entre esses elementos. [Cap. 1]

`Arquitetura de software`
:   Conjunto de estruturas fundamentais de um sistema em seu ambiente, das características de qualidade que elas asseguram, das decisões que as originaram e dos princípios que orientam sua evolução. [Cap. 1]

`Arquitetura evolutiva`
:   Arquitetura que suporta mudança guiada e incremental em múltiplas dimensões, protegida por *fitness functions*. [Cap. 7]

`Arquitetura hexagonal`
:   Arquitetura em que a aplicação se comunica com o exterior exclusivamente por portas que ela define, implementadas ou acionadas por adaptadores específicos de cada tecnologia. [Cap. 1]

`Arquitetura limpa`
:   Arquitetura em círculos concêntricos regida pela regra de que as dependências de código-fonte apontam sempre para dentro, em direção às regras de negócio. [Cap. 1]

`Arquitetura menos pior`
:   Critério que busca um conjunto aceitável de compromissos, e não um ótimo absoluto. [Ap. D]

`Árvore de utilidade`
:   Estrutura que refina a utilidade de um sistema em atributos, refinamentos e cenários, priorizados por importância e dificuldade. [Cap. 7]

`ASR`
:   Requisito arquiteturalmente significativo: qualquer requisito que influencie a arquitetura. [Ap. D]

`ATAM`
:   *Architecture Tradeoff Analysis Method*: método de avaliação baseado em cenários de qualidade que identifica riscos, pontos de sensibilidade e pontos de compromisso. [Cap. 7]

`Atributo de qualidade`
:   Propriedade mensurável de um sistema que indica o grau em que ele atende a necessidades de seus *stakeholders* além da funcionalidade. Também chamado de característica arquitetural. [Cap. 1]

`Avaliação de arquitetura`
:   Verificação, em geral pontual e com participação de terceiros, de que as soluções escolhidas satisfazem os requisitos arquiteturalmente significativos. [Cap. 7]

`Backpressure`
:   Contrapressão: mecanismo pelo qual um estágio sobrecarregado faz a sobrecarga se manifestar como atraso ou rejeição no estágio anterior, em vez de aceitar trabalho indefinidamente. [Seção 6.7]

`Balanceador de carga`
:   Componente que distribui requisições entre várias instâncias de um serviço. [Cap. 4]

`Balde de fichas`
:   Algoritmo de limitação de taxa em que requisições consomem fichas de um recipiente de capacidade fixa reabastecido a taxa constante; em inglês, *token bucket*. [Ap. F]

`Base 62`
:   Sistema de numeração posicional com os 62 símbolos [0-9a-zA-Z], usado para representar identificadores numéricos em poucos caracteres seguros para URLs. [Ap. F]

`Bloqueio semântico`
:   Estado de um objeto que indica, além da transação de banco, que ele está em uso por uma saga em andamento. [Seção 6.6]

`Cache-aside`
:   Estratégia em que a aplicação consulta o *cache* e, na falta, lê o dado da origem e o grava no *cache*; também chamada de *look-aside*. [Ap. F]

`Cadeia de regras`
:   No ThingsBoard, grafo de nós de regra ligados por relações rotuladas que processa as mensagens de um inquilino. [Seção 6.7]

`Camada (layer)`
:   Divisão lógica de responsabilidades no código de um sistema. [Cap. 4]

`Característica arquitetural`
:   Propriedade que especifica uma consideração de projeto não pertencente ao domínio, influencia algum aspecto estrutural e é crítica para o sucesso. [Ap. D]

`Característica explícita`
:   Característica arquitetural declarada nos requisitos ou pelo cliente. [Cap. 3]

`Característica implícita`
:   Característica arquitetural não declarada, mas exigida pelo domínio, como segurança em um sistema financeiro. [Cap. 3; Ap. D]

`Cartão CRC`
:   Cartão que registra uma classe, suas responsabilidades e seus colaboradores. [Ap. C]

`CDN`
:   *Content delivery network*: rede de servidores geograficamente distribuídos que entrega conteúdo estático a partir do ponto mais próximo do usuário. [Ap. F]

`Cenário de qualidade`
:   Especificação mensurável de um atributo de qualidade em seis partes: fonte, estímulo, ambiente, artefato, resposta e medida da resposta. [Cap. 1]

`Chave de idempotência`
:   Identificador de uma intenção de operação, enviado pelo cliente, que permite ao servidor reconhecer e deduplicar repetições. [Seção 6.6]

`Código legado`
:   Código que continua em uso por atender a necessidades reais; na definição de Feathers, código sem testes suficientes para ser modificado com confiança. [Ap. C]

`Coesão`
:   Medida da força de associação entre os elementos de um módulo. [Ap. E]

`Componente`
:   Agrupamento de funcionalidade com responsabilidade definida e interface explícita, materializado em geral como pacote, módulo ou *namespace*. [Cap. 5; Ap. E]

`Componente (C4)`
:   Agrupamento de funcionalidades relacionadas, com interface bem definida, que executa no mesmo processo que os demais componentes do seu *container*. [Ap. A]

`Comunicação assíncrona`
:   Forma de comunicação em que o emissor não aguarda a conclusão do processamento pelo receptor. [Cap. 5]

`Conformidade`
:   Seção do ADR que indica como a decisão será verificada, de forma manual ou por função de aptidão. [Ap. B]

`Connascence`
:   Relação entre dois elementos tal que uma mudança em um exige mudar o outro para manter a correção do sistema. [Ap. E]

`Consistência eventual`
:   Garantia de que dados processados por unidades separadas ficarão consistentes em algum momento não especificado. [Ap. D]

`Consistência representacional`
:   Prática de mostrar sempre a relação entre a parte detalhada e o todo antes de mudar de visão. [Ap. A]

`Container (C4)`
:   Unidade que executa código ou armazena dados separadamente das demais, como uma aplicação web, um processo de trabalho ou um banco de dados. Não se confunde com contêiner Docker. [Cap. 4; Ap. A]

`Contexto delimitado`
:   Fronteira explícita dentro da qual um modelo de domínio e sua linguagem são definidos e aplicados de maneira consistente. [Cap. 1]

`Contrapressão (backpressure)`
:   Mecanismo pelo qual um componente sobrecarregado limita o ritmo dos produtores ou descarta consumidores lentos para proteger o restante do sistema. [Seção 6.3]

`Cópia em conflito`
:   Arquivo criado para preservar uma das versões de um arquivo modificado concorrentemente em dois lugares, de modo que nenhuma edição seja perdida. [Seção 6.5]

`Costura (seam)`
:   Lugar em que se pode alterar o comportamento do programa sem editar naquele lugar. [Ap. C]

`Data Mapper`
:   Camada que transfere dados entre objetos em memória e o banco, mantendo-os independentes. [Seção 6.6; Ap. E]

`Data Transfer Object`
:   Objeto sem comportamento que transporta dados entre processos para reduzir o número de chamadas. [Ap. E]

`Decisão arquiteturalmente significativa`
:   Decisão que afeta a estrutura, as características não funcionais, as dependências, as interfaces ou as técnicas de construção de um sistema. [Ap. B; Ap. D]

`Declaração em Y`
:   Forma condensada de registrar uma decisão numa frase com contexto, preocupação, opção escolhida, opções descartadas, qualidade buscada e desvantagem aceita. [Ap. B]

`Determinismo temporal`
:   Propriedade de responder, para as mesmas entradas e estado, dentro de limites de tempo conhecidos e verificáveis. [Seção 6.8]

`Diagrama de contexto`
:   Diagrama do nível 1 do modelo C4, que representa o sistema como uma única caixa, cercado por seus usuários e pelos sistemas externos com que se comunica. [Cap. 3]

`Diagrama de sequência`
:   Diagrama que mostra a ordem das interações entre participantes numa operação. [Cap. 5]

`Dívida técnica`
:   Custo futuro de retrabalho decorrente de escolhas que aceleraram o desenvolvimento no presente. [Cap. 1]

`Dívida técnica arquitetural`
:   Custo futuro gerado por decisões estruturais convenientes no curto prazo, que tornam mudanças posteriores mais caras ou impossíveis. [Cap. 7]

`Domain Model`
:   Padrão em que a lógica de domínio é organizada numa rede de objetos com dados e comportamento. [Cap. 6; Ap. E]

`Elasticidade`
:   Capacidade de suportar rajadas súbitas de requisições, alocando e liberando recursos. [Ap. D; Ap. F]

`Erosão arquitetural`
:   Divergência progressiva entre a arquitetura pretendida e a arquitetura implementada. [Cap. 6]

`Escalabilidade`
:   Capacidade de atender a um número crescente de usuários ou requisições sem degradação séria de desempenho. [Ap. D]

`Estilo arquitetural`
:   Família de sistemas definida por um vocabulário de tipos de componentes e conectores e por restrições sobre sua combinação. [Cap. 1]

`Estimativa de guardanapo`
:   Cálculo aproximado, baseado em hipóteses explícitas e números de desempenho conhecidos, que determina a ordem de grandeza de vazão, armazenamento, memória ou disponibilidade exigidos por um sistema. [Ap. F]

`Estimativa de ordem de grandeza`
:   Cálculo aproximado, feito com premissas explícitas e números arredondados, para verificar se uma proposta de projeto é compatível com a escala esperada. Também chamada de estimativa de guardanapo. [Cap. 3]

`Estudo de caso`
:   Método de ensino e de pesquisa que examina em profundidade um sistema real em seu contexto. [Cap. 2]

`Evaporação do conhecimento arquitetural`
:   Perda das razões, alternativas e premissas de uma decisão quando apenas suas estruturas resultantes são documentadas. [Ap. B]

`Evento de domínio`
:   Registro imutável de algo significativo que aconteceu no domínio, nomeado no passado. [Ap. E]

`Excesso de engenharia`
:   Introdução de complexidade não justificada pelos requisitos e pela escala esperada. [Cap. 3]

`Failsafe`
:   Comportamento seguro acionado automaticamente quando uma falha ou condição perigosa é detectada. [Seção 6.8]

`Falácias da computação distribuída`
:   Oito suposições falsas comumente feitas no projeto de sistemas distribuídos, como a de que a rede é confiável ou de que a latência é zero. [Cap. 4]

`Fanout`
:   Distribuição de uma publicação para as *timelines* de todos os seguidores do autor; no modelo *push* (*fanout on write*), feita na escrita; no modelo *pull* (*fanout on read*), na leitura. [Seção 6.2]

`Federação`
:   Arquitetura em que servidores independentes, com administração própria, interoperam por um protocolo comum, como o ActivityPub, sem um centro único. [Seção 6.2]

`Fila de trabalho`
:   No PX4, *thread* com prioridade própria que executa cooperativamente vários módulos, agendados por tempo ou pela chegada de mensagens. [Seção 6.8]

`Fila morta (dead queue)`
:   No Mattermost, *buffer* circular com os últimos eventos enviados a uma conexão, usado para reenviá-los quando o cliente se reconecta. [Seção 6.3]

`Fitness function arquitetural`
:   Qualquer mecanismo que forneça uma avaliação objetiva da integridade de uma característica arquitetural ou de uma combinação delas. Também chamada de função de aptidão. [Cap. 7]

`Função de aptidão`
:   Mecanismo que fornece uma avaliação objetiva de uma característica arquitetural, tipicamente um teste automatizado. [Cap. 1; Ap. E]

`Gêmeo de código aberto`
:   Sistema de código aberto de propósito e arquitetura comparáveis aos de um sistema real de código fechado, usado neste livro para estender a análise até o código. [Cap. 2]

`Hashing consistente`
:   Técnica de distribuição de chaves entre nós que, ao acrescentar ou remover um nó, move apenas uma pequena fração das chaves. [Ap. F]

`Hidratação`
:   Montagem da resposta completa a partir de uma lista de identificadores, buscando o conteúdo de cada item em caches ou no banco. [Seção 6.2]

`Hook`
:   Ponto de extensão no qual o sistema invoca código de uma extensão quando ocorre um evento; no Mattermost, *hooks* "WillBe" podem vetar a ação, e *hooks* "HasBeen" apenas reagem a ela. [Seção 6.3]

`Hotspot`
:   Arquivo que combina alta frequência de mudança e alta complexidade. [Ap. C]

`HTTP 429`
:   Código de status *Too Many Requests*, que indica que o cliente excedeu o limite de requisições; pode vir acompanhado do cabeçalho `Retry-After`. [Ap. F]

`Hub de WebSocket`
:   No Mattermost, componente que registra as conexões WebSocket de um subconjunto de usuários e lhes entrega os eventos publicados, filtrando-os por usuário, canal ou conexão. [Seção 6.3]

`Idempotência`
:   Propriedade de uma operação cujo efeito é o mesmo quer seja executada uma ou várias vezes. [Seção 6.3; Seção 6.4]

`Identity Map`
:   Padrão que garante que cada registro seja carregado em um único objeto por transação. [Ap. E]

`Instabilidade`
:   Razão $C_e / (C_e + C_a)$; indica a suscetibilidade de um componente a mudanças provocadas por outros. [Ap. E]

`Instância`
:   Servidor Mastodon independente, com seus próprios usuários, administradores, políticas e infraestrutura, que participa da rede federada. [Seção 6.2]

`Inversão de dependência`
:   Princípio segundo o qual módulos de alto nível e de baixo nível devem depender de abstrações, e as abstrações não devem depender de detalhes. [Cap. 1]

`Isomorfismo domínio/arquitetura`
:   Coincidência entre a forma de um problema e a topologia de um estilo arquitetural. [Cap. 7]

`Jitter`
:   Componente aleatório acrescentado a um intervalo de espera para dessincronizar clientes que falharam ao mesmo tempo. [Seção 6.7]

`Kata de arquitetura`
:   Exercício de projeto arquitetural a partir de uma descrição curta, de usuários, de requisitos e de contexto. [Cap. 2]

`Known Uses`
:   Seção da descrição de um padrão que registra seus usos em sistemas reais. [Cap. 6]

`Lazy Load`
:   Padrão em que um objeto não contém todos os dados de que precisa, mas sabe como obtê-los quando necessário. [Ap. E]

`LCOM`
:   Falta de coesão em métodos: métrica que mede o quanto os métodos de uma classe deixam de compartilhar atributos. [Ap. E]

`Lei de Conway`
:   Observação de que as organizações tendem a produzir sistemas que copiam suas estruturas de comunicação. [Ap. E]

`Limitador de taxa`
:   Componente que limita o número de requisições que um cliente pode fazer em um intervalo de tempo; em inglês, *rate limiter*. [Ap. F]

`Linguagem ubíqua`
:   Linguagem comum, derivada do modelo de domínio, usada por desenvolvedores e especialistas do negócio e refletida no código. [Cap. 1]

`Locking transacional`
:   Mecanismo que impede gravações simultâneas sobre o mesmo arquivo por meio de *locks* compartilhados e exclusivos, independentemente do armazenamento. [Seção 6.5]

`Manifesto`
:   Arquivo que descreve as variantes e os segmentos de um vídeo para o reprodutor, como a *playlist* mestre do HLS ou o MPD do DASH. [Seção 6.4]

`Manobra inversa de Conway`
:   Organização deliberada das equipes de acordo com a arquitetura que se deseja obter. [Cap. 5; Cap. 7]

`Máquina de estados tabular`
:   Implementação de máquina de estados em que transições e propriedades dos estados são representadas como dados, em vez de classes com comportamento. [Seção 6.8]

`Métricas DORA`
:   Métricas de desempenho de entrega de software, como tempo de espera para mudanças, frequência de implantação, tempo de recuperação e taxa de falhas de mudanças. [Cap. 7]

`Modelo anêmico`
:   Modelo de domínio cujos objetos contêm dados, mas pouco ou nenhum comportamento, ficando as regras em serviços externos a eles. [Cap. 6]

`Modelo C4`
:   Modelo hierárquico de descrição de arquiteturas, proposto por Simon Brown, com os níveis de contexto, *containers*, componentes e código. [Ap. A]

`Modelo de atores`
:   Modelo de concorrência em que entidades isoladas, os atores, comunicam-se exclusivamente por mensagens assíncronas e processam uma mensagem por vez. [Seção 6.7]

`Modularidade`
:   Agrupamento lógico de código relacionado, independente de separação física. [Ap. E]

`Multi-tenant`
:   Multi-inquilino: uma única instalação atende vários clientes isolados entre si. [Seção 6.7]

`Nível (tier)`
:   Separação física de partes de um sistema em processos ou máquinas distintos. [Cap. 4]

`Ocultação de informação`
:   Princípio segundo o qual cada módulo deve esconder uma decisão de projeto sujeita a mudança, revelando em sua interface apenas o necessário. [Cap. 1]

`Padrão`
:   Descrição nomeada de uma solução comprovada para um problema recorrente em um contexto, com suas forças e consequências. [Cap. 1]

`Paisagem de sistemas`
:   Diagrama suplementar do C4 que mostra vários sistemas de software e seus usuários, sem foco em um sistema particular. [Ap. A]

`Particionamento por domínio`
:   Organização dos componentes de primeiro nível segundo áreas do negócio. [Cap. 5; Ap. D; Ap. E]

`Particionamento técnico`
:   Organização dos componentes de primeiro nível segundo responsabilidades técnicas, como apresentação, negócio e persistência. [Cap. 5]

`Permalink`
:   Endereço permanente de um trecho de código numa versão fixa de um repositório. [Cap. 6]

`Pipes and filters`
:   Estilo em que dados fluem por uma sequência de filtros independentes conectados por canais unidirecionais. [Seção 6.4]

`Ponto de compromisso`
:   Ponto de sensibilidade de mais de um atributo, em que melhorar um piora outro. [Cap. 7]

`Ponto de habilitação`
:   Lugar em que se decide qual comportamento uma costura usará. [Ap. C]

`Ponto de mudança`
:   Lugar do código em que uma alteração pretendida precisará ser feita. [Ap. C]

`Ponto de sensibilidade`
:   Propriedade de um ou mais componentes crítica para que um atributo de qualidade seja alcançado. [Cap. 7]

`Ponto de vista (viewpoint)`
:   Convenções para construir, interpretar e analisar um tipo de visão. [Cap. 1]

`Ponto único de falha (SPOF)`
:   Componente cuja falha interrompe o sistema inteiro. [Ap. F]

`Problema da celebridade`
:   Sobrecarga de um *shard* causado pela concentração de acessos a poucas chaves muito populares; também chamado de chave quente (*hotspot key*). [Ap. F]

`Problema N+1`
:   Execução de uma consulta para uma coleção e de mais uma consulta para cada elemento ao percorrer uma associação. [Ap. E]

`Propagação de ETag`
:   Atribuição de uma nova *ETag* a todas as pastas ancestrais de um arquivo alterado, o que permite detectar mudanças em toda a árvore comparando apenas a *ETag* da raiz. [Seção 6.5]

`QoS`
:   Nível de qualidade de serviço de entrega do MQTT: 0 (no máximo uma vez), 1 (pelo menos uma vez) ou 2 (exatamente uma vez). [Seção 6.7]

`QPS`
:   *Queries per second*: número de requisições por segundo que o sistema precisa atender, em média ou no pico. [Ap. F]

`Quantum arquitetural`
:   Artefato implantável de forma independente, com alta coesão funcional e acoplamento síncrono entre suas partes; unidade na qual as características arquiteturais são definidas. [Cap. 4; Cap. 7; Ap. D]

`Radar tecnológico`
:   Instrumento que posiciona tecnologias em quadrantes e anéis (evitar, avaliar, experimentar e adotar) para orientar decisões de investimento em aprendizado. [Cap. 7]

`Raiz de agregado`
:   Entidade que é o único ponto de acesso externo a um agregado e que garante suas invariantes. [Ap. E]

`Raiz de composição`
:   Único ponto do sistema que conhece as classes concretas de todas as camadas e as conecta. [Ap. E]

`Réplica de leitura`
:   Cópia de um banco de dados, mantida sincronizada com o primário, usada para atender consultas. [Cap. 4]

`Replicação primário-réplica`
:   Configuração em que um banco primário aceita escritas e as propaga a réplicas que atendem leituras. [Ap. F]

`Restrição`
:   Condição imposta pelo ambiente, e não escolhida pela equipe, que limita as soluções possíveis, como uma lei, um protocolo obrigatório ou um orçamento. [Cap. 3]

`Revisão ativa de projeto`
:   Revisão em que cada revisor executa uma tarefa específica que o obriga a usar a documentação. [Cap. 7]

`Risk storming`
:   Exercício colaborativo de identificação individual, consenso e mitigação de riscos arquiteturais sobre um diagrama. [Cap. 7; Ap. D]

`Runner`
:   No PeerTube, programa externo que pede *jobs* pesados à instância, executa-os e devolve o resultado. [Seção 6.4]

`SAAM`
:   *Software Architecture Analysis Method*: método que avalia uma arquitetura pela forma como ela acomoda cenários, classificados em diretos e indiretos. [Cap. 7]

`Sequência principal`
:   Reta $A + I = 1$ no plano abstração × instabilidade, sobre a qual estariam os componentes equilibrados. [Ap. E]

`Service Object`
:   Classe sem estado próprio no banco que encapsula uma operação de negócio envolvendo vários modelos, em geral exposta por um único método público. [Seção 6.2]

`Servidor de tickets`
:   Serviço central que emite identificadores numéricos únicos e densos por autoincremento. [Ap. F]

`Sharding`
:   Particionamento horizontal dos dados entre vários bancos com o mesmo esquema, segundo uma chave de particionamento. [Ap. F]

`Shared-nothing`
:   Arquitetura em que os servidores de uma camada não compartilham estado nem se comunicam entre si, o que permite acrescentá-los ou removê-los independentemente. [Ap. F]

`SLI, SLO e SLA`
:   Indicador, objetivo e acordo de nível de serviço: a medida, o valor-alvo e o contrato com consequências se o alvo não for atingido. [Ap. D]

`Snowflake`
:   Formato de identificador de 64 *bits* composto por carimbo de tempo, identificador de *data center*, identificador de máquina e número de sequência, criado pelo Twitter. [Ap. F]

`Stakeholder`
:   Indivíduo, grupo ou organização com interesse em um sistema. [Cap. 1]

`Stateless`
:   Componente que não guarda estado de sessão entre requisições e, por isso, pode ser replicado e substituído livremente. [Ap. F]

`Subida de verificação`
:   Procedimento que liga um trecho de código ao componente, à decisão e à característica arquitetural que o justificam. [Cap. 6; Ap. F]

`Substituição`
:   Mudança de *status* de um ADR aceito, que deixa de valer em favor de outro ADR, preservando-se ambos os registros. [Ap. B]

`Tag`
:   Marcador que identifica uma versão específica de um repositório Git. [Cap. 2]

`Tática`
:   Decisão de projeto elementar que influencia a resposta a um estímulo relativo a um único atributo de qualidade. [Ap. D]

`Tempo real crítico`
:   Requisito de uma função cujo prazo, se perdido, pode causar dano grave, independentemente de o resultado calculado estar correto. [Seção 6.8]

`Teorema CAP`
:   Resultado segundo o qual um sistema de dados distribuído, diante de uma partição de rede, não pode garantir simultaneamente consistência (linearizabilidade) e disponibilidade. [Ap. F]

`Teste de caracterização`
:   Teste escrito depois do fato para capturar o comportamento atual do código, inclusive defeitos, como verdade de referência. [Ap. C]

`Top-3`
:   As três características mais importantes de um sistema ou quantum, escolhidas pelos *stakeholders* em qualquer ordem. [Ap. D]

`Topologia broker`
:   Topologia orientada a eventos sem mediador, em que processadores reagem a eventos publicados por outros. [Ap. D]

`Topologia mediador`
:   Topologia orientada a eventos em que um mediador envia comandos aos processadores segundo um fluxo conhecido. [Ap. D]

`Transação pivô`
:   Etapa de uma saga a partir da qual a saga não é mais desfeita, apenas concluída. [Seção 6.6]

`Transaction Script`
:   Padrão que organiza a lógica de negócio como um procedimento por transação. [Ap. E]

`Transcodificação`
:   Conversão de um vídeo para outros formatos, resoluções e taxas de bits. [Seção 6.4]

`Último momento responsável`
:   Momento em que há informação suficiente para justificar uma decisão, antes que o adiamento bloqueie as equipes. [Ap. D]

`Unit of Work`
:   Padrão que mantém a lista de objetos afetados por uma transação de negócio e coordena a gravação das mudanças. [Ap. E]

`uORB`
:   *Middleware* de *publish/subscribe* do PX4, baseado em memória compartilhada, com tópicos tipados gerados a partir de arquivos `.msg`. [Seção 6.8]

`Upload retomável`
:   Envio de arquivo em partes, em que o cliente pode consultar o que já foi recebido e continuar depois de uma interrupção. [Seção 6.4]

`Visão (view)`
:   Parte de uma descrição de arquitetura que trata de um conjunto de interesses de *stakeholders*. [Cap. 1]

`Wrapper de armazenamento`
:   No Nextcloud, objeto que implementa o contrato de armazenamento envolvendo outro armazenamento e acrescentando uma responsabilidade, como cota ou criptografia; realização do padrão *Decorator*. [Seção 6.5]

`Zona de dor`
:   Região de componentes concretos e estáveis, rígidos e caros de mudar quando voláteis. [Ap. E]

`Zona de inutilidade`
:   Região de componentes abstratos dos quais ninguém depende. [Ap. E]
