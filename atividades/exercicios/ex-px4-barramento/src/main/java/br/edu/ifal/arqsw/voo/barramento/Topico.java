package br.edu.ifal.arqsw.voo.barramento;

/**
 * Um topico tem nome e tipo de mensagem: e' o contrato entre os modulos.
 * Equivale ao ORB_ID gerado a partir de um arquivo .msg do uORB (Secao 6.8.1).
 */
public record Topico<T>(String nome, Class<T> tipo) { }
