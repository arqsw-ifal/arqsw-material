package br.edu.ifal.arqsw.timeline.cache;

import br.edu.ifal.arqsw.timeline.aplicacao.CacheDeTimelines;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Simula o Redis: cada timeline e' um conjunto ordenado pelo ID da publicacao (como o
 * ZADD com o ID como membro e como pontuacao) e e' aparada em {@code maxItens}, como o
 * FeedManager#trim com MAX_ITEMS = 800 (Listagem 6.2.5): uma janela deslizante.
 */
public class CacheEmMemoria implements CacheDeTimelines {

    private final int maxItens;
    private final Map<Long, TreeSet<Long>> timelines = new HashMap<>();

    public CacheEmMemoria(int maxItens) {
        this.maxItens = maxItens;
    }

    @Override
    public void inserir(long donoId, long postId) {
        TreeSet<Long> ids = timelines.computeIfAbsent(donoId, d -> new TreeSet<>(Comparator.reverseOrder()));
        ids.add(postId);
        while (ids.size() > maxItens) {
            ids.pollLast(); // descarta a mais antiga
        }
    }

    @Override
    public List<Long> recentes(long donoId, int limite) {
        return timelines.getOrDefault(donoId, new TreeSet<>()).stream().limit(limite).toList();
    }
}
