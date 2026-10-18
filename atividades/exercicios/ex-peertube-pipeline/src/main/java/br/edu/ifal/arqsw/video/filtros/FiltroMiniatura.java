package br.edu.ifal.arqsw.video.filtros;

import br.edu.ifal.arqsw.video.pipeline.Filtro;
import br.edu.ifal.arqsw.video.pipeline.Tarefa;

import java.util.Optional;

/**
 * Transformador: gera a miniatura (thumbnail) do video.
 *
 * CODIGO LEGADO: valida a tarefa de novo "por garantia", chamando outro filtro diretamente.
 */
public final class FiltroMiniatura implements Filtro {

    private final FiltroValidacao validacao = new FiltroValidacao();

    @Override
    public Optional<Tarefa> processar(Tarefa t) {
        return validacao.processar(t).map(v -> v.comArtefato(v.videoId() + "-miniatura.jpg"));
    }
}
