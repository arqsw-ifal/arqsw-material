package br.edu.ifal.arqsw.voo.mensagens;

/** Torque normalizado, em [-1, 1] por eixo, entregue a alocacao de controle. */
public record SetpointTorque(long amostraUs, double rolagem, double arfagem, double guinada) { }
