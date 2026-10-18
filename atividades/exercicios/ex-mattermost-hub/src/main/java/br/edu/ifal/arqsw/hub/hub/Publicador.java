package br.edu.ifal.arqsw.hub.hub;

/** O que os produtores enxergam: apenas publicar (papel de Mediator). */
public interface Publicador {
    void publicar(Evento evento);
}
