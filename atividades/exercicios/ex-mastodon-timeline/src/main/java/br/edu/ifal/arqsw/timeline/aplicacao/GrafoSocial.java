package br.edu.ifal.arqsw.timeline.aplicacao;

import java.util.List;

/** Porta de saida para o grafo social (quem segue quem). */
public interface GrafoSocial {

    long totalDeSeguidores(long autorId);

    /** Seguidores ativos recentemente (no Mastodon, USER_ACTIVE_DAYS; Secao 6.2.4). */
    List<Long> seguidoresAtivos(long autorId);

    /** Contas seguidas pelo usuario que tem pelo menos {@code limiar} seguidores. */
    List<Long> celebridadesSeguidasPor(long usuarioId, long limiar);
}
