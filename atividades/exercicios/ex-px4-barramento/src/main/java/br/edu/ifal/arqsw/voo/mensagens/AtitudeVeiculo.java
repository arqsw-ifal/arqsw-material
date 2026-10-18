package br.edu.ifal.arqsw.voo.mensagens;

/** Atitude estimada do veiculo: quaternio unitario, convencao de Hamilton, ordem (w, x, y, z). */
public record AtitudeVeiculo(long amostraUs, double qw, double qx, double qy, double qz) { }
