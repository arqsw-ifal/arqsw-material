package br.edu.ifal.arqsw.limitador.api;

/** Strategy: cada algoritmo de limitacao de taxa e' uma implementacao desta interface (Listagem F.6). */
public interface LimitadorDeTaxa {

    /** Avalia (e, se permitida, contabiliza) uma requisicao identificada por chave. */
    Decisao avaliar(String chave);
}
