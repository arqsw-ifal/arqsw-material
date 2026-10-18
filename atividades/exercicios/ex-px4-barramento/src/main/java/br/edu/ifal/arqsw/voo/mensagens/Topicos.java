package br.edu.ifal.arqsw.voo.mensagens;

import br.edu.ifal.arqsw.voo.barramento.Topico;

/** Equivalente aos topicos gerados a partir dos arquivos .msg do PX4. */
public final class Topicos {
    public static final Topico<LeituraSensores> SENSORES =
            new Topico<>("sensor_combined", LeituraSensores.class);
    public static final Topico<AtitudeVeiculo> ATITUDE =
            new Topico<>("vehicle_attitude", AtitudeVeiculo.class);
    public static final Topico<SetpointTaxa> SETPOINT_TAXA =
            new Topico<>("vehicle_rates_setpoint", SetpointTaxa.class);
    public static final Topico<SetpointTorque> SETPOINT_TORQUE =
            new Topico<>("vehicle_torque_setpoint", SetpointTorque.class);

    private Topicos() { }
}
