package br.edu.ifal.arqsw.timeline.banco;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simula o banco relacional (no Mastodon, o PostgreSQL): contas, relacoes de seguir e
 * publicacoes. E' um detalhe de infraestrutura: a aplicacao so deve enxerga-lo pelas portas.
 */
public class BancoDeDados {

    private final Set<Long> ativos = new LinkedHashSet<>();
    private final Map<Long, Set<Long>> seguidoresPorAutor = new LinkedHashMap<>();
    private final Map<Long, List<Long>> postsPorAutor = new LinkedHashMap<>();

    public void cadastrarUsuario(long usuarioId, boolean ativo) {
        if (ativo) {
            ativos.add(usuarioId);
        } else {
            ativos.remove(usuarioId);
        }
    }

    public void seguir(long seguidorId, long autorId) {
        seguidoresPorAutor.computeIfAbsent(autorId, a -> new LinkedHashSet<>()).add(seguidorId);
    }

    public void registrarPost(long autorId, long postId) {
        postsPorAutor.computeIfAbsent(autorId, a -> new ArrayList<>()).add(postId);
    }

    public boolean estaAtivo(long usuarioId) {
        return ativos.contains(usuarioId);
    }

    /** Todos os seguidores do autor, ativos ou nao. */
    public Set<Long> seguidoresDe(long autorId) {
        return new LinkedHashSet<>(seguidoresPorAutor.getOrDefault(autorId, Set.of()));
    }

    /** Contas seguidas pelo usuario. */
    public List<Long> seguidosPor(long usuarioId) {
        List<Long> seguidos = new ArrayList<>();
        seguidoresPorAutor.forEach((autor, seguidores) -> {
            if (seguidores.contains(usuarioId)) {
                seguidos.add(autor);
            }
        });
        return seguidos;
    }

    /** Publicacoes do autor, na ordem em que foram registradas. */
    public List<Long> postsDe(long autorId) {
        return List.copyOf(postsPorAutor.getOrDefault(autorId, List.of()));
    }
}
