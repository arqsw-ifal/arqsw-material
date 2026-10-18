package br.edu.ifal.arqsw.hub.hub;

/** O que as conexoes implementam: ser notificadas (papel de Observer). */
public interface Assinante {

    String usuarioId();

    /** Filtro do assinante, analogo a WebConn.ShouldSendEvent (Listagem 6.3.4). */
    boolean deveReceber(Evento evento);

    void notificar(Evento evento);
}
