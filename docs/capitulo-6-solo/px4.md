# 6.8 PX4: o código

<span class="altitude solo">Solo</span>

Esta seção pousa no código do PX4 Autopilot, na *tag* `v1.17.0` (PX4, 2026b). Todos os trechos são literais,
foram conferidos no repositório e têm *link* permanente para as linhas citadas. O percurso segue a
decisão que define o estilo do sistema, a comunicação exclusiva por tópicos (ADR-PX4-001, Seção
4.8.5): primeiro o contrato, isto é, as mensagens; depois a publicação e a assinatura; em seguida o
ciclo de um controlador; por fim, a supervisão de modos e *failsafes* e a variação de geometria. A
seção termina com a subida de verificação e com o exercício de correção automática.

## 6.8.1 O modelo de domínio expresso em mensagens

Nas aplicações corporativas estudadas neste livro, o modelo de domínio aparece em entidades,
agregados e objetos de valor. No PX4, o vocabulário do domínio, como atitude, *setpoint* de taxa,
modo de navegação e sinalizador de falha, está expresso principalmente em **arquivos de definição
de mensagens**. Para criar um tópico, escreve-se um arquivo `.msg` em *CamelCase* no diretório
`msg/`, ou `msg/versioned/` se a mensagem precisar de versionamento, e registra-se o arquivo em
`msg/CMakeLists.txt`; o código C/C++ é gerado automaticamente; por padrão, cada arquivo define um
tópico cujo nome é a versão em *snake_case* do nome do arquivo; e toda mensagem deve conter o campo
`timestamp`, necessário ao registro em log (PX4, 2026a).

**Listagem 6.8.1** – Definição da mensagem de atitude.

```text
# This is similar to the mavlink message ATTITUDE_QUATERNION, but for onboard use
# The quaternion uses the Hamilton convention, and the order is q(w, x, y, z)

uint32 MESSAGE_VERSION = 0

uint64 timestamp                # time since system start (microseconds)

uint64 timestamp_sample         # the timestamp of the raw data (microseconds)

float32[4] q                    # Quaternion rotation from the FRD body frame to the NED earth frame
float32[4] delta_q_reset        # Amount by which quaternion has changed during last reset
uint8 quat_reset_counter        # Quaternion reset counter

# TOPICS vehicle_attitude vehicle_attitude_groundtruth external_ins_attitude
# TOPICS estimator_attitude
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`msg/versioned/VehicleAttitude.msg`, linhas
1-15](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/msg/versioned/VehicleAttitude.msg#L1-L15).

Três detalhes merecem atenção. O primeiro é a separação entre `timestamp`, instante da publicação,
e `timestamp_sample`, instante da amostra bruta que originou a estimativa: em tempo real, a idade de
um dado é informação de primeira classe. O segundo é a linha `# TOPICS`, que faz uma única
definição gerar vários tópicos com a mesma estrutura: a atitude oficial, a de referência em
simulação, a de um sistema inercial externo e a de cada instância do estimador. O terceiro é o campo
`MESSAGE_VERSION`, exigido nas mensagens versionadas desde a `v1.16`.

O gerador de código fica em `Tools/msg/`, com *templates* em `Tools/msg/templates/uorb/` que
produzem, para cada mensagem, uma estrutura C++ (por exemplo, `vehicle_attitude_s`) e os metadados
do tópico. A geração transforma o contrato em **tipo verificado pelo compilador**: um módulo que
tente publicar um campo inexistente não compila, diferentemente de barramentos baseados em cadeias
de caracteres ou JSON. A documentação recomenda ainda que campos removidos ou renomeados sejam
marcados como obsoletos e que mudanças semânticas, como a troca de graus por radianos, exijam
renomear o campo, para que ferramentas externas distingam as versões (PX4, 2026a). A regra
reconhece que uma mensagem, por ser contrato entre muitos consumidores, é difícil de mudar, isto é,
arquitetural.

Em termos de DDD tático (Seção 1.9.3), as mensagens são **objetos de valor**: não têm identidade,
são imutáveis depois de publicadas e valem pelos seus atributos. As "entidades" com ciclo de vida,
como o veículo com seu estado de armação e seu modo de navegação, não são classes de domínio, mas o
**estado mantido por componentes**, como o *commander*, e publicado periodicamente como valor, por
exemplo em `vehicle_status`. Não há agregados nem repositórios no sentido de Evans (2003): o modelo
é orientado a fluxos de dados, e não a transações sobre entidades persistentes.

## 6.8.2 Publicação tipada

**Listagem 6.8.2** – A classe `uORB::Publication<T>`.

```cpp
template<typename T>
class Publication : public PublicationBase
{
public:

	/**
	 * Constructor
	 *
	 * @param meta The uORB metadata (usually from the ORB_ID() macro) for the topic.
	 */
	Publication(ORB_ID id) : PublicationBase(id) {}
	Publication(const orb_metadata *meta) : PublicationBase(static_cast<ORB_ID>(meta->o_id)) {}

	bool advertise()
	{
		if (!advertised()) {
			_handle = orb_advertise(get_topic(), nullptr);
		}

		return advertised();
	}

	/**
	 * Publish the struct
	 * @param data The uORB message struct we are updating.
	 */
	bool publish(const T &data)
	{
		if (!advertised()) {
			advertise();
		}

		return (Manager::orb_publish(get_topic(), _handle, &data) == PX4_OK);
	}
};
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`platforms/common/uORB/Publication.hpp`, linhas
82-116](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/platforms/common/uORB/Publication.hpp#L82-L116).

O parâmetro de tipo `T` garante que só se publique a estrutura gerada para o tópico. O publicador
não conhece nenhum assinante: entrega os dados ao `Manager`, que os copia para o nó do tópico.

## 6.8.3 Assinatura com *callback*: o tópico como gatilho de agendamento

**Listagem 6.8.3** – A chegada de uma nova mensagem agenda o módulo assinante.

```cpp
// Subscription with callback that schedules a WorkItem
class SubscriptionCallbackWorkItem : public SubscriptionCallback
{
public:
	/**
	 * Constructor
	 *
	 * @param work_item The WorkItem that will be scheduled immediately on new publications.
	 * @param meta The uORB metadata (usually from the ORB_ID() macro) for the topic.
	 * @param instance The instance for multi sub.
	 */
	SubscriptionCallbackWorkItem(px4::WorkItem *work_item, const orb_metadata *meta, uint8_t instance = 0) :
		SubscriptionCallback(meta, 0, instance),	// interval 0
		_work_item(work_item)
	{
	}

	virtual ~SubscriptionCallbackWorkItem() = default;

	void call() override
	{
		// schedule immediately if updated (queue depth or subscription interval)
		if ((_required_updates == 0)
		    || (Manager::updates_available(_subscription.get_node(), _subscription.get_last_generation()) >= _required_updates)) {
			if (updated()) {
				_work_item->ScheduleNow();
			}
		}
	}
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`platforms/common/uORB/SubscriptionCallback.hpp`, linhas
144-172](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/platforms/common/uORB/SubscriptionCallback.hpp#L144-L172).

A *callback* não executa o controlador: apenas chama `ScheduleNow()` sobre o item de trabalho, que
será executado pela *thread* da fila correspondente, com a prioridade dela (Seção 5.8.3). É o elo
entre o fluxo de dados e o agendamento. O controlador de atitude declara assim suas dependências.

**Listagem 6.8.4** – Dependências declaradas do controlador de atitude.

```cpp
	uORB::SubscriptionInterval _parameter_update_sub{ORB_ID(parameter_update), 1_s};

	uORB::Subscription _hover_thrust_estimate_sub{ORB_ID(hover_thrust_estimate)};
	uORB::Subscription _vehicle_attitude_setpoint_sub{ORB_ID(vehicle_attitude_setpoint)};
	uORB::Subscription _autotune_attitude_control_status_sub{ORB_ID(autotune_attitude_control_status)};
	uORB::Subscription _manual_control_setpoint_sub{ORB_ID(manual_control_setpoint)};
	uORB::Subscription _vehicle_control_mode_sub{ORB_ID(vehicle_control_mode)};
	uORB::Subscription _vehicle_land_detected_sub{ORB_ID(vehicle_land_detected)};
	uORB::Subscription _vehicle_local_position_sub{ORB_ID(vehicle_local_position)};
	uORB::Subscription _vehicle_status_sub{ORB_ID(vehicle_status)};

	uORB::SubscriptionCallbackWorkItem _vehicle_attitude_sub{this, ORB_ID(vehicle_attitude)};

	uORB::Publication<vehicle_rates_setpoint_s>     _vehicle_rates_setpoint_pub{ORB_ID(vehicle_rates_setpoint)};    /**< rate setpoint publication */
	uORB::Publication<vehicle_attitude_setpoint_s>  _vehicle_attitude_setpoint_pub;
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/mc_att_control/mc_att_control.hpp`, linhas
102-116](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/mc_att_control/mc_att_control.hpp#L102-L116).

A listagem funciona como um **manifesto de dependências**. Das dez assinaturas, apenas a de
`vehicle_attitude` é do tipo *callback*: só a chegada de uma nova atitude dispara o controlador. As
demais são lidas sem bloquear, para consultar o último valor disponível, e a de parâmetros é
verificada no máximo uma vez por segundo. O laço de atitude é disparado pela estimativa de atitude,
não pelo rádio controle nem pelo modo de voo, que apenas modulam seu comportamento. O registro da
*callback* ocorre na inicialização do módulo
([`mc_att_control_main.cpp`, linhas
77-86](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/mc_att_control/mc_att_control_main.cpp#L77-L86)),
e o construtor associa o módulo à fila `nav_and_controllers`
([linha 58](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/mc_att_control/mc_att_control_main.cpp#L58)).

## 6.8.4 O ciclo do controlador de atitude

**Listagem 6.8.5** – Início do ciclo do controlador de atitude, com o intervalo de tempo limitado.

```cpp
	// run controller on attitude updates
	vehicle_attitude_s v_att;

	if (_vehicle_attitude_sub.update(&v_att)) {

		// Guard against too small (< 0.2ms) and too large (> 20ms) dt's.
		const float dt = math::constrain(((v_att.timestamp_sample - _last_run) * 1e-6f), 0.0002f, 0.02f);
		_last_run = v_att.timestamp_sample;

		const Quatf q{v_att.q};
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/mc_att_control/mc_att_control_main.cpp`, linhas
240-249](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/mc_att_control/mc_att_control_main.cpp#L240-L249).

A linha que limita `dt` entre 0,2 ms e 20 ms é uma pequena decisão de **robustez temporal**. O
intervalo é calculado a partir do instante da amostra (`timestamp_sample`, Listagem 6.8.1), e não do
relógio no momento da execução, e é saturado para que um atraso anômalo não produza um passo de
integração desproporcional. Mais adiante, no mesmo método, o controlador calcula os *setpoints* de
taxa e os publica
([linhas 342-365](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/mc_att_control/mc_att_control_main.cpp#L342-L365)).
O ciclo é: acordar por dado novo, ler sem bloquear os demais tópicos, calcular e publicar. Nenhuma
chamada a outro módulo e nenhuma espera.

## 6.8.5 Redundância no código do estimador

**Listagem 6.8.6** – Cada instância do EKF2 publica em um tópico próprio quando há redundância.

```cpp
EKF2::EKF2(bool multi_mode, const px4::wq_config_t &config, bool replay_mode):
	ModuleParams(nullptr),
	ScheduledWorkItem(MODULE_NAME, config),
	_replay_mode(replay_mode && !multi_mode),
	_multi_mode(multi_mode),
	_instance(multi_mode ? -1 : 0),
	_attitude_pub(multi_mode ? ORB_ID(estimator_attitude) : ORB_ID(vehicle_attitude)),
	_local_position_pub(multi_mode ? ORB_ID(estimator_local_position) : ORB_ID(vehicle_local_position)),
	_global_position_pub(multi_mode ? ORB_ID(estimator_global_position) : ORB_ID(vehicle_global_position)),
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/ekf2/EKF2.cpp`, linhas
52-60](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/ekf2/EKF2.cpp#L52-L60).

A alternância entre `vehicle_attitude` e `estimator_attitude` só é possível porque ambos os tópicos
derivam da mesma definição (`# TOPICS`, Listagem 6.8.1). A fila de trabalho chega por parâmetro:
uma instância única usa `INS0`, e cada instância redundante recebe a fila associada à sua unidade
inercial
([linhas 2883 e 2931](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/ekf2/EKF2.cpp#L2883)).

## 6.8.6 O *commander*: máquina de estados tabular

Os modos de voo são **constantes numéricas** definidas na mensagem `VehicleStatus`, como
`NAVIGATION_STATE_POSCTL = 2` e `NAVIGATION_STATE_AUTO_RTL = 5`
([`VehicleStatus.msg`, linhas
36-67](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/msg/versioned/VehicleStatus.msg#L36-L67)),
e não classes com comportamento. Os requisitos de cada modo são registrados em forma de tabela.

**Listagem 6.8.7** – Requisitos do modo de controle de posição.

```cpp
	// NAVIGATION_STATE_POSCTL
	setRequirement(vehicle_status_s::NAVIGATION_STATE_POSCTL, flags.mode_req_angular_velocity);
	setRequirement(vehicle_status_s::NAVIGATION_STATE_POSCTL, flags.mode_req_attitude);
	setRequirement(vehicle_status_s::NAVIGATION_STATE_POSCTL, flags.mode_req_local_alt);
	setRequirement(vehicle_status_s::NAVIGATION_STATE_POSCTL, flags.mode_req_local_position_relaxed);
	setRequirement(vehicle_status_s::NAVIGATION_STATE_POSCTL, flags.mode_req_manual_control);
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/commander/ModeUtil/mode_requirements.cpp`, linhas
79-84](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/commander/ModeUtil/mode_requirements.cpp#L79-L84).

O efeito de cada modo sobre os controladores também é expresso como dado: uma função traduz o modo
efetivo em sinalizadores publicados em `vehicle_control_mode`, que indicam quais laços da cascata
devem estar ativos.

**Listagem 6.8.8** – O modo de voo traduzido em sinalizadores de controle.

```cpp
	switch (nav_state) {
	case vehicle_status_s::NAVIGATION_STATE_MANUAL:
		vehicle_control_mode.flag_control_manual_enabled = true;
		vehicle_control_mode.flag_control_attitude_enabled = stabilization_required(vehicle_type);
		vehicle_control_mode.flag_control_rates_enabled = stabilization_required(vehicle_type);
		vehicle_control_mode.flag_control_allocation_enabled = true;
		break;

	case vehicle_status_s::NAVIGATION_STATE_STAB:
		vehicle_control_mode.flag_control_manual_enabled = true;
		vehicle_control_mode.flag_control_attitude_enabled = true;
		vehicle_control_mode.flag_control_rates_enabled = true;
		vehicle_control_mode.flag_control_allocation_enabled = true;
		break;
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/commander/ModeUtil/control_mode.cpp`, linhas
50-63](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/commander/ModeUtil/control_mode.cpp#L50-L63).

Os controladores consultam esses sinalizadores; o de atitude, por exemplo, só executa seu laço
quando `flag_control_attitude_enabled` está ativo
([`mc_att_control_main.cpp`, linha
290](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/mc_att_control/mc_att_control_main.cpp#L290)).
A Listagem 6.8.9 mostra o ponto em que a máquina de *failsafe* decide o modo efetivo.

**Listagem 6.8.9** – O modo efetivo resulta da ação de *failsafe* aplicada sobre a intenção do usuário.

```cpp
	uint8_t updated_user_intented_mode = _failsafe.update(hrt_absolute_time(), state, mode_change_requested,
					     _failsafe_user_override_request,
					     _failsafe_flags);
	_failsafe_user_override_request = false;

	// Force intended mode if changed by the failsafe state machine
	if (state.user_intended_mode != updated_user_intented_mode) {
		_user_mode_intention.change(updated_user_intented_mode, ModeChangeSource::User, false, true);
		_user_mode_intention.getHadModeChangeAndClear();
	}

	// Handle failsafe action
	_mode_management.setFailsafeState(_failsafe.selectedAction() > FailsafeBase::Action::Warn);
	_vehicle_status.nav_state_user_intention = _mode_management.getNavStateReplacementIfValid(_user_mode_intention.get(),
			false);
	_vehicle_status.nav_state = _mode_management.getNavStateReplacementIfValid(FailsafeBase::modeFromAction(
					    _failsafe.selectedAction(), _user_mode_intention.get()));
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/commander/Commander.cpp`, linhas
2356-2372](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/commander/Commander.cpp#L2356-L2372).

O trecho torna visível a separação entre intenção e efeito: `nav_state_user_intention` guarda o que
o operador pediu; `nav_state` guarda o que o veículo faz, calculado por `modeFromAction` a partir da
ação de *failsafe* selecionada. A escala de severidade das ações está codificada na **ordem de
declaração** de uma enumeração (Listagem 6.8.10).

**Listagem 6.8.10** – Ações de *failsafe*: a ordem de declaração é a ordem de precedência.

```cpp
	enum class Action : uint8_t {
		// Actions further down take precedence
		None,
		Warn,

		// Fallbacks
		FallbackPosCtrl,
		FallbackAltCtrl,
		FallbackStab,

		Hold,
		RTL,
		Land,
		Descend,
		Disarm,
		Terminate,

		Count
	};
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/commander/failsafe/framework.h`, linhas
52-70](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/commander/failsafe/framework.h#L52-L70).

Quando vários *failsafes* estão ativos, prevalece o mais severo, e a comparação entre ações reduz-se
a uma comparação de inteiros, como em `selectedAction() > FailsafeBase::Action::Warn` (Listagem
6.8.9). A regra de que nunca se sai do estado de terminação também é explícita
([`UserModeIntention.cpp`, linhas
71-72](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/commander/UserModeIntention.cpp#L71-L72)).

## 6.8.7 *Strategy* na alocação de controle

**Listagem 6.8.11** – Seleção do algoritmo de alocação: duas estratégias sob uma mesma abstração.

```cpp
			switch (method) {
			case AllocationMethod::PSEUDO_INVERSE:
				_control_allocation[i] = new ControlAllocationPseudoInverse();
				break;

			case AllocationMethod::SEQUENTIAL_DESATURATION:
				_control_allocation[i] = new ControlAllocationSequentialDesaturation();
				break;
```

Fonte: PX4-Autopilot, *tag* `v1.17.0`,
[`src/modules/control_allocator/ControlAllocator.cpp`, linhas
176-183](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/control_allocator/ControlAllocator.cpp#L176-L183).

O módulo trabalha com ponteiros para as abstrações `ControlAllocation` e `ActuatorEffectiveness`
([`ControlAllocator.hpp`, linhas 143 e
172](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/control_allocator/ControlAllocator.hpp#L143)).
A geometria do veículo é representada por uma hierarquia de classes de efetividade de atuadores,
com implementações para multirrotor, VTOL, asa fixa, helicópteros, veículos terrestres e outros, no
diretório
[`VehicleActuatorEffectiveness/`](https://github.com/PX4/PX4-Autopilot/tree/v1.17.0/src/modules/control_allocator/VehicleActuatorEffectiveness).

## 6.8.8 Padrões identificados

As fichas a seguir seguem o espírito da seção *Known Uses* de Gamma *et al.* (1994): registram onde
o padrão aparece em um sistema real, que problema resolve ali e com que consequências.

**Tabela 6.8.1** – *Observer* (variante com *ChangeManager*) no uORB.

| Item | Descrição |
|---|---|
| Onde | `platforms/common/uORB/` (Listagens 6.8.2 e 6.8.3) |
| Problema | Muitos componentes precisam reagir a mudanças de estado de outros, sem se conhecerem |
| Solução no código | Publicadores entregam mensagens ao `uORB::Manager`, que mantém o mapeamento entre tópicos e assinantes e os notifica |
| Variante | Gamma *et al.* (1994) descrevem, na implementação do *Observer*, um ***ChangeManager*** que mapeia sujeitos a observadores, define a estratégia de atualização e elimina referências mútuas; no uORB, esse papel cabe ao `Manager` e aos nós de tópico |
| Diferença essencial | A notificação não executa o observador na *thread* do publicador: apenas o agenda em sua fila de trabalho, com sua prioridade |
| Consequências | Desacoplamento e substituibilidade; um assinante lento não atrasa o publicador; o fluxo de dados fica implícito |

Fonte: elaborado pelo autor (2026).

**Tabela 6.8.2** – Máquina de estados tabular no *commander*, alternativa ao *State*.

| Item | Descrição |
|---|---|
| Onde | `src/modules/commander/ModeUtil/` e `failsafe/` (Listagens 6.8.7 a 6.8.10) |
| Problema | Muitos modos de voo, com requisitos e efeitos distintos, e transições forçadas por falhas |
| Solução no código | Estados como constantes; requisitos e efeitos como tabelas e sinalizadores publicados |
| Relação com o GoF | Gamma *et al.* (1994), na implementação do *State*, descrevem a alternativa tabular, atribuída a Cargill: tabelas mapeiam entradas a transições; para os autores, o *State* modela o comportamento específico de cada estado, enquanto a abordagem tabular se concentra em definir as transições |
| Por que não o *State* clássico | Classes de estado precisariam chamar os controladores, violando o ADR-PX4-001; aqui o comportamento por estado é **publicado** em `vehicle_control_mode` e executado pelos próprios controladores |
| Consequências | Regularidade e testabilidade; critérios de transição mudam por dados; menor explicitação das ações associadas a cada transição |

Fonte: elaborado pelo autor (2026).

**Tabela 6.8.3** – *Strategy* na alocação de controle.

| Item | Descrição |
|---|---|
| Onde | `src/modules/control_allocator/` e `src/lib/control_allocation/` (Listagem 6.8.11) |
| Problema | O mesmo controlador precisa servir a geometrias e métodos numéricos diferentes |
| Solução no código | Abstrações `ControlAllocation` e `ActuatorEffectiveness`, com implementações escolhidas por parâmetro |
| Consequências | Controladores independentes da geometria; nova geometria exige nova classe e registro na seleção |

Fonte: elaborado pelo autor (2026).

Um paralelo com Fowler (2002) é o ***Data Transfer Object***: as estruturas geradas de `.msg` são
objetos de dados sem comportamento, copiados entre componentes. O motivo, porém, não é reduzir
chamadas remotas, como em Fowler, mas eliminar o compartilhamento de estado mutável entre *threads*.
O PX4 usa, portanto, dois mecanismos de variação em escalas distintas: **entre módulos**, tópicos;
**dentro de um módulo**, polimorfismo. Ambos aplicam o princípio aberto-fechado (Seção 1.5.4).

## 6.8.9 Subida de verificação

A subida parte de uma linha de código e pergunta, em cada altitude, que componente ela realiza, que
decisão concretiza e que característica protege (Seção 1.11). Tome-se a linha
`WorkItem(MODULE_NAME, px4::wq_configurations::rate_ctrl)`, do construtor do controlador de taxa
([`MulticopterRateControl.cpp`, linha
48](https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/src/modules/mc_rate_control/MulticopterRateControl.cpp#L48)).

- **Solo.** A linha associa o módulo `mc_rate_control` à fila `wq:rate_ctrl`, de maior prioridade
  relativa padrão. A execução é disparada pela chegada de `vehicle_angular_velocity`, por meio de
  uma `SubscriptionCallbackWorkItem` (Listagem 6.8.3).
- **10.000 pés.** O componente é o laço mais interno da cascata de controle (Figura 5.8.1). Recebe
  *setpoints* de taxa do controlador de atitude e publica torque e empuxo para a alocação de
  controle, que está na mesma fila (Figura 5.8.2).
- **50.000 pés.** A decisão concretizada é executar todos os módulos de voo em um único quantum de
  tempo real, com comunicação por tópicos e prioridades atribuídas por criticidade (ADR-PX4-001),
  deixando funções não críticas em filas de baixa prioridade ou no computador de missão
  (ADR-PX4-002).
- **100.000 pés.** A característica protegida é o **determinismo temporal** do laço que mantém a
  aeronave estável, condição para a **segurança** de pessoas em solo, conforme o Top-3 da Seção
  3.8.6.2.

A cadeia se completa e responde à pergunta: por que um módulo não pode simplesmente chamar o outro?
Se o controlador de atitude chamasse diretamente o de taxa, o laço interno passaria a executar na
*thread* e na prioridade do laço externo, mais lento e de menor prioridade. A frequência do laço
interno ficaria presa à do externo; um atraso no estimador ou no controle de atitude se propagaria
aos motores; e substituir um dos módulos exigiria recompilar o outro. A chamada direta economizaria
uma cópia de mensagem e sacrificaria o determinismo, a substituibilidade e a capacidade de registrar
e reproduzir o tráfego entre módulos.

## 6.8.10 Versão didática em Java e exercício com correção automática

A Listagem 6.8.12 reproduz, em Java 17, a essência do uORB: tópicos tipados, um barramento que
guarda a última amostra e notifica assinantes, e módulos que não se conhecem. O código é didático e
não reproduz as garantias de tempo real do original.

**Listagem 6.8.12** – Barramento *publish/subscribe* com tópicos tipados (didático).

```java
package br.edu.ifal.arqsw.voo.barramento;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Um tópico tem nome e tipo de mensagem: o contrato entre módulos. */
public record Topico<T>(String nome, Class<T> tipo) { }

/** Barramento: guarda a última amostra de cada tópico e notifica os assinantes. */
public final class Barramento {
    private final Map<Topico<?>, Object> ultimaAmostra = new ConcurrentHashMap<>();
    private final Map<Topico<?>, List<Consumer<Object>>> assinantes = new ConcurrentHashMap<>();

    public <T> void publicar(Topico<T> topico, T mensagem) {
        ultimaAmostra.put(topico, topico.tipo().cast(mensagem));   // semântica de "última amostra"
        assinantes.getOrDefault(topico, List.of()).forEach(a -> a.accept(mensagem));
    }

    @SuppressWarnings("unchecked")
    public <T> void assinar(Topico<T> topico, Consumer<T> aoChegar) {
        assinantes.computeIfAbsent(topico, t -> new CopyOnWriteArrayList<>())
                  .add(m -> aoChegar.accept((T) m));
    }

    public <T> Optional<T> ultima(Topico<T> topico) {             // leitura não bloqueante
        return Optional.ofNullable(ultimaAmostra.get(topico)).map(topico.tipo()::cast);
    }
}
```

```java
package br.edu.ifal.arqsw.voo.mensagens;

import br.edu.ifal.arqsw.voo.barramento.Topico;

public record AtitudeVeiculo(long amostraUs, double qw, double qx, double qy, double qz) { }
public record SetpointTaxa(double rolagem, double arfagem, double guinada) { }

/** Equivalente aos tópicos gerados a partir dos arquivos .msg. */
public final class Topicos {
    public static final Topico<AtitudeVeiculo> ATITUDE = new Topico<>("vehicle_attitude", AtitudeVeiculo.class);
    public static final Topico<SetpointTaxa> SETPOINT_TAXA = new Topico<>("vehicle_rates_setpoint", SetpointTaxa.class);
    private Topicos() { }
}
```

```java
package br.edu.ifal.arqsw.voo.modulos.atitude;

import br.edu.ifal.arqsw.voo.barramento.Barramento;
import br.edu.ifal.arqsw.voo.mensagens.*;

/** Controlador de atitude: disparado pela atitude, publica setpoints de taxa. */
public final class ControleAtitude {
    private final Barramento barramento;

    public ControleAtitude(Barramento barramento) {
        this.barramento = barramento;
        barramento.assinar(Topicos.ATITUDE, this::aoReceberAtitude);   // "callback" do tópico
    }

    private void aoReceberAtitude(AtitudeVeiculo atitude) {
        double ganho = 6.5;   // lei de controle omitida: ganho proporcional ilustrativo
        barramento.publicar(Topicos.SETPOINT_TAXA,
            new SetpointTaxa(-ganho * atitude.qx(), -ganho * atitude.qy(), -ganho * atitude.qz()));
    }
}
```

Fonte: elaborado pelo autor (2026).

Em Java, cada `record` e cada classe pública ficariam em seu próprio arquivo; foram agrupados para
leitura. `ControleAtitude` depende apenas do barramento e das mensagens: não conhece o estimador que
publica a atitude nem o controlador de taxa que consome seu resultado. A Listagem 6.8.13 transforma
o ADR-PX4-001 em teste automatizado.

**Listagem 6.8.13** – *Fitness function*: módulos não dependem uns dos outros.

```java
package br.edu.ifal.arqsw.voo;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.voo";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("ADR-PX4-001: um módulo nunca chama outro módulo diretamente")
    void modulosNaoSeConhecem() {
        slices().matching(BASE + ".modulos.(*)..").should().notDependOnEachOther().check(classes);
    }

    @Test
    @DisplayName("mensagens são dados puros: não dependem de módulos")
    void mensagensSaoContratos() {
        noClasses().that().resideInAPackage(BASE + ".mensagens..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".modulos..")
                .check(classes);
    }
}
```

Fonte: elaborado pelo autor (2026), no padrão de `atividades/modelo-exercicio`.

O exercício com correção automática desta aplicação, listado em `atividades/README.md` como
"barramento *publish/subscribe* com tópicos tipados", parte dessa ideia: o estudante recebe módulos
que se chamam diretamente e deve refatorá-los para um barramento com tópicos tipados, de modo que
os testes de comportamento (JUnit) continuem passando e a *fitness function* (ArchUnit) deixe de
acusar dependências entre módulos. A nota segue a composição das atividades do livro: compilação,
comportamento e arquitetura.

## Exercícios

1. Localize no repositório, na *tag* `v1.17.0`, a definição de uma mensagem uORB diferente das
   citadas nesta seção, um módulo que a publica e um que a assina. Registre os *permalinks*.
2. Explique por que a Listagem 6.8.5 calcula `dt` a partir de `timestamp_sample` e não do relógio no
   momento da execução. Que falha a saturação entre 0,2 ms e 20 ms previne?
3. Com base em Gamma *et al.* (1994), discuta as vantagens e desvantagens de o *commander* usar uma
   máquina de estados tabular em vez do padrão *State*. Em que situação o *State* seria preferível?
4. Na Listagem 6.8.4, apenas uma assinatura dispara o módulo. Proponha uma mudança que faria o
   controlador ser disparado também por `manual_control_setpoint` e analise seus efeitos sobre o
   determinismo.
5. Acrescente à Listagem 6.8.12 uma semântica de fila curta, análoga a `ORB_QUEUE_LENGTH`, para um
   tópico de comandos que não pode perder mensagens. Que compromisso essa mudança introduz?
6. Escreva uma nova regra ArchUnit que impeça os módulos de depender de qualquer classe de E/S
   bloqueante (por exemplo, `java.io.FileInputStream`), em analogia com a proibição de E/S nas filas
   de trabalho do PX4.
7. **Atividade com correção automática:** implemente o barramento da Listagem 6.8.12 e refatore os
   módulos fornecidos para que não chamem uns aos outros diretamente; os testes de comportamento e a
   *fitness function* da Listagem 6.8.13 devem passar (ver `atividades/README.md`, linha do PX4).

## Termos

`uORB`
:   *Middleware* de *publish/subscribe* do PX4, baseado em memória compartilhada, com tópicos
    tipados gerados a partir de arquivos `.msg`.

`Fila de trabalho`
:   No PX4, *thread* com prioridade própria que executa cooperativamente vários módulos, agendados
    por tempo ou pela chegada de mensagens.

`Máquina de estados tabular`
:   Implementação de máquina de estados em que transições e propriedades dos estados são
    representadas como dados, em vez de classes com comportamento.

`Failsafe`
:   Comportamento seguro acionado automaticamente quando uma falha ou condição perigosa é
    detectada.

`Determinismo temporal`
:   Propriedade de responder, para as mesmas entradas e estado, dentro de limites de tempo
    conhecidos e verificáveis.

`Tempo real crítico`
:   Requisito de uma função cujo prazo, se perdido, pode causar dano grave, independentemente de o
    resultado calculado estar correto.

## Referências

EVANS, E. **Domain-driven design**: tackling complexity in the heart of software. Boston:
Addison-Wesley, 2003.

FOWLER, M. **Patterns of enterprise application architecture**. Boston: Addison-Wesley, 2002.

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

PX4. **uORB messaging**. Versão v1.17.0. 2026a. Disponível em:
<https://github.com/PX4/PX4-Autopilot/blob/v1.17.0/docs/en/middleware/uorb.md>. Acesso em: 18
out. 2026.

PX4. **PX4/PX4-Autopilot**: repositório de código-fonte, versão v1.17.0. GitHub, 2026b.
Disponível em: <https://github.com/PX4/PX4-Autopilot/tree/v1.17.0>. Acesso em: 18 out. 2026.
