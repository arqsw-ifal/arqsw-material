package br.edu.ifal.arqsw.timeline.aplicacao;

import java.util.List;

/**
 * Porta de saida para o cache de timelines. No Mastodon, cada timeline e' um conjunto
 * ordenado (ZSET) do Redis, mantido pelo FeedManager (Secao 6.2.3).
 */
public interface CacheDeTimelines {

    /** Insere a publicacao na timeline pre-computada do dono. */
    void inserir(long donoId, long postId);

    /** Devolve ate {@code limite} IDs, do mais recente (maior ID) para o mais antigo. */
    List<Long> recentes(long donoId, int limite);
}
