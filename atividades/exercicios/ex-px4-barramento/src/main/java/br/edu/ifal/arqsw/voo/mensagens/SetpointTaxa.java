package br.edu.ifal.arqsw.voo.mensagens;

/** Velocidades angulares desejadas (rad/s) em rolagem, arfagem e guinada. */
public record SetpointTaxa(double rolagem, double arfagem, double guinada) { }
