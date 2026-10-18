package br.edu.ifal.arqsw.voo.mensagens;

/** Leitura bruta dos sensores: um quaternio de orientacao ainda nao normalizado. */
public record LeituraSensores(long amostraUs, double qw, double qx, double qy, double qz) { }
