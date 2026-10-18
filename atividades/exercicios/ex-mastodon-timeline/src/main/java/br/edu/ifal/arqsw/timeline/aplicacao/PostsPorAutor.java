package br.edu.ifal.arqsw.timeline.aplicacao;

import java.util.List;

/** Porta de saida para as publicacoes recentes de um autor (consulta ao armazenamento). */
public interface PostsPorAutor {

    /** Devolve ate {@code limite} IDs do autor, do mais recente para o mais antigo. */
    List<Long> recentesDe(long autorId, int limite);
}
