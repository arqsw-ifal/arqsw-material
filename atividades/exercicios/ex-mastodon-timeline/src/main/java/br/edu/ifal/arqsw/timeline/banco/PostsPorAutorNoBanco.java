package br.edu.ifal.arqsw.timeline.banco;

import br.edu.ifal.arqsw.timeline.aplicacao.PostsPorAutor;

import java.util.Comparator;
import java.util.List;

/** Adaptador: implementa a porta PostsPorAutor consultando o banco. */
public class PostsPorAutorNoBanco implements PostsPorAutor {

    private final BancoDeDados banco;

    public PostsPorAutorNoBanco(BancoDeDados banco) {
        this.banco = banco;
    }

    @Override
    public List<Long> recentesDe(long autorId, int limite) {
        return banco.postsDe(autorId).stream()
                .sorted(Comparator.reverseOrder())
                .limit(limite)
                .toList();
    }
}
