# 2.8 DJI e PX4 Autopilot

A sétima aplicação estudada neste livro é a única cujo software literalmente voa. Ao longo dos
capítulos, a metáfora das altitudes serviu para organizar a análise de sistemas de informação, do
contexto de negócio ao código (Seção 1.11). Nesta aplicação, a palavra "altitude" designa também
uma variável que o próprio sistema estima a cada poucos milissegundos, e a pergunta "que
característica arquitetural esta decisão protege?" ganha uma gravidade diferente: uma decisão
equivocada pode derrubar uma aeronave sobre pessoas. O sistema real é representado pelos drones
comerciais da DJI e, de modo mais geral, pelos pilotos automáticos fechados da indústria. O gêmeo
de código aberto é o PX4 Autopilot.

## 2.8.1 O sistema real: drones comerciais e pilotos automáticos fechados

A DJI é a fabricante mais conhecida de drones civis. Segundo a própria empresa, ela cresceu de um
único escritório pequeno, em 2006, para uma força de trabalho global, tem sede em Shenzhen, na
China, e é uma empresa de capital fechado, com escritórios nos Estados Unidos, na Europa e na Ásia
(DJI, 2026a). Seus produtos integram verticalmente *hardware*, *firmware* de voo, aplicativos de
controle e serviços em nuvem.

O software de voo da DJI é fechado, e não há documentação pública suficiente para descrevê-lo
internamente com rigor. Por essa razão, este livro não especula sobre sua estrutura interna. O que
a empresa publica, entretanto, permite reconstruir a fronteira do sistema. A DJI oferece kits de
desenvolvimento (*Software Development Kits*, SDKs), entre eles o *Mobile SDK*, para aplicativos que
controlam a aeronave a partir de um dispositivo móvel, e o *Payload SDK*, para cargas úteis
acopladas ao drone (DJI, 2026b). Mantém também o sistema **GEO** (*Geospatial Environment
Online*), que informa aos pilotos as áreas em que o voo pode ser restrito por razões de segurança
ou de regulação e que substituiu, segundo a empresa, a primeira geração de zonas de exclusão de voo,
implantada em 2013 (DJI, 2026c).

Esses dois elementos revelam um modelo arquitetural característico de produtos fechados. O
fabricante controla o núcleo de voo e expõe apenas interfaces publicadas para extensão; terceiros
constroem aplicativos e cargas úteis, mas não substituem o estimador de estado nem os
controladores. Parte da segurança operacional, como as zonas geográficas, depende de dados
externos, mantidos em serviços do fabricante. O mesmo padrão aparece em pilotos automáticos de
aeronaves tripuladas, nos quais o software é desenvolvido sob normas de certificação como a
DO-178C e raramente é visível fora do fabricante (RTCA, 2011).

## 2.8.2 O gêmeo open source: PX4 Autopilot

O **PX4 Autopilot** é um piloto automático de código aberto para drones e outros veículos não
tripulados. Uma única base de código atende a multirrotores, aviões, VTOLs (veículos de decolagem
e pouso vertical), veículos terrestres, barcos e submarinos (PX4, 2026a). O projeto tem origem
acadêmica: foi apresentado por Meier, Honegger e Pollefeys (2015), da ETH Zurique, como um
*framework* robótico baseado em nós e multitarefa para plataformas profundamente embarcadas, e o
aviso de direitos autorais do repositório registra o período a partir de 2012 (PX4, 2026b).
Atualmente, o projeto é mantido sob a governança do **Dronecode**, hospedado pela Linux Foundation
(PX4, 2026c).

O código é distribuído sob a licença BSD de três cláusulas, permissiva, que permite uso e
modificação, inclusive em produtos comerciais, e o fornece "no estado em que se encontra", sem
garantias; a documentação é licenciada sob CC BY 4.0 (PX4, 2026c). A escolha da licença tem
consequência arquitetural e de negócio: fabricantes podem construir produtos proprietários sobre o
PX4 sem publicar suas modificações, o que favorece a adoção industrial, mas transfere a eles a
responsabilidade pela garantia de segurança exigida pela regulamentação.

O PX4 faz parte de uma plataforma mais ampla, que inclui a estação em solo **QGroundControl**, o
padrão de *hardware* aberto **Pixhawk** e a biblioteca **MAVSDK**, todos integrados pelo protocolo
**MAVLink** (PX4, 2026d). O QGroundControl é usado para planejar missões, acompanhar a telemetria,
configurar o veículo e instalar o *firmware* no controlador de voo (PX4, 2026d).

As tecnologias do gêmeo estão resumidas na Tabela 2.8.1.

**Tabela 2.8.1** – Ficha técnica do PX4 Autopilot na versão adotada.

| Item | Valor |
|---|---|
| Linguagem principal | C++ (com partes em C e geradores em Python) |
| Sistema operacional embarcado | NuttX, sistema operacional de tempo real; também Linux, macOS e QuRT |
| Comunicação interna | uORB, *publish/subscribe* em memória compartilhada |
| Comunicação externa | MAVLink (estação em solo, SDKs) e uXRCE-DDS (ponte para ROS 2) |
| Simulação | *Software in the loop* (SITL) com Gazebo e outros simuladores |
| Licença | BSD de três cláusulas |
| Governança | Dronecode, Linux Foundation |
| Versão adotada no livro | *tag* `v1.17.0`, versão estável publicada em maio de 2026 |

Fonte: elaborado pelo autor (2026), com base em PX4 (2026a, 2026c, 2026e).

### 2.8.2.1 Versão adotada e mudanças relevantes entre versões

A estrutura do repositório mudou de forma significativa entre versões, e por isso o livro fixa a
*tag* `v1.17.0`. Até a `v1.13`, as definições de mensagens usavam nomes em *snake_case*, como
`msg/vehicle_attitude.msg`; a partir da `v1.14`, passaram a usar *CamelCase*, como
`msg/VehicleAttitude.msg`. A `v1.16` introduziu o versionamento opcional das mensagens expostas ao
ROS 2, que passaram ao diretório `msg/versioned/` (PX4, 2026f). Na `v1.13`, a distribuição de
comandos aos motores era feita por arquivos de *mixer*; a partir da `v1.14`, foi substituída pelo
módulo de alocação de controle (PX4, 2026g). Todos os caminhos citados nos Capítulos 3 a 6 foram
conferidos na *tag* `v1.17.0`.

### 2.8.2.2 Como executar localmente

Diferentemente das aplicações web estudadas neste livro, o PX4 não oferece um arquivo Docker
Compose oficial para "subir" o sistema. A forma recomendada de experimentá-lo sem *hardware* é a
**simulação SITL** (*software in the loop*), na qual a pilha de voo executa em um computador comum
e controla um veículo modelado em um simulador de física. A documentação a descreve como uma forma
rápida, fácil e, sobretudo, segura de testar mudanças no código antes de voar no mundo real
(PX4, 2026h). Os passos básicos, em Linux, são os da Listagem 2.8.1.

**Listagem 2.8.1** – Comandos (shell, PX4): obtenção do código e execução de um quadricóptero simulado no Gazebo.

```sh
git clone https://github.com/PX4/PX4-Autopilot.git --recursive
cd PX4-Autopilot
git checkout v1.17.0 && git submodule update --init --recursive
make px4_sitl gz_x500
```

Fonte: adaptado de PX4 (2026e, 2026i).

O comando `make px4_sitl gz_x500` compila o PX4 para o alvo de simulação e executa a instância
SITL junto com o cliente do Gazebo, com um quadricóptero de referência (PX4, 2026i). Em seguida,
o QGroundControl, executado na mesma máquina, conecta-se automaticamente ao veículo simulado. Para
quem prefere não instalar as dependências no sistema, o projeto mantém imagens Docker com o
ambiente completo de desenvolvimento, publicadas no Docker Hub sob o prefixo `px4io/`, que
incluem as ferramentas para NuttX, para simulação e para ROS (PX4, 2026j). Essas imagens são
ambientes de compilação, e não uma implantação do sistema; o "produto" do PX4 é uma imagem de
*firmware* gravada no controlador de voo.

## 2.8.3 Por que este par e que dilema ele ilustra

Os seis pares anteriores são sistemas de informação. Neles, as características dominantes eram
escalabilidade, disponibilidade, elasticidade e manutenibilidade, e o pior desfecho de uma falha
era, em geral, a indisponibilidade ou a perda de dados. O par DJI e PX4 introduz um tipo
diferente de sistema: um **sistema ciberfísico embarcado de tempo real crítico**, no qual o tempo é
parte da correção. Um comando de motor correto entregue tarde é um comando errado. A propriedade
mais importante deixa de ser a vazão média e passa a ser o **pior caso**, o que altera o que se
considera boa arquitetura. Estilos distribuídos, nos quais a latência é variável por natureza,
cedem lugar a uma arquitetura **unitária**, executada em um único processador, que Richards e
Ford (2020, p. 121) observam ser rara fora dos sistemas embarcados e de outros ambientes altamente
restritos.

O dilema arquitetural que o par ilustra pode ser formulado como **flexibilidade contra
determinismo e segurança**. Pesquisadores e integradores querem trocar o estimador, acrescentar
modos de voo e conectar computadores de missão; pessoas em solo e reguladores querem que a
aeronave se comporte de modo previsível e seguro. A DJI resolve o dilema fechando o núcleo e
oferecendo extensões controladas. O PX4 precisa resolvê-lo de outro modo, pois seu núcleo é aberto
e compartilhado por muitos fabricantes. Os capítulos seguintes mostram como: módulos
substituíveis que se comunicam exclusivamente por tópicos tipados, prioridades de execução
atribuídas por criticidade, uma partição entre o controlador de voo e o computador de missão, e uma
máquina de estados de *failsafe* que nenhuma extensão pode contornar.

O par também permite discutir dois acidentes célebres, o do Ariane 5 em 1996 e os do Boeing 737
MAX em 2018 e 2019, à luz de decisões arquiteturais sobre reúso, redundância e criticidade. Esses
casos são examinados nas Seções 4.8 e 5.8.

## 2.8.4 Ficha *kata*

- **Descrição:** software de voo de drones e de outros veículos não tripulados. Estima o estado do
  veículo, estabiliza-o, navega, executa missões, comunica-se com a estação em solo e reage a
  falhas.
- **Usuários:** fabricantes de drones, integradores, pesquisadores e empresas de inspeção,
  mapeamento, agricultura e entregas. Indiretamente, operadores (pilotos remotos) e pessoas em
  solo.
- **Requisitos:** ler sensores (unidade inercial, GNSS, barômetro, magnetômetro); estimar atitude,
  velocidade e posição; controlar motores e servos; executar missões por pontos de passagem; trocar
  telemetria e comandos com a estação em solo; retornar ou pousar em caso de falha.
- **Contexto adicional:** *hardware* com memória e processamento limitados; prazos rígidos nos
  laços de controle; múltiplos tipos de veículo a partir de uma mesma base de código;
  regulamentação aeronáutica, que no Brasil envolve a ANAC, pelo RBAC-E nº 94 (ANAC, 2017), e o
  DECEA, quanto ao uso do espaço aéreo.

A Seção 3.8 retoma essa ficha em forma de conversa com o cliente e a converte em requisitos,
estimativas e cenários de qualidade ([Seção 3.8](../capitulo-3-100-mil-pes/px4.md)).

## Referências

ANAC. **RBAC-E nº 94**: requisitos gerais para aeronaves não tripuladas de uso civil. Brasília:
Agência Nacional de Aviação Civil, 2017.

DJI. **About DJI**. 2026a. Disponível em: <https://www.dji.com/company>. Acesso em: 18 out. 2026.

DJI. **DJI developer**: Mobile SDK e Payload SDK. 2026b. Disponível em:
<https://developer.dji.com>. Acesso em: 18 out. 2026.

DJI. **DJI FlySafe**: GEO zones. 2026c. Disponível em: <https://www.dji.com/flysafe>. Acesso em:
18 out. 2026.

MEIER, L.; HONEGGER, D.; POLLEFEYS, M. PX4: a node-based multithreaded open source robotics
framework for deeply embedded platforms. *In*: IEEE INTERNATIONAL CONFERENCE ON ROBOTICS AND
AUTOMATION (ICRA), 2015, Seattle. **Proceedings** [...]. Piscataway: IEEE, 2015. p. 6235-6240.

PX4. **PX4 architectural overview**. Versão v1.17.0. 2026a. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/concept/architecture.md>. Acesso em:
18 out. 2026.

PX4. **LICENSE**. Versão v1.17.0. 2026b. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/LICENSE>. Acesso em: 18 out. 2026.

PX4. **PX4 autopilot user guide**: página inicial. Versão v1.17.0. 2026c. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/index.md>. Acesso em: 18 out. 2026.

PX4. **Basic concepts**. Versão v1.17.0. 2026d. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/getting_started/px4_basic_concepts.md>.
Acesso em: 18 out. 2026.

PX4. **Building PX4 software**. Versão v1.17.0. 2026e. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/dev_setup/building_px4.md>. Acesso
em: 18 out. 2026.

PX4. **uORB messaging**. Versão v1.17.0. 2026f. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/middleware/uorb.md>. Acesso em: 18
out. 2026.

PX4. **Control allocation (mixing)**. Versão v1.17.0. 2026g. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/concept/control_allocation.md>.
Acesso em: 18 out. 2026.

PX4. **Simulation**. Versão v1.17.0. 2026h. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/simulation/index.md>. Acesso em: 18
out. 2026.

PX4. **Gazebo simulation**. Versão v1.17.0. 2026i. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/sim_gazebo_gz/index.md>. Acesso em:
18 out. 2026.

PX4. **PX4 Docker containers**. Versão v1.17.0. 2026j. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/test_and_ci/docker.md>. Acesso em: 18
out. 2026.

RICHARDS, M.; FORD, N. **Fundamentals of software architecture**: an engineering approach.
Sebastopol: O'Reilly, 2020.

RTCA. **DO-178C**: software considerations in airborne systems and equipment certification.
Washington: RTCA, 2011.
