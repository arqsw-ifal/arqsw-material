package br.edu.ifal.arqsw.telemetria.regras;

/** Uma leitura de telemetria, ja independente do protocolo por onde chegou. */
public record Leitura(String dispositivo, String grandeza, double valor, long instante) { }
